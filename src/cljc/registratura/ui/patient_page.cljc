(ns registratura.ui.patient-page
  (:require [re-frame.core :as rf]
            [registratura.ui.common :refer [<sub >evt full-name v]]
            [registratura.ui.routes :as routes]
            [registratura.ui.views.common :refer [loading]]
            [tick.core :as t]))

(rf/reg-event-db ::initialize-new-patient
  (fn [db _]
    (assoc db :patient {})))

(rf/reg-event-fx ::load-patient
  (fn [{:keys [db]} _]
    (let [patient-id (-> db
                         (get-in [:current-route :route-params :id] "")
                         parse-long)]
      {:fx [[:send-request {:method :get
                            :uri (str "/api/patients/" patient-id)
                            :on-success [::patient-loaded]
                            :on-failure [::patient-not-found]}]]})))

(rf/reg-event-db ::patient-loaded
  (fn [db [_ patient-data]]
    (assoc db
           :patient patient-data
           :patient-loaded? true)))

(rf/reg-event-db ::patient-not-found
  (fn [db _]
    (assoc db :current-route {:handler :not-found})))

(rf/reg-sub ::patient
  (fn [db _]
    (:patient db)))

(rf/reg-sub ::full-name
  :<- [::patient]
  (fn [patient _]
    (full-name patient)))

(rf/reg-sub ::new-patient?
  (fn [db _]
    (= (get-in db [:current-route :handler])
       :new-patient-page)))

(rf/reg-sub ::patient-loaded?
  (fn [db _]
    (:patient-loaded? db)))

(rf/reg-sub ::patient-attribute
  :<- [::patient]
  (fn [patient [_ attr-name]]
    (get patient attr-name)))

(rf/reg-event-db ::change-patient-attribute
  (fn [db [_ attr-name new-attr-value]]
    (assoc-in db [:patient attr-name] new-attr-value)))

(defn input [{:keys [type label attr-name style values]
              :or {type :text}}]
  [:div {:style (merge {:padding "10px"}
                       style)}
   [:label {:style {:display :flex
                    :flex-direction :column}}
    label
    (if (= type :radio)
      [:div {:style {:display :flex
                     :gap "1rem"}}
       (doall
        (for [[value label] values]
          ^{:key value}
          [:label
           [:input {:type :radio
                    :name attr-name
                    :value (name value)
                    :checked (= (<sub [::patient-attribute attr-name])
                                value)
                    :on-change #(>evt [::change-patient-attribute
                                       attr-name
                                       (keyword (name attr-name) (v %))])}]
           label]))]
      [:input {:type type
               :value (<sub [::patient-attribute attr-name])
               :on-change #(>evt [::change-patient-attribute
                                  attr-name
                                  (cond-> %
                                    :always
                                    v

                                    (= type :date)
                                    t/date)])}])]])

(defn page []
  (let [new-patient? (<sub [::new-patient?])
        patient-loaded? (<sub [::patient-loaded?])]
    (if (or new-patient? patient-loaded?)
      [:div {:style {:display :flex
                     :flex-direction :column
                     :gap "1rem"}}
       [:div {:style {:display :flex
                      :align-items :center}}
        [:div {:style {:flex 1
                       :padding "10px"}}
         [:a {:href (routes/url-for :patients-list)}
          "< Back to list"]]
        [:div {:style {:flex 1
                       :display :flex
                       :justify-content :center
                       :font-weight :bold
                       :padding "10px"}}
         (if new-patient?
           "New patient"
           (<sub [::full-name]))]
        [:div {:style {:flex 1
                       :display :flex
                       :justify-content :flex-end
                       :padding "10px"}}
         [:button (if new-patient?
                    "Create"
                    "Save")]]]
       [:div {:style {:display :grid
                      :grid-template-columns "repeat(3, 1fr)"}}
        [input {:label "First name"
                :attr-name :patient/first-name}]
        [input {:label "Middle name"
                :attr-name :patient/middle-name}]
        [input {:label "Last name"
                :attr-name :patient/last-name}]
        [input {:type :radio
                :label "Gender"
                :attr-name :patient/gender
                :values [[:gender/male "Male"]
                         [:gender/female "Female"]]}]
        [input {:type :date
                :label "Birthday"
                :attr-name :patient/birthday}]
        [input {:label "Insurance number"
                :attr-name :patient/insurance-number}]
        [input {:label "Address"
                :attr-name :patient/address
                :style {:grid-column "1 / 4"}}]]]
      [loading])))
