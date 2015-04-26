(ns treasure-hunt-website.models.schema
  (:require [treasure-hunt-website.models.db :refer :all]
            [clojure.java.jdbc :as sql]))

(defn create-teams-table
  "Creates a table to hold team entries"
  []
  (sql/db-do-commands db
    (sql/create-table-ddl
     :teams
     [:id "INTEGER PRIMARY KEY AUTOINCREMENT"]
     [:teamname "TEXT UNIQUE"]
     [:password "TEXT"]
     [:pocemail "TEXT"])

    "CREATE INDEX teams_index ON teams (teamname)"))

(defn create-clues-table
  "Creates a table to hold clues"
  []
  (sql/db-do-commands db
    (sql/create-table-ddl
     :clues
     [:clueid "INTEGER PRIMARY KEY AUTOINCREMENT"]
     [:cluetext "TEXT"]
     [:locationhint "TEXT"]
     [:cluehint "TEXT"]
     [:answercode "TEXT"])))

(defn create-progress-table
  "Creates a table to map teams to clues that they have unlocked"
  []
  (sql/db-do-commands db
    (sql/create-table-ddl
     :progress
     [:teamid "INTEGER"]
     [:clueid "INTEGER"]
     [:usedlocationhint "INTEGER"]
     [:usedcluehint "INTEGER"]
     [:solved "INTEGER"]
     [:UNIQUE "(teamid, clueid)"])))

;; Only implement this later if there's time
(defn create-clue-ordering-table
  "Creates a table to provide different permutations of the clue ordering to different teams"
  []
  (sql/db-do-commands db
    (sql/create-table-ddl
     :clue_ordering
     [:teamid "INTEGER"]
     [:clueid "INTEGER"]
     [:nextclueid "INTEGER"])))

(defn reset-database
  "Drops and re-adds all tables.  Probably a bad idea to use this in production..."
  []
  (sql/db-do-commands db
                      (sql/drop-table-ddl :progress)
                      (sql/drop-table-ddl :teams)
                      (sql/drop-table-ddl :clues))
  (create-clues-table)
  (create-teams-table)
  (create-progress-table))
