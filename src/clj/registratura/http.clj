(ns registratura.http
  (:require [compojure.core :refer [GET routes]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn app-routes []
  (routes (GET "/" [] "<h1>Hello!</h1>")
          (route/not-found {:status 404
                            :body "Not found"})))

(defn start [{:keys [server-opts]}]
  (let [handler (app-routes)]
    (run-jetty handler server-opts)))

(defn stop [server]
  (.stop server))
