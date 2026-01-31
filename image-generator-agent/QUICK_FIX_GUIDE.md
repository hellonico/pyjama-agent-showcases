# Quick Fix Guide

## Issues Fixed

### 1. Backend WebSocket Error
**Problem**: `ClassNotFoundException: org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter`

**Solution**: Updated to use Jetty 9 WebSocket API which is compatible with Ring Jetty adapter:
- Changed from Jetty 11 to Jetty 9 WebSocket dependencies
- Updated server.clj to use `WebSocketAdapter` and `WebSocketHandler`
- Uses `proxy` pattern for WebSocket implementation

### 2. Frontend React Missing
**Problem**: `The required namespace "react" is not available`

**Solution**: Added React dependencies to package.json:
```json
"dependencies": {
  "shadow-cljs": "^2.28.21",
  "react": "^18.2.0",
  "react-dom": "^18.2.0"
}
```

## How to Start

### Option 1: Clean Start (Recommended)

```bash
cd /Users/nico/cool/origami-nightweave/pyjama-agent-showcases/image-generator-agent

# Install React dependencies
npm install

# Start everything
./start.sh
```

### Option 2: Manual Start

**Terminal 1 - Backend:**
```bash
cd /Users/nico/cool/origami-nightweave/pyjama-agent-showcases/image-generator-agent
clj -M:run
```

**Terminal 2 - Frontend:**
```bash
cd /Users/nico/cool/origami-nightweave/pyjama-agent-showcases/image-generator-agent
npm install
npm run watch
```

## Verification

1. **Backend started**: Look for:
   ```
   🚀 Image Generator API server starting on port 3000...
   📡 HTTP API: http://localhost:3000/api/generate-image
   🔌 WebSocket: ws://localhost:3000/ws
   ```

2. **Frontend compiled**: Look for:
   ```
   [:app] Build completed. (XXX files, 0 compiled, 0 warnings, X.XXs)
   ```

3. **Browser**: Open http://localhost:8020

4. **WebSocket connected**: In browser console you should see:
   ```
   🔌 WebSocket connected
   ```

## Dependencies

### Backend (deps.edn)
- Jetty 9 WebSocket (`org.eclipse.jetty.websocket/websocket-server`)
- Ring, CORS, JSON
- Pyjama (local)

### Frontend (package.json)
- shadow-cljs
- react ^18.2.0
- react-dom ^18.2.0

### ClojureScript (shadow-cljs.edn)
- reagent
- cljs-ajax

## Testing WebSocket

Once everything is running:

1. Open browser DevTools (F12)
2. Go to Network tab → WS filter
3. You should see connection to `ws://localhost:3000/ws`
4. Generate an image
5. Watch progress messages flow in WebSocket frames

Messages format:
```javascript
{"type":"status","message":"Initializing..."}
{"type":"progress","completed":2,"total":9}
{"type":"complete","message":"Image generated!"}
```

## Common Issues

### "npm install" fails
- Make sure Node.js is installed: `node --version`
- Try: `rm -rf node_modules package-lock.json && npm install`

### Backend won't start
- Check Ollama is running: `curl http://localhost:11434/api/version`
- Check port 3000 is free: `lsof -i :3000`

### Frontend won't compile
- Clear shadow-cljs cache: `rm -rf .shadow-cljs`
- Restart: `npm run watch`

### WebSocket won't connect
- Check backend logs for WebSocket handler initialization
- Verify no CORS issues in browser console
- Try refreshing the browser page

## Success Indicators

✅ Backend: "WebSocket connected" in server logs  
✅ Frontend: No red errors in browser console  
✅ UI: Progress bar appears and updates during generation  
✅ Result: Image displays after generation completes  

Enjoy your AI image generator! 🎨
