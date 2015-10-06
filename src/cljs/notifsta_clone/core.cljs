(ns notifsta-clone.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljsjs.jquery]
            [secretary.core :as secretary :include-macros true :refer-macros [defroute]]
            [cljs.core.async :refer [put! chan <!]]
            [notifsta-clone.router :as router]
            [notifsta-clone.index :as index]
            [notifsta-clone.utils.auth :as auth]))

(enable-console-print!)

(defonce app-state (atom {:credentials (auth/get-credentials)
                          :current-event {}
                          :events {}
                          :new-event {}
                          :route nil }))

(defn main []
  (router/route-app app-state)
  (secretary/dispatch!
    (.substring (.. js/window -location -hash) 1))
  (om/root
    index/index-view
    app-state
    {:target (js/document.getElementById "app")}))
