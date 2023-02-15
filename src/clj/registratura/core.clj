(ns registratura.core
  (:require [compojure.core :refer [GET routes]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn app-routes []
  (routes (GET "/" [] "<h1>Hello!</h1>")
          (route/not-found {:status 404
                            :body "Not found"})))

(defn start-server [join?]
  (let [handler (app-routes)]
    (run-jetty handler {:port 8888
                        :join? join?})))

(defn stop-server [s]
  (.stop s))

(defn -main []
  (println "Hello!")
  (start-server true))
