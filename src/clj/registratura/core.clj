(ns registratura.core
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [registratura.http :as http]))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defn config []
  (-> "config.edn"
      io/resource
      aero/read-config))

(defmethod ig/init-key :http/handler [_ config]
  (println ";; Starting HTTP handler")
  (http/start config))

(defmethod ig/halt-key! :http/handler [_ server]
  (println ";; Stopping HTTP handler")
  (http/stop server))

(defn -main []
  (println "Hello!")
  (ig/init (config)))
