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
    (sql/insert! db :progress {:teamid id :clueid 1 :solved 0})))

(defn convert-solved-to-boolean [m]
  (assoc m :solved (= 1 (:solved m))))

(defn get-clues-for-team [teamid]
  (sql/query db ["SELECT c.clueid, c.cluetext, c.answercode, p.solved FROM clues AS c, progress AS p WHERE p.teamid = ? AND p.clueid = c.clueid" teamid] :row-fn convert-solved-to-boolean))

(defn add-clue [cluetext answercode]
  (sql/insert! db :clues {:cluetext cluetext :answercode answercode}))

(defn update-progress [teamid solved-clueid]
  (try
    (sql/insert! db :progress {:clueid (inc solved-clueid) :teamid teamid :solved 0})
    (sql/update! db :progress {:solved 1} ["clueid = ?" solved-clueid])
    true
    (catch java.sql.SQLException ex
      nil)))

(defn team-has-solved-all-clues? [teamid]
  (= (:num_solved_clues (first (sql/query db ["SELECT COUNT(*) AS num_solved_clues FROM progress WHERE teamid = ? AND solved = 1" teamid])))
     (:num_clues (first (sql/query db ["SELECT COUNT(*) AS num_clues FROM clues"])))
     ))

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
