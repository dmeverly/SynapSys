package dev.everly.synapsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

import dev.everly.synapsys.config.LlmConfig;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@EnableConfigurationProperties(LlmConfig.class)
@ConfigurationPropertiesScan
@Slf4j
public class SynapsysApplication {

	public static void main(String[] args) {
		log.info(">>> SynapSys Starting <<<");
		SpringApplication.run(SynapsysApplication.class, args);
		log.info(">>> SynapSys Online <<<");
	}
}