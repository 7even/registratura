(ns registratura.ui.core
  (:require [re-frame.core :as rf]
            [reagent.dom :as rd]
            [registratura.ui.common :refer [<sub]]
            [registratura.ui.db :as db]
            [registratura.ui.patients-list :as patients-list]
            [registratura.ui.patient-page :as patient-page]
            [registratura.ui.routes :as routes]
            [registratura.ui.views.common :refer [layout not-found]]))

(rf/reg-sub ::current-route
  (fn [db]
    (:current-route db)))

(def page-components
  {:patients-list patients-list/page
   :patient-page patient-page/page
   :new-patient-page patient-page/page
   :not-found not-found})

(defn interface []
  (let [{:keys [handler]} (<sub [::current-route])
        component (get page-components handler)]
    ;; don't render anything until the router sets current route
    (when (some? component)
      [layout
       ^{:key "page"} [component]])))

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
