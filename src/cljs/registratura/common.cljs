(ns registratura.common
  (:require [re-frame.core :as rf]))

(defn <sub [subscription]
  @(rf/subscribe subscription))

(defn >evt [event]
  (rf/dispatch event))
