(ns treasure-hunt-website.models.db
  (:require [clojure.java.jdbc :as sql])
  )

(def db
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "db.sq3"})

(defn get-team [id]
  (first (sql/query db ["SELECT * FROM teams WHERE id = ?" id])))

(defn get-team-by-login [login]
  (first (sql/query db ["SELECT * FROM teams WHERE teamname = ? COLLATE NOCASE" login])))

(defn create-team [team]
  (sql/insert! db :teams team)
  (let [{:keys [id] :as team} (get-team-by-login (:teamname team))]
    (sql/insert! db :progress {:teamid id :clueid 1 :usedlocationhint 0 :usedcluehint 0 :numattemptsmade 0 :solved 0})))

(defn convert-solved-to-boolean [m]
  (assoc m :solved (= 1 (:solved m))))

(defn convert-fields-to-boolean [{:keys [haslimitedattempts usedlocationhint usedcluehint solved] :as m}]
  (assoc m
         :haslimitedattempts (= 1 haslimitedattempts)
         :usedlocationhint (= 1 usedlocationhint)
         :usedcluehint (= 1 usedcluehint)
         :solved (= 1 solved)))

(defn get-clues-for-team [teamid]
  (sql/query db ["SELECT c.clueid, c.cluetext, c.locationhint, c.cluehint, c.haslimitedattempts, c.numattemptsallowed, c.answercode, p.usedlocationhint, p.usedcluehint, p.numattemptsmade, p.solved FROM clues AS c, progress AS p WHERE p.teamid = ? AND p.clueid = c.clueid" teamid] :row-fn convert-fields-to-boolean))

(defn update-attempts-made
  [clueid teamid newattemptsmade]
  (sql/update! db :progress {:numattemptsmade newattemptsmade} ["clueid = ? AND teamid = ?" clueid teamid]))

(defn get-current-clue-for-team-and-mark-an-attempt [teamid]
  (let [{:keys [clueid numattemptsmade] :as clue} (last (sql/query db ["SELECT c.clueid, c.cluetext, c.locationhint, c.cluehint, c.haslimitedattempts, c.numattemptsallowed, c.answercode, p.usedlocationhint, p.usedcluehint, p.numattemptsmade, p.solved FROM clues AS c, progress AS p WHERE p.teamid = ? AND p.clueid = c.clueid ORDER BY c.clueid ASC" teamid] :row-fn convert-fields-to-boolean))
        newattemptsmade (inc numattemptsmade)]
    (update-attempts-made clueid teamid newattemptsmade)
    (assoc clue :numattemptsmade newattemptsmade)))

(defn add-clue [cluetext locationhint cluehint haslimitedattempts numattemptsallowed answercode]
  (sql/insert! db :clues {:cluetext cluetext :locationhint locationhint :cluehint cluehint :haslimitedattempts haslimitedattempts :numattemptsallowed numattemptsallowed :answercode answercode}))

(defn unlock-next-clue [teamid previous-clueid]
  (sql/insert! db :progress {:clueid (inc previous-clueid) :teamid teamid :usedlocationhint 0 :usedcluehint 0 :numattemptsmade 0 :solved 0})
  )

(defn mark-clue-solved-and-unlock-next-clue [teamid solved-clueid]
  (try
    (unlock-next-clue teamid solved-clueid)
    (sql/update! db :progress {:solved 1} ["clueid = ?" solved-clueid])
    true
    (catch java.sql.SQLException ex
      nil)))

(defn- get-number-of-clues-solved-by-team [teamid]
  (if-let [num_solved_clues (:num_solved_clues (first (sql/query db ["SELECT COUNT(*) AS num_solved_clues FROM progress WHERE teamid = ? AND solved = 1" teamid])))]
    num_solved_clues
    0))

(defn- get-number-of-clues-failed-by-team [teamid]
  (if-let [num_failed_clues (:num_failed_clues (first (sql/query db ["SELECT COUNT(*) AS num_failed_clues FROM progress AS p, clues AS c WHERE p.clueid = c.clueid AND p.teamid = ? AND p.solved = 0 AND p.numattemptsmade >= c.numattemptsallowed" teamid])))]
    num_failed_clues
    0))

(defn- get-number-of-hints-used-by-team [teamid]
  (if-let [num_used_hints (:num_used_hints (first (sql/query db ["SELECT SUM(usedlocationhint) + SUM(usedcluehint) AS num_used_hints FROM progress WHERE teamid = ?" teamid])))]
    num_used_hints
    0))

(defn- get-total-number-of-clues []
  (if-let [{:keys [num_clues]} (first (sql/query db ["SELECT COUNT(*) AS num_clues FROM clues"]))]
    num_clues))

(defn team-has-solved-or-failed-all-clues? [teamid]
  (= (+ (get-number-of-clues-solved-by-team teamid)
        (get-number-of-clues-failed-by-team teamid))
     (get-total-number-of-clues)
     ))

;; (defn- calculate-score-for-clue [{:keys [usedlocationhint usedcluehint solved]}]
;;   [(if usedlocationhint -1 0)
;;    (if usedcluehint -1 0)
;;    (if solved 3 0)])

;; TODO Make this take hints into account
(defn calculate-score-for-team [teamid]
  ;; (reduce +
  ;;         (for [clue-progress (sql/query db ["SELECT usedlocationhint usedcluehint solved FROM progress WHERE teamid = ?" teamid])
  ;;               (reduce + (calculate-score-for-clue clue-progress))]))
  (+
   (* 3 (get-number-of-clues-solved-by-team teamid))
   (* -1 (get-number-of-hints-used-by-team teamid))))

(defn- create-clues []
  (add-clue "First clue" "The first clue is hidden in location A" "The solution is the first letter of the Greek alphabet" 0 0 (noir.util.crypt/encrypt "alpha"))
  (add-clue "Second clue" "The second clue is hidden in location B" "The solution is the second letter of the Greek alphabet" 1 1 (noir.util.crypt/encrypt "beta"))
  (add-clue "Third clue" "The third clue is hidden in location C" "The solution is the third letter of the Greek alphabet" 0 0 (noir.util.crypt/encrypt "gamma"))
  )

;; (sql/delete! db :teams ["teamname = ?" "tcepsa"])
;; (sql/query db ["Select id, teamname from teams"])

;; These two in combination reset the team with id = 2
;; (sql/delete! db :progress ["teamid = ?" 2])
;; (sql/insert! db :progress {:teamid 2 :clueid 1 :solved 0})

;; This gets all of the columns from all clues
;; (sql/query db ["SELECT * FROM clues"])



;; Use this if you need to tweak the answer to a clue (make sure to encrypt it first!  I've been using the auth namespace...)
;; (defn update-clue [clueid new-answer]
;;   (sql/update! db :clues {:answercode new-answer} ["clueid = ?" clueid]))
