(ns registratura.http
  (:require [bidi.ring :refer [make-handler]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn- get-patients [db-conn]
  (fn [req]
    {:status 200
     :headers {"Content-Type" "application/edn"}
     :body (pr-str [{:patient/id 1
                     :patient/first-name "Vsevolod"
                     :patient/last-name "Romashov"
                     :patient/gender "male"
                     :patient/birthday #inst "1984-09-27"
                     :patient/address "Tbilisi, Anjafaridze, 4"
                     :patient/insurance-number "777"}])}))

(defn handler [db-conn]
  (let [routes ["/api" {:get {"/patients" (get-patients db-conn)}
                        true (fn [ret]
                               {:status 404
                                :body "Not Found"})}]]
    (make-handler routes)))

(defn start [{:keys [db-conn server-opts]}]
  (let [handler (handler db-conn)]
    (run-jetty handler server-opts)))

(defn stop [server]
  (.stop server))
