(ns notifsta-clone.utils.http
  (:require [notifsta-clone.utils.auth :as auth]
            [goog.uri.utils :as uri-utils]
            [goog.net.XhrIo :as net-xhrio]))

(enable-console-print!)

(def BASE_URL "http://api.notifsta.com/v1")
(def LOGIN_URL (str BASE_URL "/auth/facebook"))
(def USER_URL (str BASE_URL "/users/"))
(def EVENT_URL (str BASE_URL "/events/"))

; parses goog.net.XhrIo response to a json
(defn parse-xhrio-response [success-callback fail-callback]
  (fn [response]
    (let [target (aget response "target")]
      (if (.isSuccess target)
        (let [json (.getResponseJson target)]
          (success-callback (js->clj json :keywordize-keys true)))
        (let [error (.getLastError target)]
          (fail-callback (js->clj error :keywordize-keys true)))))))

; wraps goog.net.XhrIo library in a simpler function xhr
(defn xhr [{:keys [method base-url url-params on-complete on-error]}]
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
    (parse-xhrio-response on-complete on-error)
    method))

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
