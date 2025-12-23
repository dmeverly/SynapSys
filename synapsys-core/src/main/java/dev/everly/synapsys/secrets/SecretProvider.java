package dev.everly.synapsys.secrets;

public interface SecretProvider {
    String getSecret(String key);
    String getSecret(String key, String defaultValue);
}