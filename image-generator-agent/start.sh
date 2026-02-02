#!/bin/bash

# Start script for Image Generator Agent showcase

echo "🎨 Starting Image Generator Agent Showcase..."
echo ""

# Check if Ollama is running
if ! curl -s http://localhost:11434/api/version > /dev/null 2>&1; then
    echo "❌ Error: Ollama is not running on localhost:11434"
    echo "Please start Ollama first with: ollama serve"
    exit 1
fi

# Check if image model is installed
echo "Checking for x/z-image-turbo model..."
if ! ollama list | grep -q "x/z-image-turbo"; then
    echo "⚠️  Model x/z-image-turbo not found"
    echo "Downloading model (this may take a while)..."
    ollama pull x/z-image-turbo
fi

echo "✅ Ollama is ready"
echo ""

# Install npm dependencies if needed
if [ ! -d "node_modules" ] || [ ! -d "node_modules/react" ]; then
    echo "📦 Installing npm dependencies (including React)..."
    npm install
fi

# Start the backend server in the background
echo "🚀 Starting backend server on port 3000..."
clj -M:server &
BACKEND_PID=$!

# Give the backend time to start
sleep 3

# Start the ClojureScript watch and dev server
echo "🌐 Starting ClojureScript dev server on port 8020..."
npm run watch &
FRONTEND_PID=$!

echo ""
echo "✨ Image Generator is starting!"
echo ""
echo "📡 Backend API: http://localhost:3000"
echo "🌐 Frontend UI: http://localhost:8020"
echo "🔌 WebSocket: ws://localhost:3000/ws"
echo ""
echo "Press Ctrl+C to stop all services"
echo ""

# Wait for Ctrl+C
trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT
wait
