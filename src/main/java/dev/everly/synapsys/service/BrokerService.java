package dev.everly.synapsys.service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

import dev.everly.synapsys.service.context.SystemInstructionResolver;
import dev.everly.synapsys.service.guard.GuardPhase;
import dev.everly.synapsys.service.guard.GuardViolationException;
import dev.everly.synapsys.service.guard.PostFlightGuard;
import dev.everly.synapsys.service.guard.PreFlightGuard;
import dev.everly.synapsys.service.llm.LlmProviderException;
import dev.everly.synapsys.service.llm.message.ApplicationMessage;
import dev.everly.synapsys.service.llm.message.LlmResponse;
import dev.everly.synapsys.service.llm.message.SynapsysRequest;
import dev.everly.synapsys.service.llm.message.SynapsysResponse;
import dev.everly.synapsys.service.llm.providers.LlmProvider;
import dev.everly.synapsys.service.strategy.SenderStrategy;
import dev.everly.synapsys.util.LogColor;
import dev.everly.synapsys.util.TextCanon;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BrokerService {

	private final List<PreFlightGuard> preFlightGuards;
	private final List<PostFlightGuard> postFlightGuards;
	private final Map<String, LlmProvider> llmProvidersById;
	private final List<SystemInstructionResolver> systemInstructionResolvers;
	private final List<SenderStrategy> senderStrategies;

	private final ExecutorService llmExec = Executors.newCachedThreadPool();
	private final Duration providerTimeout;

	public BrokerService(List<PreFlightGuard> preFlightGuards, List<PostFlightGuard> postFlightGuards,
			List<LlmProvider> providerList, List<SystemInstructionResolver> systemInstructionResolvers,
			List<SenderStrategy> senderStrategies,
			@Value("${synapsys.limits.providerTimeoutMs:20000}") long providerTimeoutMs) {
		this.preFlightGuards = preFlightGuards;
		this.postFlightGuards = postFlightGuards;
		this.systemInstructionResolvers = systemInstructionResolvers;
		this.senderStrategies = senderStrategies;
		this.providerTimeout = Duration.ofMillis(providerTimeoutMs);

		AnnotationAwareOrderComparator.sort(this.preFlightGuards);
		AnnotationAwareOrderComparator.sort(this.postFlightGuards);
		AnnotationAwareOrderComparator.sort(this.systemInstructionResolvers);
		AnnotationAwareOrderComparator.sort(this.senderStrategies);

		this.llmProvidersById = providerList.stream()
				.collect(Collectors.toMap(LlmProvider::getProviderId, Function.identity()));

		log.info("--- BROKER INITIALIZATION ---");
		String guards = "Loaded " + preFlightGuards.size() + " Pre-Flight Guards";
		if (preFlightGuards.isEmpty()) {
			log.warn(LogColor.live(guards));
		} else {
			log.warn(LogColor.test(guards));
		}
		guards = "Loaded " + postFlightGuards.size() + " Post-Flight Guards";
		if (postFlightGuards.isEmpty()) {
			log.warn(LogColor.live(guards));
		} else {
			log.warn(LogColor.test(guards));
		}
		providerList.forEach(
				p -> log.info(">>> Registered Provider [{}] -> {}", p.getProviderId(), p.getClass().getSimpleName()));
	}

	public SynapsysResponse executeRequestPipeline(ApplicationMessage applicationMessage) {
		return preProcess(applicationMessage);
	}

	public SynapsysResponse preProcess(ApplicationMessage applicationMessage) {
		SenderStrategy selected = senderStrategies.stream().filter(s -> s.appliesTo(applicationMessage.getSender()))
				.findFirst().orElseThrow(() -> new IllegalStateException("No SenderStrategy found for sender"));
		return process(selected.complete(applicationMessage));
	}

	public SynapsysResponse process(SynapsysRequest synapsysRequest) {

		if (!synapsysRequest.getSystemInstruction().isBlank()) {
			throw new GuardViolationException("INVALID_REQUEST", "Bad request.", "BrokerService",
					Map.of("category", "client_set_system_instruction"));
		}

		String resolvedSystemInstruction = resolveSystemInstruction(synapsysRequest.getSender(), synapsysRequest);
		String canonicalContent = TextCanon.normalize(synapsysRequest.getContent());

		SynapsysRequest finalSynapsysRequest = new SynapsysRequest(synapsysRequest.getSender(), canonicalContent,
				synapsysRequest.getContext(), synapsysRequest.getLlmProvider(), synapsysRequest.getModelVersion(),
				resolvedSystemInstruction);

		MDC.put("sender", finalSynapsysRequest.getSender());
		long startTime = System.currentTimeMillis();

		log.info(">>> TX_START | Sender: {} | Provider: {} | Model: {} | Content: \"{}\"",
				finalSynapsysRequest.getSender(), finalSynapsysRequest.getLlmProvider(),
				finalSynapsysRequest.getModelVersion().isBlank() ? "<blank>" : finalSynapsysRequest.getModelVersion(),
				truncateForLogs(finalSynapsysRequest.getContent()));

		try {
			runPreFlightGuards(finalSynapsysRequest);

			LlmProvider llmProvider = llmProvidersById.get(finalSynapsysRequest.getLlmProvider());

			LlmResponse llmResult = callWithTimeout(() -> llmProvider.generate(finalSynapsysRequest), providerTimeout,
					finalSynapsysRequest.getLlmProvider());

			LlmResponse clearedResult = runPostFlightGuards(finalSynapsysRequest, llmResult);

			long duration = System.currentTimeMillis() - startTime;
			log.info("<<< TX_SUCCESS | Time: {}ms | Tokens: {} (In:{} / Out:{})", duration,
					clearedResult.usage().totalTokens(), clearedResult.usage().promptTokens(),
					clearedResult.usage().completionTokens());

			return new SynapsysResponse("synapsys", clearedResult.content(), getMetadata(clearedResult));

		} catch (LlmProviderException llmProviderException) {
			return new SynapsysResponse("synapsys", llmProviderException.getNeutralMessage(),
					Map.of("status", "error", "reason", llmProviderException.getType().name().toLowerCase(),
							"retryable", llmProviderException.getType() == LlmProviderException.Type.RATE_LIMIT
									|| llmProviderException.getType() == LlmProviderException.Type.UNAVAILABLE));

		} finally {
			MDC.remove("sender");
		}
	}

	private void runPreFlightGuards(SynapsysRequest synapsysRequest) {
		for (PreFlightGuard guard : preFlightGuards) {
			if (guard.appliesTo(synapsysRequest.getSender(), GuardPhase.PREFLIGHT)) {
				guard.inspect(synapsysRequest);
			}
		}
	}

	private LlmResponse runPostFlightGuards(SynapsysRequest synapsysRequest, LlmResponse llmResult) {
		String safeContent = llmResult.content();

		for (PostFlightGuard guard : postFlightGuards) {
			if (guard.appliesTo(synapsysRequest.getSender(), GuardPhase.POSTFLIGHT)) {
				String preGuard = safeContent;
				safeContent = guard.sanitize(synapsysRequest, safeContent);
				if (!preGuard.equals(safeContent)) {
					log.warn("<<< TX_SANITIZED | guard={}", guard.getClass().getSimpleName());
				}
			}
		}

		if (!safeContent.equals(llmResult.content())) {
			return new LlmResponse(safeContent, llmResult.usage(), llmResult.providerUsed());
		}

		return llmResult;
	}

	private LlmResponse callWithTimeout(Callable<LlmResponse> work, Duration timeout, String providerId) {
		Future<LlmResponse> fut = llmExec.submit(work);
		try {
			return fut.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			fut.cancel(true);
			Map<String, Object> evidence = new LinkedHashMap<>();
			evidence.put("category", "provider_timeout");
			evidence.put("provider", providerId);
			evidence.put("timeoutMs", timeout.toMillis());
			throw new GuardViolationException("PROVIDER_TIMEOUT", "", "BrokerService", evidence);
		} catch (ExecutionException e) {
			Throwable c = e.getCause();
			if (c instanceof RuntimeException re) {
				throw re;
			}
			throw new RuntimeException(c);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new GuardViolationException("UNAVAILABLE", "Service unavailable.", "BrokerService",
					Map.of("category", "interrupted"));
		}
	}

	private String resolveSystemInstruction(String sender, SynapsysRequest request) {
		for (SystemInstructionResolver resolver : systemInstructionResolvers) {
			if (resolver.appliesTo(sender)) {
				String candidate = resolver.resolve(request);
				if (candidate != null && !candidate.isBlank()) {
					return candidate;
				}
			}
		}
		return "";
	}

	private Map<String, Object> getMetadata(LlmResponse result) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("status", "success");
		metadata.put("providerUsed", result.providerUsed());
		metadata.put("total_tokens", result.usage().totalTokens());
		metadata.put("prompt_tokens", result.usage().promptTokens());
		metadata.put("completion_tokens", result.usage().completionTokens());
		return metadata;
	}

	private String truncateForLogs(String input) {
		if (input == null) {
			return "";
		}
		return input.length() > 80 ? input.substring(0, 80) + "..." : input;
	}
}
