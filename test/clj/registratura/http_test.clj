(ns registratura.http-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [registratura.http :as sut]
            [registratura.test :refer :all]
            [ring.mock.request :as mock]))

(use-fixtures :each with-db)

(defn- make-handler []
  (sut/handler @db-conn))

(defn- get-response
  ([method url] (get-response method url {}))
  ([method url params]
   (let [handler (make-handler)
         request-with-body? (contains? #{:post :patch} method)
         request-without-body? (not request-with-body?)
         parse-edn (fn [edn]
                     (edn/read-string {:readers time-literals.read-write/tags} edn))]
     (cond-> (mock/request method url)
       request-with-body? (mock/content-type "application/edn")
       request-with-body? (mock/body (pr-str params))
       request-without-body? (mock/query-string params)
       :always handler
       :always (update :body parse-edn)))))

(def ^:private valid-patients-list-params
  {"patient/query" "vsevolod"
   "patient/genders" ["gender/male" "gender/female"]
   "patient/min-age" "20"
   "patient/max-age" "50"})

(deftest process-patient-list-params-test
  (is (= {:patient/query "vsevolod"
          :patient/genders [:gender/male :gender/female]
          :patient/min-age 20
          :patient/max-age 50}
         (sut/process-patient-list-params valid-patients-list-params))))

(deftest list-patients-test
  (testing "on an empty patients table"
    (let [{:keys [status body]} (get-response :get "/api/patients")]
      (is (= 200 status))
      (is (= {:entities []
              :total-count 0}
             body))))
  (testing "with an existing patient"
    (create-patient)
    (let [{:keys [status body]} (get-response :get "/api/patients")]
      (is (= 200 status))
      (is (= {:entities [(assoc patient-attrs :patient/id 1)]
              :total-count 1}
             body)))
    (testing "with valid filter params"
      (let [{:keys [status]} (get-response :get
                                           "/api/patients"
                                           valid-patients-list-params)]
        (is (= 200 status))))
    (testing "with invalid filter params"
      (doseq [filter-params [{"patient/genders" ["foo/bar"]}
                             {"patient/min-age" "-1"}
                             {"patient/min-age" "foobar"}
                             {"patient/max-age" "-15"}]]
        (let [{:keys [status]} (get-response :get
                                             "/api/patients"
                                             filter-params)]
          (is (= 400 status)))))))

(deftest get-patient-test
  (create-patient)
  (testing "with id of an existing patient"
    (let [{:keys [status body]} (get-response :get "/api/patients/1")]
      (is (= 200 status))
      (is (= (assoc patient-attrs :patient/id 1)
             body))))
  (testing "with an unknown patient id"
    (let [{:keys [status body]} (get-response :get "/api/patients/777")]
      (is (= 404 status))
      (is (nil? body)))))

(deftest create-patient-test
  (testing "with valid patient attributes"
    (let [{:keys [status body]} (get-response :post
                                              "/api/patients"
                                              patient-attrs)]
      (is (= 201 status))
      (is (= {:patient/id 1}
             body))
      (let [{new-patients-list :body} (get-response :get "/api/patients")]
        (is (= [(assoc patient-attrs :patient/id 1)]
               (:entities new-patients-list))))))
  (testing "with invalid patient attributes"
    (let [{:keys [status]} (get-response :post
                                         "/api/patients"
                                         {:patient/first-name :foo
                                          :patient/last-name :bar})]
      (is (= 422 status)))))

(deftest update-patient-test
  (create-patient)
  (testing "with valid patient attributes"
    (let [{:keys [status]} (get-response :patch
                                         "/api/patients/1"
                                         {:patient/middle-name "foobar"})]
      (is (= 200 status))
      (let [{updated-patient :body} (get-response :get "/api/patients/1")]
        (is (= "foobar"
               (:patient/middle-name updated-patient))))))
  (testing "with invalid patient attributes"
    (let [{:keys [status]} (get-response :patch
                                         "/api/patients/1"
                                         {:patient/middle-name :foo/bar})]
      (is (= 422 status))
      (let [{not-updated-patient :body} (get-response :get "/api/patients/1")]
        (is (= "foobar"
               (:patient/middle-name not-updated-patient)))))))

(deftest delete-patient
  (create-patient)
  (let [{:keys [status]} (get-response :delete "/api/patients/1")]
    (is (= 200 status))
    (let [{new-patients-list :body} (get-response :get "/api/patients")]
      (is (empty? (:entities new-patients-list))))))
