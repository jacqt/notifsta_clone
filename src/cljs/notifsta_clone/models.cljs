(ns notifsta-clone.models
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [notifsta-clone.utils.auth :as auth]))

(defonce app-state (atom {:credentials (auth/get-credentials)
                          :current-event {:event-name ""
                                          :event-address ""
                                          :start-time {}
                                          :end-time {}}
                          :events {}
                          :new-event {:event-name ""
                                      :event-address ""
                                      :start-time {}
                                      :end-time {}}
                          :route nil }))

(defn new-event []
  (om/ref-cursor (:new-event (om/root-cursor app-state))))
