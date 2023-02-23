(ns registratura.patients-list
  (:require day8.re-frame.http-fx
            [registratura.http :as http]
            [re-frame.core :as rf]))

(rf/reg-event-fx ::load-patients
  (fn []
    {:fx [[:http-xhrio {:method :get
                        :uri "/api/patients"
                        :response-format (http/edn-response-format)
                        :on-success [::patients-loaded]
                        :on-failure [::failed-to-load-patients]}]]}))

(rf/reg-event-db ::patients-loaded
  (fn [db [_ patients]]
    (assoc db :patients patients)))

;; TODO: render unhandled error in the interface
(rf/reg-event-db ::failed-to-load-patients
  (fn [db]
    (assoc db :unhandled-error? true)))

(defn page []
  [:div {:style {:display :flex
                 :justify-content :center}}
   [:h2 "Patients"]])
