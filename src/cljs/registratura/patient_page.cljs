(ns registratura.patient-page
  (:require [re-frame.core :as rf]
            [registratura.common :refer [<sub]]))

(rf/reg-sub ::patient-id
  (fn [db]
    (-> db
        (get-in [:current-route :route-params :id])
        parse-long)))

(defn page []
  (let [patient-id (<sub [::patient-id])]
    [:div {:style {:display :flex
                   :justify-content :center}}
     [:h2 (str "Patient #" patient-id)]]))
