(ns notifsta-clone.components.create-event
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.pprint :refer [pprint]]
            [notifsta-clone.models :as models]
            [notifsta-clone.utils.auth :as auth]
            [notifsta-clone.utils.inputs :as inputs]
            [notifsta-clone.utils.http :as http]
            [notifsta-clone.router :as router]
            ))

(def required-fields-1 [:name :description :address :start_time :end_time])

(defn step-view [{:keys [title description completed]}]
  (reify
    om/IRender
    (render [_]
      (dom/div
        #js {:className (if completed "completed step" "active step")}
        (dom/div
          #js {:className "content"}
          (dom/div
            #js {:className "title"}
            title)
          (dom/div
            #js {:className "description"}
            description))))))

(defn step-list-view [{:keys [steps current-step-index]} owner]
  (reify
    om/IRenderState
    (render-state [_ _]
      (dom/div
        #js {:className "ui ordered steps center fluid"}
        (om/build-all step-view (map (fn [step] (merge step {:completed true})) (take current-step-index steps)))
        (om/build-all step-view (map (fn [step] (merge step {:completed false})) (drop current-step-index steps)))))))

(defn create-event-step-view [_ owner]
  (reify
    om/IRenderState
    (render-state [_ _]
      (let [new-event (om/observe owner (models/new-event))]
        (om/build step-list-view {:steps [{:title "General" :description "Fill out some basic details"}
                                          {:title "Images" :description "Upload your event images"}
                                          {:title "Finish!" :description "Go to your event page!"} ]
                                  :current-step-index (:current-step-index new-event)})))))

(defn invalid? [val]
  (or (= val "") (nil? val)))

(defn validate-form-1 [new-event]
  (let [{:keys [succeeded error-msg]}
        (reduce
          (fn [{:keys [succeeded error-msg]} next-field]
            (if (invalid? (next-field new-event))
              {:succeeded false
               :error-msg (str error-msg (name next-field) ", ") }
              {:succeeded succeeded
               :error-msg error-msg }))
          {:succeeded true
           :error-msg "Missing required fields: " }
          required-fields-1)]
    (if succeeded
      (do
        (http/xhr {:method "GET"
                   :base-url "https://maps.googleapis.com/maps/api/timezone/json"
                   :url-params {:location (str (:address-lat new-event) "," (:address-long new-event))
                                :timestamp  (.toString (/ (js/Math.round (.getTime (js/Date.))) 1000))
                                :sensor false}
                   :on-complete (fn [response]
                                  (om/transact! new-event :timezone (fn [_] (:timeZoneId response)))
                                  (om/transact! new-event :errors (fn [_] ""))
                                  (http/post-new-event
                                    new-event
                                    (fn [response]
                                      (js/console.log (clj->js response))
                                      (case (:status response)
                                        "success" (om/update! new-event (merge (:data response) {:current-step-index 1}))
                                        "failure" (do
                                                    (js/console.log "Failed to create event" )
                                                    (om/transact! new-event :errors (fn [_] (:error response))))))))}))

      (om/transact! new-event :errors (fn [_] (.slice error-msg 0 -2))))))

(defn create-event-step-1 [new-event owner]
  (reify
    om/IRender
    (render [_]
      (dom/div
        nil
        (dom/div
          #js {:className "field"}
          (dom/label nil "Event Name")
          (om/build inputs/editable-input [new-event {:edit-key :name
                                                      :className "event-name-input"
                                                      :placeholder-text "Your event name"}]))
        (dom/div
          #js {:className "field"}
          (dom/label nil "Description")
          (om/build inputs/editable-input [new-event {:edit-key :description
                                                      :className "event-description-input"
                                                      :placeholder-text "Event description"}]))
        (dom/div
          #js {:className "field"}
          (dom/label nil "Event Address")
          (om/build inputs/address-autocomplete-input [new-event {:edit-key :address
                                                                  :lat-key :address-lat
                                                                  :long-key :address-long
                                                                  :className "ui input event-address-input"
                                                                  :placeholder-text "Type your address here"}]))
        (dom/div
          #js {:className "field"}
          (dom/label nil "Start time")
          (om/build inputs/datetime-picker-input [new-event {:edit-key :start_time
                                                             :min-date 0
                                                             :className "start_time-input"
                                                             :placeholder-text "Start time"}]))
        (dom/div
          #js {:className "field"}
          (dom/label nil "End time")
          (om/build inputs/datetime-picker-input [new-event {:edit-key :end_time
                                                             :min-date (-> new-event :start_time inputs/extract-date)
                                                             :className "end_time-input"
                                                             :placeholder-text "End time"}]))
        (dom/div
          #js {:className "field"}
          (dom/input
            #js {:className "ui positive button"
                 :onClick #(validate-form-1 new-event)
                 :type "button"
                 :value "Submit" }))))))

(defn handle-photo-url-update [new-event]
  (let [response-channel (http/post-event-update new-event)]
    (go
      (let [result (<! response-channel)]
        (case (:status result)
          "success" (om/transact! new-event :current-step-index inc)
          "error"  (om/transact! new-event :errors (fn [_] (:error result))))))))

(defn create-event-step-2 [new-event owner]
  (reify
    om/IRender
    (render [_]
      (dom/div
        nil
        (dom/div
          #js {:className "field"}
          (dom/label nil "Your cover photo")
          (om/build inputs/editable-input [new-event {:edit-key :cover_photo_url
                                                      :className "event-name-input"
                                                      :placeholder-text "URL of your cover photo"}]))
        (dom/div
          #js {:className "field"}
          (dom/label nil "Your event map url")
          (om/build inputs/editable-input [new-event {:edit-key :event_map_url
                                                      :className "event-address-input"
                                                      :placeholder-text "URL of the image of your event map"}]))
        (dom/div
          #js {:className "field"}
          (dom/input
            #js {:className "ui button"
                 :onClick (fn [e] (om/transact! new-event :current-step-index inc))
                 :type "button"
                 :value "Skip" })
          (dom/input
            #js {:className "ui positive button"
                 :onClick #(handle-photo-url-update new-event)
                 :type "button"
                 :value "Submit" }))))))

(defn final-message [new-event owner]
  (reify
    om/IRender
    (render [_]
      (dom/div
        nil
        (dom/h3 #js {:className "page-title"} "Congratulations, you've created your event!" )
        (dom/p nil "We're excited to have you use our app for you event. Here's a brief list of what your next steps can be:")
        (dom/ul nil
          (dom/li nil "Send push notifications to your users")
          (dom/li nil "Create a timetable for your event")
          (dom/li nil "Customize your app with links to your homepage or Facebook page"))
        (dom/a
          #js {:className "ui big positive button"
               :href (str "#/event/" (:id new-event))}
          "Lets get started!")))))

(defn create-event-form-view [_ owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys []}]
      (let [new-event (om/observe owner (models/new-event))]
        (dom/div
          #js {:className "create-event-form ui grid"}
          (dom/form
            #js {:className "ui form twelve wide column centered segment"}
            (dom/h1 #js {:className "page-title"} "Create your event")
            (om/build create-event-step-view nil)
            (case (:current-step-index new-event)
              0 (om/build create-event-step-1 new-event)
              1 (om/build create-event-step-2 new-event)
              2 (om/build final-message new-event))
            (dom/div
              #js {:className "error field"}
              (:errors new-event))))))))


(defn create-event-view [_ owner]
  (reify
    om/IInitState
    (init-state [this] { :step 0 })

    om/IRenderState
    (render-state [_ _]
      (dom/div
        #js {:className "create-event-view ui container"}
        (om/build create-event-form-view {})))))
