(ns notifsta-clone.components.create-event
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.pprint :refer [pprint]]
            [cljsjs.jquery]
            [cljsjs.jquery-ui]
            [cljsjs.bootstrap]
            [cljsjs.bootstrap-timepicker]
            [notifsta-clone.utils.auth :as auth]
            [notifsta-clone.utils.http :as http]))

;;; These components require jquery, jquery-ui, bootstrap, and bootstrap-timepicker

;; Generic component for an input box with a two-way databinding
;; between a property of the state and the value of the input
;;
;; Works by passing into the component a vector of length two
;; with the first being a cursor to the parent of the input, and the second
;; being a map with the keys
;;  :className        (maps to the classname on the input created)
;;  :edit-key         (the key in the parent-state to the text)
;;  :placeholder-text (the placeholder text in the creatd input)

(defn handle-change [e data edit-key]
  (om/transact! data edit-key (fn [_] (.. e -target -value))))

(defn editable-input [[parent-state {:keys [className edit-key placeholder-text]}]  owner]
  (reify
    om/IRenderState
    (render-state [_ _]
      (dom/input
        #js {:className className
             :placeholder placeholder-text
             :onChange #(handle-change % parent-state edit-key)
             :value (edit-key parent-state)
             :type "text" }))))


;; Generic component for using google places autocomplete input
;; Same API as the editable-input component above
(defn address-autocomplete-input [[parent-state {:keys [className edit-key placeholder-text]}] owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [address-input (om/get-node owner)]
        (let [autocomplete (js/google.maps.places.Autocomplete. address-input)]
          (.addListener
            autocomplete
            "place_changed"
            #(om/transact! parent-state edit-key (fn [_] (.-value address-input)))))))
    om/IRenderState
    (render-state [_ _]
      (om/build editable-input [parent-state {:className className
                                              :edit-key edit-key
                                              :placeholder-text placeholder-text }]))))

;; Generic datepicker component that uses jquery-ui for the calendar view
;; Same API as the editable-input component above along with the min-date parameter
(defn datepicker-input [[parent-state {:keys [className min-date placeholder-text edit-key]}] owner]
  (reify
    om/IDidUpdate
    (did-update [_ _ _]
      (->
        owner
        om/get-node
        js/$.
        (.datepicker
          #js {:minDate min-date
               :onSelect #(om/transact! parent-state edit-key (fn [_] %)) })))

    om/IRenderState
    (render-state [_ _]
      (dom/input
        #js {:className className
             :placeholder placeholder-text
             :onChange #(handle-change % parent-state edit-key)
             :value (edit-key parent-state) 
             :type "text" }))))


;; Generic timepicker component that uses the bootstrap timepicker library
;; Same API as the editable-input component
(defn timepicker-input [[parent-state {:keys [className min-date placeholder-text edit-key]}] owner]
  (reify
    om/IDidUpdate
    (did-update [_ _ _]
      (let
        [timepicker
         (->
           owner
           om/get-node
           js/$.
           (.timepicker
             #js {:minuteStep 5
                  :showInputs false
                  :disableFocus true
                  :template false }))]
        (.on
          timepicker
          "changeTime.timepicker"
          (fn [e]
            (om/transact! parent-state edit-key (fn [_] (.. e -time -value)))))))

    om/IRenderState
    (render-state [this state]
      (dom/input 
        #js {:className className
             :onChange #(handle-change % parent-state edit-key)
             :value (-> parent-state edit-key)
             :type "text" }))))

;; Geneirc date and time picker. Combines the date picker and time picker components above into one
(defn datetime-picker-input [[parent-state {:keys [className min-date placeholder-text edit-key]}] owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (when (nil? (edit-key parent-state))
        (om/update! parent-state edit-key {:date nil
                                           :time nil })))

    om/IRenderState
    (render-state [this state]
      (dom/div
        #js {:className (str "two fields " className) }
        (dom/div
          #js {:className "twelve wide field"}
          (om/build datepicker-input [(edit-key parent-state) {:edit-key :date
                                                               :min-date 0
                                                               :className "start-time-input"
                                                               :placeholder-text placeholder-text}]))
        (dom/div
          #js {:className "four wide field"}
          (om/build timepicker-input [(edit-key parent-state) {:edit-key :time
                                                               :className "start-time-input"
                                                               :placeholder-text placeholder-text}]))))))


;; the create-event specific components
(defn create-event-form-view [new-event owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys []}]
      (dom/div
        #js {:className "create-event-form ui grid"}
        (dom/form
          #js {:className "ui form twelve wide column centered segment"}
          (dom/h1 #js {:className "page-title"} "Create your event")
          (dom/div
            #js {:className "field"}
            (dom/label nil "Event Name")
            (om/build editable-input [new-event {:edit-key :event-name
                                                 :className "event-name-input"
                                                 :placeholder-text "Your event name"}]))
          (dom/div
            #js {:className "field"}
            (dom/label nil "Event Address")
            (om/build address-autocomplete-input [new-event {:edit-key :event-address
                                                             :className "event-address-input"
                                                             :placeholder-text "Type your address here"}]))
          (dom/div
            #js {:className "field"}
            (dom/label nil "Start time")
            (om/build datetime-picker-input [new-event {:edit-key :start-time
                                                        :min-date 0
                                                        :className "start-time-input"
                                                        :placeholder-text "Start time"}]))
          (dom/div
            #js {:className "field"}
            (dom/label nil "End time")
            (om/build datetime-picker-input [new-event {:edit-key :end-time
                                                        :min-date (:start-time new-event)
                                                        :className "end-time-input"
                                                        :placeholder-text "End time"}]))
          (dom/pre nil (with-out-str (pprint @new-event))))))))

(defn create-event-view [new-event owner]
  (reify
    om/IInitState
    (init-state [this] { :step 0 })

    om/IRenderState
    (render-state [_ _]
      (dom/div
        #js {:className "create-event-view ui container"}
        (om/build create-event-form-view new-event)))))
