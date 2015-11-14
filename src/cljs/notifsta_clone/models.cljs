(ns notifsta-clone.models
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [notifsta-clone.utils.auth :as auth]))

(defn empty-event []
  {:name ""
   :description ""
   :address ""
   :start_time ""
   :end_time ""})

(defn empty-notification []
  {:notification ""
   :drafting-notification false})

(defn empty-subevent []
  {:drafting-subevent false
   :name ""
   :location ""
   :start_time ""
   :end_time ""})

(defonce app-state (atom {:credentials (auth/get-credentials)
                          :current-event {:name ""
                                          :description ""
                                          :address ""
                                          :start_time ""
                                          :end_time "" }
                          :events {}
                          :new-event {:current-step-index 0
                                      :event-name ""
                                      :event-address ""
                                      :start_time ""
                                      :end_time ""}
                          :temp-event (empty-event)
                          :temp-notification (empty-notification)
                          :temp-subevent (empty-subevent)
                          :route nil }))

(defn new-event []
  (om/ref-cursor (:new-event (om/root-cursor app-state))))

(defn current-event []
  (om/ref-cursor (:current-event (om/root-cursor app-state))))

(defn temp-event []
  (om/ref-cursor (:temp-event (om/root-cursor app-state))))

(defn temp-notification []
  (om/ref-cursor (:temp-notification (om/root-cursor app-state))))

(defn temp-subevent []
  (om/ref-cursor (:temp-subevent (om/root-cursor app-state))))
