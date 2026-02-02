# Quick Start: Creating a New Showcase

## 1. Setup

```bash
# Create your new showcase directory
mkdir my-agent-showcase
cd my-agent-showcase
```

Create `deps.edn`:
```clojure
{:paths ["src" "resources"]
 :deps {org.clojure/clojure {:mvn/version "1.12.1"}
        
        ;; Common showcase framework (provides Pyjama, Ring, Jetty, Reagent, AJAX, etc.)
        hellonico/showcase-framework {:local/root "../showcase-framework"}
        
        ;; Add any showcase-specific dependencies here
        ;; e.g., HTTP clients, async libraries, etc.
        }
 
 :aliases {:server {:main-opts ["-m" "my-agent.server"]}}}
```

## 2. Backend (server.clj)

```clojure
(ns my-agent.server
  (:require [showcase.server :as showcase]
            [pyjama.agent.core :as agent])
  (:gen-class))

(defn execute-my-agent
  "Your agent execution logic
  
  Receives: params map (from frontend POST body)
  Optional: progress-fn callback (fn [completed total] -> nil)
  Returns: {:success bool :result any :error string}"
  [params progress-fn]
  (try
    ;; Your agent logic here
    (let [result (agent/call {:id :my-agent
                              :input (:input params)})]
      ;; Return in standard format
      {:success true
       :result (:text result)})
    (catch Exception e
      {:success false
       :error (.getMessage e)})))

(defn -main [& _args]
  (showcase/start-server 
   :execute-fn execute-my-agent
   :service-name "my-agent"
   :port 3000))
```

## 3. Frontend (core.cljs)

```clojure
(ns my-agent.ui
  (:require [reagent.dom :as rdom]
            [showcase.ui :as ui]))

;; Define custom input section
(defn input-section [state api-url]
  [:div
   [ui/text-input state :input "Enter your input"]
   [ui/action-button state "Process"
    #(ui/execute-agent! state api-url {:input (:input @state)})
    {:icon "✨" :loading-label "Processing..."}]])

;; Define custom result display
(defn result-section [result state]
  [:div
   [:h2 "Result"]
   [:p result]
   [:button {:on-click #(swap! state assoc :result nil)} 
    "Clear"]])

;; Create the app
(def initial-state {:input ""})
(defonce app-state (ui/create-state initial-state))

(defn app []
  [ui/showcase-app
   {:title "My Agent"
    :subtitle "Description of what it does"
    :input-component input-section
    :result-component result-section
    :state app-state
    :api-url "http://localhost:3000"}])

;; Init
(defn init! []
  (rdom/render [app] (.getElementById js/document "app")))

(defn reload! []
  (init!))
```

## 4. Run

```bash
# Start backend
clj -M:server

# Start frontend (with shadow-cljs)
npx shadow-cljs watch app
```

**Note**: The `:server` alias provides a consistent way to start the backend across all showcases.

## Available UI Components

### Inputs
- `[ui/text-input state :key "placeholder" opts]`
  - opts: `:disabled?`, `:multiline?`, `:rows`
- `[ui/number-input state :key "label" opts]`
  - opts: `:min`, `:max`, `:step`, `:disabled?`
- `[ui/action-button state "label" on-click opts]`
  - opts: `:icon`, `:loading-label`, `:disabled?`

### Display
- `[ui/progress-bar state]` - Automatic from `:progress` key
- `[ui/error-display state opts]` - Automatic from `:error` key
  - opts: `:dismissible?`
- `[ui/loading-display state "message" opts]`
  - opts: `:steps` (vector of step descriptions)

### Layout
- `[ui/showcase-container "title" "subtitle" ...children]`
- `[ui/section :title "Title" :class "css-class" :children [...]]`

## State Structure

Your state atom will automatically have:

```clojure
{:loading? false      ; true during execution
 :progress nil        ; {:completed N :total M}
 :result nil          ; your agent's result
 :error nil           ; error message if failed
 :request-id nil      ; current request ID
 :poll-interval nil   ; cleanup handle
 
 ;; Your custom keys
 :input ""
 :other-field 123}
```

## Backend API

Your `execute-fn` receives:
1. `params` - map from frontend POST body
2. `progress-fn` - optional callback for progress updates

Must return:
```clojure
{:success true
 :result "your result"}

;; OR on error:
{:success false
 :error "error message"}
```

## Examples

See existing showcases:
- `image-generator-agent/` - Multi-step with progress
- `movie-review-agent/` - Simple text processing

Both show:
- Custom input layouts
- Specialized result displays
- Progress tracking
- Error handling
