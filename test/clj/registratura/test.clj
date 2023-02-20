(ns registratura.test
  (:require [integrant.core :as ig]
            [registratura.core :as core]
            [registratura.db :as db]
            [tick.core :as t]))

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

(def patient-attrs
  {:patient/first-name "Vsevolod"
   :patient/middle-name "Borisovych"
   :patient/last-name "Romashov"
   :patient/gender :gender/male
   :patient/birthday (t/date "1984-09-27")
   :patient/address "Tbilisi"
   :patient/insurance-number "123"})

(defn create-patient []
  (db/create-patient @db-conn patient-attrs))
