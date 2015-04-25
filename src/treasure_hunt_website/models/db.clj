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
    (sql/insert! db :progress {:teamid id :clueid 1 :usedlocationhint 0 :usedcluehint 0 :solved 0})))

(defn convert-solved-to-boolean [m]
  (assoc m :solved (= 1 (:solved m))))

(defn convert-progress-fields-to-boolean [{:keys [usedlocationhint usedcluehint solved] :as m}]
  (assoc m
         :usedlocationhint (= 1 usedlocationhint)
         :usedcluehint (= 1 usedcluehint)
         :solved (= 1 solved)))

(defn get-clues-for-team [teamid]
  (sql/query db ["SELECT c.clueid, c.cluetext, c.locationhint, c.cluehint, c.answercode, p.usedlocationhint, p.usedcluehint, p.solved FROM clues AS c, progress AS p WHERE p.teamid = ? AND p.clueid = c.clueid" teamid] :row-fn convert-progress-fields-to-boolean))

(defn add-clue [cluetext locationhint cluehint answercode]
  (sql/insert! db :clues {:cluetext cluetext :locationhint locationhint :cluehint cluehint :answercode answercode}))

(defn mark-clue-solved-and-unlock-next-clue [teamid solved-clueid]
  (try
    (sql/insert! db :progress {:clueid (inc solved-clueid) :teamid teamid :usedlocationhint 0 :usedcluehint 0 :solved 0})
    (sql/update! db :progress {:solved 1} ["clueid = ?" solved-clueid])
    true
    (catch java.sql.SQLException ex
      nil)))

(defn- get-number-of-clues-solved-by-team [teamid]
  (if-let [num_solved_clues (:num_solved_clues (first (sql/query db ["SELECT COUNT(*) AS num_solved_clues FROM progress WHERE teamid = ? AND solved = 1" teamid])))]
    num_solved_clues
    0))

(defn- get-number-of-hints-used-by-team [teamid]
  (if-let [num_used_hints (:num_used_hints (first (sql/query db ["SELECT SUM(usedlocationhint) + SUM(usedcluehint) AS num_used_hints FROM progress WHERE teamid = ?" teamid])))]
    num_used_hints
    0))

(defn- get-total-number-of-clues []
  (if-let [{:keys [num_clues]} (first (sql/query db ["SELECT COUNT(*) AS num_clues FROM clues"]))]
    num_clues))

(defn team-has-solved-all-clues? [teamid]
  (= (get-number-of-clues-solved-by-team teamid)
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
  (add-clue "First clue" "The first clue is hidden in location A" "The solution is the first letter of the Greek alphabet" (noir.util.crypt/encrypt "alpha"))
  (add-clue "Second clue" "The second clue is hidden in location A" "The solution is the second letter of the Greek alphabet" (noir.util.crypt/encrypt "beta"))
  (add-clue "Third clue" "The third clue is hidden in location A" "The solution is the third letter of the Greek alphabet" (noir.util.crypt/encrypt "gamma"))
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
