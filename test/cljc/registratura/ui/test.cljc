(ns registratura.ui.test
  (:require [re-frame.core :as rf]))

(defonce requests
  (atom []))

#?(:clj
   (rf/reg-fx :send-request
     (fn [request]
       (swap! requests conj request))))
