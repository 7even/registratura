(ns registratura.ui.core
  (:require [re-frame.core :as rf]
            [reagent.dom :as rd]
            [registratura.ui.common :refer [<sub]]
            [registratura.ui.db :as db]
            [registratura.ui.patients-list :as patients-list]
            [registratura.ui.patient-page :as patient-page]
            [registratura.ui.routes :as routes]))

(rf/reg-sub ::current-route
  (fn [db]
    (:current-route db)))

(defn not-found []
  [:div {:style {:display :flex
                 :justify-content :center
                 :align-items :center
                 :height "100vh"}}
   [:h2 "Page Not Found"]])

(defn interface []
  (let [{:keys [handler]} (<sub [::current-route])]
    (case handler
      :patients-list [patients-list/page]
      :patient-page [patient-page/page]
      [not-found])))

(defn- render []
  (rd/render [interface]
             (js/document.getElementById "root")))

(defn init []
  (routes/start)
  (rf/dispatch-sync [::db/initialize])
  (render))

(defn load []
  (rf/clear-subscription-cache!)
  (render))
