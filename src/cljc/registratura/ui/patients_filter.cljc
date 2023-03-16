(ns registratura.ui.patients-filter
  (:require [clojure.string :as str]
            [registratura.ui.common :refer [<sub >evt error-input-style v]]
            [registratura.ui.routes :as routes]
            [re-frame.core :as rf]))

(def ^:private available-genders
  #{:gender/male :gender/female})

(defn- serialize-filter [{:patient/keys [query genders min-age max-age]}]
  (cond-> {}
    (not (str/blank? query))
    (assoc :query query)

    (and (seq genders)
         (not= genders available-genders))
    (assoc :genders (->> genders
                         (map name)
                         (str/join ",")))

    (some? min-age)
    (assoc :min-age min-age)

    (some? max-age)
    (assoc :max-age max-age)))

(defn- deserialize-filter [{:keys [query genders min-age max-age]}]
  (merge (when (some? query)
           {:patient/query query})
         (if (some? genders)
           {:patient/genders (->> (str/split genders #",")
                                  (map #(keyword "gender" %))
                                  (into #{}))}
           {:patient/genders available-genders})
         (when-let [min-age-number (and (some? min-age)
                                        (parse-long min-age))]
           {:patient/min-age min-age-number})
         (when-let [max-age-number (and (some? max-age)
                                        (parse-long max-age))]
           {:patient/max-age max-age-number})))

(defn set-filter-from-query-params [db]
  (let [filter (-> db
                   (get-in [:current-route :query-params])
                   deserialize-filter)]
    (assoc db :patients-filter filter)))

(rf/reg-sub ::patients-filter
  (fn [db _]
    (:patients-filter db)))

(rf/reg-sub ::search-query
  :<- [::patients-filter]
  (fn [patients-filter _]
    (get patients-filter :patient/query "")))

(rf/reg-event-db ::change-search-query
  (fn [db [_ new-search-query]]
    (if (str/blank? new-search-query)
      (update db
              :patients-filter
              dissoc
              :patient/query)
      (assoc-in db
                [:patients-filter :patient/query]
                new-search-query))))

(rf/reg-sub ::include-patients-with-gender?
  :<- [::patients-filter]
  (fn [patients-filter [_ gender]]
    (-> patients-filter
        :patient/genders
        (contains? gender))))

(rf/reg-event-db ::toggle-gender-filter
  (fn [db [_ toggled-gender]]
    (update-in db
               [:patients-filter :patient/genders]
               (fn [selected-genders]
                 (let [toggle-fn (-> (if (contains? selected-genders toggled-gender)
                                       disj
                                       conj)
                                     (fnil #{}))]
                   (toggle-fn selected-genders toggled-gender))))))

(rf/reg-sub ::min-age
  :<- [::patients-filter]
  (fn [patients-filter _]
    (get patients-filter :patient/min-age "")))

(rf/reg-sub ::max-age
  :<- [::patients-filter]
  (fn [patients-filter _]
    (get patients-filter :patient/max-age "")))

(rf/reg-event-db ::change-min-age
  (fn [db [_ new-min-age]]
    (if (str/blank? new-min-age)
      (update db :patients-filter dissoc :patient/min-age)
      (assoc-in db [:patients-filter :patient/min-age] (parse-long new-min-age)))))

(rf/reg-event-db ::change-max-age
  (fn [db [_ new-max-age]]
    (if (str/blank? new-max-age)
      (update db :patients-filter dissoc :patient/max-age)
      (assoc-in db [:patients-filter :patient/max-age] (parse-long new-max-age)))))

(def ^:private max-search-query-length
  100)

(defn- get-filter-errors [{:patient/keys [query min-age max-age]}]
  (let [add-error (fnil conj [])]
    (cond-> {}
      (and (some? min-age)
           (neg? min-age))
      (update :patient/min-age
              add-error
              "Minimum age cannot be negative")

      (and (some? max-age)
           (neg? max-age))
      (update :patient/max-age
              add-error
              "Maximum age cannot be negative")

      (and (some? min-age)
           (some? max-age)
           (< max-age min-age))
      (update :patient/max-age
              add-error
              "Maximum age cannot be lower than minimum age")

      (> (count query) max-search-query-length)
      (update :patient/query
              add-error
              (str "Search query length cannot exceed " max-search-query-length " characters")))))

(rf/reg-event-fx ::submit-new-filter
  (fn [{:keys [db]} _]
    (let [filter (:patients-filter db)
          errors (get-filter-errors filter)]
      (if (empty? errors)
        {:db (update db :patients-filter dissoc :errors)
         :fx [[:go-to [:patients-list {} (serialize-filter filter)]]]}
        {:db (assoc-in db [:patients-filter :errors] errors)}))))

(rf/reg-sub ::search-query-errors
  :<- [::patients-filter]
  (fn [patients-filter _]
    (get-in patients-filter [:errors :patient/query])))

(rf/reg-sub ::min-age-errors
  :<- [::patients-filter]
  (fn [patients-filter _]
    (get-in patients-filter [:errors :patient/min-age])))

(rf/reg-sub ::max-age-errors
  :<- [::patients-filter]
  (fn [patients-filter _]
    (get-in patients-filter [:errors :patient/max-age])))

(defn filter-panel []
  [:div {:style {:display :flex
                 :flex-direction :column
                 :gap "1rem"
                 :padding "10px"}}
   [:form {:style {:display :contents}
           :on-submit (fn [e]
                        (.preventDefault e))}
    (let [error-messages (<sub [::search-query-errors])]
      [:div
       [:input {:type :text
                :value (<sub [::search-query])
                :on-change (fn [e]
                             (>evt [::change-search-query (v e)]))
                :placeholder "Search patients by name, address or insurance number"
                :style (merge {:width "100%"}
                              (when (seq error-messages)
                                error-input-style))}]
       (doall
        (for [message error-messages]
          ^{:key message}
          [:div {:style {:color :red}} message]))])
    [:div {:style {:display :grid
                   :grid-template-columns "20% 11% 10% 1fr 15%"
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
                 :value (<sub [::min-age])
                 :on-change (fn [e]
                              (>evt [::change-min-age (v e)]))
                 :style (merge {:height "1rem"}
                               (when (seq error-messages)
                                 error-input-style))}]
        [:div {:style {:padding-left "10px"}}
         (doall
          (for [message error-messages]
            ^{:key message}
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
                 :value (<sub [::max-age])
                 :on-change (fn [e]
                              (>evt [::change-max-age (v e)]))
                 :style (merge {:height "1rem"}
                               (when (seq error-messages)
                                 error-input-style))}]
        [:div {:style {:padding-left "10px"}}
         (doall
          (for [message error-messages]
            ^{:key message}
            [:div {:style {:color :red}} message]))]])
     [:div {:style {:display :flex
                    :justify-content :flex-end}}
      [:a {:href (routes/url-for :new-patient-page)}
       "Create new patient"]]]]])
