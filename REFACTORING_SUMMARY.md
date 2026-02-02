# Showcase Framework Refactoring

## Overview

This refactoring extracted common patterns from `image-generator-agent` and `movie-review-agent` into a reusable framework (`showcase-framework`), reducing code duplication and standardizing the architecture.

## What Changed

### Created: `showcase-framework/`

A new shared framework with two main components:

#### 1. Backend (`showcase.server.clj`)
- **Progress tracking**: In-memory request ID-based progress store
- **Async execution**: `run-agent-async` with progress callbacks
- **Standard API endpoints**:
  - `POST /api/execute` - Start agent execution
  - `GET /api/progress/:request-id` - Poll for progress
  - `GET /api/health` - Health check
- **Middleware**: Pre-configured CORS and JSON handling
- **Simple API**: Just implement an `execute-fn` and call `start-server`

#### 2. Frontend (`showcase.ui.cljs`)
- **State management**: `create-state` with standard keys
- **API client**: `execute-agent!` and `poll-progress!`
- **Reusable components**:
  - `text-input`, `number-input`, `action-button`
  - `progress-bar`, `error-display`, `loading-display`
  - `showcase-app` complete template
- **Automatic cleanup**: Interval management for polling

### Refactored: `image-generator-agent/`

**Before**: 194 lines server + 200 lines frontend = 394 lines
**After**: 105 lines server + 109 lines frontend = 214 lines
**Reduction**: 45% less code

#### Changes:
- `server.clj`: Now uses `showcase/start-server` with custom `execute-image-generator`
- `core.cljs`: Uses `showcase.ui` components and `showcase-app` template
- Focus shifted to **image-specific logic only**: dimension controls, presets, image display
- All boilerplate (routing, CORS, polling, state) moved to framework

### Refactored: `movie-review-agent/`

**Before**: 102 lines server + 114 lines frontend = 216 lines
**After**: 63 lines server + 88 lines frontend = 151 lines
**Reduction**: 30% less code

#### Changes:
- `server.clj`: Now uses `showcase/start-server` with custom `execute-movie-review-agent`
- `core.cljs`: Uses `showcase.ui` components and `showcase-app` template
- Focus shifted to **movie-specific logic only**: search input, markdown rendering
- All boilerplate (routing, CORS, polling, state) moved to framework

## Common Patterns Extracted

### 0. Access Web UI
- Handled by `showcase-app` template
- Automatic component layout and structure

### 1. Enter Prompt
- Provided by `text-input`, `number-input` components
- Custom input sections compose these primitives
- Built-in disabled state management

### 2. Call Agent
- Handled by `execute-agent!` with automatic:
  - Request ID management
  - Progress polling setup
  - State updates
- Server-side `run-agent-async` manages execution

### 3. Display Answer from Agent
- Standard `result-section` pattern
- Custom result components receive result + state
- Built-in error and loading displays

## Benefits

1. **Less Code**: 45% reduction in image-generator, 30% in movie-review
2. **Consistency**: Both showcases now follow identical patterns
3. **Maintainability**: Bug fixes in framework benefit all showcases
4. **Easier to Build New Showcases**: Just implement execute-fn and custom UI
5. **Separation of Concerns**: Framework handles infrastructure, showcases handle domain logic

## API Contract

All showcases now follow this standard:

```
POST /api/execute         → {request-id, message}
GET  /api/progress/:id    → {status, progress?, result?, error?}
GET  /api/health          → {status, service}
```

Frontend always has:
```clojure
{:loading? bool
 :progress {:completed N :total M}
 :result   any
 :error    string
 :request-id string}
```

## Next Steps

Future showcases can:
1. Add `showcase-framework` to deps.edn
2. Implement an `execute-fn` that returns `{:success bool :result any}`
3. Create custom input and result components
4. Call `showcase/start-server` and `showcase-app`

**Estimated effort**: 1-2 hours vs 4-6 hours from scratch
