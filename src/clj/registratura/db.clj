(ns registratura.db
  "Functions for reading from & writing to database."
  (:require [camel-snake-kebab.core :refer [->snake_case_keyword]]
            [clojure.java.io :as io]
            [dsql.core :as ql]
            [dsql.pg :as dsql]
            [next.jdbc :as jdbc])
  (:import [java.time LocalDate]))

(defn make-raw-query
  "Runs `query` (a string or a vector of string and arbitrary arguments)
  against `db-conn` and returns the results."
  [db-conn query]
  (jdbc/execute! db-conn
                 (if (vector? query)
                   query
                   [query])
                 jdbc/snake-kebab-opts))

(defmethod ql/to-sql LocalDate
  [acc opts date]
  (-> acc
      (conj (str (ql/string-litteral date) "::date"))))

(defn make-query
  "Processes the `query` map using dsql library, then runs it against `db-conn`,
  returning the results."
  [db-conn query]
  (jdbc/execute! db-conn
                 (dsql/format query)
                 jdbc/snake-kebab-opts))

(defn- fresh-db?
  "Returns `true` if database at `db-conn` is empty (in fact just checks
  if the \"patients\" table exists), `false` otherwise."
  [db-conn]
  (let [tables-query {:select 1
                      :from :information_schema.tables
                      :where {:ql/type :pg/and
                              :type [:= :table_type "BASE TABLE"]
                              :schema [:= :table_schema "public"]
                              :name [:= :table_name [:pg/param "patients"]]}}]
    (empty? (make-query db-conn tables-query))))

(defn- get-db-schema
  "Returns database schema as an SQL string that can be run on an empty
  database in order to get the desired database structure."
  []
  (-> "schema.sql"
      io/resource
      slurp))

(defn start
  "Establishes the connection to database specified by `db-spec`, then migrates
  it if required."
  [db-spec]
  (let [conn (->> db-spec
                  (reduce-kv (fn [acc k v]
                               (if (some? v)
                                 (assoc acc k v)
                                 acc))
                             {})
                  jdbc/get-connection)]
    (when (fresh-db? conn)
      (make-raw-query conn (get-db-schema)))
    conn))

(defn stop
  "Closes the `conn` (database connection)."
  [conn]
  (.close conn))

(defn truncate-patients
  "Deletes all patients from the database at `db-conn` and resets the patient id
  sequence to 1.

  Supposed to be run after each test to clean the database."
  [db-conn]
  (make-raw-query db-conn
                  "TRUNCATE patients RESTART IDENTITY"))

(defn- normalize-patient
  "Transforms the `patient` entity map from format returned by next.jdbc to
  a format preferred by the application."
  [patient]
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

(defn- denormalize-patient
  "Transforms the `patient` entity map from application's preferred format
  to a format recognized by next.jdbc."
  [patient]
  (cond-> patient
    (contains? patient :patient/gender)
    (update :patient/gender name)

    :always
    (update-keys ->snake_case_keyword)))

(defmethod ql/to-sql
  :pg/op
  [acc opts [op operand & operands]]
  (reduce (fn [acc' operand]
            (-> acc'
                (conj (name op))
                (ql/to-sql opts operand)))
          (ql/to-sql acc opts operand)
          operands))

(defn- fulltext-search-condition [search-query]
  (let [fulltext-parts (->> [:first_name
                             :middle_name
                             :last_name
                             :address
                             :insurance_number]
                            (map (fn [attr-name]
                                   ^:pg/fn [:coalesce attr-name]))
                            (interpose " "))
        fulltext (with-meta (cons :|| fulltext-parts) {:pg/op true})
        tsvector ^:pg/fn [:to_tsvector "english" fulltext]
        tsquery ^:pg/fn [:plainto_tsquery search-query]]
    ^:pg/op ["@@" tsvector tsquery]))

(defn list-patients
  "Returns all patients from the database at `db-conn` as a vector of
  entity maps."
  [db-conn {:patient/keys [query genders min-age max-age] :as filter}]
  (let [years-from-age ^:pg/kfn[:extract :year :from ^:pg/fn[:age :birthday]]
        conditions (cond-> []
                     (some? query)
                     (conj (fulltext-search-condition query))

                     (= (count genders) 1)
                     (conj [:= :gender [:pg/cast (-> genders first name) :patient_gender]])

                     (some? min-age)
                     (conj [:<= min-age years-from-age])

                     (some? max-age)
                     (conj [:<= years-from-age max-age]))
        query (merge {:select :*
                      :from :patients
                      :order-by :id}
                     (when (seq conditions)
                       {:where (cons :and conditions)}))]
    (->> (make-query db-conn query)
         (mapv normalize-patient))))

(defn get-patient
  "Returns patient identified by `id` from the database at `db-conn`
  as an entity map."
  [db-conn id]
  (let [[patient] (make-query db-conn
                              {:select :*
                               :from :patients
                               :where [:= :id [:pg/param id]]})]
    (when (some? patient)
      (normalize-patient patient))))

(defn create-patient
  "Creates a new patient with `attrs` in the database at `db-conn` and returns
  its id."
  [db-conn attrs]
  (-> (make-query db-conn
                  {:ql/type :pg/insert
                   :into :patients
                   :value (denormalize-patient attrs)
                   :returning :id})
      first
      normalize-patient
      :patient/id))

(defn update-patient
  "Updates the patient identified by `id` in the database at `db-conn`
  with `new-attrs`."
  [db-conn id new-attrs]
  (make-query db-conn
              {:ql/type :pg/update
               :update :patients
               :set (denormalize-patient new-attrs)
               :where [:= :id [:pg/param id]]}))

(defn delete-patient
  "Deletes the patient identified by `id` from the database at `db-conn`."
  [db-conn id]
  (make-query db-conn
              {:ql/type :pg/delete
               :from :patients
               :where [:= :id [:pg/param id]]}))
