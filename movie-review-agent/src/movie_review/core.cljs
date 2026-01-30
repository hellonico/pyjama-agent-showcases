(ns movie-review.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [ajax.core :refer [POST]]
            [clojure.string :as str]))

;; State
(def state (r/atom {:movie-name ""
                    :loading? false
                    :result nil
                    :error nil}))

;; API Call
(defn analyze-movie! [movie-name]
  (swap! state assoc :loading? true :error nil :result nil)
  (POST "http://localhost:3000/api/analyze-movie"
    {:params {:movie-name movie-name}
     :format :json
     :response-format :json
     :keywords? true
     :handler (fn [response]
                (swap! state assoc
                       :loading? false
                       :result (:result response)))
     :error-handler (fn [{:keys [response]}]
                      (swap! state assoc
                             :loading? false
                             :error (or (:error response)
                                        "Unknown error occurred")))}))


;; Components
(defn input-section []
  [:div.input-section
   [:h1 "🎬 Movie Review Agent"]
   [:p.subtitle "Enter a movie name to get AI-powered analysis with TMDB data"]

   [:div.search-box
    [:input.movie-input
     {:type "text"
      :placeholder "Enter movie name (e.g., 'Inception', 'The Matrix')"
      :value (:movie-name @state)
      :on-change #(swap! state assoc :movie-name (.. % -target -value))
      :on-key-press #(when (= "Enter" (.-key %))
                       (when-not (str/blank? (:movie-name @state))
                         (analyze-movie! (:movie-name @state))))
      :disabled (:loading? @state)}]

    [:button.analyze-btn
     {:on-click #(when-not (str/blank? (:movie-name @state))
                   (analyze-movie! (:movie-name @state)))
      :disabled (or (:loading? @state)
                    (str/blank? (:movie-name @state)))}
     (if (:loading? @state)
       [:span
        [:span.spinner "⏳"]
        " Analyzing..."]
       "🔍 Analyze Movie")]]])

(defn loading-section []
  (when (:loading? @state)
    [:div.loading-section
     [:div.loading-spinner]
     [:p "Searching TMDB and analyzing with AI..."]
     [:p.loading-steps
      "Steps: Movie Search → Fetch Reviews → LLM Analysis → Summary"]]))

(defn error-section []
  (when-let [error (:error @state)]
    [:div.error-section
     [:h3 "❌ Error"]
     [:p error]]))

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

(defn result-section []
  (when-let [result (:result @state)]
    [:div.result-section
     [:div.result-header
      [:h2 "✨ Analysis Complete"]
      [:button.new-search-btn
       {:on-click #(swap! state assoc :result nil :movie-name "")}
       "🔄 New Search"]]

     [:div.result-content
      {:dangerouslySetInnerHTML
       {:__html (markdown-to-html result)}}]]))

(defn app []
  [:div.container
   [input-section]
   [loading-section]
   [error-section]
   [result-section]])

;; Init & Reload
(defn init! []
  (rdom/render [app] (.getElementById js/document "app")))

(defn reload! []
  (init!))
