(ns notifsta-clone.components.navbar
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn computer-tablet-navbar-view [_ owner]
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
            #js {:className "item" :href "#/"}
            "Create Event")
          )
        )
      )
    )
  )

(defn logged-in-navbar-view [_ owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "computer"}
        (om/build computer-tablet-navbar-view nil)
        )
      )
    )
  )
