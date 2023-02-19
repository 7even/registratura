(ns registratura.html
  (:require [hiccup.page :refer [html5]]))

(def page
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:title "Registratura"]]
   [:body
    [:div#root]
    [:script {:src "/js/main.js"}]]))
