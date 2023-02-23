(ns registratura.core
  (:require [reagent.dom :as rd]
            [registratura.patients-list :as patients-list]
            [registratura.patient-page :as patient-page]
            [registratura.routes :as routes]
            [re-frame.core :as rf]))

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
  (let [{:keys [handler]} @(rf/subscribe [::current-route])]
    (case handler
      :patients-list [patients-list/page]
      :patient-page [patient-page/page]
      [not-found])))

(defn- render []
  (rd/render [interface]
             (js/document.getElementById "root")))

(defn init []
  (routes/start)
  (render))

(defn load []
  (rf/clear-subscription-cache!)
  (render))
