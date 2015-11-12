(ns notifsta-clone.models
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [notifsta-clone.utils.auth :as auth]))

(defonce app-state (atom {:credentials (auth/get-credentials)
                          :current-event {:event-name ""
                                          :event-address ""
                                          :start-time {}
                                          :end-time {}
                                          }
                          :events {}
                          :new-event {:current-step-index 0
                                      :event-name ""
                                      :event-address ""
                                      :start-time {}
                                      :end-time {}}
                          :temp-event {}
                          :route nil }))

(defn new-event []
  (om/ref-cursor (:new-event (om/root-cursor app-state))))

(defn current-event []
  (om/ref-cursor (:current-event (om/root-cursor app-state))))

(defn temp-event []
  (om/ref-cursor (:temp-event (om/root-cursor app-state))))
