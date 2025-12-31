(ns game-project.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def screen-sizeX 1000)
(def screen-sizeY 700)

(def box-sizeX 50)
(def box-sizeY 50)
(def hitbox-radius 20)                                     ;independent collision radius

(def jump-strength 10)

(def obstacle-gap 200)
(def obstacle-density 200)
(def obstacle-speed 4)

(def obstacle-sizeX 50)
(def obstacle-sizeY (max screen-sizeY screen-sizeX))

(def player (atom {:x (/ screen-sizeX 3) :y (/ screen-sizeY 3) :vx 0 :vy 0}))
(def player-rotation (atom 0))
(def obstacles (atom []))

(def key-pressed-trigger (atom true))
(def game-over (atom false))
(def menu-active (atom true))
(def menu-time (atom 0))
(def menu-bob-amplitude 10)

(def score (atom 0))

(def leaderboard-open (atom false))
(def leaderboard-data (atom []))
(def saving-score? (atom false))
(def name-entry (atom ""))
(def save-message (atom nil))
(def ui-key-ready (atom true))
(def ignored-modifiers #{:shift :meta})

(def db-file "resources/scores.db")
(def datasource
  (delay (let [url (str "jdbc:sqlite:" (.getPath (io/file db-file)))]
           (jdbc/get-datasource {:jdbcUrl url}))))

(defn ensure-db! []
  (jdbc/execute! @datasource
                 ["CREATE TABLE IF NOT EXISTS scores (\n                    id INTEGER PRIMARY KEY AUTOINCREMENT,\n                    name TEXT NOT NULL,\n                    score INTEGER NOT NULL,\n                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP\n                  )"]))

(defn save-score! [player-name score-value]
  (ensure-db!)
  (jdbc/execute! @datasource ["INSERT INTO scores (name, score) VALUES (?, ?)" player-name score-value]))

(defn load-top-scores []
  (ensure-db!)
  (jdbc/execute! @datasource
                 ["SELECT name, score, created_at FROM scores ORDER BY score DESC, created_at ASC LIMIT 10"]
                 {:builder-fn rs/as-unqualified-lower-maps}))

(defn refresh-leaderboard! []
  (reset! leaderboard-data (load-top-scores)))

(defn poll-key []
  (let [pressed? (q/key-pressed?)
        keyword (when pressed? (q/key-as-keyword))]
    (when-not pressed?
      (reset! ui-key-ready true))
    (when (and pressed? @ui-key-ready (not (ignored-modifiers keyword)))
      (reset! ui-key-ready false)
      {:keyword keyword
       :raw (q/raw-key)
       :key-code (q/key-code)})))

(defn begin-save-flow []
  (reset! saving-score? true)
  (reset! name-entry "")
  (reset! save-message nil))

(defn handle-name-input [key-event]
  (when key-event
    (let [{:keys [keyword raw key-code]} key-event
          raw-code (when raw (int raw))
          raw-char (when raw-code (char raw-code))
          left-ctrl? (or (= keyword :control)
                         (= key-code java.awt.event.KeyEvent/VK_CONTROL)
                         (= raw-code java.awt.event.KeyEvent/VK_CONTROL))
          alt-exit? (or (= keyword :alt)
                        (= key-code java.awt.event.KeyEvent/VK_ALT)
                        (= raw-code java.awt.event.KeyEvent/VK_ALT))
          enter? (or (= keyword :enter) (= keyword :return))
          backspace? (or (= keyword :backspace)
                         (= key-code java.awt.event.KeyEvent/VK_BACK_SPACE)
                         (= raw-code java.awt.event.KeyEvent/VK_BACK_SPACE)
                         (= keyword :delete)
                         (= key-code java.awt.event.KeyEvent/VK_DELETE)
                         (= raw-code java.awt.event.KeyEvent/VK_DELETE))]
      (cond
        (or left-ctrl? enter?)
        (let [trimmed (-> @name-entry str/trim)]
          (when (seq trimmed)
            (save-score! trimmed @score)
            (refresh-leaderboard!)
            (reset! save-message "Score saved!")
            (reset! saving-score? false)
            (reset! name-entry "")))

        alt-exit?
        (do (reset! saving-score? false)
            (reset! name-entry "")
            (reset! save-message "Save canceled"))

        backspace?
        (swap! name-entry #(if (seq %) (subs % 0 (dec (count %))) ""))

        (and raw-char (or (Character/isLetterOrDigit raw-char) (= raw-char \space)))
        (when (< (count @name-entry) 12)
          (swap! name-entry str raw-char))))))

(defn setup []
  ; Set frame rate.
  (q/frame-rate 60)
  ; Set color mode to HSB (HSV) instead of default RGB.
  (q/color-mode :hsb)
  (ensure-db!)
  (let [; create url to load image 100x100
        url "bird.png"]
    (q/set-state! :image (q/load-image url))))


(defn random-range [min max]
  (+ min (rand-int (- max min 1))))

(defn reset-game []
  (reset! game-over false)
  (reset! score 0)
  (reset! key-pressed-trigger true)
  (reset! player {:x (/ screen-sizeX 3) :y (/ screen-sizeY 3) :vx 0 :vy 0})
  (reset! obstacles [])
  (reset! menu-time 0)
  (reset! saving-score? false)
  (reset! name-entry "")
  (reset! save-message nil))

(defn clamp-to-top []
  (when (< (:y @player) 0)
    (swap! player assoc :y 0 :vy 0)))

(defn circle-rect-collide? [cx cy r rx ry rw rh]
  (let [closest-x (-> cx (max rx) (min (+ rx rw)))
        closest-y (-> cy (max ry) (min (+ ry rh)))
        dx (- cx closest-x)
        dy (- cy closest-y)]
    (<= (+ (* dx dx) (* dy dy)) (* r r))))

(defn handle-menu-state [key-event]
  (swap! menu-time + 0.1)
  (let [base (/ screen-sizeY 3)
        bob (+ base (* menu-bob-amplitude (Math/sin @menu-time)))]
    (swap! player assoc :y bob :vy 0))
  (when key-event
    (case (:keyword key-event)
      :space (do (reset-game)
                 (reset! menu-active false))
      :l (do (swap! leaderboard-open not)
             (when @leaderboard-open (refresh-leaderboard!)))
      :escape (q/exit)
      nil)))

(defn handle-game-state [key-event]
  (when @saving-score?
    (handle-name-input key-event))

  (let [x (:x @player)
        y (:y @player)
        vy (:vy @player)]
    (if (> (+ y box-sizeY vy) screen-sizeY)                 ;Gravity
      (do (swap! player assoc :vy 0)
          (swap! player assoc :y (- screen-sizeY box-sizeY -1))
          (q/fill 0)
          (q/text-size 100)
          (q/text-align :center :center)
          (q/text "Game over!" (/ screen-sizeX 2) (/ screen-sizeY 2)))
      (swap! player update :vy + 0.7))

    (swap! player update :y + vy)                           ;player movement
    (clamp-to-top)

    (when (and @key-pressed-trigger (q/key-pressed?))
      (swap! player assoc :vy (- 0 jump-strength))
      (reset! key-pressed-trigger false))
    (when (and (not @key-pressed-trigger) (not (q/key-pressed?)))
      (reset! key-pressed-trigger true)))


  (when (not @game-over)
    (swap! obstacles #(mapv (fn [param1] (update param1 :x - obstacle-speed)) %)))

  (when (= @game-over true)
    (when (and key-event (= (:keyword key-event) :s) (not @saving-score?))
      (begin-save-flow))
    (when (and key-event (#{:r :right :up :down :m} (:keyword key-event)) (not @saving-score?))
      (case (:keyword key-event)
        :r (reset-game)
        :m (do (reset-game)
               (reset! menu-active true))
        nil)))

  (doseq [m @obstacles]
    (let [x (:x @player)
          y (:y @player)
          ox (:x m)
          oy (:y m)
          cx (+ x (/ box-sizeX 2))                          ;circle center x
          cy (+ y (/ box-sizeY 2))                          ;circle center y
          r  hitbox-radius                                  ;decoupled circular hitbox radius
          top-rect-y (- oy obstacle-sizeY obstacle-gap)
          bottom-rect-y oy]
      (when (or (circle-rect-collide? cx cy r ox top-rect-y obstacle-sizeX obstacle-sizeY)
                (circle-rect-collide? cx cy r ox bottom-rect-y obstacle-sizeX obstacle-sizeY))
        (q/fill 0)
        (q/text-size 100)
        (q/text-align :center :center)
        (q/text "Game over!" (/ screen-sizeX 2) (/ screen-sizeY 2))
        (swap! player assoc :vy 0 :vx 0)
        (reset! game-over true))))

  (when (empty? @obstacles)
    (swap! obstacles conj {:x (+ screen-sizeX box-sizeX)
                           :y (random-range obstacle-gap screen-sizeY)}))
  (when (and (not (empty? @obstacles))
             (< (:x (last @obstacles)) (- screen-sizeX obstacle-density)))
    (swap! obstacles conj {:x (+ screen-sizeX box-sizeX)
                           :y (random-range obstacle-gap screen-sizeY)}))

  (swap! obstacles #(filter (fn [param1] (>= (:x param1) (- 0 box-sizeX))) %))

  (let [filtered-obstacle (first (filter #(> (:x %) (:x @player)) @obstacles))]
    (when (and filtered-obstacle (< (:x filtered-obstacle) (+ (:x @player) 1)))
      (swap! score + 1)))

  (reset! player-rotation 0)

  (when (and (= (q/key-as-keyword) :escape) (not @saving-score?))
    (q/exit)))

(defn update-state []
  (let [key-event (poll-key)]
    (if @menu-active
      (handle-menu-state key-event)
      (handle-game-state key-event))))


(defn draw-leaderboard []
  (q/no-stroke)
  (q/fill 0 0 255 230)
  (q/rect 80 40 (- screen-sizeX 160) (- screen-sizeY 80) 20)
  (q/fill 0)
  (q/text-align :center :top)
  (q/text-size 50)
  (q/text "Leaderboard" (/ screen-sizeX 2) 80)
  (q/text-size 28)
  (doseq [[idx row] (map-indexed vector @leaderboard-data)]
    (let [{:keys [name score]} row
          y (+ 140 (* idx 30))
          line (format "%2d. %-12s %5d" (inc idx) name score)]
      (q/text line (/ screen-sizeX 2) y)))
  (q/text-size 20)
  (q/text "Press L to close" (/ screen-sizeX 2) (+ 140 (* 10 30) 20)))

(defn draw-state []
  (q/background 240)
  (update-state)

  (doseq [m @obstacles]
    (let [x (:x m)
          y (:y m)]
      (q/fill 100)
      (q/rect x (- y obstacle-sizeY obstacle-gap) obstacle-sizeX obstacle-sizeY)
      (q/rect x y obstacle-sizeX obstacle-sizeY)))

  (q/fill 0)
  (q/text-size 100)
  (q/text-align :center :center)
  (q/text (str @score) (/ screen-sizeX 2) (/ screen-sizeY 7))
  (when @game-over
    (q/fill 0)
    (q/text-size 40)
    (q/text-align :center :center)
    (q/text "Press R to Restart" (/ screen-sizeX 2) (+ (/ screen-sizeY 2) 80))
    (q/text "Press S to Save" (/ screen-sizeX 2) (+ (/ screen-sizeY 2) 120))
    (q/text "Press M for Menu" (/ screen-sizeX 2) (+ (/ screen-sizeY 2) 160)))

  (let [im (q/state :image)]
    (when (q/loaded? im)
      (q/push-matrix)
      (q/image-mode :corners)
      (q/image im
               (:x @player) (:y @player)
               (+ (:x @player) box-sizeX) (+ (:y @player) box-sizeY))
      (q/pop-matrix)))

  (when @menu-active
    (q/fill 0)
    (q/text-align :center :center)
    (q/text-size 80)
    (q/text "Clojure Bird" (/ screen-sizeX 2) (/ screen-sizeY 3))
    (q/text-size 40)
    (q/text "Press Start (Space Bar)" (/ screen-sizeX 2) (+ (/ screen-sizeY 3) 80))
    (q/text-size 30)
    (q/text "Press L for Leaderboard" (/ screen-sizeX 2) (+ (/ screen-sizeY 3) 125))
    (q/text "Press Esc to Exit" (/ screen-sizeX 2) (+ (/ screen-sizeY 3) 170)))

  (when (and @menu-active @leaderboard-open)
    (draw-leaderboard))

  (when (and @game-over @saving-score?)
    (q/no-stroke)
    (q/fill 0 0 255 230)
    (q/rect 150 200 (- screen-sizeX 300) 220 20)
    (q/fill 0)
    (q/text-align :center :center)
    (q/text-size 36)
    (q/text "Enter your name:" (/ screen-sizeX 2) (- (/ screen-sizeY 2) 60))
    (q/text-size 30)
    (q/text (str @name-entry (if (< (mod (q/frame-count) 60) 30) "_" ""))
            (/ screen-sizeX 2) (- (/ screen-sizeY 2) 20))
    (q/text-size 20)
    (q/text "Press Left Ctrl or Enter to save, Left Alt to cancel" (/ screen-sizeX 2) (+ (/ screen-sizeY 2) 20)))

  (when @save-message
    (q/fill 0)
    (q/text-align :center :center)
    (q/text-size 24)
    (q/text @save-message (/ screen-sizeX 2) (+ (/ screen-sizeY 2) 170))))



(defn -main []
  (q/defsketch game-project
               :title "Game"
               :size [screen-sizeX screen-sizeY]
               :setup setup
               :update update-state
               :draw draw-state
               :features [:keep-on-top]))
























