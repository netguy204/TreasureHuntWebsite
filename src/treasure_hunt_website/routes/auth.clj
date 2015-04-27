(ns treasure-hunt-website.routes.auth
  (:require [hiccup.form :refer :all]
            [compojure.core :refer :all]
            [treasure-hunt-website.routes.home :refer :all]
            [treasure-hunt-website.views.layout :as layout]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.validation :as vali]
            [noir.util.crypt :as crypt]
            [clojure.string :refer [lower-case trim]]
            [treasure-hunt-website.models.db :as db]))


(defn valid? [id pass pass1 pocemail]
  (vali/rule (vali/has-value? (trim id))
             [:id "Team name is required"])

  (vali/rule (not= (lower-case id) (lower-case (get (db/get-team-by-login id) :teamname "")))
             [:id (str "Team name \"" id  "\" has already been registered; please choose another")])
  (vali/rule (vali/min-length? pass 5)
             [:pass "Password must be at least 5 characters long"])
  (vali/rule (= pass pass1)
             [:pass "The password fields do not match"])
  (vali/rule (vali/has-value? (trim pocemail))
             [:pocemail "An email address at which we can contact your team is required"])
  (not (vali/errors? :id :pass :pass1)))

(defn valid-login? [id pass]
  (let [team (db/get-team-by-login id)]
    (vali/rule team
               [:id "Invalid team"])
    (when team
      (vali/rule (crypt/compare pass (:password team))
                 [:pass "Incorrect password"]))
      ))

(defn error-item [[error]]
  [:div.error error])

(defn registration-page [& [id pocemail]]
  (layout/base
   (form-to [:post "/register"]
            [:div.row
             [:div.large-12.columns
              (vali/on-error :id error-item)
              (label "team-id" "Team name")
              (text-field {:tabindex 1}  "id" id)]]

            [:div.row
             [:div.large-12.columns
              (vali/on-error :pocemail error-item)
              (label "pocemail" "Team Point-of-Contact e-mail address")
              (text-field {:tabindex 2} "pocemail" pocemail)]]

            [:div.row
             [:div.large-12.columns
              (vali/on-error :pass error-item)
              (label "pass" "Password")
              (password-field {:tabindex 3} "pass")]]

            [:div.row
             [:div.large-12.columns
              (vali/on-error :pass1 error-item)
              (label "pass1" "Confirm password")
              (password-field {:tabindex 4} "pass1")]]

            ;; (vali/on-error :pocname error-item)
            ;; (label "pocname" "Name (optional)")
            ;; (text-field {:tabindex 5} "pocname" pocname)
            ;; [:br]
            ;; (vali/on-error :pocname error-item)
            ;; (label "pocname" "Name (optional)")
            ;; (text-field {:tabindex 5} "pocname" pocname)
            ;; [:br]
            [:div.row
             [:div.large-12.columns
              (submit-button {:tabindex 5 :class "button"} "Create Account")]])))

;; (db/create-team {:teamname "Demo" :password (crypt/encrypt "qwerty")})
;; (do (db/update-clue 1 (crypt/encrypt "alpha"))
;;     (db/update-clue 2 (crypt/encrypt "beta"))
;;     (db/update-clue 3 (crypt/encrypt "gamma")))


(defn handle-registration [id pass pass1 pocemail]
  (if (valid? id pass pass1 pocemail)
    (do
      (try
        (db/create-team {:teamname id :password (crypt/encrypt pass) :pocemail pocemail})
        (let [{:keys [teamname id]} (db/get-team-by-login id) ]
          (session/put! :teamname teamname)
          (session/put! :teamid id))
        (resp/redirect "/")
        (catch java.sql.SQLException ex
          (vali/set-error :id (str "Something went wrong; please send the following text to +SparkGameControl (just the first bit, not the whole stacktrace!): " (.getMessage ex)))
          (registration-page id))))
    (registration-page id pocemail)))

(defn login-page [& [id]]
  (layout/common
   (form-to [:post "/login"]
            [:div.row
             [:div.large-12.columns
              (vali/on-error :id error-item)
              (label "team-id" "Team name")
              (text-field {:tabindex 1} "id" id)]]

            [:div.row
             [:div.large-12.columns
              (vali/on-error :pass error-item)
              (label "pass" "Password")
              (password-field {:tabindex 2} "pass")]]

            [:div.row
             [:div.large-12.columns
              (submit-button {:tabindex 3 :class "button expand"} "Login")]])))

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
  (POST "/register" [id pass pass1 pocemail]
        (handle-registration id pass pass1 pocemail))
  (GET "/login" []
       (login-page))
  (POST "/login" [id pass]
        (handle-login id pass))
  (GET "/logout" []
       (handle-logout)))
