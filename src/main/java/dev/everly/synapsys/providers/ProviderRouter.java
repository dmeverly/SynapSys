package dev.everly.synapsys.providers;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProviderRouter {

    private final Map<String, ProviderStrategy> providers;
    private final Set<String> validSynapSysKeys;

    public ProviderRouter(
            java.util.List<ProviderStrategy> providerAdapters,
            @Value("${SYNAPSYS_AUTH_KEYS:dev-synapsys-key}") String synapsysAuthKeys) {
        this.providers = providerAdapters.stream()
                .collect(Collectors.<ProviderStrategy, String, ProviderStrategy>toMap(
                        p -> p.getProvider().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (left, right) -> left));

        this.validSynapSysKeys = Arrays.stream(synapsysAuthKeys.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isValidSynapSysKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return validSynapSysKeys.contains(key.trim());
    }

    public ProviderStrategy getProviderAdapter(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        ProviderStrategy adapter = providers.get(normalized);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
        return adapter;
    }
}
