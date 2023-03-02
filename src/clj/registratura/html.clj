(ns registratura.html
  (:require [hiccup.page :refer [html5]]))

(def page
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:title "Registratura"]
    [:style {:type "text/css"}
     "a { text-decoration: none; color: blue; }
      a:hover { text-decoration: underline; }
      * { font-family: \"SF Pro Text\",\"SF Pro Icons\",\"Helvetica Neue\",\"Helvetica\",\"Arial\",sans-serif
          font-size: 11pt; }"]]
   [:body
    [:div#root]
    [:script {:src "/js/main.js"}]]))
