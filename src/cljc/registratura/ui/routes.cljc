(ns registratura.ui.routes
  (:require [bidi.bidi :refer [match-route path-for]]
            #?(:cljs [pushy.core :as pushy])
            [re-frame.core :as rf]
            [registratura.ui.common :refer [>evt]]))

(def routes
  ["/" {"" :patients-list
        "patients/new" :new-patient-page
        ["patients/" [#"\d+" :id]] :patient-page
        true :not-found}])

(def url-for
  (partial path-for routes))

(def page-load-events
  {:patients-list :registratura.ui.patients-list/load-patients
   :new-patient-page :registratura.ui.patient-page/initialize-new-patient
   :patient-page :registratura.ui.patient-page/load-patient})

(rf/reg-event-fx ::set-route
  (fn [{:keys [db]} [_ route]]
    {:db (assoc db :current-route route)
     :fx [(when-let [page-load-event (get page-load-events (:handler route))]
            [:dispatch [page-load-event route]])]}))

#?(:cljs
   (def history
     (pushy/pushy #(>evt [::set-route %])
                  (partial match-route routes))))

#?(:cljs
   (defn start []
     (pushy/start! history)))
