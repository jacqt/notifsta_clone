(ns notifsta-clone.components.create-event
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljsjs.jquery]
            [notifsta-clone.utils.auth :as auth]
            [notifsta-clone.utils.http :as http]))

(defn handle-change [e data edit-key owner]
  (om/transact! data edit-key (fn [_] (.. e -target -value))))

(defn create-event-form-view [new-event owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [event-address-input (-> owner om/get-node js/$. (.find ".event-address-input") (.get 0))]
        (let [autocomplete (js/google.maps.places.Autocomplete. event-address-input)]
          (om/set-state! owner :autocomplete autocomplete)
          (.addListener
            autocomplete
            "place_changed"
            #(om/transact! new-event :event-address (fn [_] (.-value event-address-input)))))))

    om/IRenderState
    (render-state [_ {:keys []}]
      (dom/div
        #js {:className "create-event-form ui grid"}
        (dom/form
          #js {:className "ui form twelve wide column centered segment"}
          (dom/h1 #js {:className "page-title"} "Create your event")
          (dom/div
            #js {:className "field"}
            (dom/input
              #js {:placeholder "Event name"
                   :onChange #(handle-change % new-event :event-name owner)
                   :value (:event-name new-event)
                   :type "text"}))
          (dom/div
            #js {:className "field"}
            (dom/input
              #js {:className "event-address-input"
                   :placeholder "Event Address"
                   :onChange #(handle-change % new-event :event-address owner)
                   :value (:event-address new-event)
                   :type "text"})
            (dom/h3 nil (str "Event name: " (:event-name new-event)))
            (dom/h3 nil (str "Event address: " (:event-address new-event)))))))))

(defn create-event-view [new-event owner]
  (reify
    om/IInitState
    (init-state [this] { :step 0 })

    om/IRenderState
    (render-state [_ _]
      (dom/div
        #js {:className "create-event-view ui container"}
        (om/build create-event-form-view new-event)))))
