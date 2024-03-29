(ns registratura.ui.patients-list
  (:require [ajax.edn :as edn]
            [clojure.string :as str]
            [registratura.ui.common :refer [<sub >evt full-name]]
            #?(:cljs [registratura.ui.http :as http])
            [registratura.ui.patients-filter :as filter]
            [registratura.ui.routes :as routes]
            [registratura.ui.views.common :refer [loading]]
            [re-frame.core :as rf]
            [tick.core :as t]
            [tick.locale-en-us]))

(def ^:private patients-per-page
  20)

(defn load-patients-fx
  "Returns an effect that loads the patients list using `pagination-options`
  (uses options for the first page by default). Dispatches `::patients-loaded`
  on success, passing it `load-more?` (if true, new patients will be appended
  to the end of the list of existing patients; otherwise new patients replace
  the old list)."
  [db pagination-options load-more?]
  (let [filter (:patients-filter db)]
    [:send-request {:method :get
                    :uri "/api/patients"
                    :params (merge (dissoc filter :errors)
                                   (or pagination-options
                                       {:pagination/limit patients-per-page
                                        :pagination/offset 0}))
                    :on-success [::patients-loaded load-more?]
                    :on-failure [:unhandled-error]}]))

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
  (fn [{:keys [db]} _]
    (let [new-db (filter/set-filter-from-query-params db)]
      {:db new-db
       :fx [(load-patients-fx new-db nil false)]})))

(rf/reg-event-fx ::load-more-patients
  (fn [{:keys [db]} _]
    (let [loaded-patients-count (-> db :patients :entities count)]
      {:db (assoc db :loading-more? true)
       :fx [(load-patients-fx db
                              {:pagination/limit patients-per-page
                               :pagination/offset loaded-patients-count}
                              true)]})))

(rf/reg-event-fx ::reload-patients
  (fn [{:keys [db]} _]
    (let [loaded-patients-count (-> db :patients :entities count)]
      {:fx [(load-patients-fx db
                              {:pagination/limit loaded-patients-count
                               :pagination/offset 0}
                              false)]})))

(rf/reg-event-db ::patients-loaded
  (fn [db [_ load-more? {:keys [entities total-count]}]]
    (-> db
        (update-in [:patients :entities]
                   (fn [existing-entities]
                     (if load-more?
                       (into existing-entities entities)
                       entities)))
        (assoc-in [:patients :total-count] total-count)
        (assoc :loading-more? false
               :patients-loaded? true))))

(rf/reg-event-fx ::delete-patient-after-confirmation
  (fn [_ [_ patient-id]]
    {:fx [[:dispatch-after-js-confirmation
           ["Delete patient?"
            [::delete-patient patient-id]]]]}))

(rf/reg-event-fx ::delete-patient
  (fn [{:keys [db]} [_ patient-id]]
    {:fx [[:send-request {:method :delete
                          :uri (str "/api/patients/" patient-id)
                          :on-success [::reload-patients]
                          :on-failure [:unhandled-error]}]]}))

(def ^:private date-formatter
  (t/formatter "dd.MM.YYYY"))

(rf/reg-sub ::patients
  (fn [db _]
    (->> (get-in db [:patients :entities])
         (map (fn [patient]
                (-> patient
                    (dissoc :patient/first-name :patient/middle-name :patient/last-name)
                    (assoc :patient/full-name (full-name patient))
                    (update :patient/gender (comp str/capitalize name))
                    (update :patient/birthday (partial t/format date-formatter))))))))

(rf/reg-sub ::can-load-more?
  (fn [db _]
    (and (not (:loading-more? db))
         (> (get-in db [:patients :total-count])
            (count (get-in db [:patients :entities]))))))

(rf/reg-sub ::patients-loaded?
  (fn [db _]
    (:patients-loaded? db)))

(def ^:private cell-style
  {:border-bottom "1px solid black"
   :padding "10px"})

(def ^:private header-style
  (assoc cell-style :font-weight :bold))

(defn page []
  (let [patients-loaded? (<sub [::patients-loaded?])
        patients (<sub [::patients])]
    (if patients-loaded?
      [:div {:style {:display :flex
                     :flex-direction :column
                     :gap "1rem"}}
       [filter/filter-panel]
       [:div {:style {:display :grid
                      :grid-template-columns "2fr 1fr 1fr 4fr 150px"}}
        [:div {:style header-style} "Name"]
        [:div {:style header-style} "Gender"]
        [:div {:style header-style} "Birthday"]
        [:div {:style header-style} "Address"]
        [:div {:style header-style} "Insurance number"]
        (if (seq patients)
          (doall
           (for [{:patient/keys [id
                                 full-name
                                 gender
                                 birthday
                                 address
                                 insurance-number]} patients
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
                "×"]]]))
          [:div {:style {:grid-column "1 / 6"
                         :padding "10px"
                         :display :flex
                         :justify-content :center
                         :font-style :italic}}
           "No patients found"])]
       [:div {:style {:display :flex
                      :justify-content :center}}
        [:button {:on-click #(>evt [::load-more-patients])
                  :disabled (not (<sub [::can-load-more?]))}
         "Load More"]]]
      [loading])))
