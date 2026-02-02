# Showcase Framework Architecture

## High-Level Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Pyjama Agent Showcases                        │
│                                                                   │
│  ┌─────────────────────┐         ┌─────────────────────┐        │
│  │ image-generator     │         │ movie-review        │        │
│  │ -agent              │         │ -agent              │        │
│  │                     │         │                     │        │
│  │ • Custom UI         │         │ • Custom UI         │        │
│  │ • Image logic       │         │ • Review logic      │        │
│  │ • Ollama API        │         │ • TMDB API          │        │
│  └─────────┬───────────┘         └─────────┬───────────┘        │
│            │                               │                     │
│            └───────────────┬───────────────┘                     │
│                            │                                     │
│              ┌─────────────▼──────────────┐                      │
│              │  showcase-framework        │                      │
│              │                            │                      │
│              │  Backend:                  │                      │
│              │  • Ring server setup       │                      │
│              │  • CORS middleware         │                      │
│              │  • Progress tracking       │                      │
│              │  • Async execution         │                      │
│              │  • Standard API routes     │                      │
│              │                            │                      │
│              │  Frontend:                 │                      │
│              │  • State management        │                      │
│              │  • API communication       │                      │
│              │  • Progress polling        │                      │
│              │  • Reusable components     │                      │
│              │  • App template            │                      │
│              └────────────────────────────┘                      │
└───────────────────────────────────────────────────────────────────┘
```

## Request Flow

```
    User                Frontend               Backend              Agent
     │                    │                     │                    │
     │                    │                     │                    │
     │  1. Enter prompt   │                     │                    │
     ├───────────────────▶│                     │                    │
     │                    │                     │                    │
     │  2. Click button   │                     │                    │
     ├───────────────────▶│                     │                    │
     │                    │                     │                    │
     │                    │  POST /api/execute  │                    │
     │                    ├────────────────────▶│                    │
     │                    │                     │                    │
     │                    │  {request-id: ...}  │                    │
     │                    │◀────────────────────┤                    │
     │                    │                     │                    │
     │                    │  Start polling      │                    │
     │                    │  (500ms interval)   │                    │
     │                    │                     │                    │
     │                    │  GET /api/progress  │  Execute agent     │
     │                    ├────────────────────▶├───────────────────▶│
     │                    │                     │                    │
     │                    │  {status: process}  │                    │
     │                    │◀────────────────────┤                    │
     │                    │                     │                    │
     │  Show progress     │                     │                    │
     │◀───────────────────┤                     │                    │
     │                    │                     │                    │
     │                    │  GET /api/progress  │                    │
     │                    ├────────────────────▶│                    │
     │                    │                     │                    │
     │                    │  {status: process}  │                    │
     │                    │◀────────────────────┤                    │
     │                    │                     │                    │
     │                    │  GET /api/progress  │    Result ready    │
     │                    ├────────────────────▶│◀───────────────────┤
     │                    │                     │                    │
     │                    │  {status: complete, │                    │
     │                    │   result: ...}      │                    │
     │                    │◀────────────────────┤                    │
     │                    │                     │                    │
     │  Display result    │  Stop polling       │                    │
     │◀───────────────────┤                     │                    │
     │                    │                     │                    │
```

## Code Organization

```
pyjama-agent-showcases/
│
├── showcase-framework/           # Common framework
│   ├── README.md                 # Framework documentation
│   ├── QUICK_START.md            # How to use guide
│   ├── deps.edn                  # Framework dependencies
│   └── src/
│       └── showcase/
│           ├── server.clj        # Backend framework
│           └── ui.cljs           # Frontend framework
│
├── image-generator-agent/        # Showcase 1
│   ├── deps.edn                  # References ../showcase-framework
│   ├── src/
│   │   └── image_generator/
│   │       ├── server.clj        # Uses showcase.server
│   │       └── core.cljs         # Uses showcase.ui
│   └── ...
│
├── movie-review-agent/           # Showcase 2
│   ├── deps.edn                  # References ../showcase-framework
│   ├── src/
│   │   └── movie_review/
│   │       ├── server.clj        # Uses showcase.server
│   │       └── core.cljs         # Uses showcase.ui
│   └── ...
│
└── REFACTORING_SUMMARY.md        # This refactoring effort
```

## Responsibilities

### Framework Layer (`showcase-framework/`)

**Backend (`showcase.server`):**
- HTTP server setup (Jetty)
- Middleware (JSON + CORS)
- Route handling for standard endpoints
- Progress store management
- Async execution wrapper
- Request/Response normalization

**Frontend (`showcase.ui`):**
- State atom creation
- HTTP client (AJAX)
- Progress polling logic
- Interval cleanup
- Reusable UI components
- Standard layout components

### Application Layer (each showcase)

**Backend:**
- Agent-specific execution logic
- Parameter validation
- Integration with Pyjama agents
- Domain-specific error handling

**Frontend:**
- Custom input forms
- Domain-specific result display
- Custom styling
- Specialized interactions

## Extension Points

### Backend

To create a new showcase backend:

1. Implement `execute-fn`:
   ```clojure
   (defn execute-my-agent [params progress-fn]
     {:success true :result "..."})
   ```

2. Call `start-server`:
   ```clojure
   (showcase/start-server 
     :execute-fn execute-my-agent
     :service-name "my-agent"
     :port 3000)
   ```

### Frontend

To create a new showcase frontend:

1. Create custom sections:
   ```clojure
   (defn my-input-section [state api-url] ...)
   (defn my-result-section [result state] ...)
   ```

2. Use the app template:
   ```clojure
   [ui/showcase-app
    {:title "..." :subtitle "..."
     :input-component my-input-section
     :result-component my-result-section}]
   ```

## Benefits

1. **Consistency**: All showcases use identical infrastructure
2. **Less Code**: 30-45% reduction in boilerplate
3. **Maintainability**: Framework fixes benefit all showcases
4. **Faster Development**: New showcases in hours, not days
5. **Clear Separation**: Framework vs. domain logic
