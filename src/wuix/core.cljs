(ns ^:figwheel-always wuix.core
    (:require-macros [reagent.ratom :as ra :refer [reaction]])
    (:require [reagent.core :as r]
              [re-com.core :as c]
              [re-frame.core :as rf]))

(enable-console-print!)

(defn init-db []
  {:text (str "Hiya" (rand-int 100))
   :history []
   :layout {:type :h
            :p1 :x1
            :p2 :x4
            }

   :pages {:x1 {:headers [:h1 :h2]}
           :x2 {:headers [:h3]}
           :x3 {:headers [:h4]}
           :x4 {:headers [:h5 :h6 :h2 :h3]}
           :x5 {:headers [:h2]}}
   :views {:h1 {:label "tab1" :content "foo"}
           :h2 {:label "tab2" :content "bar"}
           :h3 {:label "tab3" :content  "baz"}
           :h4 {:label "tab4" :content "trololo"}
           :h5 {:label "tab5" :content "moo"}
           :h6 {:label "tab6" :content "doo"}}})

(rf/register-handler
 :initialise-db
 (fn [db _]
   (init-db)))

(rf/register-sub
 :text
 (fn [db] (reaction (:text @db))))

(rf/register-sub
 :layout
 (fn [db] (reaction (:layout @db))))

(rf/register-sub
 :content
 (fn [db [_ view]] (reaction  (get-in @db [:views view :content]))))

(rf/register-sub
 :views
 (fn [db [_ pages]]
   (let [pages (into #{} pages)]
     (reaction
      (->> (get @db :views)
           (filter (fn [[id _]] (pages id)))
           (map (fn [[id content]] (into content {:id id}))))))))

(rf/register-sub
 :page
 (fn [db [_ page]] (reaction (get-in @db [:pages page]))))

(rf/register-sub
 :history
 (fn [db] (reaction (:history @db))))

(rf/register-handler
 :add-history
 (fn [db [_ content]]
   (assoc db :history (conj (:history db) content))))

(declare render-pane)

(defn render-split [{:keys [type p1 p2]}]
  (let [c1 [render-pane p1]
        c2 [render-pane p2]
        split-box (if (= type :h) c/h-split c/v-split)]
    [split-box :margin "0px" :panel-1 c1 :panel-2 c2]))


#_(defn pane-header [pages]
    (let [tabs (rf/subscribe [:views pages])
          current (r/atom :h1)]
      [c/border :border "1px solid lightgray"
       :child [c/box :size "30px"
               :child
               (when @tabs [c/horizontal-pill-tabs
                            :model current
                            :on-change #(rf/dispatch [:tab-change %])
                            :tabs @tabs])]]))

#_(defn content [page]
    (let [content (rf/subscribe [:content page])]
      [c/box :size "auto" :child @content]))

#_(defn render-pane-content [layout]
    (println :debug layout)
    (let [pages (rf/subscribe [:page layout])]
      (println :d2 @pages)
      [c/v-box :size "auto"
       :children [
                  (pane-header (:headers @pages))
                  (content (or (:focus @pages) (first (:headers @pages)) ))]]))

(defn render-single-pane [pane-name]
  (let [pages (rf/subscribe [:page pane-name])
        views (rf/subscribe [:views (:headers @pages)])
        selected (r/atom (:id (first @views)))
        content (reaction (:content (first (filter (fn [{id :id}] (= id @selected)) @views))))]
    (fn [layout]
      (when layout
        [c/v-box
         :size "auto"
         :children
         [[c/horizontal-tabs
           :model selected
           :on-change #(reset! selected %)
           :tabs @views]
          [c/box :child (str @content)]]]))))

(defn render-pane [layout]
  (fn [layout]
    (if-not
        (map? layout)
      [c/box
       :size "auto"
       :class "pane"
       :child [render-single-pane layout]]
      [render-split layout])))

(defn main-pane []
  (fn []
    (let [layout (rf/subscribe [:layout])]
      [c/box :size "auto" :child [render-pane @layout]])))

(defn footer []
  (fn []
    [c/border
     :border "1px solid lightgray"
     :child [c/h-box :size "30px" :style {:background-color "lightgray"}  :children [[c/box :child "footer"]]]]))

(defn render-app []
  [c/v-box
   :height "100%"
   :children [[main-pane]
              [footer]]])

(rf/register-handler
 :window-resize
 (fn [db [_ {:keys [width height]}]]
   (assoc-in db [:window :size] {:width width :height height})))

(defn ^:extern init []

  #_(set! (.-onresize js/window) #(rf/dispatch [:window-resize {:height (.-innerHeight js/window)
                                                                :width (.-innerWidth js/window)}]))
  (r/render-component [render-app]
                      (. js/document (getElementById "app")))
  (rf/dispatch [:initialise-db]))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (rf/dispatch [:initialise-db])
  )
