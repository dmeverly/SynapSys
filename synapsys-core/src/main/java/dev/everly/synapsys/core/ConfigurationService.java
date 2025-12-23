package dev.everly.synapsys.core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
public class ConfigurationService extends SynapSysComponentBase {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationService.class);
    public static final String COMPONENT_ID = "core-config-service";
    public static final String COMPONENT_NAME = "Core Configuration Service";
    dev.everly.synapsys.secrets.SecretProvider secretProvider;
    public ConfigurationService() {
        super(COMPONENT_ID, COMPONENT_NAME);
        this.secretProvider = null;
    }
    public ConfigurationService(String id, String name, dev.everly.synapsys.secrets.SecretProvider secretProvider) {
        super(id, name);
        this.secretProvider = Objects.requireNonNull(secretProvider, "dev.everly.synapsys.secrets.SecretProvider cannot be null.");
    }
    public void setSecretProvider(dev.everly.synapsys.secrets.SecretProvider secretProvider) {
        if (this.secretProvider != null) {
            log.warn("ConfigurationService: dev.everly.synapsys.secrets.SecretProvider is being overridden from {} to {}.",
                    this.secretProvider.getClass().getSimpleName(), secretProvider.getClass().getSimpleName());
        }
        this.secretProvider = Objects.requireNonNull(secretProvider, "dev.everly.synapsys.secrets.SecretProvider cannot be null.");
    }
    @Override
    public void initialize() throws Exception {
        log.info("Initializing ConfigurationService (ID: {})...", getID());
        if (this.secretProvider == null) {
            this.secretProvider = SynapSysManager.getInstance().getGlobalSecretProvider();
            log.info("ConfigurationService (ID: {}) using global dev.everly.synapsys.secrets.SecretProvider: {}", getID(), secretProvider.getClass().getSimpleName());
        } else {
            log.info("ConfigurationService (ID: {}) using injected dev.everly.synapsys.secrets.SecretProvider: {}", getID(), secretProvider.getClass().getSimpleName());
        }
    }
    @Override
    public void shutdown() throws Exception {
        log.info("Shutting down ConfigurationService (ID: {}).", getID());
        this.secretProvider = null;
    }
    public Optional<String> getProperty(String key) {
        Object paramValue = getParameter(key);
        if (paramValue instanceof String) {
            return Optional.of((String) paramValue);
        }
        if (secretProvider == null) {
            log.error("ConfigurationService (ID: {}): dev.everly.synapsys.secrets.SecretProvider is null. Configuration cannot be retrieved for key: {}", getID(), key);
            return Optional.empty();
        }
        return Optional.ofNullable(secretProvider.getSecret(key));
    }
    public Optional<String> getApiKey(String providerName) {
        String envKey = "SYNAPSYS_" + providerName.toUpperCase() + "_API_KEY";
        return getProperty(envKey);
    }
    public Map<String, String> getAllLoadedProperties() {
        log.warn("getAllLoadedProperties() in ConfigurationService now only returns " +
                "parameters explicitly set on this component instance, as direct environment " +
                "variable loading has been delegated to dev.everly.synapsys.secrets.SecretProvider.");
        return Collections.unmodifiableMap(parameters.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof String)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue())));
    }
}