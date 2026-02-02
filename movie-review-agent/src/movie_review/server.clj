(ns movie-review.server
  "Ring server for movie review agent API - refactored to use showcase framework"
  (:require [showcase.server :as showcase]
            [pyjama.agent.core :as agent]
            [pyjama.core]
            [clojure.java.io :as io])
  (:gen-class))

;; ============================================================================
;; Movie Review Agent Execution
;; ============================================================================

(defn execute-movie-review-agent
  "Execute the movie review agent with parameters
  
  Takes opts map with:
    :params - {:movie-name string}
    :progress-fn - (fn [completed total] -> nil) [not currently used]
  Returns {:success bool :result any :error string}"
  [{:keys [params _progress-fn]}]
  (try
    (let [{:keys [movie-name]} params]
      (println "🎬 Starting movie analysis for:" movie-name)

      ;; Set the agent config file path as a system property so pyjama can find it
      (System/setProperty "agents.edn"
                          (.getAbsolutePath (io/file "movie-review-agent.edn")))

      ;; Force reload of agents by creating a new delay
      ;; This is a bit hacky but works for runtime agent loading
      (alter-var-root #'pyjama.core/agents-registry
                      (constantly (delay (pyjama.core/load-agents))))

      ;; Execute the agent
      (let [result (agent/call {:id :movie-review-agent
                                :movie-name movie-name})]
        (println "✅ Agent execution completed")
        (println "📊 Result keys:" (keys result))
        {:success true
         :result (or (:text result)      ; LLM returns :text
                     (:message result)   ; fallback
                     (pr-str result))}))
    (catch Exception e
      (println "❌ Error executing agent:" (.getMessage e))
      (.printStackTrace e)
      {:success false
       :error (.getMessage e)})))

;; ============================================================================
;; Server
;; ============================================================================

(defn -main [& _args]
  (showcase/start-server
   :execute-fn execute-movie-review-agent
   :service-name "movie-review-agent"
   :port 3000))

(comment
  ;; Start server from REPL
  (def server (-main))

  ;; Stop server
  (.stop server))
