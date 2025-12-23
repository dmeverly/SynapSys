package dev.everly.synapsys.spring;

import dev.everly.synapsys.secrets.SecretProvider;
import org.springframework.core.env.Environment;

import java.util.Objects;

public final class EnvironmentSecretProvider implements SecretProvider {
    private final Environment env;

    public EnvironmentSecretProvider(Environment env) {
        this.env = Objects.requireNonNull(env, "env");
    }

    @Override
    public String getSecret(String key) {
        return env.getProperty(key);
    }

    @Override
    public String getSecret(String key, String defaultValue) {
        String v = getSecret(key);
        return v != null ? v : defaultValue;
    }
}
