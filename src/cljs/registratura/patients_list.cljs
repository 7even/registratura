(ns registratura.patients-list
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx ::load-patients
  (fn []
    (println "loading patients")))

(defn page []
  [:div {:style {:display :flex
                 :justify-content :center}}
   [:h2 "Patients"]])
