(ns registratura.patients-list
  (:require [ajax.edn :as edn]
            [clojure.string :as str]
            day8.re-frame.http-fx
            [registratura.common :refer [<sub >evt]]
            [registratura.http :as http]
            [registratura.patients-filter :as filter]
            [registratura.routes :as routes]
            [re-frame.core :as rf]
            [tick.core :as t]
            [tick.locale-en-us]))

;; TODO: render unhandled error in the interface
(rf/reg-event-db :unhandled-error
  (fn [db]
    (assoc db :unhandled-error? true)))

(def ^:private patients-per-page
  20)

;; Pagination options for patients list:
;; 1. initial page load:
;;   {:pagination/limit 20
;;    :pagination/offset 0}
;; 2. click on "Load More" button:
;;   {:pagination/limit 20
;;    :pagination/offset loaded-patients-count}
;; 3. reloading the list after deleting some patient:
;;   {:pagination/limit loaded-patients-count
;;    :pagination/offset 0}

(rf/reg-event-fx ::load-patients
  (fn [{:keys [db]} [_ _ pagination-options load-more?]]
    (let [filter (:patients-filter db)]
      {:fx [[:http-xhrio {:method :get
                          :uri "/api/patients"
                          :params (merge filter
                                         (or pagination-options
                                             {:pagination/limit patients-per-page
                                              :pagination/offset 0}))
                          :response-format (http/edn-response-format)
                          :on-success [::patients-loaded load-more?]
                          :on-failure [:unhandled-error]}]]})))

(rf/reg-event-fx ::load-more-patients
  (fn [{:keys [db]}]
    (let [loaded-patients-count (-> db :patients :entities count)]
      {:fx [[:dispatch [::load-patients
                        nil
                        {:pagination/limit patients-per-page
                         :pagination/offset loaded-patients-count}
                        true]]]})))

(rf/reg-event-fx ::reload-patients
  (fn [{:keys [db]}]
    (let [loaded-patients-count (-> db :patients :entities count)]
      {:fx [[:dispatch [::load-patients
                        nil
                        {:pagination/limit loaded-patients-count
                         :pagination/offset 0}]]]})))

(rf/reg-event-db ::patients-loaded
  (fn [db [_ load-more? {:keys [entities total-count]}]]
    (if load-more?
      (-> db
          (update-in [:patients :entities] into entities)
          (assoc-in [:patients :total-count] total-count))
      (assoc db :patients {:entities entities
                           :total-count total-count}))))

(rf/reg-fx :dispatch-after-js-confirmation
  (fn [[message event]]
    (when (js/confirm message)
      (>evt event))))

(rf/reg-event-fx ::delete-patient-after-confirmation
  (fn [_ [_ patient-id]]
    {:fx [[:dispatch-after-js-confirmation
           ["Delete patient?"
            [::delete-patient patient-id]]]]}))

(rf/reg-event-fx ::delete-patient
  (fn [{:keys [db]} [_ patient-id]]
    {:fx [[:http-xhrio {:method :delete
                        :uri (str "/api/patients/" patient-id)
                        :format (edn/edn-request-format)
                        :response-format (http/edn-response-format)
                        :on-success [::reload-patients]
                        :on-failure [:unhandled-error]}]]}))

(def ^:private date-formatter
  (t/formatter "dd.MM.YYYY"))

(rf/reg-sub ::patients
  (fn [db]
    (->> (get-in db [:patients :entities])
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

(rf/reg-sub ::can-load-more?
  (fn [db]
    (> (get-in db [:patients :total-count])
       (count (get-in db [:patients :entities])))))

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
                  :gap "1rem"
                  :width "1100px"}}
    [filter/filter-panel]
    [:div {:style {:display :grid
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
         [:div {:style (assoc cell-style
                              :display :flex
                              :justify-content :space-between)}
          [:span insurance-number]
          [:span {:style {:cursor :pointer}
                  :on-click #(>evt [::delete-patient-after-confirmation id])}
           "×"]]]))]
    [:div {:style {:display :flex
                   :justify-content :center}}
     [:button {:on-click #(>evt [::load-more-patients])
               :disabled (not (<sub [::can-load-more?]))}
      "Load More"]]]])
