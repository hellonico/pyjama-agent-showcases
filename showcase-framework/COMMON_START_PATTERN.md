# Common Start Script Pattern

## Problem

The original start scripts for showcases referenced undefined aliases (`:server` or `:run`) which caused errors like:

```
WARNING: Specified aliases are undeclared and are not being used: [:server]
```

## Solution

Each showcase now defines a **`:server` alias** in its `deps.edn` that points to its specific server namespace. This provides a consistent interface across all showcases.

### Implementation

**In every showcase's `deps.edn`:**

```clojure
:aliases {:server {:main-opts ["-m" "namespace.server"]}}
```

**Examples:**

- `movie-review-agent/deps.edn`:
  ```clojure
  :aliases {:server {:main-opts ["-m" "movie-review.server"]}}
  ```

- `image-generator-agent/deps.edn`:
  ```clojure
  :aliases {:server {:main-opts ["-m" "image-generator.server"]}}
  ```

### Usage

From any showcase directory, you can now run:

```bash
clj -M:server
```

This will start the backend server on port 3000.

### Benefits

1. **Consistency**: Same command works for all showcases
2. **Simplicity**: No complex framework startup logic needed
3. **Transparency**: Each showcase explicitly declares its entry point
4. **Independence**: Each showcase is self-contained

### In start.sh Scripts

The start scripts now use this alias:

```bash
# Start backend server in background
echo "🚀 Starting backend server on port 3000..."
clj -M:server &
SERVER_PID=$!
```

## Alternative Considered: Framework Start Script

We initially created a `showcase.start` namespace in the framework to centralize startup logic. However, this added unnecessary complexity:

- Required extra aliases in both framework and showcases
- Made debugging harder (namespace resolution issues)
- Less transparent about what's being started

The simple alias approach is more idiomatic and easier to maintain.

## For Future Showcases

When creating a new showcase, add this to your `deps.edn`:

```clojure
:aliases {:server {:main-opts ["-m" "your-showcase.server"]}}
```

Where `your-showcase.server` is the namespace containing your `-main` function that calls `showcase/start-server`.
