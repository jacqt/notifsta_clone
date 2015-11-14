(ns notifsta-clone.components.navbar
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [notifsta-clone.utils.auth :as auth]
            ))

(defn computer-tablet-navbar-view [app-state owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "computer tablet only row"}
        (dom/div
          #js {:className "ui inverted menu navbar"}
          (dom/a
            #js {:className "brand item" :href "#/"}
            "Notifsta")
          (dom/a
            #js {:className "item" :href "#/create_event"}
            "Create Event")
          (dom/div
            #js {:className "right menu"}
            (dom/a
              #js {:className "item"
                   :onClick #(auth/logout (:credentials app-state))}
              "Logout")))))))

(defn logged-in-navbar-view [app-state owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "computer"}
        (om/build computer-tablet-navbar-view app-state)))))
