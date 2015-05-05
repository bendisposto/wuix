(ns ^:figwheel-always wuix.core
    (:require-macros [reagent.ratom :as ra :refer [reaction]])
    (:require [reagent.core :as r]
              [re-frame.core :as rf]))

(enable-console-print!)

(defn init-db []
  {:text (str "Hiya" (rand-int 100))
   :history []})

(rf/register-handler
 :initialise-db
 (fn [db _]
   (init-db)))

(rf/register-sub
 :text
 (fn [db] (reaction (:text @db))))

(rf/register-sub
 :history
 (fn [db] (reaction (:history @db))))


(rf/register-handler
 :add-history
 (fn [db [_ content]]
   (assoc db :history (conj (:history db) content))))

(defn h-item [e]
  [:span e " "])


(defn hello-world []
  (let [text (rf/subscribe [:text])
        hist (rf/subscribe [:history])]
    [:div
     [:h1 @text]
     [:div (map h-item @hist)]
     [:input#text {:type "text"}]
     [:input {:type "button"
              :value "Submit"
              :on-click (fn [_] (let [c (.-value (.getElementById js/document "text"))] (rf/dispatch [:add-history c])))}]]))


(defn ^:extern doh []
  (let [remote (js/require "remote")
        BW (.require remote "browser-window")
        win (BW. #js {:width 400 :height 400 })
        filename (str "file://" js/__dirname "/index.html")]
(println :f filename)
    (.loadUrl win filename)))

(defn init! []
  (rf/dispatch [:initialise-db])
  (r/render-component [hello-world]
                      (. js/document (getElementById "app"))))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )


(init!)
