(ns notifsta-clone.components.event
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [put! chan <!]]
            [notifsta-clone.utils.auth :as auth]
            [notifsta-clone.utils.http :as http]))

(defn event-header-view [{:keys [current-event credentials]} owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "event-header pusher"}
        (dom/div
          #js {:className "ui inverted vertical masthead center aligned segment"
               :style #js {:background-image (str "url(" (:cover_photo_url current-event) ")")}}
          (dom/div
            #js {:className "ui container"}
            (dom/div
              #js {:className "ui large secondary inverted pointing menu"}
              (dom/a
                #js {:className "active item"
                     :href "#/"
                     }
                "Home")
              (dom/a
                #js {:className "item"}
                "Create Event")
              (dom/div
                #js {:className "right item"}
                (dom/a
                  #js {:className "ui inverted button"
                       :onClick (fn [e] (auth/logout credentials)) }
                  "Logout"))))
          (dom/div
            #js {:className "ui text container"}
            (dom/h1
              #js {:className "ui inverted header"}
              (:name current-event))))))))

(defn event-content-view [current-event owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "event-content"}
        (dom/pre
          nil
          (with-out-str (pprint @current-event)))
        ))))

(defn event-view [{:keys [current-event credentials]} owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (http/get-event
        (:id current-event)
        (fn [response]
          (let [new-event (:data response)]
            (om/update! current-event new-event)))))

    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "event-view"}
        (om/build event-header-view {:current-event current-event
                                     :credentials credentials })
        (om/build event-content-view current-event)))))
