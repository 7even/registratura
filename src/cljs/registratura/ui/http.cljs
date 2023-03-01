(ns registratura.ui.http
  (:require ajax.edn
            [ajax.interceptors :refer [map->ResponseFormat]]
            ajax.simple
            [ajax.protocols :refer [-body]]
            [cljs.reader :as edn]
            [day8.re-frame.http-fx :as http-fx]
            [re-frame.core :as rf]
            [time-literals.read-write :refer [tags]]))

(defn edn-response-format
  "Custom response format for parsing server response EDN
  with custom time literals."
  []
  (map->ResponseFormat {:read (fn [xhrio]
                                (edn/read-string {:readers tags}
                                                 (-body xhrio)))
                        :description "EDN"
                        :content-type ["application/edn"]}))

(rf/reg-fx :send-request
  (fn [request-properties]
    (let [request (merge (select-keys request-properties
                                      [:method
                                       :uri
                                       :params
                                       :on-success
                                       :on-failure])
                         {:response-format (edn-response-format)}
                         (when-not (= (:method request-properties)
                                      :get)
                           {:format (ajax.edn/edn-request-format)}))
          xhrio (-> request
                    http-fx/request->xhrio-options
                    ajax.simple/ajax-request)]
      (http-fx/dispatch-on-request request xhrio))))
