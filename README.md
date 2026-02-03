<p align="center">
  <img src="src/main/resources/emblem-mono-light.png" width="84" alt="" />
</p>

# SynapSys Broker (v2.1) — Failure-Aware LLM Orchestration & Guard Framework

**Author**: David Everly  
**Language**: Java (Spring Boot)  
**Status**: Active development  
Copyright © 2025 David Everly

SynapSys is a **broker** that sits between Internet-exposed applications and LLM providers.
It is designed for environments where **probabilistic model output must be constrained by deterministic system behavior
**.

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

## Security Model (v2.1)

SynapSys is designed as a **local control plane**, not a public API.

Key properties:

* **Local binding by default**
  The broker listens on `127.0.0.1` and is not Internet-exposed.

* **HMAC-based request authentication**
  Requests are signed using a shared secret, bound to:
    - HTTP method
    - request path
    - request body
    - timestamp
    - nonce

* **Replay protection**
  Timestamp windows and nonce caches prevent captured request reuse.

* **Explicit trust boundaries**
  Caller identity is derived from authenticated headers, never from request content.

SynapSys assumes an upstream edge (gateway, tunnel, or service) enforces network-level access control.

---

## Security Notes

* **SynapSys authenticates applications, not humans.**  Upstream applications are responsible for authenticating users.

* **Authentication is request-bound, not bearer-based.**
  All requests are HMAC-signed and bound to method, path, body, timestamp, and nonce.

* **Replay attacks are explicitly mitigated**
  via timestamp windows and nonce caches.

* **The broker binds to localhost by default**
  and is intended to sit behind a trusted edge service or tunnel.


* Do **not** expose this broker directly to the public Internet. Place it behind a trusted edge.

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
Edge Application (e.g., Everlybot and other supported applications)
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
{
  "status": "UP"
}
```

---

### Chat Endpoint

```
POST /api/v1/chat
```

**Authentication headers**:

* `X-SynapSys-Sender` — stable caller identity (policy + audit anchor)
* `X-SynapSys-Timestamp` — request epoch seconds (replay window enforcement)
* `X-SynapSys-Nonce` — per-request unique identifier
* `X-SynapSys-Signature` — HMAC-SHA256 over canonical request

Authentication uses **HMAC request signing** with bounded timestamps and nonce replay protection.  
No bearer secrets are transmitted over the wire.

**Request body**

```json
{
  "content": "string",
  "context": {
    "any": "json"
  }
}
```

**Successful response**

```json
{
  "sender": "string",
  "content": "string",
  "metadata": {
    "status": "string",
    "reason": "string"
  },
  "context": {},
  "timestamp": "string"
}
```

Blocked or policy-violating requests return structured responses with:

```
metadata.status = "blocked"
```

Raw model output is never returned on violation.

---

## Developer & Deployment Notes

### Execution Profiles

#### `test`

* permissive security (no API key required)
* deterministic **stubbed provider**
* suitable for unit, integration, and abuse testing
* config: `application-test.properties`

#### `prod`

* HMAC request authentication enforced for `/api/**`
* real LLM provider enabled
* config: `application.properties`

---

### Configuration & Secrets

Secrets are supplied via environment variables (recommended injection at runtime).

Key mappings:

* `synapsys.llm.default-model` ← `SYNAPSYS_DEFAULT_MODEL`
* `synapsys.llm.gemini-key` ← `GEMINI_API_KEY`
* `synapsys.llm.mistral-key` ← `MISTRAL_API_KEY`
* `synapsys.llm.nvd-api-key` ← `NVD_API_KEY`
* `synapsys.security.client-secret` ← `SYNAPSYS_CLIENT_SECRET`

No secrets are committed to this repository.

---

### Private Guard Pattern (Recommended)

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

### Local Development

#### Prerequisites

* Java 25
* Maven
* A local `local.mk` file (not committed)

Setup:

* copy `local.mk.template` → `local.mk`
* configure:

    * `SECRETS_DIR=/path/to/secrets`
    * `GUARDS_JAR=/path/to/private-guards.jar`

#### Build

```
make package
```

#### Run (default profile)

```
make run
```

#### Run (test profile)

```
make test
```

---

## License

This project is licensed under the **Apache License 2.0**.

You are free to use, modify, and distribute this software, including for
commercial purposes, subject to the terms of the license.

Private guard logic, policies, and runtime configurations are not included
in this repository and are not covered by this license.