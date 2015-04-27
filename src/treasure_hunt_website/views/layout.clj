(ns treasure-hunt-website.views.layout
  (:require [hiccup.page :refer [html5 include-css]]
            [hiccup.element :refer [link-to]]
            [noir.session :as session]))

(defn base [& content]
  (let [fnd "/foundation"]
    (html5
     [:head
      [:title "Spark Games"]
      (include-css (str fnd "/css/foundation.css"))
      [:script {:src (str fnd "/js/vendor/modernizr.js")}]]
     [:body

      [:div.row
       [:div.large-12.columns.panel
        content]]

      [:script {:src (str fnd "/js/vendor/jquery.js")}]
      [:script {:src (str fnd "/js/foundation.min.js")}]
      ;; [:script {:src "js/treasure-hunt-website-cljs.js"}]
      [:script "$(document).foundation();"]
      ])))

(defn common [& content]
  (base
   content))
