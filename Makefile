BROKER_JAR = target/synapsys-broker-2.1.0-exec.jar

-include local.mk

check-config:
ifndef SECRETS_DIR
	$(error "CRITICAL: local.mk is missing SECRETS_DIR")
endif
ifndef GUARDS_JAR
	$(error "CRITICAL: local.mk is missing GUARDS_JAR")
endif


.PHONY: all-prod package run clean help all-test prod

all-prod: package prod

all-test: package test

help:
	@echo "SynapSys v2 Manager"
	@echo "-------------------"
	@echo "Secrets Path: $(SECRETS_DIR)"
	@echo "Guards Path:  $(GUARDS_JAR)"

package:
	@mvn package -DskipTests

run: check-config
	@echo ">>> Loading secrets.env into environment..."
	@bash -lc 'export SECRETS_DIR="$(SECRETS_DIR)"; set -a; source "$(SECRETS_DIR)/secrets.env"; set +a; \
	  echo "GEMINI_API_KEY length: $${#GEMINI_API_KEY}"; \
	  if [ -z "$$GEMINI_API_KEY" ]; then echo "ERROR: GEMINI_API_KEY not set"; exit 1; fi; \
	  java -Dloader.path="file:///$(GUARDS_JAR)" \
	       -jar "$(BROKER_JAR)"'

prod:
	@echo ">>> Starting SynapSys (RUN)..."
	@$(MAKE) run SPRING_ARGS="--spring.profiles.active=prod"

test:
	@echo ">>> Starting SynapSys in TEST mode..."
	@bash -lc 'export SECRETS_DIR="$(SECRETS_DIR)"; SPRING_PROFILES_ACTIVE=test \
	  java -Dloader.path="file:///$(GUARDS_JAR)" \
	  -jar "$(BROKER_JAR)"'


clean:
	@echo ">>> Attempting to stop any running Java processes..."
	@-taskkill /F /IM java.exe 2>/dev/null || true
	@sleep 1
	@rm -rf target