(ns registratura.ui.routes
  (:require [bidi.bidi :refer [match-route path-for]]
            [clojure.string :as str]
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
   (defn parse-url [path+query-string]
     (let [current-url (-> js/window .-location js/URL.)
           protocol (.-protocol current-url)
           host (.-host current-url)
           url (js/URL. (str protocol "//" host path+query-string))
           path (.-pathname url)
           query-params (->> url
                             .-searchParams
                             seq
                             js->clj
                             (reduce (fn [acc [k v]]
                                       (assoc acc (keyword k) v))
                                     {}))]
       (merge (match-route routes path)
              (when (seq query-params)
                {:query-params query-params})))))

#?(:cljs
   (defn generate-url
     ([handler]
      (generate-url handler {} {}))
     ([handler route-params]
      (generate-url handler route-params {}))
     ([handler route-params query-params]
      (let [search-params (js/URLSearchParams.)]
        (doseq [[k v] query-params]
          (.set search-params
                (name k)
                (if (keyword? v)
                  (name v)
                  (str v))))
        (->> [(url-for handler route-params)
              (let [search (.toString search-params)]
                (when-not (str/blank? search)
                  search))]
             (keep identity)
             (str/join "?"))))))

#?(:cljs
   (def history
     (pushy/pushy #(>evt [::set-route %]) parse-url)))

#?(:cljs
   (defn go-to [& args]
     (pushy/set-token! history (apply generate-url args))))

#?(:cljs
   (rf/reg-fx :go-to
     (fn [route-and-params]
       (apply go-to route-and-params))))

#?(:cljs
   (defn start []
     (pushy/start! history)))
