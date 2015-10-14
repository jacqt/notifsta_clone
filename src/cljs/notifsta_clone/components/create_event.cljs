(ns notifsta-clone.components.create-event
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.pprint :refer [pprint]]
            [notifsta-clone.models :as models]
            [notifsta-clone.utils.auth :as auth]
            [notifsta-clone.utils.inputs :as inputs]
            [notifsta-clone.utils.http :as http]))

;; the create-event specific components
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
            (dom/div
              #js {:className "field"}
              (dom/label nil "Event Name")
              (om/build inputs/editable-input [new-event {:edit-key :event-name
                                                          :className "event-name-input"
                                                          :placeholder-text "Your event name"}]))
            (dom/div
              #js {:className "field"}
              (dom/label nil "Event Address")
              (om/build inputs/address-autocomplete-input [new-event {:edit-key :event-address
                                                                      :className "event-address-input"
                                                                      :placeholder-text "Type your address here"}]))
            (dom/div
              #js {:className "field"}
              (dom/label nil "Start time")
              (om/build inputs/datetime-picker-input [new-event {:edit-key :start-time
                                                                 :min-date 0
                                                                 :className "start-time-input"
                                                                 :placeholder-text "Start time"}]))
            (dom/div
              #js {:className "field"}
              (dom/label nil "End time")
              (om/build inputs/datetime-picker-input [new-event {:edit-key :end-time
                                                                 :min-date (-> new-event :start-time :date)
                                                                 :className "end-time-input"
                                                                 :placeholder-text "End time"}]))
            (dom/pre nil (with-out-str (pprint @new-event)))))))))

(defn create-event-view [blah owner]
  (reify
    om/IInitState
    (init-state [this] { :step 0 })

    om/IRenderState
    (render-state [_ _]
      (dom/div
        #js {:className "create-event-view ui container"}
        (om/build create-event-form-view {})))))
