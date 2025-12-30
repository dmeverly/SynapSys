package dev.everly.synapsys.secrets;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EnvSecretProvider implements SecretProvider {

    private static final Logger log = LoggerFactory.getLogger(EnvSecretProvider.class);
    private static final String DOTENV_ENABLED_KEY = "SYNAPSYS_DOTENV_ENABLED";
    private static final String DOTENV_OVERRIDE_ENV_KEY = "SYNAPSYS_DOTENV_OVERRIDE_ENV";

    private final Dotenv dotenv;
    private final boolean dotenvEnabled;
    private final boolean dotenvOverridesEnv;

    public EnvSecretProvider() {
        this.dotenvEnabled = readBool(System.getenv(DOTENV_ENABLED_KEY), true);
        this.dotenvOverridesEnv = readBool(System.getenv(DOTENV_OVERRIDE_ENV_KEY), true);

        Dotenv tempDotenv = null;

        if (dotenvEnabled) {
            try {
                tempDotenv = Dotenv.load();
                log.info("EnvSecretProvider: .env loaded successfully (dotenvOverridesEnv={}).",
                        dotenvOverridesEnv);
            } catch (DotenvException e) {
                log.info("EnvSecretProvider: .env not found; using OS environment variables only.");
            }
        } else {
            log.info("EnvSecretProvider: dotenv disabled ({}=false); using OS environment variables only.",
                    DOTENV_ENABLED_KEY);
        }

        this.dotenv = tempDotenv;
    }

    @Override
    public String getSecret(String key) {
        if (!dotenvEnabled || dotenv == null) {
            return System.getenv(key);
        }

        if (dotenvOverridesEnv) {
            String fromDotenv = dotenv.get(key);
            return (fromDotenv != null) ? fromDotenv : System.getenv(key);
        } else {
            String fromEnv = System.getenv(key);
            return (fromEnv != null) ? fromEnv : dotenv.get(key);
        }
    }

    @Override
    public String getSecret(String key, String defaultValue) {
        String value = getSecret(key);
        return value != null ? value : defaultValue;
    }

    private static boolean readBool(String raw, boolean defaultValue) {
        if (raw == null) return defaultValue;
        String v = raw.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y");
    }
}
