(ns notifsta-clone.components.event
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [put! chan <!]]
            [cljsjs.d3]
            [cljsjs.jquery]
            [cljsjs.moment]
            [cljsjs.plottable]
            [notifsta-clone.models :as models]
            [notifsta-clone.utils.inputs :as inputs]
            [notifsta-clone.utils.auth :as auth]
            [notifsta-clone.utils.http :as http]))

;; TODO Refactor this button out
(defn one-two-state-button [{:keys [conditional-func on-toggle-edit on-save edit-text edit-icon]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div
        #js {:className "ui right floated header"}
        (if (conditional-func)
          (dom/div
            #js {:className "ui positive vertical animated button"
                 :onClick on-save}
            (dom/div
              #js {:className "hidden content"}
              "Submit")
            (dom/div
              #js {:className "visible content"}
              (dom/i
                #js {:className "checkmark icon"
                     :style #js {:margin-right 0}}))))
        (dom/div
          #js {:className "ui basic vertical animated button"
               :onClick on-toggle-edit}
          (dom/div
            #js {:className "hidden content"}
            (if (conditional-func) "Cancel" (if (some? edit-text) edit-text "New")))
          (dom/div
            #js {:className "visible content"}
            (dom/i
              #js {:className (if (conditional-func) "cancel icon" (if (some? edit-icon) edit-icon "plus icon"))
                   :style #js {:margin-right 0}})))))))

;; Useful functions for this component
(defn admin? [event]
  (-> event :subscription :admin))

;; Submits an update to an event to the server
;; - Returns a channel to listen on
(defn submit-update [updated-event]
  (http/post-event-update updated-event))

;; Publishes a draft notification for an event
;; - Returns a channel to listen on
(defn publish-draft-notification [draft-notification current-event]
  (http/post-new-notification draft-notification current-event))

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
                #js {:className "item"
                     :href "#/create_event" }
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

(defn handle-edit-accept-click [owner current-event temp-event _]
  (let [response-channel (submit-update temp-event)]
    (go
      (let [result (<! response-channel)]
        (case (:status result)
          "success" (do
                      (om/update! current-event (om/value temp-event))
                      (om/update! temp-event (models/empty-event))
                      (om/set-state! owner :editing false))
          "error"  (js/console.log "Error"))))))

;; view of the details like address, time, links of event
(defn event-content-detail-view [current-event owner]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false })
    om/IRenderState
    (render-state [this state]
      (let [temp-event (om/observe owner (models/temp-event))]
        (dom/div
          #js {:className "ui segment summary-content"}
          (dom/h2 #js {:className "ui left floated header"} "Summary")
          (if (admin? current-event)
            (om/build one-two-state-button {:conditional-func #(:editing state)
                                            :edit-text "Edit"
                                            :edit-icon "edit icon"
                                            :on-toggle-edit #(do
                                                               (om/update! temp-event (om/value current-event))
                                                               (om/set-state! owner :editing (-> state :editing not)))
                                            :on-save (partial handle-edit-accept-click owner current-event temp-event)}))
          (dom/div
            #js {:className "ui divided items"}
            (dom/div
              #js {:className "item"}
              (dom/div
                #js {:className "ui image"}
                (dom/i #js {:className "big info icon"}))
              (dom/div
                #js {:className "middle aligned content"}
                (if (:editing state)
                  (dom/div
                    #js {:className "ui fluid input"}
                    (om/build inputs/editable-input [temp-event {:edit-key :description
                                                                 :className "event-description-input"
                                                                 :placeholder-text "Event description "}]))
                  (:description current-event))))
            (dom/div
              #js {:className "item"}
              (dom/div
                #js {:className "ui image"}
                (dom/i #js {:className "big wait icon"}))
              (dom/div
                #js {:className "middle aligned content"}
                (if (:editing state)
                  (dom/div
                    #js {:className "ui grid datetime-pickers"}
                    (dom/div
                      #js {:className "row"}
                      (dom/div
                        #js {:className "ui fluid input"}
                        (om/build inputs/datetime-picker-input [temp-event {:edit-key :start_time
                                                                            :className "ui grid"
                                                                            :placeholder-text "Start time"}])))
                    (dom/div
                      #js {:className "row"}
                      (dom/div
                        #js {:className "ui fluid input"}
                        (om/build inputs/datetime-picker-input [temp-event {:edit-key :end_time
                                                                            :className "ui grid"
                                                                            :placeholder-text "End time"}]))))
                  (dom/p
                    #js {}
                    (-> current-event :start_time js/moment. (.format "LLL"))
                    " - "
                    (-> current-event :end_time js/moment. (.format "LLL")))
                  )))
            (dom/div
              #js {:className "item"}
              (dom/div
                #js {:className "ui image"}
                (dom/i #js {:className "big location arrow icon"}))
              (dom/div
                #js {:className "middle aligned content"}
                (if (:editing state)
                  (dom/div
                    #js {:className "ui fluid input"}
                    (om/build inputs/address-autocomplete-input [temp-event {:edit-key :address
                                                                             :className "event-address-input"
                                                                             :placeholder-text "Event location"}]))
                  (:address current-event))))
            (dom/div
              #js {:className "item"}
              (dom/div
                #js {:className "ui image"}
                (dom/i #js {:className "big home icon"}))
              (dom/div
                #js {:className "middle aligned content"}
                (if (:editing state)
                  (dom/div
                    #js {:className "ui fluid input"}
                    (om/build inputs/editable-input [temp-event {:edit-key :website_url
                                                                 :className "event-website-url-input"
                                                                 :placeholder-text "Your Website Homepage"}]))
                  (dom/a
                    #js {:href (:website_url current-event)}
                    (:website_url current-event)))))
            (dom/div
              #js {:className "item"}
              (dom/div
                #js {:className "ui image"}
                (dom/i #js {:className "big facebook square icon"}))
              (dom/div
                #js {:className "middle aligned content"}
                (if (:editing state)
                  (dom/div
                    #js {:className "ui fluid input"}
                    (om/build inputs/editable-input [temp-event {:edit-key :facebook_url
                                                                 :className "event-facebook-url-input"
                                                                 :placeholder-text "Your Facebook Page"}]))
                  (dom/a
                    #js {:href (:facebook_url current-event)}
                    (:facebook_url current-event)))))))))))

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

(defn draft-notification-view [state owner]
  (reify
    om/IRender
    (render [_]
      (dom/div
        #js {}
        (dom/div
          #js {:className "ui fluid input"}
          (om/build inputs/editable-input [state {:edit-key :notification
                                                  :className "draft-notification"
                                                  :placeholder-text "New notification" }]))
        (dom/div #js {:className "ui divider"})))))

(defn handle-notification-sent [draft-notification channel-id notifications]
  (let [response-channel (http/post-new-notification (:notification draft-notification) channel-id)]
    (go
      (let [result (<! response-channel)]
        (case (:status result)
          "success" (do
                      (om/update! draft-notification (models/empty-notification))
                      (om/update! notifications (cons (:data result) notifications )))
          "error" (js/console.log "Error in sending notification" (clj->js result)))))))

;; view of a list of notifications
(defn event-content-notifications-view [notifications owner]
  (reify
    om/IInitState
    (init-state [_]
      {:drafting-notification false})
    om/IRenderState
    (render-state [this state]
      (let [current-event (om/observe owner (models/current-event))
            temp-notification (om/observe owner (models/temp-notification))]
        (dom/div
          #js {:className "ui segment event-notifications"}
          (dom/div
            nil
            (dom/h2 #js {:className "ui left floated header"} "Notifications")
            (if (admin? current-event)
              (om/build one-two-state-button {:conditional-func #(:drafting-notification temp-notification)
                                              :on-toggle-edit #(om/update! temp-notification :drafting-notification (not (:drafting-notification temp-notification)))
                                              :on-save #(handle-notification-sent temp-notification (-> current-event :channels first :id) notifications) })
              ))
          (dom/div
            #js {:className "notification-content"}
            (if (:drafting-notification temp-notification)
              (om/build draft-notification-view temp-notification))
            (dom/div
              #js {:className "ui divided items notification-list"}
              (om/build-all notification-view notifications)))
          )))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; TIMETABLE COMPONENT ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handle-publish-subevent [timetable-events draft-subevent current-event-id]
  (js/console.log (:id draft-subevent))
  (let [response-channel (if (some? (:id draft-subevent))
                           (http/post-subevent-update draft-subevent)
                           (http/post-new-subevent draft-subevent current-event-id))]
    (go (let [result (<! response-channel)]
          (case (:status result)
            "success" (http/get-subevents
                        current-event-id
                        (fn [response]
                          (om/update! timetable-events (:data response))
                          (om/update! draft-subevent (models/empty-subevent))))
            "failure" (js/console.log "Error in creating subevent " (clj->js result)))))))

(defn delete-subevent [current-event subevent-id]
  (let [response-channel (http/delete-subevent subevent-id)]
    (go (let [result (<! response-channel)]
          (case (:status result)
            "success" (http/get-subevents (:id current-event) (fn [response] (om/update! (:subevents current-event) (:data response))))
            "failure" (js/console.log "Error in deleting subevent " (clj->js result)))))) )

(defn subevent-view [subevent owner]
  (reify
    om/IInitState
    (init-state [_]
      {:moused-over false})
    om/IRenderState
    (render-state [this _]
      (let [current-event (om/observe owner (models/current-event))
            temp-event (om/observe owner (models/temp-subevent)) ]
        (dom/div
          #js {:className (str "ui grid subevent-view " (if (admin? current-event) "admin"))
               :onClick #(if (admin? current-event)
                           (do
                             (om/update! temp-event (om/value subevent))  
                             (om/update! temp-event :drafting-subevent true)))}
          (dom/div
            #js {:className "two wide column"}
            (dom/b
              nil
              (-> subevent :start_time js/moment. (.format "hh:mm")))
            (dom/p
              nil
              (-> subevent :end_time js/moment. (.format "hh:mm"))))
          (dom/div
            #js {:className "eleven wide column"}
            (dom/b
              nil
              (:name subevent))
            (dom/div
              nil
              (dom/i #js {:className "marker icon"})
              (:location subevent)))
          (if (admin? current-event)
            (dom/div
              #js {:className "one wide column delete-button"}
              (dom/div
                #js {:className "ui basic vertical animated button" 
                     :onClick #(do
                                 (delete-subevent current-event (:id subevent))
                                 (.stopPropagation %))}
                (dom/div
                  #js {:className "hidden content"}
                  "Delete")
                (dom/div
                  #js {:className "visible content"}
                  (dom/i
                    #js {:className "trash icon"
                         :style #js {:margin-right 0}}))))))))))

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
              (om/build-all subevent-view (:events timetable-segment)))))))))

(defn draft-subevent-view [draft-subevent owner]
  (reify
    om/IRender
    (render [_]
      (dom/div
        #js {:className "ui form twelve wide column centered segment"}
        (dom/div
          #js {:className "field"}
          (dom/label nil "Event Name")
          (om/build inputs/editable-input [draft-subevent {:edit-key :name
                                                           :placeholder-text "Your event name"}]))
        (dom/div
          #js {:className "field"}
          (dom/label nil "Location")
          (om/build inputs/editable-input [draft-subevent {:edit-key :location
                                                           :placeholder-text "Location"}]))
        (dom/div
          #js {:className "field"}
          (dom/label nil "Start time")
          (om/build inputs/datetime-picker-input [draft-subevent {:edit-key :start_time
                                                                  :placeholder-text "Start time"}]))
        (dom/div
          #js {:className "field"}
          (dom/label nil "End time")
          (om/build inputs/datetime-picker-input [draft-subevent {:edit-key :end_time
                                                                  :min-date (-> draft-subevent :start_time inputs/extract-date)
                                                                  :placeholder-text "End time"}]))))))

;; view of the list of the timetable
(defn event-content-timetable-view [timetable-events owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (let [current-event (om/observe owner (models/current-event))
            temp-subevent (om/observe owner (models/temp-subevent))]
        (dom/div
          #js {:className "ui segment event-timetable"}
          (dom/h2 #js {:className "ui left floated header"} "Timetable")
          (if (admin? current-event)
            (om/build one-two-state-button {:conditional-func #(:drafting-subevent temp-subevent)
                                            :on-toggle-edit #(if (:drafting-subevent temp-subevent) 
                                                               (om/update! temp-subevent (models/empty-subevent))
                                                               (om/update! temp-subevent :drafting-subevent (not (:drafting-subevent temp-subevent))))
                                            :on-save #(handle-publish-subevent timetable-events temp-subevent (:id current-event))}))
          (dom/div
            #js {:className "timetable-container"}
            (if (:drafting-subevent temp-subevent)
              (om/build draft-subevent-view temp-subevent))
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
                    (keys timetable-events)))))))))))

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
              (om/build event-content-detail-view current-event)
              (om/build
                event-content-timetable-view
                (:subevents current-event))
              (if (admin? current-event) (om/build event-stat-view current-event)))
            (dom/div
              #js {:className "six wide column row"}
              (om/build
                event-content-notifications-view
                (-> current-event :channels first :notifications)))))
        ;(dom/pre
          ;nil
          ;(with-out-str (pprint @current-event)))
        ))))

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
