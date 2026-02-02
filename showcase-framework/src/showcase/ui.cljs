(ns showcase.ui
  "Common ClojureScript UI framework for agent showcases with Reagent"
  (:require [reagent.core :as r]
            [ajax.core :refer [POST GET]]
            [clojure.string :as str]))

;; ============================================================================
;; State Management
;; ============================================================================

(defn create-state
  "Create initial state atom for showcase apps
  
  Standard keys:
  - :loading? - Whether request is in progress
  - :progress - {:completed X :total Y} for progress bar
  - :result - Result from agent execution
  - :error - Error message if failed
  - :request-id - Current request ID for polling
  - :poll-interval - JavaScript interval ID for cleanup
  - :history - Vector of past completions
  - :show-history? - Whether history panel is visible
  
  Additional keys can be added by the specific showcase"
  [initial-state]
  (r/atom (merge {:loading? false
                  :progress nil
                  :result nil
                  :error nil
                  :request-id nil
                  :poll-interval nil
                  :history []
                  :show-history? false}
                 initial-state)))

;; ============================================================================
;; API Communication
;; ============================================================================

(defn poll-progress!
  "Poll for progress updates from the server
  
  Parameters:
  - state: Reagent atom
  - api-url: Base URL (e.g., 'http://localhost:3000')
  - request-id: Request ID to poll for"
  [state api-url request-id]
  (GET (str api-url "/api/progress/" request-id)
    {:response-format :json
     :keywords? true
     :handler (fn [response]
                (let [status (:status response)]
                  (case status
                    "initializing" (swap! state assoc :progress {:completed 0 :total 1})
                    "processing" (when-let [progress (:progress response)]
                                   (swap! state assoc :progress progress))
                    "complete" (do
                                 (swap! state assoc
                                        :loading? false
                                        :progress nil
                                        :result (:result response))
                                 ;; Stop polling
                                 (when-let [interval (:poll-interval @state)]
                                   (js/clearInterval interval)
                                   (swap! state assoc :poll-interval nil)))
                    "error" (do
                              (swap! state assoc
                                     :loading? false
                                     :progress nil
                                     :error (:error response))
                              ;; Stop polling
                              (when-let [interval (:poll-interval @state)]
                                (js/clearInterval interval)
                                (swap! state assoc :poll-interval nil)))
                    nil)))
     :error-handler (fn [error]
                      (println "Poll error:" error))}))

(defn execute-agent!
  "Execute agent via API and start polling for progress
  
  Parameters:
  - state: Reagent atom
  - api-url: Base URL (e.g., 'http://localhost:3000')
  - params: Parameters to send to agent
  - poll-interval-ms: How often to poll (default: 500ms)"
  [state api-url params & {:keys [poll-interval-ms] :or {poll-interval-ms 500}}]
  (swap! state assoc
         :loading? true
         :error nil
         :result nil
         :progress {:completed 0 :total 1})

  (POST (str api-url "/api/execute")
    {:params params
     :format :json
     :response-format :json
     :keywords? true
     :handler (fn [response]
                (when (:success response)
                  (let [request-id (:request-id response)]
                    (swap! state assoc :request-id request-id)
                    ;; Start polling
                    (let [interval (js/setInterval
                                    #(poll-progress! state api-url request-id)
                                    poll-interval-ms)]
                      (swap! state assoc :poll-interval interval)))))
     :error-handler (fn [{:keys [response]}]
                      (swap! state assoc
                             :loading? false
                             :progress nil
                             :error (or (:error response)
                                        "Unknown error occurred")))}))

;; ============================================================================
;; UI Components
;; ============================================================================

(defn text-input
  "Standard text input component
  
  Parameters:
  - state: Reagent atom
  - key: Key in state atom to bind to
  - placeholder: Placeholder text
  - opts: Optional map with :disabled?, :multiline?, :rows"
  [state key placeholder & [opts]]
  (let [{:keys [disabled? multiline? rows]} opts
        disabled? (or disabled? (:loading? @state))]
    (if multiline?
      [:textarea.input-field
       {:placeholder placeholder
        :value (get @state key)
        :on-change #(swap! state assoc key (.. % -target -value))
        :disabled disabled?
        :rows (or rows 4)}]
      [:input.input-field
       {:type "text"
        :placeholder placeholder
        :value (get @state key)
        :on-change #(swap! state assoc key (.. % -target -value))
        :disabled disabled?}])))

(defn number-input
  "Standard number input component
  
  Parameters:
  - state: Reagent atom
  - key: Key in state atom to bind to
  - label: Label text
  - opts: Optional map with :min, :max, :step, :disabled?"
  [state key label & [opts]]
  (let [{:keys [min max step disabled?]} opts
        disabled? (or disabled? (:loading? @state))]
    [:div.number-input
     [:label label]
     [:input {:type "number"
              :min (or min 0)
              :max max
              :step (or step 1)
              :value (get @state key)
              :on-change #(swap! state assoc key (js/parseInt (.. % -target -value)))
              :disabled disabled?}]]))

(defn action-button
  "Standard action button component
  
  Parameters:
  - state: Reagent atom
  - label: Button label (or loading label if loading)
  - on-click: Click handler
  - opts: Optional map with :disabled?, :loading-label, :icon"
  [state label on-click & [opts]]
  (let [{:keys [disabled? loading-label icon]} opts
        loading? (:loading? @state)
        disabled? (or disabled? loading?)]
    [:button.action-button
     {:on-click on-click
      :disabled disabled?}
     (if loading?
       [:span
        [:span.spinner (or icon "⏳")]
        " " (or loading-label "Processing...")]
       [:span (or icon "") " " label])]))

