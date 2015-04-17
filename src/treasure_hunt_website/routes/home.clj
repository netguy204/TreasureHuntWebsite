(ns treasure-hunt-website.routes.home
  (:require [compojure.core :refer :all]
            [noir.session :as session]
            [noir.validation :as vali]
            [noir.util.crypt :as crypt]
            [treasure-hunt-website.views.layout :as layout]
            [treasure-hunt-website.models.db :as db]
            [hiccup.form :refer [form-to text-field text-area label submit-button]]))

(defn clues [team-id]
  (doall (for [{:keys [clueid cluetext solved] :as clue} (db/get-clues-for-team team-id)]
           [:li {:clueid clueid} (str cluetext (when solved (str " SOLVED!")))])))

(defn wrong-guess [[message]]
  [:div.wrong-guess message])

(defn home []
  (layout/common
   (let [teamname (session/get :teamname)]
     (if teamname
       ;; [:div
       ;;  [:div#team-list
       ;;   [:ul]]
       ;;  [:div#wishes]
       ;;  [:div#editwish.hidden
       ;;   [:span
       ;;    (label "shortdesc" "Short Description:")
       ;;    (text-field {:class "shortdesc"} "shortdesc")]
       ;;   [:div
       ;;    (label "longdesc" "Additional Details:")
       ;;    (text-area {:class "longdesc"} "longdesc")]
       ;;   [:div#edit-priority
       ;;    (label "priority" "Priority:")
       ;;    (text-field {:class "priority"} "priority")]
       ;;   [:input {:id "edit-save-btn" :type "Button" :value "Save"}]
       ;;   [:input {:id "edit-cancel-btn" :type "Button" :value "Cancel"}]]
       ;; ]
       [:div
        [:h1 "Welcome: " teamname]
        [:h2 "You have discovered the following clues:"]
        (vec (conj
              (clues (session/get :teamid))
              :ul))
        (form-to [:post "/guess"]
                 
                 (vali/on-error :guess wrong-guess)
                 (label "guess-label" "Enter tne solution to this clue:")
                 (text-field {:tabindex 1} "guess")
                 [:br]
                 (submit-button {:tabindex 2} "Check!"))]
       [:div
        [:h1 "Welcome to the game!  Please log in or register!"]]
     ))))

(defn check-guess-against-clue [guess clue]
  (crypt/compare guess (:answercode clue)))

(defn correct? [guess]
  (let [clues (db/get-clues-for-team (session/get :teamid))]
    (first (filter #(check-guess-against-clue guess %) clues))
  ))

(defn check-guess [guess]
  (if-let [{:keys [clueid]} (correct? guess)]
    (do
      (println "Solved clue " clueid)
      (if (db/update-progress (session/get :teamid) clueid)
        (vali/set-error :guess (str "Correct!"))
        (vali/set-error :guess (str "You've already solved that one!")))
      (home))
    (do
      (vali/set-error :guess (str guess "does not solve the clue"))
      (home)))
  )

(defroutes home-routes
  (GET "/" [] (home))
  (POST "/guess" [guess] (check-guess guess))
  )
