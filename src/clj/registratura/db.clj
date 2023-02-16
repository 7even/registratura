(ns registratura.db
  (:require [clojure.java.io :as io]
            [next.jdbc :as jdbc]))

(defn make-query [db-conn query]
  (jdbc/execute! db-conn
                 (if (vector? query)
                   query
                   [query])
                 jdbc/snake-kebab-opts))

(defn- fresh-db? [db-conn]
  (let [tables-query ["SELECT 1
                       FROM information_schema.tables
                       WHERE table_type = 'BASE TABLE'
                       AND table_schema = 'public'
                       AND table_name = ?"
                      "patients"]]
    (empty? (make-query db-conn tables-query))))

(defn- get-db-schema []
  (-> "schema.sql"
      io/resource
      slurp))

(defn start [db-spec]
  (let [conn (jdbc/get-connection db-spec)]
    (when (fresh-db? conn)
      (make-query conn (get-db-schema)))
    conn))

(defn stop [conn]
  (.close conn))
