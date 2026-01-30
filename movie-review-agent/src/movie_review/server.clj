(ns movie-review.server
  "Ring server for movie review agent API"
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [pyjama.agent.core :as agent]
            [pyjama.core]
            [clojure.java.io :as io])
  (:gen-class))

(defn run-movie-agent-async
  "Run the movie review agent asynchronously"
  [movie-name]
  (future
    (try
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
                     (pr-str result))})
      (catch Exception e
        (println "❌ Error executing agent:" (.getMessage e))
        (.printStackTrace e)
        {:success false
         :error (.getMessage e)
         :details (ex-data e)}))))

(defn analyze-movie-handler
  "Handle POST /api/analyze-movie requests"
  [request]
  (let [movie-name (get-in request [:body :movie-name])]
    (if (or (nil? movie-name) (empty? movie-name))
      {:status 400
       :body {:success false
              :error "movie-name is required"}}
      (let [result-future (run-movie-agent-async movie-name)
            result @result-future]  ; Blocking wait for agent completion
        {:status 200
         :body result}))))

(defn health-handler [_request]
  {:status 200
   :body {:status "ok"
          :service "movie-review-agent"}})

(defn routes
  "Main routing handler"
  [request]
  (let [uri (:uri request)
        method (:request-method request)]
    (cond
      (and (= method :post) (= uri "/api/analyze-movie"))
      (analyze-movie-handler request)

      (and (= method :get) (= uri "/api/health"))
      (health-handler request)

      :else
      {:status 404
       :body {:error "Not found"}})))

(def app
  (-> routes
      (wrap-json-body {:keywords? true})
      wrap-json-response
      (wrap-cors :access-control-allow-origin [#"http://localhost:8020"]
                 :access-control-allow-methods [:get :post :options]
                 :access-control-allow-headers ["Content-Type"])))

(defn start-server
  "Start the API server"
  [& {:keys [port] :or {port 3000}}]
  (println (format "🚀 Movie Review API server starting on port %d..." port))
  (println "📡 Accepting requests at http://localhost:3000/api/analyze-movie")
  (println "🌐 ClojureScript frontend should run on http://localhost:8020")
  (jetty/run-jetty app {:port port :join? false}))

(defn -main [& _args]
  (start-server))

(comment
  ;; Start server from REPL
  (def server (start-server :port 3000))

  ;; Stop server
  (.stop server))
