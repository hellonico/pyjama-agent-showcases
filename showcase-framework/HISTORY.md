# History Feature Guide

The showcase framework includes built-in history tracking for completed requests.

## Backend (Automatic)

History is automatically tracked when requests complete successfully:

```clojure
;; In showcase.server - automatic!
(when (= status "complete")
  (add-to-history! request-id result))
```

### API Endpoint

**GET `/api/history?limit=N`**

Returns the most recent N completions (default: 10, max stored: 50)

Response:
```json
{
  "history": [
    {
      "request-id": "uuid",
      "timestamp": 1234567890,
      "result": { /* your result data */ }
    }
  ],
  "count": 10
}
```

## Frontend Usage

### 1. Add History Toggle Button

```clojure
(ns my-showcase.core
  (:require [showcase.ui :as ui]))

(defn input-section [state api-url]
  [:div
   ;; Your inputs...
   
   ;; Add history toggle button
   [ui/history-toggle-button state api-url]])
```

### 2. Create History Item Renderer

```clojure
(defn render-history-item
  "Render a single history item"
  [item state]
  (let [result (:result item)
        timestamp (:timestamp item)]
    [:div.history-item
     {:on-click #(swap! state assoc :result result)}  ; Restore this result
     
     ; Your custom rendering here
     [:p "Generated: " (format-date timestamp)]
     ; ... display relevant info from result ...
     ]))
```

### 3. Add History Panel

```clojure
(defn app []
  (let [state (ui/create-state {:width 512 :height 512})]
    (fn []
      [:div.container
       [:div.input-section
        [input-section state "http://localhost:3000"]]
       
       ;; Add history panel
       [ui/history-panel state "http://localhost:3000" render-history-item]
       
       ; ... rest of your app ...
       ])))
```

## Complete Example (Image Generator)

```clojure
(ns image-generator.core
  (:require [reagent.dom :as rdom]
            [showcase.ui :as ui]))

(defn render-image-history-item
  "Render a single image history item"
  [item state]
  (let [result (:result item)
        image-data (:image result)
        prompt (:prompt result)]
    [:div.history-item
     {:on-click #(swap! state assoc :result result)}
     [:img {:src (str "data:image/png;base64," image-data)
            :style {:max-width "100px"
                    :border-radius "4px"}}]
     [:div.history-meta
      [:p.prompt (subs prompt 0 50) "..."]
      [:p.time (format-timestamp (:timestamp item))]]]))

(defn input-section [state api-url]
  [:div
   [ui/history-toggle-button state api-url]
   
   [ui/text-input state :prompt "Describe the image..." {:multiline? true}]
   [ui/number-input state :width "Width" {:min 128 :max 2048}]
   [ui/number-input state :height "Height" {:min 128 :max 2048}]
   
   [ui/action-button state "Generate Image"
    #(ui/execute-agent! state api-url
                        {:prompt (:prompt @state)
                         :width (:width @state)
                         :height (:height @state)})]])

(defn app []
  (let [state (ui/create-state {:width 512 :height 512 :prompt ""})]
    (fn []
      [:div.container
       [:div.input-section
        [:h1 "🎨 AI Image Generator"]
        [input-section state "http://localhost:3000"]]
       
       ;; History panel
       [ui/history-panel state "http://localhost:3000" render-image-history-item]
       
       [ui/progress-bar state]
       [ui/loading-display state "Generating your image..."]
       [ui/error-display state {:dismissible? true}]
       
       (when-let [result (:result @state)]
         [:div.result-section
          [:img {:src (str "data:image/png;base64," (:image result))}]])])))
```

## Complete Example (Movie Review)

```clojure
(ns movie-review.core
  (:require [reagent.dom :as rdom]
            [showcase.ui :as ui]))

(defn render-movie-history-item
  "Render a single movie history item"
  [item state]
  (let [result (:result item)
        movie-name (get-in result [:movie :title])]
    [:div.history-item
     {:on-click #(swap! state assoc :result result)}
     [:h4 movie-name]
     [:p.time (format-timestamp (:timestamp item))]]))

(defn input-section [state api-url]
  [:div
   [ui/history-toggle-button state api-url]
   
   [:div.search-box
    [ui/text-input state :movie-name "Enter movie name..."]
    [ui/action-button state "Analyze"
     #(ui/execute-agent! state api-url {:movie-name (:movie-name @state)})]]])

(defn app []
  (let [state (ui/create-state {:movie-name ""})]
    (fn []
      [:div.container
       [:div.input-section
        [:h1 "🎬 Movie Review Agent"]
        [input-section state "http://localhost:3000"]]
       
       ;; History panel
       [ui/history-panel state "http://localhost:3000" render-movie-history-item]
       
       [ui/progress-bar state]
       [ui/loading-display state "Analyzing movie..."]
       [ui/error-display state {:dismissible? true}]
       
       (when-let [result (:result @state)]
         [:div.result-section
          [:div {:dangerouslySetInnerHTML {:__html (simple-markdown (:review result))}}]])])))
```

## Styling History Items

Add custom styles for your history items in your showcase CSS:

```css
/* Image-specific history styling */
.history-item img {
    width: 80px;
    height: 80px;
    object-fit: cover;
}

.history-meta {
    flex: 1;
    padding-left: 15px;
}

.history-item .prompt {
    font-style: italic;
    color: rgba(255, 255, 255, 0.8);
    margin-bottom: 5px;
}

.history-item .time {
    font-size: 0.85rem;
    color: rgba(255, 255, 255, 0.6);
}
```

## Features

- ✅ Automatic history storage (last 50 items)
- ✅ Timestamp tracking
- ✅ One-click restore from history
- ✅ Lazy loading (fetched only when panel opens)
- ✅ Customizable rendering per showcase
- ✅ Responsive design

## Tips

1. **Keep render-item-fn lightweight** - It's called for each history entry
2. **Show relevant preview** - Images: thumbnails, Text: first few lines
3. **Add metadata** - Timestamp, parameters used, etc.
4. **Make items clickable** - Restore previous results easily
5. **Limit what you show** - Don't overwhelm with too much history at once
