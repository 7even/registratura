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
  "Creates fake data for `required-patients-count` patients (200 by default)
  in the database at `conn` (uses connection from integrant system by default)."
  [{:keys [required-patients-count
           conn]
    :or {required-patients-count 200
         conn (db-conn)}}]
  (fake-data-generator/create-fake-patients conn
                                            required-patients-count))

(comment
  ;; to create fake patients in postgres inside k8s cluster, run:
  ;; kubectl port-forward svc/postgres 15432:5432
  ;;
  ;; then eval the following code:
  (let [conn (next.jdbc/get-connection {:dbtype "postgres"
                                        :port 15432
                                        :dbname "postgres"
                                        :user "postgres"
                                        :password "SecretPassword"})]
    (create-fake-patients {:conn conn
                           :required-patients-count 1000})))
