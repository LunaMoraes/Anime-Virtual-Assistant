# 🎀 Anime Virtual Assistant 🎀

[![Java 21](https://img.shields.io/badge/Java-21-ff69b4?logo=openjdk&logoColor=white)](https://www.oracle.com/br/java/technologies/downloads/)
[![Python 3.12](https://img.shields.io/badge/Python-3.12-87cefa?logo=python&logoColor=white)](https://www.python.org/downloads/release/python-3120/)
[![Windows Supported](https://img.shields.io/badge/OS-Windows-9370db?logo=windows&logoColor=white)]()
[![License](https://img.shields.io/github/license/LunaMoraes/Anime-Virtual-Assistant?color=90ee90)](LICENSE)
[![Stars](https://img.shields.io/github/stars/LunaMoraes/Anime-Virtual-Assistant?style=social)](https://github.com/LunaMoraes/Anime-Virtual-Assistant/stargazers)

**Welcome to the Anime Virtual Assistant**, a desktop companion that watches your screen and provides commentary based on your chosen AI personality. An always-on-top anime character keeps you company, reacting dynamically to your on-screen activities.

---

## ✨ Features

- **🔢 Always-On-Top Character** – Dynamic, expressive anime character overlay.
- **👩‍💻 Multiple Personalities** – Barbie, Nerd, Tsundere, and more, each with unique prompts, voices, and styles.
- **🌟 Custom Girly Dark Mode UI** – A themed settings panel with background and title image.
- **🤖 AI Model Flexibility** – Choose between external APIs (Google Gemini) or local models for privacy.
- **🌍 Multimodal AI** – Sends screenshots to vision-capable models for accurate commentary.
- **🛠️ Extensible** – Easily add custom characters, personalities, and prompts.

---

## 🚀 Quick Start

### **Prerequisites**
[![Java](https://img.shields.io/badge/Java-21-ff69b4?logo=openjdk&logoColor=white)](https://www.oracle.com/br/java/technologies/downloads/)
[![Python](https://img.shields.io/badge/Python-3.12-87cefa?logo=python&logoColor=white)](https://www.python.org/downloads/release/python-3120/)
[![Coqui TTS](https://img.shields.io/badge/TTS-Coqui-9370db?logo=coqui&logoColor=white)](https://github.com/coqui-ai/TTS)
[![espeak-ng](https://img.shields.io/badge/Tool-espeak--ng-90ee90)](https://github.com/espeak-ng/espeak-ng/releases/tag/1.52.0)

- **Java:** Version 21
- **Python:** Version 3.12 (No other version supported)
- **Python Dependencies:** From `requirements.txt`
- **espeak-ng:** Required for Coqui TTS

---

### **Installation**

```bash
git clone https://github.com/LunaMoraes/Anime-Virtual-Assistant/
```

#### 1️⃣ Start Backend API Server

If you use CUDA for faster processing (requires NVIDIA GPU):
```bash
py -3.12 -m pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu128
```

Install dependencies:
```bash
py -3.12 -m pip install -r requirements.txt
```

Run API server:
```bash
py -3.12 start_api_coqui.py
```
> Keep this terminal open.

#### 2️⃣ Launch the Assistant

Double-click `VirtualAssistant.jar` → Press **"Start Assistant"**.

---

## 🔧 Optional: External API Setup

For better responses, configure **Google Gemini** in `data/system/system.json`:

```json
{
  "multimodal": {
    "key": "YOUR_GEMINI_API_KEY_HERE",
    "model_name": "gemma-3-27b-it",
    "url": "https://generativelanguage.googleapis.com/v1beta/models/gemma-3-27b-it:generateContent"
  }
}
```

Then toggle between **Local** and **API** models in Settings.

---

## 🎨 Modding Support

Custom characters are easy to make – **Plugins coming soon!**

---