# 🎨 AI Image Generator Agent

A Pyjama agent showcase that generates images from text prompts using Ollama's experimental image generation with Alibaba's Z-Image Turbo model.

## Features

- **Text-to-Image**: Generate images from natural language descriptions
- **Real-time Progress**: WebSocket-based progress tracking with visual progress bar
- **Customizable Dimensions**: Choose from presets (512×512, 1024×768, 1024×1024) or custom sizes
- **Live Preview**: See generated images instantly in the browser
- **Download**: Save generated images as PNG files
- **Modern UI**: Beautiful gradient design with smooth animations

## Prerequisites

1. **Ollama** must be installed and running:
   ```bash
   ollama serve
   ```

2. **Z-Image Turbo model** must be available:
   ```bash
   ollama pull x/z-image-turbo
   ```

3. **Node.js** and **npm** for ClojureScript compilation

4. **Clojure CLI tools** for running the backend

## Quick Start

### Option 1: Using the start script (easiest)

```bash
./start.sh
```

This will:
- Check if Ollama is running
- Install the image model if needed
- Start the backend server (port 3000)
- Start the frontend dev server (port 8020)
- Open your browser automatically

### Option 2: Manual start

**Terminal 1 - Backend:**
```bash
clj -M:run
```

**Terminal 2 - Frontend:**
```bash
npm install
npm run watch
```

**Browser:**
```
http://localhost:8020
```

## Architecture

### Backend (`src/image_generator/server.clj`)
- Ring HTTP server with WebSocket support
- Uses `pyjama.state/generate-image-stream` for progress tracking
- Broadcasts progress updates to connected WebSocket clients
- Returns base64-encoded PNG images

### Frontend (`src/image_generator/core.cljs`)
- Reagent-based single-page application
- WebSocket client for real-time progress updates
- Dimension controls with presets
- Image preview and download capability

### Agent Configuration (`image-generator-agent.edn`)
- Uses `pyjama.tools.image/generate-image` tool
- Handles errors and success states
- Configurable dimensions and prompts

## Usage

1. **Enter a Prompt**: Describe the image you want to generate
   - Example: "A serene sunset over mountains in the style of Monet"
   - Example: "A futuristic city with flying cars, cyberpunk style"

2. **Choose Dimensions**: Select a preset or enter custom width/height
   - 512×512 - Fast generation (~2-3 minutes)
   - 1024×768 - Standard quality (~5-7 minutes)
   - 1024×1024 - High quality (~7-10 minutes)

3. **Generate**: Click the "Generate Image" button

4. **Watch Progress**: See real-time progress as the image generates
   - Progress bar shows completion percentage
   - Step counter (e.g., "Step 6 of 9")

5. **Download**: Once complete, download the generated image

## API Endpoints

### HTTP API

**POST /api/generate-image**
```json
{
  "prompt": "A summer beach a la Matisse",
  "width": 512,
  "height": 512
}
```

Response:
```json
{
  "success": true,
  "image-data": "base64-encoded-png-data",
  "width": 512,
  "height": 512,
  "prompt": "..."
}
```

### WebSocket API

**WS /ws**

Connect to receive real-time progress updates:

```json
{"type": "progress", "completed": 5, "total": 9}
{"type": "status", "message": "Initializing..."}
{"type": "complete", "message": "Image generated!"}
{"type": "error", "message": "Error details"}
```

## Technology Stack

### Backend
- **Clojure** - Server-side logic
- **Ring** - HTTP server
- **Jetty** - WebSocket support
- **Pyjama** - Agent framework and image generation tools
- **core.async** - Asynchronous processing

### Frontend
- **ClojureScript** - Frontend logic
- **Reagent** - React wrapper for ClojureScript
- **shadow-cljs** - ClojureScript build tool
- **cljs-ajax** - HTTP requests

## Customization

### Change Default Model

Edit `src/image_generator/server.clj`:
```clojure
(let [ollama-state (atom {:url ...
                          :model "your-model-name"})])
```

### Adjust Default Dimensions

Edit `src/image_generator/core.cljs`:
```clojure
(def state (r/atom {:width 1024
                    :height 768
                    ...}))
```

### Modify Styles

Edit `public/style.css` to customize colors, fonts, and layout.

## Troubleshooting

### "Ollama is not running"
- Start Ollama: `ollama serve`
- Check if running: `curl http://localhost:11434/api/version`

### "Model not found"
- Install the model: `ollama pull x/z-image-turbo`
- Check installed models: `ollama list`

### "WebSocket connection failed"
- Ensure backend is running on port 3000
- Check for port conflicts

### "Image generation timeout"
- Large images (>1024×1024) may take 10+ minutes
- Check system resources (RAM/GPU)
- Try smaller dimensions first

### Development
- Backend logs: Check terminal running `clj -M:run`
- Frontend logs: Open browser console (F12)
- WebSocket messages: Check browser Network tab (WS filter)

## Performance Notes

Image generation time depends on:
- **Dimensions**: Larger images take exponentially longer
- **Hardware**: GPU VRAM (requires 12-16GB for Z-Image Turbo)
- **System load**: Other running applications affect speed

Recommended for best experience:
- Start with 512×512 for testing
- Use 1024×768 for balanced quality/speed
- Reserve 1024×1024+ for final outputs

## License

Part of the Pyjama agent showcases collection.

## Related

- [movie-review-agent](../movie-review-agent) - Movie analysis showcase
- [Pyjama Documentation](../../pyjama/README.md) - Core framework docs
- [Ollama Image Generation](https://ollama.com) - Ollama official docs
