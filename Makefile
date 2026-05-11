BROKER_JAR ?= target/synapsys-broker-2.1.0-exec.jar
JAVA ?= java
SPRING_ARGS ?=

.PHONY: help package run test clean

help:
	@echo "SynapSys Manager"
	@echo "---------------"
	@echo "BROKER_JAR: $(BROKER_JAR)"

package:
	@mvn package -DskipTests

run: package
	@echo ">>> Starting SynapSys..."
	@JAR_PATH="$$(ls target/*-exec.jar 2>/dev/null | head -n 1)"; \
	if [ -z "$$JAR_PATH" ]; then \
	  echo "ERROR: no executable broker jar found under target/"; \
	  exit 1; \
	fi; \
	$(JAVA) -jar "$$JAR_PATH" $(SPRING_ARGS)

test:
	@echo ">>> Starting SynapSys in TEST mode..."
	@$(MAKE) run SPRING_ARGS="--spring.profiles.active=test"

clean:
	@echo ">>> Cleaning build artifacts..."
	@rm -rf target