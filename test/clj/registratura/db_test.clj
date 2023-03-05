(ns registratura.db-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [registratura.db :as sut]
            [registratura.test :refer :all]))

(use-fixtures :each with-db)

(def ^:private found-response
  {:entities [(assoc patient-attrs :patient/id 1)]
   :total-count 1})

(def ^:private not-found-response
  {:entities []
   :total-count 0})

(deftest list-patients-test
  (create-patient)
  (testing "without filtering"
    (is (not= found-response
              (sut/list-patients @db-conn {}))))
  (testing "with a search query"
    (is (= found-response
           (sut/list-patients @db-conn
                              {:patient/query "Vsevolod Romashov Tbilisi"})))
    (is (= not-found-response
           (sut/list-patients @db-conn
                              {:patient/query "Ivan Ivanov Moscow"}))))
  (testing "with gender filter"
    (is (= found-response
           (sut/list-patients @db-conn
                              {:patient/genders [:gender/male :gender/female]})))
    (is (= found-response
           (sut/list-patients @db-conn
                              {:patient/genders [:gender/male]})))
    (is (= not-found-response
           (sut/list-patients @db-conn
                              {:patient/genders [:gender/female]}))))
  (testing "with min-age"
    (is (= found-response
           (sut/list-patients @db-conn {:patient/min-age 30})))
    (is (= not-found-response
           (sut/list-patients @db-conn {:patient/min-age 50}))))
  (testing "with max-age"
    (is (= found-response
           (sut/list-patients @db-conn {:patient/max-age 50})))
    (is (= not-found-response
           (sut/list-patients @db-conn {:patient/max-age 30})))))

(deftest get-patient-test
  (create-patient)
  (let [patient (sut/get-patient @db-conn 1)]
    (is (= (assoc patient-attrs :patient/id 1)
           patient))))

(deftest create-patient-test
  (let [new-patient-id (sut/create-patient @db-conn patient-attrs)]
    (is (= 1
           new-patient-id
           (->> (sut/list-patients @db-conn {})
                :entities
                first
                :patient/id)))))

(deftest update-patient-test
  (create-patient)
  (sut/update-patient @db-conn 1 {:patient/insurance-number "456"})
  (let [patient (sut/get-patient @db-conn 1)]
    (is (= "456" (:patient/insurance-number patient)))))

(deftest delete-patient-test
  (create-patient)
  (is (some? (sut/get-patient @db-conn 1)))
  (sut/delete-patient @db-conn 1)
  (is (nil? (sut/get-patient @db-conn 1))))
