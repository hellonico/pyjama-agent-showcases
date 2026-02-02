(ns showcase.start
  "Common start script for all showcases.
   
   Usage from showcase root:
     clojure -M:framework/start showcase-ns.server
   
   This will start the server by requiring the namespace and calling -main.")

(defn start-server
  "Start a showcase server by namespace.
   
   Args:
     server-ns-str - String representation of the server namespace (e.g., 'movie-review-agent.server')"
  [server-ns-str]
  (println "🚀 Showcase Framework - Starting Server")
  (println "========================================")
  (println)
  (println "📦 Server namespace:" server-ns-str)

  (try
    ;; Require the server namespace
    (println "⏳ Loading namespace...")
    (require (symbol server-ns-str))

    ;; Get the -main function
    (let [main-fn (resolve (symbol server-ns-str "-main"))]
      (if main-fn
        (do
          (println "✅ Namespace loaded successfully")
          (println)
          (println "🎬 Starting server...")
          (println)
          (@main-fn))
        (do
          (println "❌ Error: No -main function found in" server-ns-str)
          (System/exit 1))))

    (catch Exception e
      (println "❌ Error loading namespace:" (.getMessage e))
      (println)
      (println "Stack trace:")
      (.printStackTrace e)
      (System/exit 1))))

(defn -main
  "Entry point for the start script.
   
   Expects a single argument: the server namespace to start."
  [& args]
  (if (empty? args)
    (do
      (println "❌ Error: No server namespace provided")
      (println)
      (println "Usage: clojure -M:framework/start <server-namespace>")
      (println "Example: clojure -M:framework/start movie-review-agent.server")
      (System/exit 1))
    (start-server (first args))))
