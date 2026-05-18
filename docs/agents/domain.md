# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Layout

This is a single-context repository. There is one Java desktop assistant plus a Python TTS sidecar, not a monorepo split into independent bounded contexts.

## Before Exploring, Read These

- `CONTEXT.md` at the repo root, if it exists.
- `docs/adr/`, if it exists.
- This file for current repository orientation.

If any of these files do not exist, proceed silently. Do not suggest creating them upfront; use `/grill-with-docs` when terms or architectural decisions need to be captured.

## Current System Shape

- Java 21 Swing desktop app with source under `src/`.
- Entry point is `core.Main`.
- Runtime state currently flows through `core.AppState` static fields and `config.ConfigurationManager`.
- `core.AssistantCore` schedules `actions.ThinkingEngine` every 10 seconds.
- `ThinkingEngine` captures screenshots, builds action context, runs maintenance tasks, and routes model output commands.
- `actions.ScreenAnalysisAction` builds the personality/speak prompt and triggers model calls.
- `api.ApiClient` currently handles local Ollama, local vision service, and Gemini-style external vision/language/multimodal calls.
- `api.TtsApiClient` talks to the Python Flask/Coqui service at `http://localhost:5005`.
- `start_api_coqui.py` exposes `/characters`, `/synthesize`, and `/list_speakers`.
- Persistent app data lives under `data/`, including personalities, prompts, memory, levels, voice list, and UI images.

## Domain Vocabulary

- Assistant: the desktop companion shown by the Java Swing app.
- Personality: JSON-backed character profile with prompt, voice, and static/speaking images.
- Thinking cycle: the scheduled loop where the assistant captures context and decides what to do.
- Action: a modular task registered with `ActionManager`, such as screen analysis, memory maintenance, or levels updates.
- Bracket command: current model-output side effect syntax, such as `[speak:(...)]`, `[memory:...]`, and `[levels:...]`.
- TTS sidecar: the Python Flask/Coqui process used for local speech synthesis.
- Local model backend: currently Ollama for language generation and a separate local vision service for image descriptions.

## Known Refactor Direction

Linear currently tracks the main cleanup path:

- P0: reproducible Java build and setup documentation.
- P1: TTS language handling, async playback, timeouts, and backend abstraction.
- P2: typed action context, injected app context, focused model clients, and structured output instead of bracket parsing.
- P3: Swing settings-panel duplication cleanup.

## ADR Rules

If future work creates `docs/adr/`, read relevant ADRs before changing the affected area. If a proposed change contradicts an ADR, surface that conflict explicitly.
