(ns registratura.http-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [registratura.http :as sut]
            [registratura.test :refer :all]
            [ring.mock.request :as mock]
            [time-literals.read-write]))

(use-fixtures :each with-db)

(defn- make-handler []
  (sut/handler @db-conn))

(defn- get-response
  ([method url] (get-response method url {}))
  ([method url params]
   (let [handler (make-handler)
         parse-edn (fn [edn]
                     (edn/read-string {:readers time-literals.read-write/tags} edn))]
     (cond-> (mock/request method url)
       (not= method :get) (mock/content-type "application/edn")
       (not= method :get) (mock/body (pr-str params))
       :always handler
       :always (update :body parse-edn)))))

(deftest list-patients-test
  (testing "on an empty patients table"
    (let [{:keys [status body]} (get-response :get "/api/patients")]
      (is (= 200 status))
      (is (= [] body))))
  (testing "with an existing patient"
    (create-patient)
    (let [{:keys [status body]} (get-response :get "/api/patients")]
      (is (= 200 status))
      (is (= [(assoc patient-attrs :patient/id 1)]
             body)))))

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
