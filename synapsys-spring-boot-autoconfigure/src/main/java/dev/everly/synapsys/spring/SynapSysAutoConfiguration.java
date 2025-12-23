package dev.everly.synapsys.spring.autoconfigure;

import dev.everly.synapsys.core.ISynapSysComponent;
import dev.everly.synapsys.core.SynapSysManager;
import dev.everly.synapsys.secrets.SecretProvider;
import dev.everly.synapsys.spring.EnvironmentSecretProvider;
import dev.everly.synapsys.spring.SynapSysLifecycle;
import dev.everly.synapsys.spring.SynapSysProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(SynapSysProperties.class)
public class SynapSysAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SynapSysManager synapSysManager() {
        return SynapSysManager.getInstance();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecretProvider synapSysSecretProvider(Environment env) {
        return new EnvironmentSecretProvider(env);
    }

    @Bean
    @ConditionalOnMissingBean(name = "synapSysLifecycle")
    public SmartLifecycle synapSysLifecycle(
            SynapSysManager manager,
            SecretProvider secretProvider,
            List<ISynapSysComponent> components,
            SynapSysProperties props
    ) {
        return new SynapSysLifecycle(manager, secretProvider, components, props);
    }
}
