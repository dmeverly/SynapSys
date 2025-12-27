<p style="text-align:center;">
  <img src="assets/emblem-mono-light.png" width="84" alt=""/>
</p>


# SynapSys — Failure-Aware LLM Orchestration Framework

**Author**: David Everly  
**Language**: Java (Spring Boot compatible)  
**Status**: Active development

---

## Overview

**SynapSys** is a modular orchestration framework for Large Language Model (LLM) systems, designed for environments where **probabilistic model output must be constrained by deterministic system behavior**.

Rather than acting as a thin wrapper around model APIs, SynapSys enforces a structured execution pipeline that explicitly separates:

- prompt construction
- model execution
- output parsing
- validation
- local repair

This design reflects real-world constraints encountered in healthcare and other safety-critical or regulated domains, where malformed or incorrect outputs cannot be blindly propagated downstream.

---

## Why This Exists

Most LLM integrations assume:
- well-formed outputs
- happy-path execution
- manual intervention on failure
- human review as a safety mechanism

SynapSys is built for scenarios where:
- outputs must conform to strict structural expectations
- failures are expected and handled programmatically
- systems must remain predictable even when models are not

The framework treats **invalid or malformed model output as a normal system state**, not an exception.

---

## Core Design Principles

- **Explicit failure boundaries**  
  Each pipeline stage declares how failure is detected and handled.

- **Separation of concerns**  
  Prompting, parsing, validation, and repair are independent, replaceable components.

- **Controlled interfaces around probabilistic systems**  
  LLMs are treated as unreliable subsystems behind deterministic abstractions.

- **Enterprise-oriented integration**  
  Implemented in Java with Spring Boot auto-configuration support.

---

## Architecture Overview

SynapSys is structured as a multi-module Maven project:

### `synapsys-core`
The orchestration engine, including:
- `PipelineAgent` — coordinates execution flow
- `Parser` — extracts structured data from raw model output
- `Validator` — enforces correctness constraints
- `Repair` — attempts local correction of invalid output
- Guard utilities (e.g., JSON shaping and enforcement helpers)

### `synapsys-spring-boot-autoconfigure`
- Auto-wires agents and clients
- Binds configuration via properties
- Manages lifecycle within a Spring context

### `synapsys-spring-boot-starter`
- Convenience dependency for production use

---

## Execution Flow (Simplified)

1. Prompt generation
2. LLM invocation
3. Parsing
4. Validation
5. Optional local repair with bounded retry attempts
6. Post-processing and return

Repair operates on **model output**, not by re-prompting the model.  
All retries are bounded and explicit.

---

## Supported LLM Providers

SynapSys includes interchangeable client implementations for:
- OpenAI
- Google Gemini
- Mistral

Clients share a common interface and are selected via configuration, allowing applications to switch providers without modifying pipeline logic.

---

## Configuration & Secrets

Authentication is handled via a `SecretProvider` abstraction (e.g., environment variables or `.env` files).  
This avoids hard-coded credentials and supports containerized deployment.

---

## Relationship to Portfolio Chatbot

SynapSys is actively used as the orchestration layer for the **Portfolio Chatbot**, a deployed reference application that demonstrates:

- structured prompting
- validation and repair paths
- controlled exposure of model output
- explicit trust boundaries around LLM calls

---

## Scope & Non-Goals

SynapSys does **not** claim to:
- provide security guarantees
- prevent all adversarial input
- enforce domain correctness

It provides **infrastructure** for building systems that require control and inspection around LLM behavior.

---

## Disclaimer

This project was developed independently on personal time and is not affiliated with any employer.
