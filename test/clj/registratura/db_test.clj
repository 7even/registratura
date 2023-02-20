(ns registratura.db-test
  (:require [clojure.test :refer [use-fixtures deftest testing is]]
            [registratura.db :as sut]
            [registratura.test :refer :all]))

(use-fixtures :each with-db)

(deftest list-patients-test
  (sut/create-patient @db-conn
                      {:first_name "Vsevolod"
                       :last_name "Romashov"
                       :gender "male"
                       :birthday #inst "1984-09-27"
                       :address "Tbilisi"
                       :insurance_number "123"})
  (let [patients (sut/list-patients @db-conn)]
    (is (= [{:patients/id 1
             :patients/first-name "Vsevolod"
             :patients/middle-name nil
             :patients/last-name "Romashov"
             :patients/gender "male"
             ;; :patients/birthday #inst "1984-09-27"
             :patients/address "Tbilisi"
             :patients/insurance-number "123"}]
           (->> patients
                (map #(dissoc % :patients/birthday)))))))
