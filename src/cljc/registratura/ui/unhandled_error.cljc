(ns registratura.ui.unhandled-error
  (:require [registratura.ui.common :refer [<sub >evt]]
            [re-frame.core :as rf]))

(rf/reg-event-db ::close-unhandled-error
  (fn [db]
    (assoc db :unhandled-error? false)))

(rf/reg-sub ::unhandled-error?
  (fn [db]
    (:unhandled-error? db)))

(defn error-modal []
  (let [error-happened? (<sub [::unhandled-error?])]
    [:<>
     [:div {:style (merge {:position "fixed"
                           :top "50%"
                           :left "50%"
                           :transition "200ms ease-in-out"
                           :z-index 5
                           :padding "1rem 1.5rem"
                           :border-color "#ccc"
                           :border-style :solid
                           :border-width :medium
                           :border-radius "0.5rem"
                           :background-color :white
                           :max-width "80%"}
                          (if error-happened?
                            {:transform "translate(-50%, -50%) scale(1)"}
                            {:transform "translate(-50%, -50%) scale(0)"}))}
      [:span {:style {:color :red}}
       "Something went wrong"]]
     [:div {:style (merge {:position "fixed"
                           :transition "200ms ease-in-out"
                           :top 0
                           :left 0
                           :bottom 0
                           :right 0
                           :z-index 4
                           :background-color "rgba(0, 0, 0, .5)"}
                          (if error-happened?
                            {:opacity 1
                             :pointer-events :all}
                            {:opacity 0
                             :pointer-events :none}))
            :on-click #(>evt [::close-unhandled-error])}]]))
