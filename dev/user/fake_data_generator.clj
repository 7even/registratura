(ns user.fake-data-generator
  "Contains a function to generate fake patient data and store it in Postgres."
  (:require [clojure.string :as str]
            faker.address
            faker.name
            [registratura.db :as db]
            [tick.core :as t]))

(defn- random-date []
  (let [max-inst-seconds (/ (.toEpochMilli (t/now)) 1000)
        random-inst-seconds (rand-int max-inst-seconds)]
    (-> (* random-inst-seconds 1000)
        t/instant
        t/date)))

(defn- random-address []
  (str/join ", "
            [(faker.address/street-address true)
             (faker.address/city)
             (str (faker.address/us-state-abbr) " " (faker.address/zip-code))]))

(defn- random-insurance-number []
  (let [random-digit #(rand-int 10)
        rand-digits-group (fn [n]
                            (->> (repeatedly n random-digit)
                                 str/join))]
    (str/join "-"
              [(rand-digits-group 3)
               (rand-digits-group 2)
               (rand-digits-group 4)])))

(defn create-fake-patients
  "Creates fake data in the database at `db-conn` for `required-patients-count` patients."
  [db-conn required-patients-count]
  (dotimes [_ required-patients-count]
    (db/create-patient db-conn
                       {:patient/first-name (faker.name/first-name)
                        :patient/middle-name (faker.name/first-name)
                        :patient/last-name (faker.name/last-name)
                        :patient/gender (rand-nth [:gender/male :gender/female])
                        :patient/birthday (random-date)
                        :patient/address (random-address)
                        :patient/insurance-number (random-insurance-number)})))
