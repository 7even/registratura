(ns registratura.http
  (:require [bidi.ring :refer [make-handler]]
            [registratura.db :as db]
            [registratura.html :as html]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [content-type not-found response]]
            [time-literals.read-write]))

(time-literals.read-write/print-time-literals-clj!)

(defn- list-patients [db-conn _]
  (response (db/list-patients db-conn)))

(defn- get-patient [db-conn {:keys [params]}]
  (let [id (-> params :id parse-long)]
    (if-let [patient (db/get-patient db-conn id)]
      (response patient)
      (not-found nil))))

(defn- make-routes [db-conn]
  [""
   {"/api" {:get {"/patients" {"" (partial list-patients db-conn)
                               ["/" [#"\d+" :id]] (partial get-patient db-conn)}}}}])

(defn- wrap-edn-response [handler]
  (fn [request]
    (when-let [handler-response (handler request)]
      (cond-> handler-response
        (some? (:body handler-response)) (update :body pr-str)
        (some? (:body handler-response)) (content-type "application/edn")))))

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
  (run-jetty (handler db-conn) server-opts))

(defn stop [server]
  (.stop server))
