(ns registratura.html
  (:require [hiccup.page :refer [html5]]))

(def page
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:title "Registratura"]
    [:style {:type "text/css"}
     "a { text-decoration: none; color: blue; }
      a:hover { text-decoration: underline; }"]]
   [:body
    [:div#root]
    [:script {:src "/js/main.js"}]]))
