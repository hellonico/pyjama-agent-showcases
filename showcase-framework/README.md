# Pyjama Agent Showcase Framework

A reusable framework for building full-stack agent showcases with ClojureScript + Ring backends.

## Architecture

The framework provides common infrastructure for agent showcases:

```
┌─────────────────────────────────────────────┐
│         ClojureScript Frontend              │
│  (Reagent + AJAX + Progress Management)     │
└─────────────────┬───────────────────────────┘
                  │ HTTP API
                  │ (JSON/CORS)
┌─────────────────┴───────────────────────────┐
│           Ring/Jetty Backend                │
│    (Request Handling + Agent Execution)     │
└─────────────────┬───────────────────────────┘
                  │
                  │ Pyjama Agent API
                  ▼
```

## Features

### Backend (`showcase.server`)
- **Standardized API endpoints**: `/api/execute`, `/api/progress/:id`, `/api/health`
- **Async execution with progress tracking**: request ID-based polling pattern
- **CORS middleware**: Pre-configured for local development
- **Agent integration**: Helpers for loading and executing Pyjama agents

### Frontend (`showcase.ui`)
- **State management**: Atom-based state with standard keys (`:loading?`, `:error`, `:result`, `:progress`)
- **UI components**: Reusable input, loading, error, and result sections
- **Progress polling**: Automatic HTTP polling with cleanup
- **API client**: AJAX helpers for backend communication

## Usage

### 1. Backend Setup

```clojure
(ns my-agent.server
  (:require [showcase.server :as showcase]))

;; Define your agent execution function
(defn execute-my-agent [params]
  {:success true
   :result "Agent output here"})

;; Create the server with your handler
(defn -main [& _args]
  (showcase/start-server 
    {:execute-handler execute-my-agent
     :service-name "my-agent"
     :port 3000}))
```

### 2. Frontend Setup

```clojure
(ns my-agent.ui
  (:require [showcase.ui :as ui]))

;; Define your custom UI sections
(defn my-input-section []
  [:div
   [:h1 "My Agent"]
   [ui/text-input "Enter your input"]])

(defn my-result-section [result]
  [:div.result
   [:p result]])

;; Create the app
(defn app []
  [ui/showcase-app
   {:title "My Agent Showcase"
    :input-component my-input-section
    :result-component my-result-section
    :api-endpoint "http://localhost:3000/api/execute"}])
```

## Standard API Contract

### POST /api/execute
**Request:**
```json
{
  "param1": "value1",
  "param2": "value2"
}
```

**Response (immediate):**
```json
{
  "success": true,
  "request-id": "uuid-here",
  "message": "Execution started"
}
```

### GET /api/progress/:request-id
**Response (polling):**
```json
{
  "status": "initializing|processing|complete|error",
  "progress": {"completed": 5, "total": 10},
  "result": "...",  // only when complete
  "error": "..."    // only when error
}
```

### GET /api/health
**Response:**
```json
{
  "status": "ok",
  "service": "agent-name"
}
```

## Examples

See the following showcases for reference implementations:
- `image-generator-agent`: Progress-based image generation
- `movie-review-agent`: Simple text-based agent execution
