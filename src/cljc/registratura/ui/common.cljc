(ns registratura.ui.common
  (:require [clojure.string :as str]
            [re-frame.core :as rf]))

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

(defn full-name [{:patient/keys [first-name middle-name last-name]}]
  (->> [first-name middle-name last-name]
       (remove str/blank?)
       (str/join " ")))

(rf/reg-sub :loading?
  (fn [db _]
    (:loading? db)))

;; TODO: render unhandled error in the interface
(rf/reg-event-db :unhandled-error
  (fn [db]
    (assoc db :unhandled-error? true)))

#?(:cljs
   (rf/reg-fx :dispatch-after-js-confirmation
     (fn [[message event]]
       (when (js/confirm message)
         (>evt event)))))
