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

(defn teamname []
  (session/get :teamname))

(defn teamid []
  (session/get :teamid))

(defn teammembers [team-id]
  (for [{:keys [teammembername]} (db/get-team-members-for-team team-id)]
    teammembername))

(def logged-in? teamname)

(defn auth-block []
  (if (logged-in?)
    [:div.row
     [:div.large-2.columns (link-to {:class "button expand"} "/logout" "Logout")]
     [:div.large-10.columns]]

    [:div.row
     [:div.large-2.columns
      (link-to {:class "button expand"} "/register" "Register")]
     [:div.large-2.columns
      (link-to {:class "button expand"} "/login" "Login")]
     [:div.large-6.columns]]))

(defn -tab-active [active current & default]
  (cond
    (not active) (if default {:class "active"} {})
    (= active current) {:class "active"}
    true {}))

(defn teammember-error [[error]]
  [:div.error error])

(defn tabbed-view [{:keys [teammembername active-tab]} content]
  (list
   [:ul.tabs {:data-tab true :data-options "deep_linking:true"}
    [:li.tab-title (-tab-active active-tab "game" true) (link-to "#game" "Game")]
    [:li.tab-title (-tab-active active-tab "team") (link-to "#team" "My Team")]
    [:li.tab-title (-tab-active active-tab "clues") (link-to "#clues" "Past Clues")]]

   [:div.tabs-content
    [:div#game.content (-tab-active active-tab "game" true)
     content]

    [:div#team.content (-tab-active active-tab "team")

     [:div.row
      [:div.large-6.columns
       (form-to [:post "/addmember"]
                [:div.row
                 [:div.large-12.columns
                  (vali/on-error :teammembername teammember-error)
                  (label "teammembername-label" "Enter team member name")
                  (text-field {:tabindex 1} "teammembername" teammembername)]]

                [:div.row
                 [:div.large-12.columns
                  (submit-button {:tabindex 2 :class "button"} "Add new member!")]])]

      [:div.large-6.columns
       [:div.row
        [:div.large-12.columns
         [:h2 "My Team"]]]
       (for [member (teammembers (teamid))]
         [:div.row
          [:div.large-12.columns
           member]])]]]

    [:div#clues.content (-tab-active active-tab "clues")
     (if (not (empty? (clues (session/get :teamid))))
       (list
        [:div.row
         [:div.large-12.columns
          "You have discovered the following clues:"]]
        (for [clue (clues (session/get :teamid))]
          [:div.row
           [:div.large-12.columns
            clue]]))
       "You haven't solved any clues yet. Check out the FAQ if you need help getting started.")]]
   (auth-block)))

(defn wrong-guess [[message]]
  [:div.wrong-guess message])

(defn clue-guess-form []
  (form-to [:post "/guess"]
           (vali/on-error :guess wrong-guess)
           (label "guess-label" "Enter the solution to this clue:")
           (text-field {:tabindex 1} "guess")
           [:br]
           (submit-button {:tabindex 2} "Check!")))

(defn home [& [opts]]
  (if-let [teamname (logged-in?)]
    (layout/common
     (list
      [:div.row
       [:div.large-12.columns
        [:h1 "Welcome, " teamname "!"]]]

      (when (db/team-has-solved-or-failed-all-clues? (session/get :teamid))
        [:div.panel.callout "Congratulations, you have completed the Spark Games challenge!"])

      (tabbed-view opts
       (list
        [:div.row
         [:div.large-12.columns
          "You have currently earned: " (db/calculate-score-for-team (session/get :teamid)) " points!"]]

        (when-not (db/team-has-solved-or-failed-all-clues? (session/get :teamid))
          [:div.row
           [:div.large-12.columns (clue-guess-form)]])))))

    ;; not logged in
    (layout/common
     (list
      [:div.row
       [:div.large-12.columns
        [:h2 "Welcome to Spark Games!"]]]

      [:div.row
       [:div.large-12.columns
        "If you have a team then please log in to continue your quest. If this is your first visit then select register to begin."]]

      (auth-block)))))

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
      (resp/redirect "/#team"))
    (home {:teammembername teammembername :active-tab "team"})
    ))

(defroutes home-routes
  (GET "/" [] (home))
  (POST "/guess" [guess] (check-guess guess))
  (POST "/addmember" [teammembername] (add-team-member teammembername)))
