(ns registratura.ui.views.common)

(defn layout [& children]
  [:div {:style {:display :flex
                 :justify-content :center}}
   [:div {:style {:width "1100px"}}
    children]])

(defn loading []
  [:div {:style {:display :flex
                 :justify-content :center
                 :align-items :center
                 :height "100vh"}}
   "Loading..."])

(defn not-found []
  [:div {:style {:display :flex
                 :justify-content :center
                 :align-items :center
                 :height "100vh"}}
   [:h2 "Page Not Found"]])
