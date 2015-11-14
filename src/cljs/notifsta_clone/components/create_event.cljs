(ns notifsta-clone.components.create-event
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.pprint :refer [pprint]]
            [notifsta-clone.models :as models]
            [notifsta-clone.utils.auth :as auth]
            [notifsta-clone.utils.inputs :as inputs]
            [notifsta-clone.utils.http :as http]))

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
        #js {:className "ui ordered steps"}
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

(defn create-event-step-1 [new-event owner]
  (reify
    om/IRender
    (render [_]
      (dom/div
        nil
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
                                                             :min-date (-> new-event :start_time :date)
                                                             :className "end_time-input"
                                                             :placeholder-text "End time"}]))))))


(defn create-event-step-2 [new-event owner]
  (reify
    om/IRender
    (render [_]
      (dom/div
        nil
        (dom/div
          #js {:className "field"}
          (dom/label nil "Your cover photo")
          (om/build inputs/editable-input [new-event {:edit-key :event-name
                                                      :className "event-name-input"
                                                      :placeholder-text "Your event name"}]))
        (dom/div
          #js {:className "field"}
          (dom/label nil "Your asdfasdf")
          (om/build inputs/address-autocomplete-input [new-event {:edit-key :event-address
                                                                  :className "event-address-input"
                                                                  :placeholder-text "Type your address here"}]))))))
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
              2 (dom/div nil "YOU'RE DONE"))
            (dom/div
              #js {:className "field"}
              (dom/input
                #js {:className "ui button"
                     :onClick (fn [e] (om/transact! new-event :current-step-index inc))
                     :type "button"
                     :value "Next" }))
            (dom/pre nil (with-out-str (pprint @new-event)))))))))


(defn create-event-view [_ owner]
  (reify
    om/IInitState
    (init-state [this] { :step 0 })

    om/IRenderState
    (render-state [_ _]
      (dom/div
        #js {:className "create-event-view ui container"}
        (om/build create-event-form-view {})))))
