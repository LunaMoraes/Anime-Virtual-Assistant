# Setup Guide

This guide is the working developer setup for Anime Virtual Assistant. It favors commands that can be run from a fresh clone and calls out which local services are required for each mode.

## Prerequisites

- Windows 11 or a recent Windows desktop environment.
- Java 21.
- Maven 3.9 or newer.
- Python 3.12 for the optional local TTS sidecar.
- Optional: NVIDIA CUDA-compatible GPU for faster Python/Torch workloads.
- Optional: Ollama for local language-model mode.
- Optional: Google Gemini API key for external API mode.

## Clone

```powershell
git clone https://github.com/LunaMoraes/Anime-Virtual-Assistant.git
cd Anime-Virtual-Assistant
```

If you are working on the stabilization project, use the development branch:

```powershell
git switch stabilization-refactor
```

## Configuration Files

`data/system/system.json` is intentionally ignored because it may contain API keys.

Create it from the example file:

```powershell
Copy-Item data/system/system.example.json data/system/system.json
```

For external API mode, fill the `analysis`, `vision`, and/or `multimodal` sections with your API key, model name, and endpoint URL. The current Gemini-style URL shape is:

```json
{
  "key": "YOUR_API_KEY",
  "model_name": "gemma-3-27b-it",
  "url": "https://generativelanguage.googleapis.com/v1beta/models/gemma-3-27b-it:generateContent"
}
```

`data/system/userSettings.json` is created automatically on first launch and is also ignored. Current defaults prefer API and multimodal mode, but the app falls back to local services if API config is missing.

## Build And Run The Java App

Build from source:

```powershell
mvn clean package
```

Run from the repository root so relative `data/` paths resolve:

```powershell
java -jar target/anime-virtual-assistant-0.1.0-SNAPSHOT.jar
```

The main window can open without starting the TTS sidecar. If TTS is enabled and the sidecar is missing, the app will show a connection warning; disable TTS in Settings if you want to run text-only while developing.

## Service Matrix

| Service | Required for | Port | Command | Notes |
| ------- | ------------ | ---- | ------- | ----- |
| Java Swing app | Main UI and assistant loop | none | `java -jar target/anime-virtual-assistant-0.1.0-SNAPSHOT.jar` | Run after `mvn clean package`. |
| Google Gemini-compatible API | External analysis, vision, or multimodal mode | remote HTTPS | none | Configure `data/system/system.json`; enable API modes in Settings. |
| Ollama | Local language-model mode | `11434` | `ollama serve` | Code currently calls `http://localhost:11434/api/generate` with model `qwen3:4b`. |
| Ollama model | Local language responses | `11434` | `ollama pull qwen3:4b` | Required before running local analysis mode. |
| Local vision service | Local image description mode | `5002` | TBD | Code calls `http://localhost:5002/describe`; no server implementation is currently committed. Use external multimodal/API mode until this is added. |
| Coqui TTS sidecar | Optional local speech synthesis | `5005` | `py -3.12 start_api_coqui.py` | Optional for now; required only when TTS is enabled. |

## External API Mode

This is currently the easiest full assistant path because it avoids the missing local vision service.

1. Copy `data/system/system.example.json` to `data/system/system.json`.
2. Fill `multimodal.key`, `multimodal.model_name`, and `multimodal.url`.
3. Optionally fill `analysis` and `vision` with the same API config.
4. Build and run the Java app.
5. In Settings, keep Multimodal enabled and choose API for the multimodal model.
6. Start the assistant from the UI.

## Local LLM Mode

Local language generation currently assumes Ollama.

Install and start Ollama, then pull the configured model:

```powershell
ollama pull qwen3:4b
ollama serve
```

In Settings, choose Local for analysis/multimodal where applicable.

Important: local multimodal fallback still needs image descriptions from the local vision service at `http://localhost:5002/describe`. That service is not present in this repo yet, so pure local vision/multimodal mode is incomplete until a vision server is added or the code is changed.

## Optional TTS Sidecar

TTS is not required to open the app or work on non-TTS features.

Install Python dependencies:

```powershell
py -3.12 -m pip install -r requirements.txt
```

For CUDA builds, install the matching Torch packages first, then install the remaining requirements:

```powershell
py -3.12 -m pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu128
py -3.12 -m pip install -r requirements.txt
```

Start the sidecar:

```powershell
py -3.12 start_api_coqui.py
```

Expected endpoints:

- `GET http://localhost:5005/characters`
- `POST http://localhost:5005/synthesize`
- `GET http://localhost:5005/list_speakers`

Keep the terminal open while using TTS.

## Common Failure Modes

### System Config Missing

Symptom: console logs `System config file not found`.

Fix: copy `data/system/system.example.json` to `data/system/system.json`. This is expected on a fresh clone because real keys stay local.

### Empty API Keys

Symptom: API mode is selected, but API config is unavailable or responses fall back to local services.

Fix: fill the relevant section in `data/system/system.json`, then restart the app or toggle the setting again.

### Ollama Missing

Symptom: local analysis requests fail or log connection errors for `localhost:11434`.

Fix:

```powershell
ollama pull qwen3:4b
ollama serve
```

### Local Vision Server Missing

Symptom: local vision or local multimodal fallback fails when calling `localhost:5002/describe`.

Fix: use external multimodal/API mode for now. The local vision server is currently a documented gap.

### TTS Missing

Symptom: startup warns that the TTS API server is unavailable.

Fix: either disable TTS in Settings or start `py -3.12 start_api_coqui.py` in a separate terminal.

### Maven Build Fails Downloading Dependencies

Symptom: Maven cannot resolve plugins or Gson from Maven Central.

Fix: check network access, proxy/VPN settings, or rerun after Maven has access to download dependencies into the local cache.
