package dev.everly.synapsys.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.everly.synapsys.dto.SynapSysRequest;
import dev.everly.synapsys.dto.SynapSysResponse;
import dev.everly.synapsys.providers.ProviderRouter;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@Slf4j
public class WebController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ProviderRouter providerRouter;

    public WebController(ProviderRouter providerRouter) {
        this.providerRouter = providerRouter;
    }

    @PostMapping("/generate")
    public ResponseEntity<SynapSysResponse> generate(
            @RequestHeader(name = "Authorization", required = false) String authHeader,
            @RequestBody SynapSysRequest request) {
        if (request.hasAdditionalProperties()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(SynapSysResponse.error("Request contains unknown properties"));
        }

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(SynapSysResponse.error("Missing or invalid Authorization header"));
        }

        String synapsysKey = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (!providerRouter.isValidSynapSysKey(synapsysKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(SynapSysResponse.error("Invalid SynapSys key"));
        }

        String providerKey = request.getApiKey();
        try {
            SynapSysResponse response = providerRouter
                    .getProviderAdapter(request.getProvider())
                    .generate(request.getModel(), request.getQuery(), providerKey);
            return ResponseEntity.ok(response);
        } finally {
            providerKey = null;
        }
    }
}
