(ns image-generator.server
  "Ring server for image generator agent API with polling-based progress updates"
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [pyjama.core]
            [clojure.string :as str])
  (:gen-class))

;; Progress state storage (in-memory with request IDs)
(defonce progress-store (atom {}))

(defn run-image-generator-async
  "Run the image generator asynchronously with progress updates"
  [request-id prompt width height]
  (future
    (try
      (println "🎨 Starting image generation for prompt:" prompt)
      (swap! progress-store assoc request-id {:status "initializing" :progress {:completed 0 :total 9}})

      ;; Use direct Ollama call without streaming for simplicity
      (let [ollama-url (or (System/getenv "OLLAMA_URL") "http://localhost:11434")
            result (try
                     (println "� Calling Ollama API...")
                     (pyjama.core/ollama
                      ollama-url
                      :generate-image
                      {:model "x/z-image-turbo"
                       :prompt prompt
                       :width width
                       :height height
                       :stream false})  ; Non-streaming for simplicity
                     (catch Exception e
                       (println "❌ Ollama call failed:" (.getMessage e))
                       (.printStackTrace e)
                       nil))]

        (println "📦 Result type:" (type result))
        (println "📦 Result (first 100 chars):" (when (string? result) (subs result 0 (min 100 (count result)))))

        (if (and result (string? result) (pos? (count result)))
          (do
            (println "✅ Image generation completed, image size:" (count result) "bytes")
            (swap! progress-store assoc request-id {:status "complete"
                                                    :image-data result
                                                    :width width
                                                    :height height
                                                    :prompt prompt})
            {:success true
             :image-data result
             :width width
             :height height
             :prompt prompt})
          (do
            (println "❌ Image generation failed - no valid image data")
            (swap! progress-store assoc request-id {:status "error"
                                                    :error "No image data returned"})
            {:success false
             :error "No image data returned"})))
      (catch Exception e
        (println "❌ Error generating image:" (.getMessage e))
        (.printStackTrace e)
        (swap! progress-store assoc request-id {:status "error"
                                                :error (.getMessage e)})
        {:success false
         :error (.getMessage e)
         :details (ex-data e)}))))

(defn generate-image-handler
  "Handle POST /api/generate-image requests - starts generation and returns request ID"
  [request]
  (let [{:keys [prompt width height]} (:body request)
        width  (or width 512)
        height (or height 512)
        request-id (str (java.util.UUID/randomUUID))]
    (if (or (nil? prompt) (empty? prompt))
      {:status 400
       :body {:success false
              :error "prompt is required"}}
      (do
        ;; Start async generation
        (run-image-generator-async request-id prompt width height)
        ;; Return request ID immediately
        {:status 200
         :body {:success true
                :request-id request-id
                :message "Image generation started"}}))))

(defn progress-handler
  "Handle GET /api/progress/:request-id requests - returns current progress"
  [request]
  (let [uri (:uri request)
        request-id (last (str/split uri #"/"))
        progress-data (get @progress-store request-id)]
    (if progress-data
      (let [;; Extract only serializable fields
            safe-data (cond
                        (= (:status progress-data) "complete")
                        {:status "complete"
                         :image-data (:image-data progress-data)
                         :width (:width progress-data)
                         :height (:height progress-data)
                         :prompt (:prompt progress-data)}

                        (= (:status progress-data) "error")
                        {:status "error"
                         :error (:error progress-data)}

                        :else  ; initializing or generating
                        {:status (or (:status (:progress progress-data)) (:status progress-data) "initializing")
                         :progress (select-keys (:progress progress-data) [:completed :total])})]
        {:status 200
         :body safe-data})
      {:status 404
       :body {:error "Request not found"}})))

(defn health-handler [_request]
  {:status 200
   :body {:status "ok"
          :service "image-generator-agent"}})

(defn routes
  "Main routing handler"
  [request]
  (let [uri (:uri request)
        method (:request-method request)]
    (cond
      (and (= method :post) (= uri "/api/generate-image"))
      (generate-image-handler request)

      (and (= method :get) (.startsWith uri "/api/progress/"))
      (progress-handler request)

      (and (= method :get) (= uri "/api/health"))
      (health-handler request)

      :else
      {:status 404
       :body {:error "Not found"}})))

(def app
  (-> routes
      (wrap-json-body {:keywords? true})
      wrap-json-response
      (wrap-cors :access-control-allow-origin [#".*"]  ; Allow all origins for development
                 :access-control-allow-methods [:get :post :put :delete :options]
                 :access-control-allow-headers ["Content-Type" "Accept" "Authorization" "X-Requested-With"]
                 :access-control-expose-headers ["Content-Type"])))

(defn start-server
  "Start the API server"
  [& {:keys [port] :or {port 3000}}]
  (println (format "🚀 Image Generator API server starting on port %d..." port))
  (println "📡 HTTP API: http://localhost:3000/api/generate-image")
  (println "📊 Progress API: http://localhost:3000/api/progress/:request-id")
  (println "🌐 ClojureScript frontend should run on http://localhost:8020")
  (jetty/run-jetty app {:port port :join? false}))

(defn -main [& _args]
  (start-server))

(comment
  ;; Start server from REPL
  (def server (start-server :port 3000))

  ;; Stop server
  (.stop server))
