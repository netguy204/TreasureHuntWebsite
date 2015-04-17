(ns treasure-hunt-website.handler
  (:require [compojure.core :refer [defroutes]]
            [compojure.route :as route]
            [noir.util.middleware :as noir-middleware]
            [treasure-hunt-website.routes.home :refer [home-routes]]
            [treasure-hunt-website.routes.auth :refer [auth-routes]]
))

(defn init []
  (println "treasure-hunt-website is starting"))

(defn destroy []
  (println "treasure-hunt-website is shutting down"))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Sorry, no clue what you're talking about!"))

(def app (noir-middleware/app-handler [auth-routes home-routes app-routes])) 
