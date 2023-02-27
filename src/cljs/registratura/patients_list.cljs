(ns registratura.patients-list
  (:require [clojure.string :as str]
            day8.re-frame.http-fx
            [registratura.common :refer [<sub >evt]]
            [registratura.http :as http]
            [registratura.patients-filter :as filter]
            [registratura.routes :as routes]
            [re-frame.core :as rf]
            [tick.core :as t]
            [tick.locale-en-us]))

(rf/reg-event-fx ::load-patients
  (fn [{:keys [db]}]
    (let [filter (:patients-filter db)]
      {:fx [[:http-xhrio {:method :get
                          :uri "/api/patients"
                          :params filter
                          :response-format (http/edn-response-format)
                          :on-success [::patients-loaded]
                          :on-failure [::failed-to-load-patients]}]]})))

(rf/reg-event-db ::patients-loaded
  (fn [db [_ patients]]
    (assoc db :patients patients)))

;; TODO: render unhandled error in the interface
(rf/reg-event-db ::failed-to-load-patients
  (fn [db]
    (assoc db :unhandled-error? true)))

(def ^:private date-formatter
  (t/formatter "dd.MM.YYYY"))

(rf/reg-sub ::patients
  (fn [db]
    (->> (:patients db)
         (map (fn [{:patient/keys [first-name
                                   middle-name
                                   last-name]
                    :as patient}]
                (-> patient
                    (dissoc :patient/first-name :patient/middle-name :patient/last-name)
                    (assoc :patient/full-name
                           (->> [first-name middle-name last-name]
                                (remove str/blank?)
                                (str/join " ")))
                    (update :patient/gender (comp str/capitalize name))
                    (update :patient/birthday (partial t/format date-formatter))))))))

(def ^:private cell-style
  {:border-bottom "1px solid black"
   :padding "10px"})

(def ^:private header-style
  (assoc cell-style :font-weight :bold))

(defn page []
  [:div {:style {:display :flex
                 :justify-content :center}}
   [:div {:style {:display :flex
                  :flex-direction :column
                  :gap "1rem"}}
    [filter/filter-panel]
    [:div {:style {:width "1100px"
                   :display :grid
                   :grid-template-columns "2fr 1fr 1fr 4fr 150px"}}
     [:div {:style header-style} "Name"]
     [:div {:style header-style} "Gender"]
     [:div {:style header-style} "Birthday"]
     [:div {:style header-style} "Address"]
     [:div {:style header-style} "Insurance number"]
     (doall
      (for [{:patient/keys [id
                            full-name
                            gender
                            birthday
                            address
                            insurance-number]} (<sub [::patients])
            :let [patient-url (routes/url-for :patient-page {:id id})]]
        ^{:key id}
        [:<>
         [:div {:style cell-style}
          [:a {:href patient-url} full-name]]
         [:div {:style cell-style} gender]
         [:div {:style cell-style} birthday]
         [:div {:style cell-style} address]
         [:div {:style cell-style} insurance-number]]))]]])
