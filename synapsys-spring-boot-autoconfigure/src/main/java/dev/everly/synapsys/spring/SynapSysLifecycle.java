package dev.everly.synapsys.spring;

import dev.everly.synapsys.core.ConfigurationService;
import dev.everly.synapsys.core.ISynapSysComponent;
import dev.everly.synapsys.core.SynapSysManager;
import dev.everly.synapsys.secrets.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.List;
import java.util.Objects;

public final class SynapSysLifecycle implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(SynapSysLifecycle.class);

    private final SynapSysManager manager;
    private final SecretProvider secretProvider;
    private final List<ISynapSysComponent> components;
    private final SynapSysProperties props;

    private volatile boolean running = false;

    public SynapSysLifecycle(
            SynapSysManager manager,
            SecretProvider secretProvider,
            List<ISynapSysComponent> components,
            SynapSysProperties props
    ) {
        this.manager = Objects.requireNonNull(manager);
        this.secretProvider = Objects.requireNonNull(secretProvider);
        this.components = Objects.requireNonNull(components);
        this.props = Objects.requireNonNull(props);
    }

    @Override
    public void start() {
        if (!props.isEnabled()) {
            log.info("SynapSysLifecycle: synapsys.enabled=false; not starting SynapSys.");
            return;
        }
        if (running) return;

        try {
            manager.setGlobalSecretProvider(secretProvider);

            if (manager.getComponent(ConfigurationService.COMPONENT_ID) == null) {
                manager.registerComponent(new ConfigurationService());
            }

            for (ISynapSysComponent c : components) {
                if (c instanceof ConfigurationService) continue;
                if (manager.getComponent(c.getID()) != null) continue;
                manager.registerComponent(c);
            }

            manager.initializeAllComponents();
            running = true;
            log.info("SynapSysLifecycle: SynapSys started successfully.");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start SynapSys", e);
        }
    }

    @Override
    public void stop() {
        if (!running) return;
        try {
            manager.shutdownAllComponents();
        } catch (Exception e) {
            log.error("SynapSysLifecycle: shutdown failed", e);
        } finally {
            running = false;
        }
    }

    @Override public boolean isRunning() { return running; }

    @Override public int getPhase() { return 0; }
}
