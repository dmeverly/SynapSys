package dev.everly.synapsys.service.guard;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

import dev.everly.synapsys.service.context.SystemInstructionResolver;
import dev.everly.synapsys.service.llm.LlmProvider;
import dev.everly.synapsys.service.llm.model.ApplicationMessage;
import dev.everly.synapsys.service.llm.model.LlmResult;
import dev.everly.synapsys.service.llm.model.SynapsysRequest;
import dev.everly.synapsys.service.strategy.SenderStrategy;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BrokerService {

	private final List<PreFlightGuard> preFlightGuards;
	private final List<PostFlightGuard> postFlightGuards;
	private final Map<String, LlmProvider> llmProvidersById;
	private final List<SystemInstructionResolver> systemInstructionResolvers;
	private final List<SenderStrategy> senderStrategies;

	public BrokerService(List<PreFlightGuard> preFlightGuards, List<PostFlightGuard> postFlightGuards,
			List<LlmProvider> providerList, List<SystemInstructionResolver> systemInstructionResolvers,
			List<SenderStrategy> senderStrategies) {
		this.preFlightGuards = preFlightGuards;
		this.postFlightGuards = postFlightGuards;
		this.systemInstructionResolvers = systemInstructionResolvers;
		this.senderStrategies = senderStrategies;

		AnnotationAwareOrderComparator.sort(this.preFlightGuards);
		AnnotationAwareOrderComparator.sort(this.postFlightGuards);
		AnnotationAwareOrderComparator.sort(this.systemInstructionResolvers);
		AnnotationAwareOrderComparator.sort(this.senderStrategies);

		this.llmProvidersById = providerList.stream()
				.collect(Collectors.toMap(LlmProvider::getProviderId, Function.identity()));

		log.info("--- BROKER INITIALIZATION ---");
		log.info("Loaded {} Pre-Flight Guards", preFlightGuards.size());
		providerList.forEach(
				p -> log.info(">>> Registered Provider [{}] -> {}", p.getProviderId(), p.getClass().getSimpleName()));
	}

	public LlmResult process(ApplicationMessage inboundApplicationMessage) {
		SenderStrategy selected = senderStrategies.stream()
				.filter(s -> s.appliesTo(inboundApplicationMessage.getSender())).findFirst()
				.orElseThrow(() -> new IllegalStateException("No SenderStrategy found for sender"));

		return process(selected.complete(inboundApplicationMessage));
	}

	public LlmResult process(SynapsysRequest inboundSynapsysRequest) {

		if (!inboundSynapsysRequest.getSystemInstruction().isBlank()) {
			throw new SecurityException("Client may not set systemInstruction");
		}

		String resolvedSystemInstruction = resolveSystemInstruction(inboundSynapsysRequest.getSender(),
				inboundSynapsysRequest);

		SynapsysRequest effective = new SynapsysRequest(inboundSynapsysRequest.getSender(),
				inboundSynapsysRequest.getContent(), inboundSynapsysRequest.getContext(),
				inboundSynapsysRequest.getLlmProvider(), inboundSynapsysRequest.getModelVersion(),
				resolvedSystemInstruction);

		MDC.put("sender", effective.getSender());
		long t0 = System.currentTimeMillis();

		log.info(">>> TX_START | Sender: {} | Provider: {} | Model: {} | Content: \"{}\"", effective.getSender(),
				effective.getLlmProvider(),
				effective.getModelVersion().isBlank() ? "<blank>" : effective.getModelVersion(),
				truncateForLogs(effective.getContent()));

		try {
			runPreFlightGuards(effective);

			LlmProvider provider = llmProvidersById.get(effective.getLlmProvider());
			if (provider == null) {
				throw new IllegalArgumentException("Unknown Provider: " + effective.getLlmProvider());
			}

			LlmResult raw = provider.generate(effective);
			LlmResult safe = runPostFlightGuards(effective, raw);

			long ms = System.currentTimeMillis() - t0;
			log.info("<<< TX_SUCCESS | Time: {}ms | Tokens: {} (In:{} / Out:{})", ms, safe.usage().totalTokens(),
					safe.usage().promptTokens(), safe.usage().completionTokens());

			return safe;

		} finally {
			MDC.remove("sender");
		}
	}

	private void runPreFlightGuards(SynapsysRequest req) {
		for (PreFlightGuard guard : preFlightGuards) {
			if (guard.appliesTo(req.getSender())) {
				guard.inspect(req);
			}
		}
	}

	private LlmResult runPostFlightGuards(SynapsysRequest req, LlmResult raw) {
		String safeContent = raw.content();

		for (PostFlightGuard guard : postFlightGuards) {
			if (guard.appliesTo(req.getSender())) {
				safeContent = guard.sanitize(req, safeContent);
			}
		}

		if (!safeContent.equals(raw.content())) {
			log.info("--- SANITIZED | Content modified by Post-Flight Guard");
			return new LlmResult(safeContent, raw.usage(), raw.providerUsed());
		}

		return raw;
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

	private String truncateForLogs(String input) {
		return input.length() > 80 ? input.substring(0, 80) + "..." : input;
	}
}
