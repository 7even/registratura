(ns registratura.ui.patient-page
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [registratura.ui.common :refer [<sub >evt full-name v]]
            [registratura.ui.routes :as routes]
            [registratura.ui.views.common :refer [loading]]
            [tick.core :as t]))

(rf/reg-event-db ::initialize-new-patient
  (fn [db _]
    (assoc db
           :patient
           {:patient/first-name nil
            :patient/middle-name nil
            :patient/last-name nil
            :patient/gender nil
            :patient/birthday nil
            :patient/insurance-number nil
            :patient/address nil})))

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

(defn- new-patient? [db]
  (= (get-in db [:current-route :handler])
     :new-patient-page))

(rf/reg-sub ::new-patient?
  (fn [db _]
    (new-patient? db)))

(rf/reg-sub ::patient-loaded?
  (fn [db _]
    (:patient-loaded? db)))

(rf/reg-sub ::patient-attribute
  :<- [::patient]
  (fn [patient [_ attr-name]]
    (get patient attr-name)))

(rf/reg-event-db ::change-patient-attribute
  (fn [db [_ attr-name new-attr-value]]
    (assoc-in db
              [:patient attr-name]
              (when-not (and (string? new-attr-value)
                             (str/blank? new-attr-value))
                new-attr-value))))

(rf/reg-sub ::patient-attribute-errors
  :<- [::patient]
  (fn [patient [_ attr-name]]
    (get-in patient [:errors attr-name])))

(defn- get-errors [{:patient/keys [first-name last-name gender
                                   birthday insurance-number address]}]
  (let [add-error (fnil conj [])]
    (cond-> {}
      (str/blank? first-name)
      (update :patient/first-name add-error "First name is required")

      (str/blank? last-name)
      (update :patient/last-name add-error "Last name is required")

      (nil? gender)
      (update :patient/gender add-error "Gender is required")

      (nil? birthday)
      (update :patient/birthday add-error "Birthday is required")

      (str/blank? insurance-number)
      (update :patient/insurance-number add-error "Insurance number is required")

      (str/blank? address)
      (update :patient/address add-error "Address is required"))))

(rf/reg-event-fx ::submit-patient
  (fn [{:keys [db]} _]
    (let [patient (:patient db)
          errors (get-errors patient)]
      (if (empty? errors)
        {:db (-> db
                 (update :patient dissoc :errors)
                 (assoc :saving-patient? true))
         :fx [[:send-request (if (new-patient? db)
                               {:method :post
                                :uri "/api/patients"
                                :params (dissoc patient :patient/id :errors)
                                :on-success [::patient-created]
                                :on-failure [:unhandled-error]}
                               {:method :patch
                                :uri (str "/api/patients/" (:patient/id patient))
                                :params (dissoc patient :patient/id :errors)
                                :on-success [::patient-saved]
                                :on-failure [:unhandled-error]})]]}
        {:db (assoc-in db [:patient :errors] errors)}))))

(rf/reg-event-fx ::patient-created
  (fn [{:keys [db]} [_ {:patient/keys [id]}]]
    {:db (dissoc db :saving-patient?)
     :fx [[:go-to [:patient-page {:id id}]]]}))

(rf/reg-event-db ::patient-saved
  (fn [db _]
    (dissoc db :saving-patient?)))

(rf/reg-sub ::saving-patient?
  (fn [db _]
    (:saving-patient? db)))

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
                                    t/date)])}])
    (doall
     (for [message (<sub [::patient-attribute-errors attr-name])]
       ^{:key message}
       [:div {:style {:color :red}}
        message]))]])

(defn page []
  (let [new-patient? (<sub [::new-patient?])
        patient-loaded? (<sub [::patient-loaded?])]
    (if (or new-patient? patient-loaded?)
      [:form {:style {:display :flex
                      :flex-direction :column
                      :gap "1rem"}
              :on-submit (fn [e]
                           (.preventDefault e))}
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
         [:button {:on-click #(>evt [::submit-patient])
                   :disabled (<sub [::saving-patient?])}
          (if new-patient?
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
