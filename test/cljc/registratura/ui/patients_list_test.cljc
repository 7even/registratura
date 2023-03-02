(ns registratura.ui.patients-list-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [registratura.ui.common :refer [<sub >evt!]]
            [registratura.ui.db :as db]
            [registratura.ui.patients-list :as sut]
            [registratura.ui.test :refer :all]))

(use-fixtures :each with-stubbed-requests)

(def ^:private first-page-data
  {:entities [{:patient/id 1
               :patient/first-name "John"
               :patient/middle-name nil
               :patient/last-name "Smith"
               :patient/gender :gender/male
               :patient/birthday #time/date "1993-07-15"
               :patient/address "Chicago, IL"
               :patient/insurance-number "145-29-7635"}]
   :total-count 2})

(def ^:private second-page-data
  {:entities [{:patient/id 2
               :patient/first-name "Matilda"
               :patient/middle-name "Brandon"
               :patient/last-name "Crooks"
               :patient/gender :gender/female
               :patient/birthday #time/date "1978-02-26"
               :patient/address "San Francisco, CA"
               :patient/insurance-number "980-92-8448"}]
   :total-count 2})

(deftest list-test
  (testing "in initial state"
    (>evt! [::db/initialize])
    (is (empty? (<sub [::sut/patients]))))
  (testing "after loading first page of patients"
    (>evt! [::sut/load-patients])
    (is (= [{:method :get
             :uri "/api/patients"
             :params {:patient/genders #{:gender/male :gender/female}
                      :pagination/limit 20
                      :pagination/offset 0}
             :on-success [::sut/patients-loaded false]
             :on-failure [:unhandled-error]}]
           @requests))
    (>evt! [::sut/patients-loaded false first-page-data])
    (is (= [{:patient/id 1
             :patient/full-name "John Smith"
             :patient/gender "Male"
             :patient/birthday "15.07.1993"
             :patient/address "Chicago, IL"
             :patient/insurance-number "145-29-7635"}]
           (<sub [::sut/patients])))
    (is (<sub [::sut/can-load-more?]))
    (is (<sub [::sut/patients-loaded?])))
  (testing "after loading next page"
    (is (<sub [::sut/can-load-more?]))
    (>evt! [::sut/load-more-patients])
    (is (= {:method :get
            :uri "/api/patients"
            :params {:patient/genders #{:gender/male :gender/female}
                     :pagination/limit 20
                     :pagination/offset 1}
            :on-success [::sut/patients-loaded true]
            :on-failure [:unhandled-error]}
           (last @requests)))
    (is (not (<sub [::sut/can-load-more?])))
    (>evt! [::sut/patients-loaded true second-page-data])
    (is (= [{:patient/id 1
             :patient/full-name "John Smith"
             :patient/gender "Male"
             :patient/birthday "15.07.1993"
             :patient/address "Chicago, IL"
             :patient/insurance-number "145-29-7635"}
            {:patient/id 2
             :patient/full-name "Matilda Brandon Crooks"
             :patient/gender "Female"
             :patient/birthday "26.02.1978"
             :patient/address "San Francisco, CA"
             :patient/insurance-number "980-92-8448"}]
           (<sub [::sut/patients])))
    (is (not (<sub [::sut/can-load-more?]))))
  (testing "after deleting a patient"
    (>evt! [::sut/delete-patient 2])
    (is (= {:method :delete
            :uri "/api/patients/2"
            :on-success [::sut/reload-patients]
            :on-failure [:unhandled-error]}
           (last @requests)))
    (>evt! [::sut/reload-patients])
    (is (= {:method :get
            :uri "/api/patients"
            :params {:patient/genders #{:gender/male :gender/female}
                     :pagination/limit 2
                     :pagination/offset 0}
            :on-success [::sut/patients-loaded false]
            :on-failure [:unhandled-error]}
           (last @requests)))
    (>evt! [::sut/patients-loaded false (assoc first-page-data :total-count 1)])
    (is (= [{:patient/id 1
             :patient/full-name "John Smith"
             :patient/gender "Male"
             :patient/birthday "15.07.1993"
             :patient/address "Chicago, IL"
             :patient/insurance-number "145-29-7635"}]
           (<sub [::sut/patients])))
    (is (not (<sub [::sut/can-load-more?])))))
