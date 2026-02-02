(ns image-generator.server
  "Ring server for image generator agent API - refactored to use showcase framework"
  (:require [showcase.server :as showcase]
            [pyjama.core]
            [clojure.core.async :as async])
  (:gen-class))

;; ============================================================================
;; Image Generation Logic
;; ============================================================================

(defn combined-callback
  "Callback that BOTH sends progress to channel AND returns image when done"
  [progress-ch]
  (fn [parsed]
    ;; Send progress updates to channel
    (when (or (:completed parsed) (:total parsed))
      (async/go (async/>! progress-ch parsed)))

    ;; Return image data when done (this is what stream function returns)
    (when (:done parsed)
      (:image parsed))))

(defn execute-image-generator
  "Execute the image generator with parameters
  
  Takes opts map with:
    :params - {:prompt string :width int :height int}
    :progress-fn - (fn [completed total] -> nil)
  Returns {:success bool :result any :error string}"
  [{:keys [params progress-fn]}]
  (try
    (let [{:keys [prompt width height]} params
          width  (or width 512)
          height (or height 512)
          ollama-url (or (System/getenv "OLLAMA_URL") "http://localhost:11434")
          progress-ch (async/chan)]

      (println "🎨 Starting image generation:" prompt)

      ;; Start the Ollama request in a go block
      (let [result-ch (async/go
                        (pyjama.core/ollama
                         ollama-url
                         :generate-image
                         {:model "x/z-image-turbo"
                          :prompt prompt
                          :width width
                          :height height
                          :stream true}
                         (combined-callback progress-ch)))]

        ;; Monitor progress updates in a separate thread
        (future
          (loop []
            (when-let [progress-update (async/<!! progress-ch)]
              (when (:completed progress-update)
                (println "📊 Progress:" (:completed progress-update) "/" (:total progress-update))
                ;; Call the progress function to update state
                (progress-fn (:completed progress-update) (:total progress-update)))
              (recur))))

        ;; Wait for the result from the async channel
        (let [image-data (async/<!! result-ch)]
          (async/close! progress-ch)

          (println "🔍 Got result - is-string?=" (string? image-data) "length=" (when (string? image-data) (count image-data)))

          (if (and image-data (string? image-data) (pos? (count image-data)))
            (do
              (println "✅ Image generated successfully (" (count image-data) "bytes)")
              {:success true
               :result {:image-data image-data
                        :width width
                        :height height
                        :prompt prompt}})
            (do
              (println "❌ Image generation failed - no image data")
              {:success false
               :error "No image data returned"})))))
    (catch Exception e
      (println "❌ Error generating image:" (.getMessage e))
      (.printStackTrace e)
      {:success false
       :error (.getMessage e)})))

;; ============================================================================
;; Server
;; ============================================================================

(defn -main [& _args]
  (showcase/start-server
   :execute-fn execute-image-generator
   :service-name "image-generator-agent"
   :port 3000))

(comment
  ;; Start server from REPL
  (def server (-main))

  ;; Stop server
  (.stop server))
