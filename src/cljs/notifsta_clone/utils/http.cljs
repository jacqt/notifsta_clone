(ns notifsta-clone.utils.http
  (:require [notifsta-clone.utils.auth :as auth]
            [cljs.core.async :refer [put! chan <!]]
            [goog.uri.utils :as uri-utils]
            [goog.net.XhrIo :as net-xhrio]))

(enable-console-print!)

(def BASE_URL "http://api.notifsta.com/v1")
(def LOGIN_URL (str BASE_URL "/auth/facebook"))
(def USER_URL (str BASE_URL "/users/"))
(def EVENT_URL (str BASE_URL "/events/"))
(def CHANNEL_URL (str BASE_URL "/channels/"))

; parses goog.net.XhrIo response to a json
(defn parse-xhrio-response [response-channel success-callback fail-callback]
  (fn [response]
    (let [target (aget response "target")]
      (if (.isSuccess target)
        (let [json (.getResponseJson target)]
          (put! response-channel (js->clj json :keywordize-keys true))
          (success-callback (js->clj json :keywordize-keys true)))
        (let [error (.getLastError target)]
          (put! response-channel (js->clj error :keywordize-keys true))
          (fail-callback (js->clj error :keywordize-keys true)))))))

; wraps goog.net.XhrIo library in a simpler function xhr
(defn xhr [{:keys [method base-url url-params on-complete on-error]}]
  (let [response-channel (chan)]
    (.send
      goog.net.XhrIo
      (reduce
        (fn [partial-url param-key]
          (.appendParams
            goog.uri.utils
            partial-url
            (name param-key)
            (url-params param-key)))
        base-url
        (keys url-params))
      (parse-xhrio-response response-channel on-complete on-error)
      method)
    response-channel))

(defn login [facebook-id facebook-token email on-complete]
  (xhr {:method "GET"
        :base-url LOGIN_URL
        :url-params {:email email
                     :facebook_id facebook-id
                     :facebook_token facebook-token}
        :on-complete on-complete
        :on-error (fn [error] (println "[LOG] Failed to login or signup")) }))

(defn get-user [on-complete]
  (xhr {:method "GET"
        :base-url (str USER_URL (:user-id (auth/get-credentials)))
        :url-params (auth/get-api-credentials)
        :on-complete on-complete
        :on-error (fn [error] (println "[LOG] Failed to get user info"))}))

(defn get-event [event-id on-complete]
  (xhr {:method "GET"
        :base-url (str EVENT_URL event-id)
        :url-params (auth/get-api-credentials)
        :on-complete on-complete
        :on-error (fn [error] (println "[LOG] Failed to get event info"))}))

(defn get-all-events [on-complete]
  (xhr {:method "GET"
        :base-url EVENT_URL
        :url-params (auth/get-api-credentials)
        :on-complete on-complete
        :on-error (fn [error] (println "[LOG] Failed to get event info"))}))

(defn get-event-subscribers [event-id on-complete]
  (xhr {:method "GET"
        :base-url (str EVENT_URL event-id "/subscriptions")
        :url-params (auth/get-api-credentials)
        :on-complete on-complete
        :on-error (fn [error] (println "[LOG] Failed to get event info"))}))

(defn get-subevents [event-id on-complete]
  (xhr {:method "GET"
        :base-url (str EVENT_URL event-id "/subevents")
        :url-params (auth/get-api-credentials)
        :on-complete on-complete
        :on-error #(println "[LOG] Failed to get subevents") }))

(defn post-event-update [{:keys [id name description cover_photo_url event_map_url start_time end_time
                                 address twitter_widget_id timezone published website_url]}]
  (xhr {:method "POST"
        :base-url (str EVENT_URL id)
        :url-params (merge
                      (auth/get-api-credentials)
                      {"event[name]" name
                       "event[description]" description
                       "event[cover_photo_url]" cover_photo_url
                       "event[event_map_url]" event_map_url
                       "event[start_time]" start_time
                       "event[end_time]" end_time
                       "event[address]" address
                       "event[twitter_widget_id]" twitter_widget_id
                       "event[timezone]" timezone
                       "event[published]" published
                       "event[website_url]" website_url })
        :on-complete #()
        :on-error #() }))

(defn post-new-subevent [{:keys [name location start-time end-time]} event-id]
  (xhr {:method "POST"
        :base-url (str EVENT_URL event-id "/subevents")
        :url-params (merge
                      (auth/get-api-credentials)
                      {"name" name
                       "description" ""
                       "location" location
                       "start_time" start-time
                       "end_time" end-time })
        :on-complete #()
        :on-error #()}))

(defn post-new-notification [new-notification channel-id]
  (xhr {:method "POST"
        :base-url (str CHANNEL_URL channel-id "/notifications")
        :url-params (merge
                      (auth/get-api-credentials)
                      {"notification[notification_guts]" new-notification
                       "notification[type]" "Message"})
        :on-complete #()
        :on-error #()
        }))
