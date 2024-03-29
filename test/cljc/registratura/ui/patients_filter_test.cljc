(ns registratura.ui.patients-filter-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest testing is use-fixtures]]
            [registratura.ui.common :refer [<sub >evt!]]
            [registratura.ui.patients-filter :as sut]
            [registratura.ui.patients-list :refer [load-patients-fx]]
            [registratura.ui.test :refer :all]))

(use-fixtures :each
  with-stubbed-requests
  (fn [tests]
    (reset! re-frame.db/app-db
            {:patients-filter {:patient/genders #{:gender/male
                                                  :gender/female}}})
    (tests)))

(deftest filter-test
  (testing "in initial state"
    (is (= "" (<sub [::sut/search-query])))
    (is (<sub [::sut/include-patients-with-gender? :gender/male]))
    (is (<sub [::sut/include-patients-with-gender? :gender/female]))
    (is (= "" (<sub [::sut/min-age])))
    (is (= "" (<sub [::sut/max-age]))))
  (testing "with search query set"
    (>evt! [::sut/change-search-query "foobar"])
    (is (= "foobar" (<sub [::sut/search-query])))
    (>evt! [::sut/change-search-query ""]))
  (testing "with male gender toggle turned off"
    (>evt! [::sut/toggle-gender-filter :gender/male])
    (is (not (<sub [::sut/include-patients-with-gender? :gender/male])))
    (is (<sub [::sut/include-patients-with-gender? :gender/female])))
  (testing "with female gender toggle turned off"
    (>evt! [::sut/toggle-gender-filter :gender/female])
    (is (not (<sub [::sut/include-patients-with-gender? :gender/male])))
    (is (not (<sub [::sut/include-patients-with-gender? :gender/female]))))
  (testing "with male gender toggle turned back on"
    (>evt! [::sut/toggle-gender-filter :gender/male])
    (is (<sub [::sut/include-patients-with-gender? :gender/male]))
    (is (not (<sub [::sut/include-patients-with-gender? :gender/female]))))
  (testing "with minimum age set"
    (>evt! [::sut/change-min-age "20"])
    (is (= 20 (<sub [::sut/min-age]))))
  (testing "with minimum age unset"
    (>evt! [::sut/change-min-age ""])
    (is (= "" (<sub [::sut/min-age]))))
  (testing "with maximum age set"
    (>evt! [::sut/change-max-age "45"])
    (is (= 45 (<sub [::sut/max-age]))))
  (testing "with maximum age unset"
    (>evt! [::sut/change-max-age ""])
    (is (= "" (<sub [::sut/max-age]))))
  (testing "on form submit"
    (testing "with search string that is too long"
      (>evt! [::sut/change-search-query (->> (repeat 20 "foobar")
                                             (str/join " "))])
      (>evt! [::sut/submit-new-filter load-patients-fx])
      (is (= ["Search query length cannot exceed 100 characters"]
             (<sub [::sut/search-query-errors])))
      (is (empty? @requests))
      (>evt! [::sut/change-search-query ""]))
    (testing "with negative minimum age"
      (>evt! [::sut/change-min-age "-5"])
      (>evt! [::sut/submit-new-filter load-patients-fx])
      (is (= ["Minimum age cannot be negative"]
             (<sub [::sut/min-age-errors])))
      (is (empty? @requests))
      (>evt! [::sut/change-min-age ""]))
    (testing "with negative maximum age"
      (>evt! [::sut/change-max-age "-10"])
      (>evt! [::sut/submit-new-filter load-patients-fx])
      (is (= ["Maximum age cannot be negative"]
             (<sub [::sut/max-age-errors])))
      (is (empty? @requests))
      (>evt! [::sut/change-max-age ""]))
    (testing "with minimum age higher than maximum age"
      (>evt! [::sut/change-min-age "20"])
      (>evt! [::sut/change-max-age "18"])
      (>evt! [::sut/submit-new-filter load-patients-fx])
      (is (= ["Maximum age cannot be lower than minimum age"]
             (<sub [::sut/max-age-errors])))
      (is (empty? @requests))
      (>evt! [::sut/change-min-age ""])
      (>evt! [::sut/change-max-age ""]))
    (testing "with valid filter"
      (>evt! [::sut/submit-new-filter load-patients-fx])
      (is (nil? (<sub [::sut/search-query-errors])))
      (is (nil? (<sub [::sut/min-age-errors])))
      (is (nil? (<sub [::sut/max-age-errors]))))))
