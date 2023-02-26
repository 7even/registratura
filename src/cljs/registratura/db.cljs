(ns registratura.db
  (:require [re-frame.core :as rf]))

(rf/reg-event-db ::initialize
  (fn [db]
    {:patients-filter {:patient/genders #{:gender/male
                                          :gender/female}}}))
