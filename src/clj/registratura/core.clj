(ns registratura.core
  (:require [compojure.core :refer [GET routes]]
            [compojure.route :as route]
            [integrant.core :as ig]
            [ring.adapter.jetty :refer [run-jetty]]))

(def config
  {:http/handler {:server-opts {:port 8888
                                :join? false}}})

(defn app-routes []
  (routes (GET "/" [] "<h1>Hello!</h1>")
          (route/not-found {:status 404
                            :body "Not found"})))

(defmethod ig/init-key :http/handler [_ {:keys [server-opts]}]
  (println ";; Starting HTTP handler")
  (let [handler (app-routes)]
    (run-jetty handler server-opts)))

(defmethod ig/halt-key! :http/handler [_ server]
  (println ";; Stopping HTTP handler")
  (.stop server))

(defn -main []
  (println "Hello!")
  (ig/init config))
