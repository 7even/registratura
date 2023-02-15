(ns user
  (:require [clojure.tools.namespace.repl :refer [set-refresh-dirs]]
            [integrant.repl :refer [go halt set-prep!]]
            [registratura.core :refer [config]]))

(set-prep! (constantly config))

(set-refresh-dirs "dev" "src/clj")

(defn system []
  integrant.repl.state/system)

(def reset integrant.repl/reset)
