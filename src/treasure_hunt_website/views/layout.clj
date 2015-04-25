(ns treasure-hunt-website.views.layout
  (:require [hiccup.page :refer [html5 include-css]]
            [hiccup.element :refer [link-to]]
            [noir.session :as session]))

(defn base [& content]
  (let [fnd "/foundation/css"]
    (html5
     [:head
      [:title "Spark Games"]
      (include-css (str fnd "/foundation.css"))
      (include-css "/css/screen.css")
      [:script {:src (str fnd "/js/vendor/modernizr.js")}]]
     [:body

      [:div.row
       [:div.large-12.columns.panel

        content

        (when (session/get :teamname)
          [:div.row
           [:div.large-10.columns]
           [:div.large-2.columns (link-to {:class "button"} "/logout" "logout")]])]]

      [:script {:src (str fnd "/js/vendor/jquery.js")}]
      [:script {:src (str fnd "/js/foundation.min.js")}]
      ;; [:script {:src "js/treasure-hunt-website-cljs.js"}]
      ;; [:script "treasure-hunt-website.init();"]
      ])))

(defn auth-block []
  [:div.row
   [:div.large-2.columns
    (link-to {:class "button expand"} "/register" "Register")]
   [:div.large-2.columns
    (link-to {:class "button expand"} "/login" "Login")]
   [:div.large-6.columns]])

(defn common [& content]
  (base
   content
   (when-not (session/get :teamname)
     (auth-block))))
