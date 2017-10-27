(ns dbas-fb-hook.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.logger :as logger]
            [ring.util.response :refer [response]]
            [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [clj-http.client :as chttp :refer [json-encode json-decode]]))

(def dialogflow-webhook "https://bots.dialogflow.com/facebook/72b13434-f5fb-4f51-9a9e-e1a6527f7190/webhook")
(def eauth "https://e5acae5a.eu.ngrok.io/success")

(defn forward-to! [service body]
  (log/info "Forwarding to" service)
  (client/post service
               {:content-type :json
                :body (json-encode body)}))

(defn message? [msg]
  (get msg :message false))

(defn auth? [msg]
  (get msg :account_linking false))

(defn process! [params]
  (let [entries (get-in params [:entry])]
    (doseq [entry entries]
      (doseq [msg (:messaging entry)]
        (cond
          (message? msg) (forward-to! dialogflow-webhook msg)
          (auth? msg) (forward-to! eauth msg)
          :else (log/error (str "Unknown type!\n" msg))))))
  ; send ok to facebook.
  "ok")

(defroutes app-routes
  (GET "/" request (response (get-in request [:params "hub.challenge"])))
  (POST "/" request (response (process! (log/spy :info (:params request)))))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (logger/wrap-with-logger)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      (wrap-json-body {:keywords? true :pretty? true})
      (wrap-keyword-params)
      (wrap-json-params)
      (wrap-json-response)))




