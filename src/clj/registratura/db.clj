(ns registratura.db
  (:require [camel-snake-kebab.core :refer [->snake_case_keyword]]
            [clojure.java.io :as io]
            [dsql.core :as ql]
            [dsql.pg :as dsql]
            [next.jdbc :as jdbc])
  (:import [java.time LocalDate]))

(defn make-raw-query [db-conn query]
  (jdbc/execute! db-conn
                 (if (vector? query)
                   query
                   [query])
                 jdbc/snake-kebab-opts))

(defmethod ql/to-sql LocalDate
  [acc opts date]
  (-> acc
      (conj (str (ql/string-litteral date) "::date"))))

(defn make-query [db-conn query]
  (jdbc/execute! db-conn
                 (dsql/format query)
                 jdbc/snake-kebab-opts))

(defn- fresh-db? [db-conn]
  (let [tables-query {:select 1
                      :from :information_schema.tables
                      :where {:ql/type :pg/and
                              :type [:= :table_type "BASE TABLE"]
                              :schema [:= :table_schema "public"]
                              :name [:= :table_name [:pg/param "patients"]]}}]
    (empty? (make-query db-conn tables-query))))

(defn- get-db-schema []
  (-> "schema.sql"
      io/resource
      slurp))

(defn start [db-spec]
  (let [conn (jdbc/get-connection db-spec)]
    (when (fresh-db? conn)
      (make-raw-query conn (get-db-schema)))
    conn))

(defn stop [conn]
  (.close conn))

(defn- normalize-patient [patient]
  (cond-> patient
    :always
    (update-keys (fn [attr-full-name]
                   (let [attr-ns (namespace attr-full-name)
                         attr-name (name attr-full-name)]
                     (keyword (if (= attr-ns "patients")
                                "patient"
                                attr-ns)
                              attr-name))))

    (contains? patient :patients/gender)
    (update :patient/gender #(keyword "gender" %))

    (contains? patient :patients/birthday)
    (update :patient/birthday #(.toLocalDate %))))

(defn- denormalize-patient [patient]
  (cond-> patient
    (contains? patient :patient/gender)
    (update :patient/gender name)

    :always
    (update-keys ->snake_case_keyword)))

(defn list-patients [db-conn]
  (->> (make-query db-conn
                   {:select :*
                    :from :patients})
       (mapv normalize-patient)))

(defn get-patient [db-conn id]
  (let [[patient] (make-query db-conn
                              {:select :*
                               :from :patients
                               :where [:= :id [:pg/param id]]})]
    (when (some? patient)
      (normalize-patient patient))))

(defn create-patient [db-conn attrs]
  (-> (make-query db-conn
                  {:ql/type :pg/insert
                   :into :patients
                   :value (denormalize-patient attrs)
                   :returning :id})
      first
      normalize-patient
      :patient/id))

(defn update-patient [db-conn id new-attrs]
  (make-query db-conn
              {:ql/type :pg/update
               :update :patients
               :set (denormalize-patient new-attrs)
               :where [:= :id [:pg/param id]]}))

(defn delete-patient [db-conn id]
  (make-query db-conn
              {:ql/type :pg/delete
               :from :patients
               :where [:= :id [:pg/param id]]}))

(defn truncate-patients [db-conn]
  (make-raw-query db-conn
                  "TRUNCATE patients RESTART IDENTITY"))
