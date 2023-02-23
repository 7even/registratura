(ns registratura.patient-page
  (:require [re-frame.core :as rf]))

(rf/reg-sub ::patient-id
  (fn [db]
    (-> db
        (get-in [:current-route :route-params :id])
        parse-long)))

(defn page []
  [:div {:style {:display :flex
                 :justify-content :center}}
   [:h2 (str "Patient #" @(rf/subscribe [::patient-id]))]])
