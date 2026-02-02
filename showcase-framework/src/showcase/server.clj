(ns showcase.server
  "Common Ring server framework for agent showcases with async execution and progress tracking"
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [clojure.string :as str]))

;; ============================================================================
;; Progress State Management
;; ============================================================================

;; In-memory storage for request progress and results
(defonce progress-store (atom {}))

;; History storage - keeps completed requests
(defonce history-store (atom []))

(defn update-progress!
  "Update progress for a request"
  [request-id status & {:keys [progress result error]}]
  (swap! progress-store assoc request-id
         (cond-> {:status status}
           progress (assoc :progress progress)
           result   (assoc :result result)
           error    (assoc :error error)))

  ;; If complete, add to history
  (when (= status "complete")
    (swap! history-store
           (fn [history]
             (let [entry {:request-id request-id
                          :timestamp (System/currentTimeMillis)
                          :result result}]
               ;; Keep last 50 entries
               (take 50 (cons entry history)))))))

(defn get-progress
  "Get progress data for a request"
  [request-id]
  (get @progress-store request-id))

(defn get-history
  "Get history of completed requests
  
  Options:
  - limit: Number of entries to return (default: 10)"
  [& {:keys [limit] :or {limit 10}}]
  (take limit @history-store))

(defn clear-progress!
  "Clear progress data for a request (cleanup)"
  [request-id]
  (swap! progress-store dissoc request-id))

;; ============================================================================
;; Async Execution
;; ============================================================================

(defn run-agent-async
  "Execute an agent function asynchronously with progress tracking
  
  Parameters:
  - request-id: Unique identifier for this request
  - execute-fn: Function that executes the agent (fn [opts] -> result)
                where opts is {:params map :progress-fn (fn [completed total])}
  - params: Parameters from HTTP request
  
  Returns: future"
  [request-id execute-fn params]
  (update-progress! request-id "initializing")
  (future
    (try
      (update-progress! request-id "processing")

      ;; Create progress callback function
      (let [progress-fn (fn [completed total]
                          (update-progress! request-id "processing"
                                            :progress {:completed completed :total total}))
            ;; Execute the agent function with a map of options
            result (execute-fn {:params params
                                :progress-fn progress-fn})]

        (if (:success result)
          (update-progress! request-id "complete" :result (:result result))
          (update-progress! request-id "error" :error (or (:error result) "Unknown error"))))

      (catch Exception e
        (println "❌ Error executing agent:" (.getMessage e))
        (.printStackTrace e)
        (update-progress! request-id "error" :error (.getMessage e))))))

;; ============================================================================
;; HTTP Handlers
;; ============================================================================

(defn execute-handler
  "Generic handler for POST /api/execute - starts async execution"
  [execute-fn request]
  (let [params (:body request)
        request-id (str (java.util.UUID/randomUUID))]
    (println "🚀 Starting execution with request-id:" request-id)
    (println "📋 Parameters:" params)

    ;; Start async execution
    (run-agent-async request-id execute-fn params)

    ;; Return request ID immediately
    {:status 200
     :body {:success true
            :request-id request-id
            :message "Execution started"}}))

(defn progress-handler
  "Generic handler for GET /api/progress/:request-id"
  [request]
  (let [uri (:uri request)
        request-id (last (str/split uri #"/"))
        progress-data (get-progress request-id)]
    (if progress-data
      {:status 200
       :body progress-data}
      {:status 404
       :body {:error "Request not found"}})))

(defn history-handler
  "Generic handler for GET /api/history"
  [request]
  (let [limit (-> request :params :limit (or "10") Integer/parseInt)
        history (get-history :limit limit)]
    {:status 200
     :body {:history history
            :count (count history)}}))

(defn health-handler
  "Generic handler for GET /api/health"
  [service-name]
  (fn [_request]
    {:status 200
     :body {:status "ok"
            :service service-name}}))

;; ============================================================================
;; Routing
;; ============================================================================

(defn make-routes
  "Create routing handler with custom execute function
  
  Parameters:
  - execute-fn: Function to execute agent (fn [opts] -> {:success bool :result any})
                where opts is {:params map :progress-fn fn}
  - service-name: Name of the service for health check"
  [execute-fn service-name]
  (fn [request]
    (let [uri (:uri request)
          method (:request-method request)]
      (cond
        ;; Execute endpoint
        (and (= method :post) (= uri "/api/execute"))
        (execute-handler execute-fn request)

        ;; Progress endpoint
        (and (= method :get) (.startsWith uri "/api/progress/"))
        (progress-handler request)

        ;; History endpoint
        (and (= method :get) (= uri "/api/history"))
        (history-handler request)

        ;; Health endpoint
        (and (= method :get) (= uri "/api/health"))
        ((health-handler service-name) request)

        ;; Not found
        :else
        {:status 404
         :body {:error "Not found"}}))))

;; ============================================================================
;; Middleware
;; ============================================================================

(defn wrap-showcase-middleware
  "Apply standard middleware stack for showcase apps"
  [handler]
  (-> handler
      (wrap-json-body {:keywords? true})
      wrap-json-response
      (wrap-cors :access-control-allow-origin [#".*"]  ; Allow all origins for development
                 :access-control-allow-methods [:get :post :put :delete :options]
                 :access-control-allow-headers ["Content-Type" "Accept" "Authorization" "X-Requested-With"]
                 :access-control-expose-headers ["Content-Type"])))

;; ============================================================================
;; Server
;; ============================================================================

(defn start-server
  "Start a showcase server
  
  Options:
  - :execute-fn - Function to execute the agent (required)
                  Signature: (fn [opts] -> {:success bool :result any})
                  where opts is {:params map :progress-fn (fn [completed total])}
  - :service-name - Name of service (default: \"agent-showcase\")
  - :port - Port to run on (default: 3000)"
  [& {:keys [execute-fn service-name port]
      :or {service-name "agent-showcase"
           port 3000}}]
  (when-not execute-fn
    (throw (IllegalArgumentException. "execute-fn is required")))

  (println (format "🚀 %s server starting on port %d..." service-name port))
  (println "📡 HTTP API: http://localhost:3000/api/execute")
  (println "📊 Progress API: http://localhost:3000/api/progress/:request-id")
  (println "🌐 ClojureScript frontend should run on http://localhost:8020")

  (let [routes (make-routes execute-fn service-name)
        app (wrap-showcase-middleware routes)]
    (jetty/run-jetty app {:port port :join? false})))

(comment
  ;; Example usage
  (defn my-agent-fn [{:keys [params progress-fn]}]
    (Thread/sleep 2000)
    (progress-fn 1 2)
    {:success true
     :result (str "Processed: " (:input params))})

  (def server (start-server :execute-fn my-agent-fn
                            :service-name "my-agent"
                            :port 3000))

  (.stop server))
