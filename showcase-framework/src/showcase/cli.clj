(ns showcase.cli
  "Common CLI interface for running showcase agents from the command line.
   
   Usage from showcase root:
     clojure -M:cli <agent-namespace> <args...>
   
   Examples:
     clojure -M:cli image-generator.agent 'a sunset over mountains'
     clojure -M:cli movie-review.agent 'Terminator'"
  (:require [pyjama.agent.core :as agent]
            [pyjama.core]
            [clojure.java.io :as io]))

(defn find-agent-config
  "Find the agent config file in the current directory"
  []
  (let [files ["agent.edn"
               "movie-review-agent.edn"
               "image-generator-agent.edn"]]
    (first (filter #(.exists (io/file %)) files))))

(defn run-agent
  "Run an agent with the given parameters
   
   Args:
     agent-id - keyword agent ID (e.g., :movie-review-agent)
     params - map of parameters to pass to the agent"
  [agent-id params]
  (let [config-file (find-agent-config)]
    (when-not config-file
      (println "❌ Error: No agent config file found in current directory")
      (println "   Looking for: agent.edn, movie-review-agent.edn, or image-generator-agent.edn")
      (System/exit 1))

    (println "📋 Using config file:" config-file)
    (System/setProperty "agents.edn" (.getAbsolutePath (io/file config-file)))

    ;; Force reload of agents
    (alter-var-root #'pyjama.core/agents-registry
                    (constantly (delay (pyjama.core/load-agents))))

    (println "🚀 Starting agent:" agent-id)
    (println "📝 Parameters:" params)
    (println)

    (try
      (let [result (agent/call (merge {:id agent-id} params))]
        (println)
        (println "✅ Agent completed successfully")
        (println)
        (println "═══════════════════════════════════════")
        (println "RESULT:")
        (println "═══════════════════════════════════════")
        (println)
        (if-let [text (:text result)]
          (println text)
          (do
            (println "Result keys:" (keys result))
            (println)
            (println result)))
        (println)
        (println "═══════════════════════════════════════")
        0)
      (catch Exception e
        (println)
        (println "❌ Error running agent:" (.getMessage e))
        (.printStackTrace e)
        1))))

(defn -main
  "CLI entry point
   
   Usage:
     clojure -M:cli <agent-type> <input>
   
   Where:
     agent-type = movie | image
     input = movie name or image prompt"
  [& args]
  (when (< (count args) 2)
    (println "Usage: clojure -M:cli <agent-type> <input>")
    (println)
    (println "Examples:")
    (println "  clojure -M:cli movie 'Terminator'")
    (println "  clojure -M:cli image 'a sunset over mountains'")
    (println)
    (println "Agent types:")
    (println "  movie - Movie review agent (requires movie-review-agent.edn)")
    (println "  image - Image generator agent (requires image-generator-agent.edn)")
    (System/exit 1))

  (let [agent-type (first args)
        input (second args)
        [agent-id params] (case agent-type
                            "movie" [:movie-review-agent {:movie-name input}]
                            "image" [:image-generator-agent {:prompt input}]
                            (do
                              (println "❌ Unknown agent type:" agent-type)
                              (println "   Valid types: movie, image")
                              (System/exit 1)))]

    (System/exit (run-agent agent-id params))))
