(ns notifsta-clone.components.event
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [put! chan <!]]
            [cljsjs.moment]
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
                     :href "#/" }
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

(defn event-content-detail-view [current-event owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "ui segment"}
        (dom/h2 nil "Summary")
        (dom/div
          #js {:className "ui divided items"}
          (dom/div
            #js {:className "item"}
            (dom/div
              #js {:className "ui image"}
              (dom/i #js {:className "big wait icon"}))
            (dom/div
              #js {:className "middle aligned content"}
              "September, 26th 2015"))
          (dom/div
            #js {:className "item"}
            (dom/div
              #js {:className "ui image"}
              (dom/i #js {:className "big location arrow icon"}))
            (dom/div
              #js {:className "middle aligned content"}
              (:address current-event)))
          (dom/div
            #js {:className "item"}
            (dom/div
              #js {:className "ui image"}
              (dom/i #js {:className "big home icon"}))
            (dom/div
              #js {:className "middle aligned content"}
              (dom/a
                #js {:href (:website_url current-event)}
                (:website_url current-event))))
          (dom/div
            #js {:className "item"}
            (dom/div
              #js {:className "ui image"}
              (dom/i #js {:className "big facebook square icon"}))
            (dom/div
              #js {:className "middle aligned content"}
              (dom/a
                #js {:href (:facebook_url current-event)}
                (:facebook_url current-event)))))))))

(defn notification-view [notification]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "item"}
        (dom/div
          #js {:className "middle aligned content"}
          (dom/div
            #js {:className "date"}
            (.fromNow (js/moment. (:created_at notification))))
          (:notification_guts notification))))))

(defn event-content-notifications-view [channels]
  (reify
    om/IRenderState
    (render-state [this _]
    (dom/div
      #js {:className "ui segment event-notifications"}
      (dom/h2 nil "Notifications")
      (dom/div
        #js {:className ""}
         (dom/div
          #js {:className "ui divided items"}
          (om/build-all notification-view (-> channels first :notifications)))
        )
      ))))


(defn event-content-view [current-event owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "event-content"}
        (dom/div
          #js {:className "ui centered grid"}
          (dom/div
            #js {:className "twelve wide column row"}
            (dom/div
              #js {:className "ten wide column"}
              (om/build event-content-detail-view current-event)
              (dom/div
                #js {:className "ui segment" }
                (:description current-event)))
            (dom/div
              #js {:className "six wide column"}
              (om/build event-content-notifications-view (:channels current-event)))))
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
        #js {:className "event-view container"}
        (om/build event-header-view {:current-event current-event
                                     :credentials credentials })
        (om/build event-content-view current-event)))))
