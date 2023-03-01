(ns registratura.ui.common
  (:require [re-frame.core :as rf]))

(defn <sub [subscription]
  @(rf/subscribe subscription))

(defn >evt [event]
  (rf/dispatch event))

(defn >evt! [event]
  (rf/dispatch-sync event))

(def error-input-style
  {:outline-color :red
   :border-color :red})

(defn v
  "Returns input value from its on-change event `e`."
  [e]
  (-> e .-target .-value))

;; TODO: render unhandled error in the interface
(rf/reg-event-db :unhandled-error
  (fn [db]
    (assoc db :unhandled-error? true)))

#?(:cljs
   (rf/reg-fx :dispatch-after-js-confirmation
     (fn [[message event]]
       (when (js/confirm message)
         (>evt event)))))
