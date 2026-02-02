# Dependency Management Improvement

## The Problem

Originally, each showcase was declaring all dependencies individually, leading to duplication:

### Before (image-generator-agent/deps.edn)
```clojure
{:deps {org.clojure/clojure {:mvn/version "1.12.1"}
        hellonico/pyjama {...}
        hellonico/showcase-framework {:local/root "../showcase-framework"}
        
        ;; These are duplicated in every showcase!
        ring/ring-core {:mvn/version "1.13.0"}
        ring/ring-jetty-adapter {:mvn/version "1.13.0"}
        ring/ring-json {:mvn/version "0.5.1"}
        ring-cors/ring-cors {:mvn/version "0.1.13"}
        
        ;; Showcase-specific
        org.clojure/core.async {:mvn/version "1.8.741"}}}
```

### Before (movie-review-agent/deps.edn)
```clojure
{:deps {org.clojure/clojure {:mvn/version "1.11.1"}
        pyjama/pyjama {...}
        hellonico/showcase-framework {:local/root "../showcase-framework"}
        
        ;; These are duplicated again!
        ring/ring-core {:mvn/version "1.10.0"}
        ring/ring-jetty-adapter {:mvn/version "1.10.0"}
        ring/ring-json {:mvn/version "0.5.1"}
        ring-cors/ring-cors {:mvn/version "0.1.13"}
        
        ;; Showcase-specific
        clj-http/clj-http {:mvn/version "3.12.3"}
        cheshire/cheshire {:mvn/version "5.11.0"}}}
```

**Issues:**
- Duplication across all showcases
- Version inconsistencies (note `ring-core` 1.13.0 vs 1.10.0)
- More maintenance burden
- Each showcase needs to know framework internals

## The Solution

The **showcase-framework** declares all common dependencies, and showcases only declare what's unique to them.

### Framework (showcase-framework/deps.edn)
```clojure
{:deps {org.clojure/clojure {:mvn/version "1.12.0"}
        
        ;; Backend dependencies
        ring/ring-core {:mvn/version "1.13.0"}
        ring/ring-jetty-adapter {:mvn/version "1.13.0"}
        ring/ring-json {:mvn/version "0.5.1"}
        ring-cors/ring-cors {:mvn/version "0.1.13"}
        
        ;; Frontend dependencies  
        reagent/reagent {:mvn/version "1.2.0"}
        cljs-ajax/cljs-ajax {:mvn/version "0.8.4"}}}
```

### After (image-generator-agent/deps.edn)
```clojure
{:deps {org.clojure/clojure {:mvn/version "1.12.1"}
        hellonico/pyjama {...}
        
        ;; Framework provides: Ring, Jetty, Reagent, AJAX, etc.
        hellonico/showcase-framework {:local/root "../showcase-framework"}
        
        ;; Only showcase-specific dependencies
        org.clojure/core.async {:mvn/version "1.8.741"}  ; for streaming
        ch.qos.logback/logback-classic {:mvn/version "1.5.12"}}}
```

### After (movie-review-agent/deps.edn)
```clojure
{:deps {org.clojure/clojure {:mvn/version "1.11.1"}
        pyjama/pyjama {...}
        
        ;; Framework provides: Ring, Jetty, Reagent, AJAX, etc.
        hellonico/showcase-framework {:local/root "../showcase-framework"}
        
        ;; Only showcase-specific dependencies
        clj-http/clj-http {:mvn/version "3.12.3"}  ; for TMDB API
        cheshire/cheshire {:mvn/version "5.11.0"}}}
```

## Benefits

1. **No Duplication**: Common deps declared once in framework
2. **Consistent Versions**: All showcases use same Ring/Reagent versions
3. **Simpler Showcase deps.edn**: Only 3-5 lines instead of 10-15
4. **Clear Intent**: Easy to see what's showcase-specific
5. **Easier Maintenance**: Update Ring version in one place
6. **Faster New Showcases**: Less to copy/paste/configure

## Creating a New Showcase

Now showcase authors only need to think about:

```clojure
{:deps {org.clojure/clojure {:mvn/version "1.12.1"}
        hellonico/pyjama {...}                              ; Required
        hellonico/showcase-framework {:local/root "..."}    ; Required
        
        ;; What's unique to MY showcase?
        my-special-lib/my-lib {...}}}                       ; Optional
```

## Dependency Flow

```
New Showcase
    │
    ├── Declares: Clojure, Pyjama, showcase-framework, my-special-lib
    │
    └──▶ showcase-framework (transitively provides)
            ├── Ring (server)
            ├── Jetty (HTTP)
            ├── Ring-JSON (serialization)
            ├── Ring-CORS (cross-origin)
            ├── Reagent (UI framework)
            └── cljs-ajax (HTTP client)
```

## Examples

### Showcase with no special deps
```clojure
{:deps {org.clojure/clojure {:mvn/version "1.12.1"}
        hellonico/pyjama {...}
        hellonico/showcase-framework {:local/root "../showcase-framework"}}}
```

### Showcase with async streaming
```clojure
{:deps {org.clojure/clojure {:mvn/version "1.12.1"}
        hellonico/pyjama {...}
        hellonico/showcase-framework {:local/root "../showcase-framework"}
        org.clojure/core.async {:mvn/version "1.8.741"}}}  ; For streaming
```

### Showcase with external API
```clojure
{:deps {org.clojure/clojure {:mvn/version "1.12.1"}
        hellonico/pyjama {...}
        hellonico/showcase-framework {:local/root "../showcase-framework"}
        clj-http/clj-http {:mvn/version "3.12.3"}}}  ; For API calls
```

## Important: Shadow-cljs vs deps.edn

There's a crucial split in dependency management:

### Backend Dependencies → `deps.edn`
These are **Clojure/JVM dependencies** that run on the server:
- ✅ Pulled transitively from `showcase-framework`
- ✅ Examples: Ring, Jetty, Ring-JSON, Ring-CORS
- ✅ Managed entirely by `deps.edn`

### Frontend Dependencies → `shadow-cljs.edn`
These are **ClojureScript dependencies** that compile to JavaScript:
- ⚠️ **Must be declared in `shadow-cljs.edn`**
- ⚠️ Cannot be pulled transitively from `deps.edn`
- ⚠️ Examples: Reagent, cljs-ajax

### Why the Split?

Shadow-cljs needs to know about ClojureScript libraries to compile them to JavaScript. While it reads `deps.edn` for Clojure dependencies, it needs ClojureScript dependencies explicitly declared in `shadow-cljs.edn`.

### Example: Complete Showcase

**deps.edn** (Backend):
```clojure
{:deps {org.clojure/clojure {:mvn/version "1.12.1"}
        hellonico/pyjama {...}
        hellonico/showcase-framework {:local/root "../showcase-framework"}
        ;; ↑ This brings Ring, Jetty, etc. transitively
        
        ;; Only YOUR backend-specific deps here:
        clj-http/clj-http {:mvn/version "3.12.3"}}}
```

**shadow-cljs.edn** (Frontend):
```clojure
{:source-paths ["src" "../showcase-framework/src"]
 :dependencies [[reagent "1.2.0"]      ; ClojureScript UI library
                [cljs-ajax "0.8.4"]]   ; ClojureScript HTTP client
 :builds {...}}
```

This way:
- Backend deps (Ring, Jetty) → No duplication ✅
- Frontend deps (Reagent, AJAX) → Declared where needed ✅

Much cleaner! 🎉
