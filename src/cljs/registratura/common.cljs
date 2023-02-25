(ns registratura.common
  (:require [re-frame.core :as rf]))

(defn <sub [subscription]
  @(rf/subscribe subscription))

(defn >evt [event]
  (rf/dispatch event))

(def error-input-style
  {:outline-color :red
   :border-color :red})

(defn v
  "Returns input value from its on-change event `e`."
  [e]
  (-> e .-target .-value))
