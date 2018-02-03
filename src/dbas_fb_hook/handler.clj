(ns dbas-fb-hook.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [clojure.core.async :as async :refer [go go-loop >! <! put! chan]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.logger :as logger]
            [ring.util.response :refer [response]]
            [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [clj-http.client :as chttp :refer [json-encode json-decode]]))

(defonce dialogflow-webhook (or (System/getenv "DIALOGFLOW_URL") "https://bots.dialogflow.com/facebook/72b13434-f5fb-4f51-9a9e-e1a6527f7190/webhook"))
(defonce eauth (or (System/getenv "EAUTH_URL") "https://discuss.cs.uni-duesseldorf.de/eauth/success"))

;; TODO implement message to user, if dialogflow isn't responding
(defn start-dialogflow-consumer [in-c]
  (go-loop []
      (if-let [msg (<! in-c)]
        (do
          (client/post dialogflow-webhook
                         {:content-type :json
                          :body (json-encode {:object "page"
                                              :entry [{:id :something
                                                       :time 123
                                                       :messaging [msg]}]})})
          (log/info "Redirected to dialogflow")
          (recur))
        (log/warn (str "Dialogflow processor closed!")))))


;; TODO implement message to user, if eauth isn't responding
(defn start-eauth-consumer [in-c]
  (go-loop []
      (if-let [msg (<! in-c)]
        (do
          (client/post eauth {:content-type :json
                              :body (json-encode msg)})
          (log/info "Redirected to eauth")
          (recur))
        (log/warn (str "Eauth processor closed!")))))

(defn message? [msg]
  (or (contains? msg :message)
      (contains? msg :postback)))

(defn auth? [msg]
  (contains? msg :account_linking))

(defn message-type [msg]
    (cond
        (message? msg) :message
        (auth? msg) :auth
        :default :unknown-type))

(defn start-splitter [process-chan type-channels]
  (go-loop []
      (when-let [params (<! process-chan)]
        (doseq [message (mapcat :messaging (:entry params))
                :let [msg-type (message-type message)]]
          (if (not= msg-type :unknown-type)
            (>! (type-channels (message-type message)) message)
            (log/error "An unknown message-type occured!"))
          (log/info message))
        (recur))))


;; Setup server
(defonce process-chan (chan 1))
(defonce type-channels
    {:message (chan 2)
     :auth (chan 2)})

(start-dialogflow-consumer (:message type-channels))
(start-eauth-consumer (:auth type-channels))
(start-splitter process-chan type-channels)

(defroutes app-routes
  (GET "/" request (response (get-in request [:params "hub.challenge"])))
  (POST "/" request (do (put! process-chan (:params request))
                        (response "ok")))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      ;(logger/wrap-with-logger)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      (wrap-json-body {:keywords? true :pretty? true})
      (wrap-keyword-params)
      (wrap-json-params)
      (wrap-json-response)))



