# 🎀 Anime Virtual Assistant 🎀

**Welcome to the Anime Virtual Assistant**, a desktop companion that watches your screen and provides commentary based on your chosen AI personality. An always-on-top character will keep you company, with expressions that change as she reacts to your on-screen activities.

This application is **highly customizable**, allowing you to switch between different characters, voices, and even the underlying AI models that power the assistant.

---

## ✨ Features

- **Always-On-Top Character**  
  A friendly (or not-so-friendly) anime character stays on your screen, brought to life with dynamic images.

- **Multiple Personalities**  
  Choose from a variety of personalities like *Barbie*, *Nerd*, or *Tsundere*. Each has a unique voice, prompt, and style of commentary.

- **Dynamic UI**  
  The settings window is styled with a custom **girly dark-mode** theme, complete with a background and title image.

- **AI Model Flexibility**  
  Switch between powerful external APIs (e.g., **Google Gemini**) or run entirely on **local models** for offline use and privacy.

- **Multimodal AI**  
  The assistant can understand your on-screen content by sending screenshots to a vision-capable AI model for **more accurate commentary**.

- **Extensible**  
  Easily add your own **custom characters**, personalities, and prompts!

---

## 🚀 How to Set Up

To get the assistant running on your machine, follow these steps.

### Prerequisites

- **Java:** Version 21 or newer
- **Python:** Version 3.12 -- DO NOT USE ANY OTHER VERSION!
- **Python Dependencies:** Required libraries (e.g., `Flask`, `Coqui TTS`) must be installed.

---

### Installation & Launch

#### 1. Clone the Repository

```bash
git clone https://github.com/LunaMoraes/Anime-Virtual-Assistant/
```

#### 2. Start the Backend API Server

Navigate into the cloned folder, then run:

```bash
# (First, ensure you’ve installed the required packages from requirements.txt)
py -3.12 start_api_coqui.py
```

> **Keep this terminal open.** It's required for speech and local AI support.

#### 3. Run the Assistant

Navigate to the project folder and double-click `VirtualAssistant.jar` to open the Settings window. Then press **"Start Assistant"** to summon the character!

---

## 🔧 Configuration (Optional)

### Enable External APIs

For higher-quality responses, configure external APIs (like **Google Gemini**). We recommend using Gemma 3 as it is  and multimodal, meaning it can handle both text and images:

1. Open: `data/system/system.json`
2. Add your API keys:

```json
{
  "analysis": {
    "key": "",
    "model_name": "",
    "url": ""
  },
  "vision": {
    "key": "",
    "model_name": "",
    "url": ""
  },
  "multimodal": {
    "key": "YOUR_GEMINI_API_KEY_HERE",
    "model_name": "gemma-3-27b-it",
    "url": "https://generativelanguage.googleapis.com/v1beta/models/gemma-3-27b-it:generateContent"
  }
}
```

3. Save the file.
4. In the **Settings window**, toggle between **"Local"** and **"API"** models.

---

## 🎨 Modding: Plugins are coming soon!

Creating your own character is easy!