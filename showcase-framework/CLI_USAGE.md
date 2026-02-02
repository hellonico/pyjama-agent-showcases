# Showcase CLI Usage

All showcases now support command-line execution via a common CLI interface.

## Quick Start

### Movie Review Agent

From the `movie-review-agent` directory:

```bash
# Using the wrapper script (easiest)
./review.sh "Terminator"

# Or directly with the CLI alias
clj -M:cli movie "Terminator"
```

### Image Generator Agent

From the `image-generator-agent` directory:

```bash
# Using the wrapper script (easiest)
./generate.sh "a sunset over mountains"

# Or directly with the CLI alias
clj -M:cli image "a sunset over mountains"
```

## CLI Architecture

The CLI is implemented in the `showcase-framework` at `src/showcase/cli.clj`:

- **Unified entry point**: `showcase.cli` namespace
- **Automatic config detection**: Finds the agent config file (`.edn`) in the current directory
- **Formatted output**: Clean, readable results printed to stdout

## Adding CLI to Your Showcase

### 1. Add the `:cli` alias to your `deps.edn`

```clojure
:aliases {:server {:main-opts ["-m" "your-agent.server"]}
          :cli {:main-opts ["-m" "showcase.cli"]}}
```

### 2. Create a wrapper script (optional but recommended)

Create `run.sh`:

```bash
#!/bin/bash
if [ $# -eq 0 ]; then
    echo "Usage: ./run.sh \"your input\""
    exit 1
fi

clj -M:cli your-type "$1"
```

Make it executable:
```bash
chmod +x run.sh
```

### 3. Update the CLI to support your agent type

Edit `showcase-framework/src/showcase/cli.clj` and add your agent type to the case statement:

```clojure
(case agent-type
  "movie" [:movie-review-agent {:movie-name input}]
  "image" [:image-generator-agent {:prompt input}]
  "your-type" [:your-agent-id {:your-param input}]  ; Add this
  ...)
```

## CLI Output

The CLI provides:
- ✅ **Success indicator** when the agent completes
- 📋 **Formatted result** with clear visual separation
- ❌ **Error messages** with stack traces for debugging
- 🎨 **Progress indicators** for long-running operations

## Benefits

1. **Quick testing**: Test your agent without starting the web UI
2. **Automation**: Easy to integrate into scripts and workflows
3. **Debugging**: See full output and errors directly in terminal
4. **Consistency**: Same interface across all showcases

## Examples

### Movie Review

```bash
$ ./review.sh "Inception"
📋 Using config file: movie-review-agent.edn
🚀 Starting agent: :movie-review-agent
📝 Parameters: {:movie-name "Inception"}

✅ Agent completed successfully

═══════════════════════════════════════
RESULT:
═══════════════════════════════════════

**Summary**
Inception is a masterful blend of sci-fi and thriller that keeps you on the edge...
[... full review ...]
```

### Image Generation

```bash
$ ./generate.sh "a futuristic city"
📋 Using config file: image-generator-agent.edn
🚀 Starting agent: :image-generator-agent
📝 Parameters: {:prompt "a futuristic city"}

📊 Progress: 1/4
📊 Progress: 2/4
📊 Progress: 3/4
📊 Progress: 4/4

✅ Agent completed successfully
✅ Image saved to: generated-image-2026-02-02-105930.png
```
