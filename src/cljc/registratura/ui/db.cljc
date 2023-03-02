(ns registratura.ui.db
  (:require [re-frame.core :as rf]))

(rf/reg-event-db ::initialize
  (fn [db _]
    {:patients-filter {:patient/genders #{:gender/male
                                          :gender/female}}}))
