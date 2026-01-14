(ns game-project.core-test
  (:require [clojure.test :refer :all]
            [game-project.core :as core]
            [next.jdbc :as jdbc]))

(defmacro with-temp-db [& body]
  `(let [temp-file# (java.io.File/createTempFile "scores" ".db")
         path# (.getPath temp-file#)
         datasource# (delay (jdbc/get-datasource {:jdbcUrl (str "jdbc:sqlite:" path#)}))]
     (try
       (with-redefs [core/db-file path#
                     core/datasource datasource#]
         ~@body)
       (finally
         (try
           (.delete temp-file#)
           (catch Exception _#))))))

(deftest random-range-stays-within-bounds
  (let [min-val 200
        max-val 700
        samples (repeatedly 200 #(core/random-range min-val max-val))]
    (is (every? (fn [n] (and (<= min-val n) (< n max-val))) samples))))

(deftest circle-rect-collide-basics
  (is (true? (core/circle-rect-collide? 10 10 5 0 0 20 20)))
  (is (true? (core/circle-rect-collide? 15 10 5 0 0 10 20)))
  (is (false? (core/circle-rect-collide? 50 50 5 0 0 10 10))))

(deftest clamp-to-top-resets-negative-y
  (let [player (atom {:x 0 :y -5 :vx 0 :vy -3})]
    (with-redefs [core/player player]
      (core/clamp-to-top)
      (is (= 0 (:y @player)))
      (is (= 0 (:vy @player))))))

(deftest begin-save-flow-resets-state
  (let [saving (atom false)
        entry (atom "old")
        message (atom "hi")]
    (with-redefs [core/saving-score? saving
                  core/name-entry entry
                  core/save-message message]
      (core/begin-save-flow)
      (is (true? @saving))
      (is (= "" @entry))
      (is (nil? @message)))))

(deftest handle-name-input-accepts-letters-digits-and-space
  (let [entry (atom "")]
    (with-redefs [core/name-entry entry]
      (core/handle-name-input {:raw \A})
      (core/handle-name-input {:raw \1})
      (core/handle-name-input {:raw \space})
      (core/handle-name-input {:raw \!})
      (is (= "A1 " @entry)))))

(deftest handle-name-input-backspace-removes-last-char
  (let [entry (atom "Bird")]
    (with-redefs [core/name-entry entry]
      (core/handle-name-input {:keyword :backspace})
      (is (= "Bir" @entry)))))

(deftest handle-name-input-enforces-max-length
  (let [entry (atom "ABCDEFGHIJKL")]
    (with-redefs [core/name-entry entry]
      (core/handle-name-input {:raw \M})
      (is (= "ABCDEFGHIJKL" @entry)))))

(deftest handle-name-input-saves-and-resets
  (let [entry (atom "Ada")
        saving (atom true)
        message (atom nil)
        saved (atom nil)
        refreshed (atom 0)
        score (atom 42)]
    (with-redefs [core/name-entry entry
                  core/saving-score? saving
                  core/save-message message
                  core/score score
                  core/save-score! (fn [player-name score-value]
                                     (reset! saved {:name player-name :score score-value}))
                  core/refresh-leaderboard! (fn [] (swap! refreshed inc))]
      (core/handle-name-input {:keyword :enter})
      (is (= {:name "Ada" :score 42} @saved))
      (is (= "" @entry))
      (is (false? @saving))
      (is (= "Score saved!" @message))
      (is (= 1 @refreshed)))))

(deftest handle-name-input-cancel
  (let [entry (atom "Ada")
        saving (atom true)
        message (atom nil)]
    (with-redefs [core/name-entry entry
                  core/saving-score? saving
                  core/save-message message]
      (core/handle-name-input {:keyword :alt})
      (is (= "" @entry))
      (is (false? @saving))
      (is (= "Save canceled" @message)))))

(deftest leaderboard-db-roundtrip
  (with-temp-db
    (core/save-score! "Nova" 12)
    (core/save-score! "Rex" 30)
    (let [rows (core/load-top-scores)]
      (is (= 2 (count rows)))
      (is (= ["Rex" "Nova"] (mapv :name rows)))
      (is (= [30 12] (mapv :score rows))))))
