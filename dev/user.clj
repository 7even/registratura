(ns user
  (:require [clojure.tools.namespace.repl :refer [set-refresh-dirs]]
            [integrant.repl :refer [go halt set-prep!]]
            [registratura.core :refer [config]]))

(set-prep! config)

(set-refresh-dirs "dev" "src/clj")

(defn system []
  integrant.repl.state/system)

(defn db-conn []
  (:jdbc/connection (system)))

(def reset integrant.repl/reset)
