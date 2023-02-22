(ns registratura.core
  (:require [reagent.dom :as rd]
            [re-frame.core :as rf]))

(defn interface []
  [:h2 "Registratura"])

(defn- render []
  (rd/render [interface]
             (js/document.getElementById "root")))

(defn init []
  (render))

(defn load []
  (rf/clear-subscription-cache!)
  (render))
