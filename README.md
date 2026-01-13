<p align="center">
  <img src="src/main/resources/emblem-mono-light.png" width="84" alt="" />
</p>

# SynapSys Broker (v2) — Failure-Aware LLM Orchestration & Guard Framework

**Author**: David Everly
**Language**: Java (Spring Boot)
**Status**: Active development

SynapSys is a **broker** that sits between Internet-exposed applications and LLM providers.
It is designed for environments where **probabilistic model output must be constrained by deterministic system behavior**.

This repository contains the **public broker implementation**.
**Private guard logic and policies** are intentionally excluded and are expected to be loaded at runtime.

---

## Purpose

SynapSys exists to centralize and harden LLM usage in systems where:

* callers are untrusted
* output must be auditable and bounded
* failure modes must be predictable

It prioritizes **containment, policy enforcement, and observability** over flexible or open-ended generation.

---

## What This Is

SynapSys provides:

* **A single authenticated API** for upstream applications
* **Deterministic guard stages** around all LLM calls (pre- and post-execution)
* **Provider abstraction**, supporting real providers in production and stubbed providers in tests
* **Explicit trust boundaries** — caller identity is derived from auth headers, never request content
* **Failure-aware responses** with structured status and metadata

This is infrastructure for **controlled LLM exposure**, not a chatbot.

---

## What This Is Not

SynapSys does **not** guarantee:

* perfect resistance to adversarial input
* factual correctness of model output
* complete protection from prompt injection in isolation

It is a **control plane**, not a magic shield.
Security and safety emerge from **architecture + policy**, not models alone.

---

## High-Level Architecture

```
Internet
   │
   ▼
Edge Application (e.g., Everlybot)
   │   (authenticated, minimal payload)
   ▼
SynapSys Broker
   │
   ├─ Pre-LLM Guards
   ├─ Provider / Model Selection
   ├─ Prompt Assembly & Grounding
   ├─ LLM Execution
   └─ Post-LLM Guards
   │
   ▼
Controlled Response / Deterministic Fallback
```

**Key design choice:** SynapSys binds to localhost by default:

* `server.address=127.0.0.1`
* `server.port=8080`

It is intended to be reachable **only by co-located or internal services** you explicitly trust.

---

## API

### Health Endpoints

```
GET /health
GET /api/health
GET /actuator/health
```

Returns:

```json
{ "status": "UP" }
```

---

### Chat Endpoint

```
POST /api/v1/chat
```

**Authentication headers** (required outside `test` profile):

* `X-SynapSys-Key` — shared secret between caller and broker
* `X-SynapSys-Sender` — stable caller identity (used for policy and audit)

**Request body**

```json
{
  "content": "string",
  "context": { "any": "json" }
}
```

**Successful response**

```json
{
  "sender": "synapsys",
  "content": "string",
  "metadata": {
    "status": "success",
    "providerUsed": "...",
    "total_tokens": 0,
    "prompt_tokens": 0,
    "completion_tokens": 0
  }
}
```

Blocked or policy-violating requests return structured responses with:

```
metadata.status = "blocked"
```

Raw model output is never returned on violation.

---

## Execution Profiles

### `test`

* permissive security (no API key required)
* deterministic **stubbed provider**
* suitable for unit, integration, and abuse testing
* config: `application-test.properties`

### default / `prod`

* API-key authentication enforced for `/api/**`
* real provider enabled (Gemini is active in this codebase)
* config: `application.properties`

---

## Configuration & Secrets

Secrets are supplied via environment variables (recommended injection at runtime).

Key mappings:

* `synapsys.llm.default-model` ← `SYNAPSYS_DEFAULT_MODEL`
* `synapsys.llm.gemini-key` ← `GEMINI_API_KEY`
* `synapsys.llm.mistral-key` ← `MISTRAL_API_KEY`
* `synapsys.llm.nvd-api-key` ← `NVD_API_KEY`
* `synapsys.security.client-secret` ← `SYNAPSYS_CLIENT_SECRET`

No secrets are committed to this repository.

---

## Private Guard Pattern (Recommended)

Guard policies are expected to live **outside the public codebase**.

Recommended runtime pattern:

1. Build broker → `target/*-exec.jar`
2. Package private guards as a separate JAR
3. Load guards via Spring Boot loader path:

```
-Dloader.path=file:///path/to/private-guards.jar
```

This allows sensitive policy logic to remain private while reusing the public broker.

---

## Local Development

### Prerequisites

* Java 25
* Maven
* A local `local.mk` file (not committed)

Setup:

* copy `local.mk.template` → `local.mk`
* configure:

  * `SECRETS_DIR=/path/to/secrets`
  * `GUARDS_JAR=/path/to/private-guards.jar`

### Build

```
make package
```

### Run (default profile)

```
make run
```

### Run (test profile)

```
make test
```

---

## Security Notes

* **CORS is not authentication.**
  All real enforcement occurs via `X-SynapSys-Key`.

* Do **not** expose this broker directly to the public Internet without:

  * rate limiting
  * network ACLs
  * mTLS or equivalent transport controls

* Treat guard logic as sensitive infrastructure.

---

## License

Add a LICENSE file if this repository is intended for long-term public use.
