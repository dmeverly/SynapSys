package dev.everly.synapsys.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.everly.synapsys.service.BrokerService;
import dev.everly.synapsys.service.llm.message.ApplicationMessage;
import dev.everly.synapsys.service.llm.message.InboundApplicationMessage;
import dev.everly.synapsys.service.llm.message.SynapsysResponse;
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
	public SynapsysResponse execute(@RequestBody InboundApplicationMessage inboundApplicationMessage,
			Authentication authentication) {
		String authenticatedSender = authenticateSender(authentication);

		ApplicationMessage applicationMessage = new ApplicationMessage(authenticatedSender,
				inboundApplicationMessage.getContent(), inboundApplicationMessage.getContext());

		return brokerService.executeRequestPipeline(applicationMessage);
	}

	private String authenticateSender(Authentication authentication) {
		if (authentication == null) {
			return "anonymous";
		}
		return authentication.getName();
	}
}
