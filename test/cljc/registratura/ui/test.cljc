(ns registratura.ui.test
  (:require [re-frame.core :as rf]))

(defonce requests
  (atom []))

#?(:clj
   (rf/reg-fx :send-request
     (fn [request]
       (swap! requests conj request))))

(defn with-stubbed-requests [tests]
  (tests)
  (reset! requests []))

(def patient-attrs
  {:patient/id 1
   :patient/first-name "John"
   :patient/middle-name nil
   :patient/last-name "Smith"
   :patient/gender :gender/male
   :patient/birthday #time/date "1993-07-15"
   :patient/address "Chicago, IL"
   :patient/insurance-number "145-29-7635"})
