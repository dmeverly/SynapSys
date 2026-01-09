package dev.everly.synapsys.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.everly.synapsys.service.guard.BrokerService;
import dev.everly.synapsys.service.llm.model.ApplicationMessage;
import dev.everly.synapsys.service.llm.model.InboundApplicationMessage;
import dev.everly.synapsys.service.llm.model.LlmResult;
import dev.everly.synapsys.service.llm.model.SynapsysResponse;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class WebController {

	private final BrokerService brokerService;

	public WebController(BrokerService brokerService) {
		this.brokerService = brokerService;
	}

	@PostMapping("/chat")
	public SynapsysResponse execute(@RequestBody InboundApplicationMessage inbound, Authentication authentication) {
		String authenticatedSender = (authentication != null) ? authentication.getName() : "anonymous";

		ApplicationMessage trustedApplicationMessage = new ApplicationMessage(authenticatedSender,
				inbound.getContent(), inbound.getContext());

		LlmResult result = brokerService.process(trustedApplicationMessage);

		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("status", "success");
		metadata.put("providerUsed", result.providerUsed());
		metadata.put("total_tokens", result.usage().totalTokens());
		metadata.put("prompt_tokens", result.usage().promptTokens());
		metadata.put("completion_tokens", result.usage().completionTokens());

		return new SynapsysResponse("synapsys", result.content(), metadata);
	}
}
