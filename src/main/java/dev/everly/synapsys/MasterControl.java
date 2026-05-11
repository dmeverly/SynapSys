package dev.everly.synapsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@ConfigurationPropertiesScan
@Slf4j
public class MasterControl {

	public static void main(String[] args) {
		log.info(">>> SynapSys Starting <<<");
		SpringApplication.run(MasterControl.class, args);
		log.info(">>> SynapSys Online <<<");
	}
}