(ns registratura.patients-filter
  (:require [clojure.string :as str]
            [registratura.common :refer [<sub >evt error-input-style v]]
            [re-frame.core :as rf]))

(rf/reg-sub ::patients-filter
  (fn [db]
    (:patients-filter db)))

(rf/reg-sub ::search-string
  :<- [::patients-filter]
  (fn [patients-filter]
    (get patients-filter :patient/search "")))

(rf/reg-event-db ::change-search-string
  (fn [db [_ new-search-string]]
    (if (str/blank? new-search-string)
      (update db
              :patients-filter
              dissoc
              :patient/search)
      (assoc-in db
                [:patients-filter :patient/search]
                new-search-string))))

(rf/reg-sub ::include-patients-with-gender?
  :<- [::patients-filter]
  (fn [patients-filter [_ gender]]
    (-> patients-filter
        :patient/gender
        (contains? gender))))

(rf/reg-event-db ::toggle-gender-filter
  (fn [db [_ toggled-gender]]
    (update-in db
               [:patients-filter :patient/gender]
               (fn [selected-genders]
                 (if (contains? selected-genders toggled-gender)
                   (disj selected-genders toggled-gender)
                   (conj selected-genders toggled-gender))))))

(rf/reg-sub ::age-filter
  :<- [::patients-filter]
  (fn [patients-filter [_ filter-kind]]
    (get-in patients-filter [:patient/age filter-kind] "")))

(rf/reg-event-db ::change-age-filter
  (fn [db [_ filter-kind new-value]]
    (if (str/blank? new-value)
      (update-in db
                 [:patients-filter :patient/age]
                 dissoc
                 filter-kind)
      (assoc-in db
                [:patients-filter :patient/age filter-kind]
                (parse-long new-value)))))

(def ^:private max-search-string-length
  100)

(defn- get-filter-errors [{search-string :patient/search
                           {min-age :minimum
                            max-age :maximum} :patient/age}]
  (let [add-error (fnil conj [])]
    (cond-> {}
      (and (some? min-age)
           (neg? min-age))
      (update-in [:patient/age :minimum]
                 add-error
                 "Minimum age cannot be negative")

      (and (some? max-age)
           (neg? max-age))
      (update-in [:patient/age :maximum]
                 add-error
                 "Maximum age cannot be negative")

      (and (some? min-age)
           (some? max-age)
           (< max-age min-age))
      (update-in [:patient/age :maximum]
                 add-error
                 "Maximum age cannot be lower than minimum age")

      (> (count search-string) max-search-string-length)
      (update :patient/search
              add-error
              (str "Search string length cannot exceed " max-search-string-length " characters")))))

(rf/reg-event-fx ::submit-new-filter
  (fn [{:keys [db]}]
    (let [filter (:patients-filter db)
          errors (get-filter-errors filter)]
      (if (empty? errors)
        {:db (update db :patients-filter dissoc :errors)
         :fx [[:dispatch [:registratura.patients-list/load-patients]]]}
        {:db (assoc-in db [:patients-filter :errors] errors)}))))

(rf/reg-sub ::search-string-errors
  :<- [::patients-filter]
  (fn [patients-filter]
    (get-in patients-filter [:errors :patient/search])))

(rf/reg-sub ::min-age-errors
  :<- [::patients-filter]
  (fn [patients-filter]
    (get-in patients-filter [:errors :patient/age :minimum])))

(rf/reg-sub ::max-age-errors
  :<- [::patients-filter]
  (fn [patients-filter]
    (get-in patients-filter [:errors :patient/age :maximum])))

(defn filter-panel []
  [:div {:style {:display :flex
                 :flex-direction :column
                 :gap "1rem"
                 :padding "10px"}}
   [:form {:style {:display :contents}
           :on-submit (fn [e]
                        (.preventDefault e))}
    (let [error-messages (<sub [::search-string-errors])]
      [:div
       [:input {:type :text
                :value (<sub [::search-string])
                :on-change (fn [e]
                             (>evt [::change-search-string (v e)]))
                :placeholder "Search patients by name, address or insurance number"
                :style (merge {:width "100%"}
                              (when (seq error-messages)
                                error-input-style))}]
       (doall
        (for [message error-messages]
          [:div {:style {:color :red}} message]))])
    [:div {:style {:display :grid
                   :grid-template-columns "20% 11% 10% 1fr 10%"
                   :grid-auto-rows "30px"
                   :align-items :center}}
     [:label
      [:input {:type :checkbox
               :checked (<sub [::include-patients-with-gender? :gender/male])
               :on-change #(>evt [::toggle-gender-filter :gender/male])}]
      "Include male patients"]
     [:div "Minimum age:"]
     (let [error-messages (<sub [::min-age-errors])]
       [:<>
        [:input {:type :number
                 :value (<sub [::age-filter :minimum])
                 :on-change (fn [e]
                              (>evt [::change-age-filter :minimum (v e)]))
                 :style (merge {:height "1rem"}
                               (when (seq error-messages)
                                 error-input-style))}]
        [:div {:style {:padding-left "10px"}}
         (doall
          (for [message error-messages]
            [:div {:style {:color :red}} message]))]])
     [:div {:style {:display :flex
                    :justify-content :flex-end}}
      [:button {:on-click #(>evt [::submit-new-filter])}
       "Apply"]]
     [:label
      [:input {:type :checkbox
               :checked (<sub [::include-patients-with-gender? :gender/female])
               :on-change #(>evt [::toggle-gender-filter :gender/female])}]
      "Include female patients"]
     [:div "Maximum age:"]
     (let [error-messages (<sub [::max-age-errors])]
       [:<>
        [:input {:type :number
                 :value (<sub [::age-filter :maximum])
                 :on-change (fn [e]
                              (>evt [::change-age-filter :maximum (v e)]))
                 :style (merge {:height "1rem"}
                               (when (seq error-messages)
                                 error-input-style))}]
        [:div {:style {:padding-left "10px"}}
         (doall
          (for [message error-messages]
            [:div {:style {:color :red}} message]))]])
     [:div]]]])
