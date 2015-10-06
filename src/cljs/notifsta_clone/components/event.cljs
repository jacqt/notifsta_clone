(ns notifsta-clone.components.event
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [put! chan <!]]
            [cljsjs.d3]
            [cljsjs.jquery]
            [cljsjs.moment]
            [cljsjs.plottable]
            [notifsta-clone.utils.auth :as auth]
            [notifsta-clone.utils.http :as http]))

;; View of the header; image and title of event
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

;; view of the details like address, time, links of event
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
              (dom/i #js {:className "big info icon"}))
            (dom/div
              #js {:className "middle aligned content"}
              (:description current-event)))
          (dom/div
            #js {:className "item"}
            (dom/div
              #js {:className "ui image"}
              (dom/i #js {:className "big wait icon"}))
            (dom/div
              #js {:className "middle aligned content"}
              (-> current-event :start_time js/moment. (.format "LLL"))))
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

;; view of one notification
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

;; view of a list of notifications
(defn event-content-notifications-view [notifications]
  (reify
    om/IRenderState
    (render-state [this _]
    (dom/div
      #js {:className "ui segment event-notifications"}
      (dom/h2 nil  "Notifications")
      (dom/div #js {:className "ui divider"})
      (dom/div
        #js {:className "ui divided items notification-list"}
        (om/build-all notification-view notifications))
      ))))

(defn subevent-view [subevent owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "ui grid"}
        (dom/div
          #js {:className "two wide column"}
          (dom/b
            nil
            (-> subevent :start_time js/moment. (.format "hh:mm")))
          (dom/p
            nil
            (-> subevent :end_time js/moment. (.format "hh:mm"))))
        (dom/div
          #js {:className "ten wide column"}
          (dom/b
            nil
            (:name subevent))
          (dom/div
            nil
            (dom/i #js {:className "marker icon"})
            (:location subevent))
          )))))

;; view for a time segment in the timetable
(defn time-segment-view [timetable-segment owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "item"}
        (dom/div
          #js {:className "middle aligned content"}
          (dom/div
            #js {:className "timetable-time-segment"}
            (dom/h3
              #js {:className "timetable-time ui"}
              (-> timetable-segment :time name js/moment. (.format "LLL")))
            (dom/div
              #js {:className "timetable-content"}
              (om/build-all subevent-view (:events timetable-segment)))
            ))))))


;; view of the list of the timetable
(defn event-content-timetable-view [timetable-events owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "ui segment event-timetable"}
        (dom/h2 nil "Timetable")
        (dom/div #js {:className "ui divider"})
        (dom/div
          #js {:className "ui divided items timetable-segments"}
          (om/build-all
            time-segment-view
            (map
              (fn [key]
                {:time key
                 :events (key timetable-events) })
              (sort-by
                (fn [tstring]
                  (-> tstring name js/moment. (.unix)))
                (keys timetable-events)))))))))

(defn sum-subscriptions [subscriptions]
  (let [sorted-subs (sort-by (fn [sub] (-> sub :created_at js/moment. (.unix))) subscriptions)]
    (reduce
      (fn [data data-point]
        {:total-subscriptions (inc (:total-subscriptions data))
         :data-set (conj (:data-set data)
                         {:time (-> data-point :created_at js/moment. )
                          :subscriptions (inc (:total-subscriptions data))})})
      {:total-subscriptions 0 :data-set []}
      sorted-subs)))

(defn update-stats [event-id owner]
  (http/get-event-subscribers
    event-id
    (fn [response]
      (let [data-set (-> response :data vec sum-subscriptions :data-set vec clj->js js/Plottable.Dataset.)
            plot (om/get-state owner :plot) ]
        (.addDataset plot data-set)
        (om/set-state! owner :data-set data-set)))))

(defn event-stat-view [current-event owner]
  (reify
    om/IInitState
    (init-state [_]
      {:subscriptions nil :chart nil})

    om/IDidMount
    (did-mount [_]
      (.renderTo (om/get-state owner :chart) "svg.stat-sub-graph"))

    om/IWillMount
    (will-mount [_]
      (update-stats (:id current-event) owner)
      (let [plot (js/Plottable.Plots.Line. )
            xScale (js/Plottable.Scales.Time.)
            yScale (js/Plottable.Scales.Linear.)]
        (let [xAxis (js/Plottable.Axes.Time. xScale "bottom")
              yAxis (js/Plottable.Axes.Numeric. yScale "left")
              panZoomInteraction (js/Plottable.Interactions.PanZoom. xScale nil)]
          (.x plot #(aget %1 "time") xScale)
          (.y plot #(aget %1 "subscriptions") yScale)
          (.attachTo panZoomInteraction plot)
          (om/set-state! owner :plot plot)
          (om/set-state! owner :chart (js/Plottable.Components.Table. (clj->js [[yAxis plot] [nil xAxis]]))))))

    om/IRenderState
    (render-state [this state]
      (dom/div
        #js {:className "ui segment event-stats"}
        (dom/h2 nil "Statistics")
        (dom/div
          #js {}
          (dom/h3 nil "Cumulative Subscribers")
          (dom/svg #js {:className "stat-sub-graph"})
          )))

    om/IWillUpdate
    (will-update [_ next-props _]
      (if-not (= (:id (om.core/get-props owner)) (:id next-props))
        (do
          (if-not (nil? (om/get-state owner :data-set))
            (.removeDataset (om/get-state owner :plot) (om/get-state owner :data-set)))
          (update-stats (:id next-props) owner))))))

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
              (om/build event-stat-view current-event)
              (om/build event-content-detail-view current-event)
              (om/build
                event-content-timetable-view
                (:subevents current-event)))
            (dom/div
              #js {:className "six wide column"}
              (om/build
                event-content-notifications-view
                (-> current-event :channels first :notifications)))))
        (dom/pre
          nil
          (with-out-str (pprint @current-event)))))))

;; This is the view that is used to show the event page
(defn event-view [{:keys [current-event credentials]} owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "event-view container"}
        (om/build event-header-view {:current-event current-event
                                     :credentials credentials })
        (om/build event-content-view current-event)))))
