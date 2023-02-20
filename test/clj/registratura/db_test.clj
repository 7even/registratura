(ns registratura.db-test
  (:require [clojure.test :refer [use-fixtures deftest testing is]]
            [registratura.db :as sut]
            [registratura.test :refer :all]
            [tick.core :as t]))

(use-fixtures :each with-db)

(def ^:private patient-attrs
  {:patient/first-name "Vsevolod"
   :patient/middle-name "Borisovych"
   :patient/last-name "Romashov"
   :patient/gender :gender/male
   :patient/birthday (t/date "1984-09-27")
   :patient/address "Tbilisi"
   :patient/insurance-number "123"})

(deftest list-patients-test
  (sut/create-patient @db-conn patient-attrs)
  (let [patients (sut/list-patients @db-conn)]
    (is (= [(assoc patient-attrs :patient/id 1)]
           patients))))
