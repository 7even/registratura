(ns registratura.core
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [compojure.core :refer [GET routes]]
            [compojure.route :as route]
            [integrant.core :as ig]
            [ring.adapter.jetty :refer [run-jetty]]))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defn config []
  (-> "config.edn"
      io/resource
      aero/read-config))

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
  (ig/init (config)))
