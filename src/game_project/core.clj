(ns game-project.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(def screen-sizeX 1000)
(def screen-sizeY 700)

(def box-sizeX 50)
(def box-sizeY 50)

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


(defn setup []
  ; Set frame rate to 30 frames per second.
  (q/frame-rate 60)
  ; Set color mode to HSB (HSV) instead of default RGB.
  (q/color-mode :hsb)
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
  (reset! menu-time 0))

(defn clamp-to-top []
  (when (< (:y @player) 0)
    (swap! player assoc :y 0 :vy 0)))

(defn handle-menu-state []
  (swap! menu-time + 0.1)
  (let [base (/ screen-sizeY 3)
        bob (+ base (* menu-bob-amplitude (Math/sin @menu-time)))]
    (swap! player assoc :y bob :vy 0))
  (when (q/key-pressed?)
    (let [key (q/key-as-keyword)]
      (cond
        (= key :space) (do (reset-game)
                           (reset! menu-active false))
        (= key :escape) (q/exit)))))

(defn handle-game-state []
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
    (let [key (q/key-as-keyword)]
      (when (#{:r :right :up :down} key)
        (case key
          :r (reset-game)
          nil))))

  (doseq [m @obstacles]
    (let [x (:x @player)
          y (:y @player)
          ox (:x m)
          oy (:y m)]
      (when (or (and (> (+ x box-sizeX) ox)
                     (> (+ y box-sizeY) oy)
                     (< x (+ ox obstacle-sizeX)))
                (and (> (+ x box-sizeX) ox)
                     (< y (- oy obstacle-gap))
                     (< x (+ ox obstacle-sizeX))))
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

  (def filtered-obstacle (first (filter #(> (:x %) (:x @player)) @obstacles)))
  (when (and filtered-obstacle (< (:x filtered-obstacle) (+ (:x @player) 1)))
    (swap! score + 1))

  (reset! player-rotation 0)

  (when (= (q/key-as-keyword) :escape)
    (q/exit)))

(defn update-state []
  (if @menu-active
    (handle-menu-state)
    (handle-game-state)))


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

  (let [im (q/state :image)]
    (when (q/loaded? im)
      (q/push-matrix)
      (q/image-mode :corners)
      (q/image im (:x @player) (:y @player) (+ (:x @player) box-sizeX) (+ (:y @player) box-sizeX))
      (q/pop-matrix)))

  (when @menu-active
    (q/fill 0)
    (q/text-align :center :center)
    (q/text-size 80)
    (q/text "Clojure Bird" (/ screen-sizeX 2) (/ screen-sizeY 3))
    (q/text-size 40)
    (q/text "Press Start (Space Bar)" (/ screen-sizeX 2) (+ (/ screen-sizeY 3) 80))
    (q/text-size 30)
    (q/text "Press Esc to Exit" (/ screen-sizeX 2) (+ (/ screen-sizeY 3) 130))))

; b1



(defn -main []
  (q/defsketch game-project
               :title "Game"
               :size [screen-sizeX screen-sizeY]
               :setup setup
               :update update-state
               :draw draw-state
               :features [:keep-on-top]))
