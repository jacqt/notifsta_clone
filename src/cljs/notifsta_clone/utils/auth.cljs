(ns notifsta-clone.utils.auth
  (:require [om.core :as om :include-macros true]
            [goog.net.cookies :as cookies]))

(defn set-credentials [{:keys [auth-token email user-id]}]
  (println (str "Trying to set credential cookies now... " user-id ", " auth-token))
  (.set goog.net.cookies "email" email -1)
  (.set goog.net.cookies "user-id" user-id -1)
  (.set goog.net.cookies "auth-token" auth-token -1))

(defn clear-credentials []
  (.clear goog.net.cookies))

(defn get-credentials []
  (let [auth-token  (goog.net.cookies.get "auth-token")
        email (goog.net.cookies.get "email")
        user-id (goog.net.cookies.get "user-id")]
    (if (or (nil? auth-token) (nil? email) (nil? user-id))
      {}
      {:auth-token auth-token
       :email email
       :user-id user-id })))

(defn get-api-credentials []
  (let [auth-token  (goog.net.cookies.get "auth-token")
        email (goog.net.cookies.get "email") ]
    (if (or (nil? auth-token) (nil? email))
      {}
      {:user_email email
       :user_token auth-token})))

(defn logout [credentials]
  (clear-credentials)
  (om/update! credentials (get-credentials)))
