(ns registratura.ui.patient-page-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [registratura.ui.common :refer [<sub >evt!]]
            [registratura.ui.db :as db]
            [registratura.ui.patient-page :as sut]
            [registratura.ui.routes :as routes]
            [registratura.ui.test :refer :all]))

(use-fixtures :each with-stubbed-requests)

(deftest create-or-update-patient-test
  (testing "in initial state"
    (>evt! [::db/initialize])
    (is (nil? (<sub [::sut/patient]))))
  (testing "after initializing new patient"
    (>evt! [::sut/initialize-new-patient])
    (is (= {:patient/first-name nil
            :patient/middle-name nil
            :patient/last-name nil
            :patient/gender nil
            :patient/birthday nil
            :patient/insurance-number nil
            :patient/address nil}
           (<sub [::sut/patient]))))
  (testing "after loading existing patient"
    (>evt! [::routes/set-route {:handler :patient-page
                                :route-params {:id "1"}}])
    (>evt! [::sut/load-patient])
    (is (= [{:method :get
             :uri "/api/patients/1"
             :on-success [::sut/patient-loaded]
             :on-failure [::sut/patient-not-found]}]
           @requests))
    (is (not (<sub [::sut/patient-loaded?])))
    (>evt! [::sut/patient-loaded patient-attrs])
    (is (= patient-attrs
           (<sub [::sut/patient])))
    (is (<sub [::sut/patient-loaded?])))
  (testing "after changing some attribute to a blank value"
    (>evt! [::sut/change-patient-attribute :patient/first-name ""])
    (is (nil? (<sub [::sut/patient-attribute :patient/first-name]))))
  (testing "after changing some attribute to a non-blank value"
    (>evt! [::sut/change-patient-attribute :patient/first-name "John"])
    (is (= "John" (<sub [::sut/patient-attribute :patient/first-name]))))
  (testing "on form submit"
    (testing "without first name"
      (>evt! [::sut/change-patient-attribute :patient/first-name ""])
      (>evt! [::sut/submit-patient])
      (is (= ["First name is required"]
             (<sub [::sut/patient-attribute-errors :patient/first-name])))
      (>evt! [::sut/change-patient-attribute
              :patient/first-name
              (:patient/first-name patient-attrs)]))
    (testing "without last name"
      (>evt! [::sut/change-patient-attribute :patient/last-name ""])
      (>evt! [::sut/submit-patient])
      (is (= ["Last name is required"]
             (<sub [::sut/patient-attribute-errors :patient/last-name])))
      (>evt! [::sut/change-patient-attribute
              :patient/last-name
              (:patient/last-name patient-attrs)]))
    (testing "without gender"
      (>evt! [::sut/change-patient-attribute :patient/gender nil])
      (>evt! [::sut/submit-patient])
      (is (= ["Gender is required"]
             (<sub [::sut/patient-attribute-errors :patient/gender])))
      (>evt! [::sut/change-patient-attribute
              :patient/gender
              (:patient/gender patient-attrs)]))
    (testing "without birthday"
      (>evt! [::sut/change-patient-attribute :patient/birthday nil])
      (>evt! [::sut/submit-patient])
      (is (= ["Birthday is required"]
             (<sub [::sut/patient-attribute-errors :patient/birthday])))
      (>evt! [::sut/change-patient-attribute
              :patient/birthday
              (:patient/birthday patient-attrs)]))
    (testing "without insurance number"
      (>evt! [::sut/change-patient-attribute :patient/insurance-number ""])
      (>evt! [::sut/submit-patient])
      (is (= ["Insurance number is required"]
             (<sub [::sut/patient-attribute-errors :patient/insurance-number])))
      (>evt! [::sut/change-patient-attribute
              :patient/insurance-number
              (:patient/insurance-number patient-attrs)]))
    (testing "without address"
      (>evt! [::sut/change-patient-attribute :patient/address ""])
      (>evt! [::sut/submit-patient])
      (is (= ["Address is required"]
             (<sub [::sut/patient-attribute-errors :patient/address])))
      (>evt! [::sut/change-patient-attribute
              :patient/address
              (:patient/address patient-attrs)]))
    (testing "with valid patient attributes"
      (testing "when creating a new patient"
        (>evt! [::routes/set-route {:handler :new-patient-page}])
        (>evt! [::sut/submit-patient])
        (is (= {:method :post
                :uri "/api/patients"
                :params (dissoc patient-attrs :patient/id)
                :on-success [::sut/patient-created]
                :on-failure [:unhandled-error]}
               (last @requests)))
        (is (not (contains? (<sub [::sut/patient]) :errors)))
        (is (<sub [::sut/saving-patient?]))
        (>evt! [::sut/patient-created {:patient/id 1}])
        (is (not (<sub [::sut/saving-patient?]))))
      (testing "when updating an existing patient"
        (>evt! [::routes/set-route {:handler :patient-page
                                    :route-params {:id "1"}}])
        (>evt! [::sut/submit-patient])
        (is (= {:method :patch
                :uri "/api/patients/1"
                :params (dissoc patient-attrs :patient/id)
                :on-success [::sut/patient-saved]
                :on-failure [:unhandled-error]}
               (last @requests)))
        (is (<sub [::sut/saving-patient?]))
        (>evt! [::sut/patient-saved])
        (is (not (<sub [::sut/saving-patient?])))))))
