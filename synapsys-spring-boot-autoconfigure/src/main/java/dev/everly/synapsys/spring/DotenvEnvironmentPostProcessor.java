package dev.everly.synapsys.spring;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

    private static final String DOTENV_ENABLED_KEY = "synapsys.dotenv.enabled";
    private static final String DOTENV_OVERRIDE_ENV_KEY = "synapsys.dotenv.override-env";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = environment.getProperty(DOTENV_ENABLED_KEY, Boolean.class, true);
        if (!enabled) {
            log.info("SynapSys dotenv: disabled ({}=false).", DOTENV_ENABLED_KEY);
            return;
        }

        boolean overrideEnv = environment.getProperty(DOTENV_OVERRIDE_ENV_KEY, Boolean.class, true);

        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

            Map<String, Object> map = new LinkedHashMap<>();
            dotenv.entries().forEach(e -> map.put(e.getKey(), e.getValue()));

            MapPropertySource ps = new MapPropertySource("synapsysDotenv", map);

            if (overrideEnv) {
                environment.getPropertySources().addFirst(ps);
            } else {
                environment.getPropertySources().addLast(ps);
            }

            log.info("SynapSys dotenv: loaded .env into Spring Environment (overrideEnv={}).", overrideEnv);
        } catch (DotenvException ex) {
            log.info("SynapSys dotenv: .env not loaded (missing or unreadable). Using existing env/config.");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
