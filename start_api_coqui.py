import sys
from flask import Flask, request, jsonify, send_file
import torch
import io
import numpy as np
from TTS.api import TTS
import os
import json

# --- Configuration ---
def load_models_from_json(file_path):
    """Loads the model configuration from a JSON file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"FATAL: The voice list file was not found at '{file_path}'", file=sys.stderr)
        sys.exit(1)
    except json.JSONDecodeError:
        print(f"FATAL: The voice list file at '{file_path}' is not a valid JSON.", file=sys.stderr)
        sys.exit(1)

# Load available models from the external JSON file
VOICE_LIST_PATH = os.path.join("data", "voices", "voiceList.json")
AVAILABLE_MODELS = load_models_from_json(VOICE_LIST_PATH)

# Default model
DEFAULT_MODEL = "Default Girl Voice"

# The host and port for the API server
HOST = "127.0.0.1"
PORT = 5005
os.environ["COQUI_TOS_AGREED"] = "1"

# --- Flask App Initialization ---
app = Flask(__name__)

# --- TTS Model Loading ---
loaded_models = {}

def get_or_load_model(model_info):
    """Load a Coqui TTS model if not already loaded"""
    model_name = model_info["model"]

    if model_name not in loaded_models:
        print(f"Loading Coqui TTS model: {model_name}...")

        # Determine the device to use (GPU if available, otherwise CPU)
        device = "cuda" if torch.cuda.is_available() else "cpu"

        if device == "cuda":
            print(">>> CONFIRMATION: CUDA is available. Loading model on GPU.")
        else:
            print(">>> CONFIRMATION: CUDA not available. Loading model on CPU.")
        try:
            # Initialize TTS without the deprecated 'gpu' parameter
            tts = TTS(model_name=model_name, progress_bar=False)
            # Move the model to the determined device
            tts.to(device)

            loaded_models[model_name] = tts
            print(f"Model '{model_name}' loaded successfully.")
        except Exception as e:
            print(f"Error loading model {model_name}: {e}")
            return None

    return loaded_models[model_name]

# Load default model at startup
print("Loading default Coqui TTS model... This may take a moment.")
if DEFAULT_MODEL not in AVAILABLE_MODELS:
    print(f"FATAL: Default model '{DEFAULT_MODEL}' not found in the voice list.", file=sys.stderr)
    sys.exit(1)

default_model_info = AVAILABLE_MODELS[DEFAULT_MODEL]
default_model = get_or_load_model(default_model_info)
if not default_model:
    print(f"FATAL: Could not load default model.", file=sys.stderr)
    sys.exit(1)

@app.route('/characters', methods=['GET'])
def get_characters():
    """
    API endpoint to get available characters/speakers.
    Returns a JSON list of available speaker names.
    """
    return jsonify(list(AVAILABLE_MODELS.keys()))

@app.route('/synthesize', methods=['POST'])
def synthesize():
    """
    API endpoint to synthesize speech from text using Coqui TTS.
    Accepts a JSON payload with:
    - "text" (required): text to synthesize
    - "character" (optional): speaker to use (default: "jenny_female")
    - "speed" (optional): speaking rate multiplier (default: 1.0)
    Returns the generated audio as a WAV file.
    """

    if not request.json or 'text' not in request.json:
        return jsonify({"error": "Missing 'text' in request body"}), 400

    text = request.json['text']
    if not text:
        return jsonify({"error": "'text' field cannot be empty"}), 400

    # Get optional parameters
    character = request.json.get('character', DEFAULT_MODEL)
    speed = float(request.json.get('speed', 1.0))
    language = request.json.get('language', 'en')  # 👈 default to English if not given

    print(f"Received request to synthesize: '{text}' with character: '{character}', speed: {speed}")

    # Validate speaker
    if character not in AVAILABLE_MODELS:
        return jsonify({"error": f"Speaker '{character}' not available. Available speakers: {list(AVAILABLE_MODELS.keys())}"}), 400

    try:
        model_info = AVAILABLE_MODELS[character]
        tts_model = get_or_load_model(model_info)

        if not tts_model:
            return jsonify({"error": f"Failed to load model for speaker '{character}'"}), 500

        # Generate speech
        speaker_name = model_info.get("speaker")

        if speaker_name:
            # Multi-speaker model
            audio_data = tts_model.tts(text=text, speaker=speaker_name, language=language)
        else:
            # Single speaker model
            audio_data = tts_model.tts(text=text)

        # Convert to numpy array if needed
        if not isinstance(audio_data, np.ndarray):
            audio_data = np.array(audio_data)

        # Ensure it's float32
        audio_data = audio_data.astype(np.float32)

        # Normalize audio
        if np.max(np.abs(audio_data)) > 0:
            audio_data = audio_data / np.max(np.abs(audio_data))

        # Get sample rate from the model
        sample_rate = tts_model.synthesizer.output_sample_rate

        # Apply speed adjustment by resampling if needed
        if speed != 1.0:
            sample_rate = int(sample_rate * speed)

        # Use an in-memory buffer to hold the audio data
        buffer = io.BytesIO()

        # Save as WAV
        import soundfile as sf
        sf.write(
            buffer,
            audio_data,
            samplerate=sample_rate,
            format='WAV',
            subtype='PCM_16'
        )

        # Reset buffer's cursor to the beginning
        buffer.seek(0)

        print(f"Speech synthesized successfully with {character}. Audio shape: {audio_data.shape}, Sample rate: {sample_rate}")

        # Send the buffer's contents as a file
        return send_file(
            buffer,
            mimetype="audio/wav",
            as_attachment=False,
            download_name="speech.wav"
        )

    except Exception as e:
        print(f"Error during synthesis: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return jsonify({"error": f"Failed to generate speech: {str(e)}"}), 500

@app.route('/list_speakers', methods=['GET'])
def list_speakers():
    """
    API endpoint to list all available speakers for the VCTK model.
    Returns detailed information about each speaker including gender and accent.
    """
    try:
        # Load the VCTK model to get speaker information
        model_name = "tts_models/multilingual/multi-dataset/xtts_v2"
        if model_name not in loaded_models:
            # Determine the device to use (GPU if available, otherwise CPU)
            device = "cuda" if torch.cuda.is_available() else "cpu"
            # Initialize TTS without the deprecated 'gpu' parameter
            tts = TTS(model_name=model_name, progress_bar=False)
            # Move the model to the determined device
            tts.to(device)
            loaded_models[model_name] = tts
        else:
            tts = loaded_models[model_name]

        # Get available speakers from the model
        speakers = tts.speakers

        return jsonify({
            "model": model_name,
            "total_speakers": len(speakers),
            "speakers": speakers
        })

    except Exception as e:
        print(f"Error listing speakers: {e}")
        return jsonify({"error": f"Failed to list speakers: {str(e)}"}), 500

if __name__ == '__main__':
    print(f"Starting Coqui TTS API server at http://{HOST}:{PORT}")
    print(f"Available speakers: {list(AVAILABLE_MODELS.keys())}")
    app.run(host=HOST, port=PORT)
