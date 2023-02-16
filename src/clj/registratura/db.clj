(ns registratura.db
  (:require [clojure.java.io :as io]
            [next.jdbc :as jdbc]))

(defn make-query [db-conn query]
  (jdbc/execute! db-conn
                 (if (vector? query)
                   query
                   [query])
                 jdbc/snake-kebab-opts))

(defn- db-initialized? [db-conn]
  (let [tables-query "SELECT table_name
                      FROM information_schema.tables
                      WHERE table_type = 'BASE TABLE'
                      AND table_schema = 'public'"
        table-names (->> (make-query db-conn tables-query)
                         (map :tables/table-name)
                         (into #{}))]
    (contains? table-names "patients")))

(defn- get-db-schema []
  (-> "schema.sql"
      io/resource
      slurp))

(defn start [db-spec]
  (let [conn (jdbc/get-connection db-spec)]
    (when-not (db-initialized? conn)
      (make-query conn (get-db-schema)))
    conn))

(defn stop [conn]
  (.close conn))
