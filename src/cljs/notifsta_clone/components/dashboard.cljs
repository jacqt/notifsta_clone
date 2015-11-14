(ns notifsta-clone.components.dashboard
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [notifsta-clone.models :as models]
            [notifsta-clone.utils.auth :as auth]
            [notifsta-clone.utils.http :as http]))

(defn enter-event-view [event]
  (pr (str "Entering event: " (:name event)))
  (set! js/window.location.hash (str "/event/" (:id event))))

(defn event-card-view [event owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "event-card ui fluid centered card"
             :onClick (fn [e] (enter-event-view event)) }
        (dom/div
          #js {:className "image"
               :style #js {:background-image (str "url("(:cover_photo_url event) ")")}})
        (dom/div
          #js {:className "content"}
          (dom/div
            #js {:className "header"}
            (:name event))
          (dom/div
            #js {:className "description"}
            (:description event)))))))

(defn dashboard-view [{:keys [credentials events]} owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (js/console.log (clj->js (:subscribed events)))
      (dom/div
        #js {:className "dashboard-view"}
        (dom/div
          #js {:className "dashboard-welcome"}
          (dom/h1 #js {:className "page-title"} "Welcome to Notifsta!"))
        (dom/div
          #js {:className "dashboard-content"}
          (dom/hr nil)
          (dom/h2 nil "Subscribed")
          (dom/div
            #js {:className "ui link four stackable cards event-cards"}
            (om/build-all event-card-view (:subscribed events)))
          (dom/h2 nil "Not subscribed")
          (dom/div
            #js {:className "ui link four stackable cards event-cards"}
            (om/build-all event-card-view (filter
                                            #(:published %)
                                            (:not_subscribed events)))))))))
