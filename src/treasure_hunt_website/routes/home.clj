(ns treasure-hunt-website.routes.home
  (:require [compojure.core :refer :all]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.validation :as vali]
            [noir.util.crypt :as crypt]
            [treasure-hunt-website.views.layout :as layout]
            [treasure-hunt-website.models.db :as db]
            [hiccup.form :refer [form-to text-field text-area label submit-button]]
            [hiccup.element :refer [link-to]]
            [clojure.string :refer [lower-case trim]]))

(defn clues [team-id]
  (doall (for [{:keys [clueid cluetext haslimitedattempts numattemptsallowed numattemptsmade flairchallenge solved] :as clue} (db/get-clues-for-team team-id)]
           [:li {:clueid clueid}
            (when flairchallenge
              [:img {:src "sun.jpg"}])
            (str cluetext (if solved
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
              :ol))
        (if (db/team-has-solved-or-failed-all-clues? (session/get :teamid))
          [:div.victory "Congratulations, you have completed the Spark Games challenge!"]
          (form-to [:post "/guess"]

                   (vali/on-error :guess wrong-guess)
                   (label "guess-label" "Enter the solution to this clue:")
                   (text-field {:tabindex 1} "guess")
                   [:br]
                   (submit-button {:tabindex 2} "Check!")))
        [:h2 "You have currently earned: " (db/calculate-score-for-team (session/get :teamid)) " points!"]
        [:div.large-2.columns (link-to {:class "button"} "/editteam" "Edit Team")]]

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

(defn teammembers [team-id]
  (doall (for [{:keys [teammembername]} (db/get-team-members-for-team team-id)]
           [:li teammembername])))

(defn teammember-error [[error]]
  [:div.error error])

(defn teampage [& [teammembername]]
  (if-let [teamname (session/get :teamname)]
    (layout/common
     [:div
      [:h1 "Members of " teamname ":"]
      [:ul
       (teammembers (session/get :teamid))]
      (form-to [:post "/addmember"]
               (vali/on-error :teammembername teammember-error)
               (label "teammembername-label" "Enter team member name")
               (text-field {:tabindex 1} "teammembername" teammembername)
               [:br]
               (submit-button {:tabindex 2} "Add new member!"))
      [:div.large-2.columns (link-to {:class "button"} "/" "Back to the clues!")]])
    (resp/redirect "/")))

(defn valid-teammember? [teammembername]
  (vali/rule (vali/has-value? (trim teammembername))
             [:teammembername "Can't add a blank teammember"])
  (vali/rule (nil? (db/get-team-member teammembername (session/get :teamid)))
             [:teammembername (str teammembername " has already been added")])
  (not (vali/errors? :teammembername)))

(defn add-team-member [teammembername]
  (if (valid-teammember? teammembername)
    (do
      (db/add-member-to-team teammembername (session/get :teamid))
      (resp/redirect "/editteam"))
    (teampage [teammembername])
    ))

(defroutes home-routes
  (GET "/" [] (home))
  (POST "/guess" [guess] (check-guess guess))
  (GET "/editteam" [] (teampage))
  (POST "/addmember" [teammembername] (add-team-member teammembername))
  )
