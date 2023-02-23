(ns registratura.routes
  (:require [bidi.bidi :refer [match-route]]
            [pushy.core :as pushy]
            [re-frame.core :as rf]
            [registratura.common :refer [>evt]]))

(def routes
  ["/" {"" :patients-list
        "patients/new" :new-patient-page
        ["patients/" [#"\d+" :id]] :patient-page}])

(def page-load-events
  {:patients-list :registratura.patients-list/load-patients})

(rf/reg-event-fx ::set-route
  (fn [{:keys [db]} [_ route]]
    {:db (assoc db :current-route route)
     :fx [(when-let [page-load-event (get page-load-events (:handler route))]
            [:dispatch [page-load-event route]])]}))

(def history
  (pushy/pushy #(>evt [::set-route %])
               (partial match-route routes)))

(defn start []
  (pushy/start! history))
