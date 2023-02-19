(ns registratura.http
  (:require [bidi.ring :refer [make-handler]]
            [registratura.html :as html]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [content-type response]]))

(defn- get-patients [db-conn]
  (fn [req]
    [{:patient/id 1
      :patient/first-name "Vsevolod"
      :patient/last-name "Romashov"
      :patient/gender "male"
      :patient/birthday #inst "1984-09-27"
      :patient/address "Tbilisi, Anjafaridze, 4"
      :patient/insurance-number "777"}]))

(defn- make-routes [db-conn]
  ["" {"/api" {:get {"/patients" (get-patients db-conn)}}}])

(defn- wrap-edn-response [handler]
  (fn [request]
    (when-let [response-body (handler request)]
      (-> response-body
          pr-str
          response
          (content-type "application/edn")))))

(defn- wrap-html-page-response [handler]
  (fn [request]
    (or (handler request)
        (-> (response html/page)
            (content-type "text/html")))))

(defn handler [db-conn]
  (let [routes (make-routes db-conn)]
    (-> (make-handler routes)
        wrap-edn-response
        (wrap-resource "public")
        wrap-html-page-response)))

(defn start [{:keys [db-conn server-opts]}]
  (let [handler (handler db-conn)]
    (run-jetty handler server-opts)))

(defn stop [server]
  (.stop server))