(defn progress-bar
  "Standard progress bar component
  
  Parameters:
  - state: Reagent atom (reads :progress key)"
  [state]
  (when-let [progress (:progress @state)]
    (let [completed (or (:completed progress) 0)
          total (or (:total progress) 1)
          percentage (* 100 (/ completed (max total 1)))]
      [:div.progress-section
       [:div.progress-bar-container
        [:div.progress-bar
         {:style {:width (str percentage "%")}}]]
       [:p.progress-text
        (str "Progress: Step " completed " of " total " (" (int percentage) "%)")]])))

(defn error-display
  "Standard error display component
  
  Parameters:
  - state: Reagent atom (reads :error key)
  - opts: Optional map with :dismissible?"
  [state & [opts]]
  (when-let [error (:error @state)]
    [:div.error-section
     [:h3 "❌ Error"]
     [:p error]
     (when (:dismissible? opts)
       [:button.dismiss-button
        {:on-click #(swap! state assoc :error nil)}
        "Dismiss"])]))

(defn loading-display
  "Standard loading/spinner display
  
  Parameters:
  - state: Reagent atom (reads :loading? key)
  - message: Loading message
  - opts: Optional map with :steps (vector of step descriptions)"
  [state message & [opts]]
  (when (:loading? @state)
    [:div.loading-section
     [:div.loading-spinner]
     [:p message]
     (when-let [steps (:steps opts)]
       [:p.loading-steps
        (str/join " → " steps)])]))

;; ============================================================================
;; Layout Helpers
;; ============================================================================

(defn showcase-container
  "Standard container wrapper with title
  
  Parameters:
  - title: Main title
  - subtitle: Optional subtitle
  - children: Child components"
  [title subtitle & children]
  [:div.showcase-container
   [:div.header
    [:h1 title]
    (when subtitle
      [:p.subtitle subtitle])]
   [:div.content
    children]])

(defn section
  "Standard section wrapper
  
  Parameters:
  - title: Section title (optional)
  - class: CSS class (optional)
  - children: Child components"
  [& {:keys [title class children]}]
  [:div {:class (or class "section")}
   (when title [:h2 title])
   children])

;; ============================================================================
;; Full Showcase App Template
;; ============================================================================

(defn showcase-app
  "Complete showcase app template
  
  Parameters (as map):
  - :title - Main title
  - :subtitle - Subtitle
  - :input-component - Component function for input section
  - :result-component - Component function for result display (receives result)
  - :state - Reagent atom (optional, will create if not provided)
  - :api-url - API base URL (default: http://localhost:3000)"
  [{:keys [title subtitle input-component result-component state api-url]
    :or {api-url "http://localhost:3000"}}]
  (let [state (or state (create-state {}))]
    (fn []
      [:div.container
       [:div.input-section
        [:h1 title]
        (when subtitle [:p.subtitle subtitle])
        [input-component state api-url]]

       [progress-bar state]
       [loading-display state "Processing your request..."]
       [error-display state {:dismissible? true}]

       (when-let [result (:result @state)]
         [:div.result-section
          [result-component result state]])])))

(comment
  ;; Example usage
  (defn my-input [state api-url]
    [:div
     [text-input state :prompt "Enter prompt" {:multiline? true}]
     [action-button state "Generate"
      #(execute-agent! state api-url {:prompt (:prompt @state)})
      {:icon "✨" :loading-label "Generating..."}]])

  (defn my-result [result state]
    [:div
     [:h2 "Result"]
     [:p result]
     [:button {:on-click #(swap! state assoc :result nil)} "Clear"]])

  (defn app []
    [showcase-app
     {:title "My Agent"
      :subtitle "Agent description"
      :input-component my-input
      :result-component my-result}]))

(defn fetch-history!
  "Fetch history from the server
  
  Parameters:
  - state: Reagent atom
  - api-url: Base URL (e.g., 'http://localhost:3000')
  - opts: Optional map with :limit (default: 10)"
  [state api-url & {:keys [limit] :or {limit 10}}]
  (GET (str api-url "/api/history")
    {:params {:limit limit}
     :response-format :json
     :keywords? true
     :handler (fn [response]
                (swap! state assoc :history (:history response)))
     :error-handler (fn [error]
                      (println "History fetch error:" error))}))

(defn history-panel
  "History panel component - showcase-specific rendering
  
  Parameters:
  - state: Reagent atom
  - api-url: API base URL
  - render-item-fn: Function to render each history item (fn [item state] -> hiccup)"
  [state api-url render-item-fn]
  (when (:show-history? @state)
    [:div.history-panel
     [:div.history-header
      [:h3 "📜 History"]
      [:button.close-btn
       {:on-click #(swap! state assoc :show-history? false)}
       "×"]]
     
     (if (empty? (:history @state))
       [:p.empty-state "No history yet. Generate something to get started!"]
       [:div.history-list
        (for [item (:history @state)]
          ^{:key (:request-id item)}
          [render-item-fn item state])])]))

(defn history-toggle-button
  "Button to toggle history panel"
  [state api-url]
  [:button.history-toggle
   {:on-click #(do
                 (when-not (:show-history? @state)
                   (fetch-history! state api-url :limit 20))
                 (swap! state update :show-history? not))}
   (if (:show-history? @state)
     "Hide History"
     "Show History")])
