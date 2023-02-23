(ns registratura.http
  (:require [ajax.interceptors :refer [map->ResponseFormat]]
            [ajax.protocols :refer [-body]]
            [cljs.reader :as edn]
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
