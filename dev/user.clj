(ns user
  (:require [clojure.tools.namespace.repl :refer [set-refresh-dirs]]
            [integrant.repl :refer [go halt set-prep!]]
            [registratura.core :refer [config]]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as server]
            [user.fake-data-generator :as fake-data-generator]))

(set-prep! config)

(set-refresh-dirs "dev" "src/clj")

(defn system []
  integrant.repl.state/system)

(defn db-conn []
  (:jdbc/connection (system)))

(def reset integrant.repl/reset)

(defonce app-started?
  (atom false))

;; start the app when REPL is launched
;; (but not when this namespace is recompiled)
(when-not @app-started?
  (go)
  (reset! app-started? true))

(defn cljs-repl []
  (server/start!)
  (shadow/watch :main)
  (shadow/nrepl-select :main))

(defn create-fake-patients
  "Creates fake data in the database for `required-patients-count` patients
  (200 by default)."
  ([] (create-fake-patients 200))
  ([required-patients-count]
   (fake-data-generator/create-fake-patients (db-conn)
                                             required-patients-count)))
