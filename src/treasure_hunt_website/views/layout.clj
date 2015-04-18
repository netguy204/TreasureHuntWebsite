(ns treasure-hunt-website.views.layout
  (:require [hiccup.page :refer [html5 include-css]]
            [hiccup.element :refer [link-to]]
            [noir.session :as session]))

(defn base [& content]
  (html5
    [:head
     [:title "Spark Games"]
     (include-css "/css/screen.css")]
    [:body
     (when (session/get :teamname)
       (link-to "/logout" "logout"))
     content
     [:script {:src "js/treasure-hunt-website-cljs.js"}]
     ;; [:script "treasure-hunt-website.init();"]
     ]))

(defn common [& content]
  (base
   (if-not (session/get :teamname)
     (do (println "No teamname found")
         (seq [(link-to "/register" "register")
               " "
               (link-to "/login" "login")]))
     )
   content))
