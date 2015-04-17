(ns treasure-hunt-website.routes.auth
  (:require [hiccup.form :refer :all]
            [compojure.core :refer :all]
            [treasure-hunt-website.routes.home :refer :all]
            [treasure-hunt-website.views.layout :as layout]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.validation :as vali]
            [noir.util.crypt :as crypt]
            [treasure-hunt-website.models.db :as db]))


(defn valid? [id pass pass1]
  (vali/rule (vali/has-value? id)
             [:id "Team name is required"])
  (vali/rule (not (db/get-team-by-login id))
             [:id (str "Team name \"" id  "\" has already been registered; please choose another")])
  (vali/rule (vali/min-length? pass 5)
             [:pass "Password must be at least 5 characters long"])
  (vali/rule (= pass pass1)
             [:pass "The password fields do not match"])
  (not (vali/errors? :id :pass :pass1)))

(defn valid-login? [id pass]
  (let [team (db/get-team-by-login id)]
    (println "Got team: " team)
    (vali/rule team
               [:id "Invalid team"])
    (when team
      (vali/rule (crypt/compare pass (:password team))
                 [:pass "Incorrect password"]))
      ))

(defn error-item [[error]]
  [:div.error error])

(defn registration-page [& [id]]
  (layout/base
   (form-to [:post "/register"]
            (vali/on-error :id error-item)
            (label "team-id" "Team name")
            (text-field {:tabindex 1}  "id" id)
            [:br]
            (vali/on-error :pass error-item)
            (label "pass" "Password")
            (password-field {:tabindex 2} "pass")
            [:br]
            (vali/on-error :pass1 error-item)
            (label "pass1" "Confirm password")
            (password-field {:tabindex 3} "pass1")
            [:br]
            (submit-button {:tabindex 4} "Create Account"))))

;; (db/create-team {:teamname "Sweet" :password (crypt/encrypt "qwerty")})


(defn handle-registration [id pass pass1]
  (if (valid? id pass pass1)
    (do
      (try
        (db/create-team {:teamname id :password (crypt/encrypt pass)})
        (let [{:keys [teamname id]} (db/get-team-by-login id) ]
          (session/put! :teamname teamname)
          (session/put! :teamid id))
        (resp/redirect "/")
        (catch java.sql.SQLException ex
          (vali/set-error :id ("Something went wrong: " (.getMessage ex)))
          (registration-page id))))
    (registration-page id)))

(defn login-page [& [id]]
  (layout/common
   (form-to [:post "/login"]
            (vali/on-error :id error-item)
            (label "team-id" "Team name")
            (text-field {:tabindex 1} "id" id)
            [:br]
            (vali/on-error :pass error-item)
            (label "pass" "Password")
            (password-field {:tabindex 2} "pass")
            [:br]
            (submit-button {:tabindex 3} "Login"))))

(defn handle-login [id pass]
  (if (valid-login? id pass)
    (do
      (let [{:keys [teamname id]} (db/get-team-by-login id)]
        (session/put! :teamname teamname)
        (session/put! :teamid id)
        (println "Storing teamname" teamname " and id" id "in session"))
      (resp/redirect "/"))
    (login-page id)))

(defn handle-logout []
  (session/clear!)
  (resp/redirect "/"))


(defroutes auth-routes
  (GET "/register" []
       (registration-page))
  (POST "/register" [id pass pass1]
        (handle-registration id pass pass1))
  (GET "/login" []
       (login-page))
  (POST "/login" [id pass]
        (handle-login id pass))
  (GET "/logout" []
       (handle-logout)))

