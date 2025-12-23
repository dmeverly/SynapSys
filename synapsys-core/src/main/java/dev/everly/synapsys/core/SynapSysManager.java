package dev.everly.synapsys.core;
import dev.everly.synapsys.secrets.EnvSecretProvider;
import dev.everly.synapsys.secrets.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
public class SynapSysManager {
    private static final Logger log = LoggerFactory.getLogger(SynapSysManager.class);
    private static SynapSysManager instance;
    private final Map<String, ISynapSysComponent> components;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private SecretProvider globalSecretProvider;
    private SynapSysManager() {
        this.components = new LinkedHashMap<>();
        this.globalSecretProvider = new EnvSecretProvider();
    }
    public static synchronized SynapSysManager getInstance() {
        if (instance == null) {
            instance = new SynapSysManager();
        }
        return instance;
    }
    public synchronized SecretProvider getGlobalSecretProvider() {
        return this.globalSecretProvider;
    }
    public synchronized void setGlobalSecretProvider(SecretProvider provider) {
        if (initialized.get()) {
            throw new IllegalStateException("Cannot set global SecretProvider after SynapSysManager has been initialized.");
        }
        this.globalSecretProvider = Objects.requireNonNull(provider, "SecretProvider cannot be null.");
        log.info("SynapSysManager: Global SecretProvider set to: {}", provider.getClass().getSimpleName());
    }
    public synchronized void registerComponent(ISynapSysComponent component) {
        if (initialized.get()) {
            throw new IllegalStateException("Cannot register components after SynapSysManager has been initialized.");
        }
        if (component == null) {
            throw new IllegalArgumentException("Cannot register a null component.");
        }
        if (components.containsKey(component.getID())) {
            throw new IllegalArgumentException("Component with ID '" + component.getID() + "' already registered.");
        }
        if (component instanceof ConfigurationService) {
            ConfigurationService configService = (ConfigurationService) component;
            if (configService.secretProvider == null) {
                configService.setSecretProvider(this.globalSecretProvider);
                log.debug("SynapSysManager: Injected global SecretProvider into ConfigurationService (ID: {}).", component.getID());
            }
        }
        this.components.put(component.getID(), component);
        log.info("Registered component '{}' (ID: {})", component.getName(), component.getID());
    }
    public ISynapSysComponent getComponent(String id) {
        return components.get(id);
    }
    public <T extends ISynapSysComponent> T getComponent(String id, Class<T> type) {
        ISynapSysComponent component = components.get(id);
        if (type != null && type.isInstance(component)) {
            return type.cast(component);
        }
        return null;
    }
    public Map<String, ISynapSysComponent> getAllComponents() {
        return Collections.unmodifiableMap(components);
    }
    public synchronized void initializeAllComponents() throws Exception {
        if (initialized.compareAndSet(false, true)) {
            log.info("Initializing all components...");
            for (ISynapSysComponent component : components.values()) {
                log.info("  Initializing component: {} (ID: {})", component.getName(), component.getID());
                component.initialize();
            }
            log.info("All components initialized successfully.");
        } else {
            log.warn("Components already initialized.");
        }
    }
    public synchronized void shutdownAllComponents() throws Exception {
        if (initialized.compareAndSet(true, false)) {
            log.info("Shutting down all components...");
            List<ISynapSysComponent> componentsToShutdown = new ArrayList<>(components.values());
            Collections.reverse(componentsToShutdown);
            for (ISynapSysComponent component : componentsToShutdown) {
                log.info("  Shutting down component: {} (ID: {})", component.getName(), component.getID());
                component.shutdown();
            }
            components.clear();
            log.info("All components shut down successfully.");
        } else {
            log.warn("Components already shut down or not initialized.");
        }
    }
}