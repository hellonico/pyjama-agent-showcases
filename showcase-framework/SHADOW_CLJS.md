# Shadow-cljs Configuration for Showcases

When creating a new showcase that uses the framework, your `shadow-cljs.edn` needs to include the framework's source path.

## Minimal Configuration

```clojure
{:source-paths ["src" "../showcase-framework/src"]  ; Include framework's ClojureScript source
 :dependencies [[reagent "1.2.0"]      ; ClojureScript deps needed by shadow-cljs
                [cljs-ajax "0.8.4"]]   ; Backend deps (Ring, Jetty) come from deps.edn
 :builds {:app {:target :browser
                :output-dir "public/js"
                :asset-path "/js"
                :modules {:main {:init-fn my-showcase.core/init!}}
                :devtools {:http-root "public"
                           :http-port 8020
                           :after-load my-showcase.core/reload!}}}}
```

## Important: Dependency Split

**ClojureScript dependencies (Reagent, AJAX) must be in `shadow-cljs.edn`**
- Shadow-cljs needs these for ClojureScript compilation
- They cannot be pulled transitively from `deps.edn`

**Backend dependencies (Ring, Jetty, etc.) come from `deps.edn`**
- These are JVM/Clojure dependencies
- Pull transitively from `showcase-framework`

### Correct Split

`shadow-cljs.edn`:
```clojure
{:dependencies [[reagent "1.2.0"]      ; ✅ ClojureScript library
                [cljs-ajax "0.8.4"]]}  ; ✅ ClojureScript library
```

`deps.edn`:
```clojure
{:deps {hellonico/showcase-framework {...}}}  ; ✅ Pulls Ring, Jetty transitively
```

## Why This is Needed

1. **ClojureScript Source Discovery**: Shadow-cljs needs explicit source paths to find `.cljs` files
2. **Framework Namespace Access**: The `showcase.ui` namespace lives in `../showcase-framework/src/`
3. **Dependency Management**: All library deps (Reagent, AJAX) come from `deps.edn`, not `shadow-cljs.edn`

## Common Mistake

❌ **Wrong** - Missing framework source path:
```clojure
{:source-paths ["src"]  ; Won't find showcase.ui!
 ...}
```

✅ **Correct** - Includes framework source path:
```clojure
{:source-paths ["src" "../showcase-framework/src"]  ; Can find showcase.ui
 ...}
```

## Complete Example

For a showcase with custom configuration:

```clojure
{:source-paths ["src" "../showcase-framework/src"]
 :dependencies []
 :dev-http {8020 "public"}  ; Optional: serve static files
 :builds
 {:app
  {:target :browser
   :output-dir "public/js"
   :asset-path "/js"
   :modules {:main {:init-fn my-showcase.core/init!}}
   :devtools {:http-root "public"
              :http-port 8020
              :after-load my-showcase.core/reload!}}}
 :nrepl {:port 9000}}  ; Optional: for REPL connection
```

## Checklist for New Showcase

- [ ] Add `"../showcase-framework/src"` to `:source-paths`
- [ ] Set `:dependencies []` (deps come from deps.edn)
- [ ] Configure your `:init-fn` and `:after-load`
- [ ] Set `:http-port` (usually 8020)
- [ ] Verify compilation works: `npx shadow-cljs watch app`

## Troubleshooting

### Error: "The required namespace showcase.ui is not available"

**Cause**: Missing framework source path

**Fix**: Add `"../showcase-framework/src"` to `:source-paths`

### Error: Duplicate dependency versions

**Cause**: Dependencies declared in both `shadow-cljs.edn` and `deps.edn`

**Fix**: Remove dependencies from `shadow-cljs.edn`, let `deps.edn` handle them
