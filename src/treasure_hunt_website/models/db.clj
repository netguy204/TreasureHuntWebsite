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
  (first (sql/query db ["SELECT * FROM teams WHERE teamname = ?" login])))

(defn create-team [team]
  (sql/insert! db :teams team)
  (let [{:keys [id] :as team} (get-team-by-login (:teamname team))]
    (sql/insert! db :progress {:teamid id :clueid 1 :solved 0})))

(defn convert-solved-to-boolean [m]
  (assoc m :solved (= 1 (:solved m))))

;; TODO Reimplement this to actaully work
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
      nil))
)

;; (defn update-progress [teamid solved-clueid]
;;   (sql/with-connection db
;;     (sql/update-values :progress
;;      )
;;     (sql/insert-record :progress {:teamid})))








;; (defn enable-team [id]
;;   (sql/with-connection db
;;     (sql/update-values
;;       :teams ["id = ?" id] {:active true})))


;; (defn disable-team [id]
;;   (sql/with-connection db
;;     (sql/update-values
;;       :teams ["id = ?" id] {:active false})))

;; (defn get-active-team-names
;;   "Returns a seq of the names of all active teams"
;;   []
;;   (sql/with-connection db
;;     (sql/with-query-results
;;       res ["SELECT name, id FROM teams WHERE active = true"]
;;       ;; (doall (for [r res]
;;       ;;          (:name r)))
;;       (doall res))))

;; (defn get-full-team-list
;;   "Returns a seq of records of all teams"
;;   []
;;   (sql/with-connection db
;;     (sql/with-query-results
;;       res ["SELECT * FROM teams"]
;;       (doall res))))

;; (defn add-new-wish
;;   "Add a new wish to the wishes table"
;;   ([wisher-id shortdesc longdesc priority]
;;    (add-new-wish {:wisher wisher-id :shortdesc shortdesc :longdesc longdesc :priority priority}))
;;   ([wishmap]
;;    (sql/with-connection db
;;      (sql/insert-record :wishes wishmap))))

;; (defn delete-wish
;;   "Remove a wish from the wishes table"
;;   [wisher-id wish-id]
;;   (sql/with-connection db
;;     (sql/delete-rows :wishes ["wisher=? AND id=?" wisher-id wish-id] )))

;; (defn get-wishes-for-team
;;   "Returns a seq of wish records for the team with the given id"
;;   [team-id]
;;   (sql/with-connection db
;;     (sql/with-query-results
;;       res ["SELECT * FROM wishes WHERE wisher = ?" team-id]
;;       (doall res))))
