#!/bin/bash
# Quick start script for movie review agent

echo "🎬 Movie Review Agent - Quick Start"
echo "===================================="
echo ""

# Check if we're in the right directory
if [ ! -f "movie-review-agent.edn" ]; then
    echo "❌ Error: Run this script from the movie-review-agent directory"
    exit 1
fi

# Install npm dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "📦 Installing npm dependencies..."
    npm install
    echo ""
fi

# Start backend server in background
echo "🚀 Starting backend server on port 3000..."
clj -M:server &
SERVER_PID=$!
echo "   Backend PID: $SERVER_PID"
echo ""

# Cleanup function
cleanup() {
    echo ""
    echo "🛑 Stopping services..."
    kill $SERVER_PID 2>/dev/null
    echo "👋 Shutdown complete"
    exit 0
}

# Trap Ctrl+C and call cleanup
trap cleanup SIGINT SIGTERM

# Give server time to start
sleep 3

# Start shadow-cljs
echo "🎨 Starting ClojureScript frontend on port 8020..."
echo "   Open http://localhost:8020 in your browser"
echo ""
echo "Press Ctrl+C to stop all services"
echo ""

# Start shadow-cljs (this will block)
npx shadow-cljs watch app

# If shadow-cljs exits normally, cleanup
cleanup
