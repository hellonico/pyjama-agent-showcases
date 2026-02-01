(ns image-generator.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [ajax.core :refer [POST GET]]
            [clojure.string :as str]))

;; State
(def state (r/atom {:prompt ""
                    :width 512
                    :height 512
                    :loading? false
                    :progress nil
                    :image-data nil
                    :error nil
                    :request-id nil
                    :poll-interval nil}))

;; Polling for progress updates
(defn poll-progress! [request-id]
  (GET (str "http://localhost:3000/api/progress/" request-id)
    {:response-format :json
     :keywords? true
     :handler (fn [response]
                (let [status (:status response)]
                  (case status
                    "initializing" (swap! state assoc :progress {:completed 0 :total 9})
                    "generating" (when-let [progress (:progress response)]
                                   (swap! state assoc :progress progress))
                    "complete" (do
                                 (swap! state assoc
                                        :loading? false
                                        :progress nil
                                        :image-data (:image-data response))
                                 (when-let [interval (:poll-interval @state)]
                                   (js/clearInterval interval)
                                   (swap! state assoc :poll-interval nil)))
                    "error" (do
                              (swap! state assoc
                                     :loading? false
                                     :progress nil
                                     :error (:error response))
                              (when-let [interval (:poll-interval @state)]
                                (js/clearInterval interval)
                                (swap! state assoc :poll-interval nil)))
                    nil)))
     :error-handler (fn [error]
                      (println "Poll error:" error))}))

;; API Call
(defn generate-image! [prompt width height]
  (swap! state assoc
         :loading? true
         :error nil
         :image-data nil
         :progress {:completed 0 :total 9})

  (POST "http://localhost:3000/api/generate-image"
    {:params {:prompt prompt
              :width width
              :height height}
     :format :json
     :response-format :json
     :keywords? true
     :handler (fn [response]
                (when (:success response)
                  (let [request-id (:request-id response)]
                    (swap! state assoc :request-id request-id)
                    ;; Start polling every 500ms
                    (let [interval (js/setInterval #(poll-progress! request-id) 500)]
                      (swap! state assoc :poll-interval interval)))))
     :error-handler (fn [{:keys [response]}]
                      (swap! state assoc
                             :loading? false
                             :progress nil
                             :error (or (:error response)
                                        "Unknown error occurred")))}))

;; Components
(defn input-section []
  [:div.input-section
   [:h1 "🎨 AI Image Generator"]
   [:p.subtitle "Generate images from text using Ollama + Alibaba Z-Image Turbo"]

   [:div.prompt-box
    [:textarea.prompt-input
     {:placeholder "Describe the image you want to generate...\nExample: \"A serene sunset over mountains in the style of Monet\""
      :value (:prompt @state)
      :on-change #(swap! state assoc :prompt (.. % -target -value))
      :disabled (:loading? @state)
      :rows 4}]]

   [:div.dimension-controls
    [:div.dimension-input
     [:label "Width"]
     [:input {:type "number"
              :min 128
              :max 2048
              :step 64
              :value (:width @state)
              :on-change #(swap! state assoc :width (js/parseInt (.. % -target -value)))
              :disabled (:loading? @state)}]]

    [:div.dimension-input
     [:label "Height"]
     [:input {:type "number"
              :min 128
              :max 2048
              :step 64
              :value (:height @state)
              :on-change #(swap! state assoc :height (js/parseInt (.. % -target -value)))
              :disabled (:loading? @state)}]]

    [:div.preset-buttons
     [:button.preset-btn
      {:on-click #(swap! state assoc :width 128 :height 128)
       :disabled (:loading? @state)}
      "128×128"]
     [:button.preset-btn
      {:on-click #(swap! state assoc :width 512 :height 512)
       :disabled (:loading? @state)}
      "512×512"]
     [:button.preset-btn
      {:on-click #(swap! state assoc :width 1024 :height 768)
       :disabled (:loading? @state)}
      "1024×768"]
     [:button.preset-btn
      {:on-click #(swap! state assoc :width 1024 :height 1024)
       :disabled (:loading? @state)}
      "1024×1024"]]]

   [:button.generate-btn
    {:on-click #(when-not (str/blank? (:prompt @state))
                  (generate-image! (:prompt @state) (:width @state) (:height @state)))
     :disabled (or (:loading? @state)
                   (str/blank? (:prompt @state)))}
    (if (:loading? @state)
      [:span
       [:span.spinner "🎨"]
       " Generating..."]
      "✨ Generate Image")]])

(defn progress-section []
  (when-let [progress (:progress @state)]
    (let [completed (or (:completed progress) 0)
          total (or (:total progress) 9)
          percentage (* 100 (/ completed (max total 1)))]
      [:div.progress-section
       [:div.progress-bar-container
        [:div.progress-bar
         {:style {:width (str percentage "%")}}]]
       [:p.progress-text
        (str "Generating: Step " completed " of " total " (" (int percentage) "%)")]
       [:p.progress-tip "This may take a few minutes depending on image size..."]])))

(defn error-section []
  (when-let [error (:error @state)]
    [:div.error-section
     [:h3 "❌ Error"]
     [:p error]
     [:button.retry-btn
      {:on-click #(swap! state assoc :error nil)}
      "Dismiss"]]))

(defn result-section []
  (when-let [image-data (:image-data @state)]
    [:div.result-section
     [:div.result-header
      [:h2 "✨ Image Generated!"]
      [:button.new-generation-btn
       {:on-click #(swap! state assoc :image-data nil :prompt "")}
       "🔄 Generate Another"]]

     [:div.image-container
      [:img {:src (str "data:image/png;base64," image-data)
             :alt "Generated image"}]]

     [:div.image-info
      [:p (str "Dimensions: " (:width @state) " × " (:height @state) " pixels")]
      [:p.prompt "Prompt: \"" (:prompt @state) "\""]]

     [:div.download-section
      [:a.download-btn
       {:href (str "data:image/png;base64," image-data)
        :download (str "ai-generated-" (.getTime (js/Date.)) ".png")}
       "💾 Download Image"]]]))

(defn app []
  [:div.container
   [input-section]
   [progress-section]
   [error-section]
   [result-section]])

;; Init & Reload
(defn init! []
  (rdom/render [app] (.getElementById js/document "app")))

(defn reload! []
  (init!))
