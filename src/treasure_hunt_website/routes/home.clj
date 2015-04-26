(ns treasure-hunt-website.routes.home
  (:require [compojure.core :refer :all]
            [noir.session :as session]
            [noir.validation :as vali]
            [noir.util.crypt :as crypt]
            [treasure-hunt-website.views.layout :as layout]
            [treasure-hunt-website.models.db :as db]
            [hiccup.form :refer [form-to text-field text-area label submit-button]]
            [clojure.string :refer [lower-case trim]]))

(defn clues [team-id]
  (doall (for [{:keys [clueid cluetext haslimitedattempts numattemptsallowed numattemptsmade solved] :as clue} (db/get-clues-for-team team-id)]
           [:li {:clueid clueid} (str cluetext (if solved
                                                 (str " SOLVED!")
                                                 (if haslimitedattempts
                                                   (str " (" numattemptsmade "/" numattemptsallowed " attempts used)"))))])))

(defn wrong-guess [[message]]
  [:div.wrong-guess message])

(defn home []
  (layout/common
   (let [teamname (session/get :teamname)]
     (if teamname
       [:div
        [:h1 "Welcome, " teamname "!"]
        [:h2 "You have discovered the following clues:"]
        (vec (conj
              (clues (session/get :teamid))
              :ul))
        (if (db/team-has-solved-all-clues? (session/get :teamid))
          [:div.victory "Congratulations, you have completed the Spark Games challenge!"]
          (form-to [:post "/guess"]

                   (vali/on-error :guess wrong-guess)
                   (label "guess-label" "Enter the solution to this clue:")
                   (text-field {:tabindex 1} "guess")
                   [:br]
                   (submit-button {:tabindex 2} "Check!")))
        [:h2 "You have currently earned: " (db/calculate-score-for-team (session/get :teamid)) " points!"]]

       ;; no team name
       (list
        [:div.row
         [:div.large-12.columns
          [:h2 "Welcome to Spark Games!"]]]

        [:div.row
         [:div.large-12.columns
          "If you have a team then please log in to continue your quest. If this is your first visit then select register to begin."]])))))

(defn check-guess-against-clue [guess {:keys [answercode]}]
  (crypt/compare (lower-case (trim guess)) answercode))

;; TODO Fix the architecture (correct? probably shouldn't be handling ALL of this!)
(defn correct? [guess]
  (let [{:keys [clueid haslimitedattempts numattemptsallowed numattemptsmade] :as clue} (db/get-current-clue-for-team-and-mark-an-attempt (session/get :teamid))]
    (if (check-guess-against-clue guess clue)
      (do
        (db/mark-clue-solved-and-unlock-next-clue (session/get :teamid) clueid)
        (assoc clue :solved true))
      (when (and haslimitedattempts
               (>= numattemptsmade numattemptsallowed))
        (db/unlock-next-clue (session/get :teamid) clueid)
        (assoc clue :solved nil)))
  ))

(defn check-guess [guess]
  (let [{:keys [solved] :as clue} (correct? guess)]
    (if solved
      (vali/set-error :guess (str "Correct!"))
      (vali/set-error :guess (str "\"" guess "\" does not solve the clue")))
    (home)))

(defroutes home-routes
  (GET "/" [] (home))
  (POST "/guess" [guess] (check-guess guess))
  )
