(ns registratura.test
  (:require [integrant.core :as ig]
            [registratura.core :as core]
            [registratura.db :as db]))

(def ^:private config
  (core/config :test))

(def ^:private db-config
  (:jdbc/connection config))

(def db-conn
  (atom nil))

(defn with-db [tests]
  (try
    (reset! db-conn
            (ig/init-key :jdbc/connection db-config))
    (tests)
    (finally
      (db/truncate-patients @db-conn)
      (ig/halt-key! :jdbc/connection @db-conn)
      (reset! db-conn nil))))
