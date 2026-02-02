(ns movie-review.core
  "Movie review showcase - refactored to use showcase framework"
  (:require [reagent.dom :as rdom]
            [showcase.ui :as ui]
            [clojure.string :as str]))

;; ============================================================================
;; Helper Functions
;; ============================================================================

(defn markdown-to-html
  "Simple markdown to HTML converter for basic formatting"
  [text]
  (when text
    (-> text
        (str/replace #"### (.+)" "<h3>$1</h3>")
        (str/replace #"## (.+)" "<h2>$1</h2>")
        (str/replace #"# (.+)" "<h1>$1</h1>")
        (str/replace #"\*\*(.+?)\*\*" "<strong>$1</strong>")
        (str/replace #"\*(.+?)\*" "<em>$1</em>")
        (str/replace #"- (.+)" "<li>$1</li>")
        (str/replace #"\n\n" "<br/><br/>")
        (str/replace #"\n" "<br/>"))))

;; ============================================================================
;; Custom UI Components
;; ============================================================================

(defn render-movie-history-item
  "Render a single movie history item"
  [item state]
  (let [result (:result item)
        timestamp (:timestamp item)
        date (js/Date. (* timestamp 1000))
        time-str (.toLocaleString date)]
    [:div.history-item
     {:on-click #(swap! state assoc :result result)}
     [:div.history-content
      [:div.history-icon "🎬"]
      [:div.history-details
       [:div.history-preview
        (or (subs result 0 (min 100 (count result))) "Movie review")]
       [:div.history-time time-str]]]]))

(defn input-section
  "Custom input section for movie review"
  [state api-url]
  [:div
   ;; History toggle button
   [:div.controls-row
    [ui/history-toggle-button state api-url]]

   ;; Movie name input
   [:div.search-box
    [:input.movie-input
     {:type "text"
      :placeholder "Enter movie name (e.g., 'Inception', 'The Matrix')"
      :value (:movie-name @state)
      :on-change #(swap! state assoc :movie-name (.. % -target -value))
      :on-key-press #(when (= "Enter" (.-key %))
                       (when-not (str/blank? (:movie-name @state))
                         (ui/execute-agent! state api-url
                                            {:movie-name (:movie-name @state)})))
      :disabled (:loading? @state)}]

    ;; Analyze button
    [ui/action-button state "Analyze Movie"
     #(when-not (str/blank? (:movie-name @state))
        (ui/execute-agent! state api-url
                           {:movie-name (:movie-name @state)}))
     {:icon "🔍" :loading-label "Analyzing..."
      :disabled? (str/blank? (:movie-name @state))}]]])

(defn result-section
  "Custom result section for displaying movie analysis"
  [result state]
  [:div
   [:div.result-header
    [:h2 "✨ Analysis Complete"]
    [:button.new-search-btn
     {:on-click #(swap! state assoc :result nil :movie-name "")}
     "🔄 New Search"]]

   [:div.result-content
    {:dangerouslySetInnerHTML
     {:__html (markdown-to-html result)}}]])

;; ============================================================================
;; App
;; ============================================================================

(def initial-state
  {:movie-name ""})

(defonce app-state (ui/create-state initial-state))

(defn app []
  [:div
   ;; History panel
   [ui/history-panel app-state "http://localhost:3000" render-movie-history-item]

   ;; Main app
   [ui/showcase-app
    {:title "🎬 Movie Review Agent"
     :subtitle "Enter a movie name to get AI-powered analysis with TMDB data"
     :input-component input-section
     :result-component result-section
     :state app-state
     :api-url "http://localhost:3000"}]])

;; Init & Reload
(defn init! []
  (rdom/render [app] (.getElementById js/document "app")))

(defn reload! []
  (init!))
