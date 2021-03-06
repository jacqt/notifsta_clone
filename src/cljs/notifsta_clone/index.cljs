(ns notifsta-clone.index
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljsjs.jquery]
            [cljs.pprint :as pprint]
            [cljs.core.async :refer [put! chan <!]]
            [notifsta-clone.components.create-event :as create-event]
            [notifsta-clone.components.dashboard :as dashboard]
            [notifsta-clone.components.event :as event]
            [notifsta-clone.components.landing-page :as landing-page]
            [notifsta-clone.components.navbar :as navbar]
            [notifsta-clone.utils.http :as http]
            [notifsta-clone.utils.auth :as auth]))

(defn index-logged-in-view [state owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (http/get-user
        (fn [response]
          (let [user (:data response)]
            (om/update! state :user user))))
      (http/get-all-events
        (fn [response]
          (let [events (:data response)]
            (om/update! state :events events)))))

    om/IRenderState
    (render-state [this _]
      (dom/div
        nil
        (if-not (= :event (:route @state))
          (om/build navbar/logged-in-navbar-view state))
        (case (@state :route)
          :home (om/build dashboard/dashboard-view state)
          :create-event (om/build create-event/create-event-view (:new-event state))
          :event (om/build event/event-view {:current-event (:current-event state)
                                              :credentials (:credentials state)})
          (om/build dashboard/dashboard-view state))))))

(defn index-view [state owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (if (empty? (@state :credentials))
        (om/build landing-page/landing-page-view state)
        (om/build index-logged-in-view state)))))
