(ns image-generator.core
  "Image generator showcase - refactored to use showcase framework"
  (:require [reagent.dom :as rdom]
            [showcase.ui :as ui]
            [clojure.string :as str]))

;; ============================================================================
;; Custom UI Components
;; ============================================================================

(defn input-section
  "Custom input section for image generation"
  [state api-url]
  [:div
   ;; Prompt input
   [:div.prompt-box
    [ui/text-input state :prompt
     "Describe the image you want to generate...\nExample: \"A serene sunset over mountains in the style of Monet\""
     {:multiline? true :rows 4}]]

   ;; Dimension controls
   [:div.dimension-controls
    [ui/number-input state :width "Width" {:min 128 :max 2048 :step 64}]
    [ui/number-input state :height "Height" {:min 128 :max 2048 :step 64}]

    ;; Preset buttons
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

   ;; Generate button
   [ui/action-button state "Generate Image"
    #(when-not (str/blank? (:prompt @state))
       (ui/execute-agent! state api-url
                          {:prompt (:prompt @state)
                           :width (:width @state)
                           :height (:height @state)}))
    {:icon "✨" :loading-label "Generating..."
     :disabled? (str/blank? (:prompt @state))}]])

(defn result-section
  "Custom result section for displaying generated images"
  [result state]
  (let [{:keys [image-data width height prompt]} result]
    [:div
     [:div.result-header
      [:h2 "✨ Image Generated!"]
      [:button.new-generation-btn
       {:on-click #(swap! state assoc :result nil :prompt "")}
       "🔄 Generate Another"]]

     [:div.image-container
      [:img {:src (str "data:image/png;base64," image-data)
             :alt "Generated image"}]]

     [:div.image-info
      [:p (str "Dimensions: " width " × " height " pixels")]
      [:p.prompt "Prompt: \"" prompt "\""]]

     [:div.download-section
      [:a.download-btn
       {:href (str "data:image/png;base64," image-data)
        :download (str "ai-generated-" (.getTime (js/Date.)) ".png")}
       "💾 Download Image"]]]))

;; ============================================================================
;; App
;; ============================================================================

(def initial-state
  {:prompt ""
   :width 512
   :height 512})

(defonce app-state (ui/create-state initial-state))

(defn app []
  [ui/showcase-app
   {:title "🎨 AI Image Generator"
    :subtitle "Generate images from text using Ollama + Alibaba Z-Image Turbo"
    :input-component input-section
    :result-component result-section
    :state app-state
    :api-url "http://localhost:3000"}])

;; Init & Reload
(defn init! []
  (rdom/render [app] (.getElementById js/document "app")))

(defn reload! []
  (init!))
