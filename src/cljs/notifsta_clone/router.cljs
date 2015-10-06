(ns notifsta-clone.router
  (:require [secretary.core :as secretary :include-macros true :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [notifsta-clone.utils.http :as http])
  (:import goog.History))

(defn route-app [app-state]
  (defroute
    "/" []
    (swap! app-state assoc :route :home))

  (defroute
    "/create_event" []
    (swap! app-state assoc :route :create-event))

  (defroute
    "/event/:id" [id]
    (http/get-event
      id
      (fn [response]
        (let [new-event (:data response)]
          (swap! app-state assoc :route :event)
          (swap! app-state assoc :current-event new-event))))))

; enable fallback that don't have HTML 5 History
(secretary/set-config! :prefix "#")

; Quick and dirty history configuration.
(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
