package dev.everly.synapsys.secrets;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class EnvSecretProvider implements SecretProvider {
    private static final Logger log = LoggerFactory.getLogger(EnvSecretProvider.class);
    private final Dotenv dotenv;
    public EnvSecretProvider() {
        Dotenv tempDotenv = null;
        try {
            tempDotenv = Dotenv.load();
            log.info("env file loaded successfully.");
        } catch (DotenvException e) {
            log.warn("env file not found. " +
                    "Falling back to system environment variables only. " +
                    "If you're developing locally, consider creating a .env file in your project root.");
        }
        this.dotenv = tempDotenv;
    }
    @Override
    public String getSecret(String key) {
        String value = System.getenv(key);
        if (value == null && dotenv != null) {
            value = dotenv.get(key);
        }
        return value;
    }
    @Override
    public String getSecret(String key, String defaultValue) {
        String value = getSecret(key);
        return value != null ? value : defaultValue;
    }
}