(ns game-project.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(def screen-sizeX 1000)
(def screen-sizeY 600)

(def box-sizeX 50)
(def box-sizeY 50)

(defn setup []
  ; Set frame rate to 30 frames per second.
  (q/frame-rate 60)
  ; Set color mode to HSB (HSV) instead of default RGB.
  (q/color-mode :hsb))

(def player (atom {:x (/ screen-sizeX 3) :y 100 :vx 0 :vy 0}))
(def obstacle (atom {:x (+ screen-sizeX box-sizeX)  :y (/ screen-sizeY 2)}))

(def key-pressed-trigger (atom true))

(defn update-state []
  (let [x (:x @player)
        y (:y @player)
        vy (:vy @player)]
    (if (> (+ y box-sizeY vy) screen-sizeY)                 ;Gravity
      (do (swap! player assoc :vy 0)
          (swap! player assoc :y (- screen-sizeY box-sizeY -1))
          (q/fill 0)                                        ;set text color
          (q/text-size 100)                                 ;set text size
          (q/text-align :center :center)                    ;align text horizontal and vertical
          (q/text "Game over!" (/ screen-sizeX 2) (/ screen-sizeY 2)) ;text
          )
      (do (swap! player update :vy + 0.7)))
    (if (< y 0)
      (do (swap! player assoc :vy 0)
          (swap! player assoc :y 0)))
    (swap! player update :y + vy)

    (if (and @key-pressed-trigger (q/key-pressed?) )
      (do
        (swap! player assoc :vy -7)
        (reset! key-pressed-trigger false)) )      ;jump
    (if (and  (not @key-pressed-trigger) (not (q/key-pressed?)) )
      (do
        (reset! key-pressed-trigger true)) )
    )
  (let [x (:x @obstacle)
        y (:y @obstacle)]
    (swap! obstacle update :x - 2)))                        ;obstacle moving



(defn draw-state []
  ; Clear the sketch by filling it with light-grey color.
  (q/background 240)
  (update-state)
  (let [x (:x @player)
        y (:y @player)]
    (q/fill 200)
    (q/rect x y box-sizeX box-sizeY))
  (let [x (:x @obstacle)
        y (:y @obstacle)]
    (q/fill 100)
    (q/rect x (- y 200) box-sizeX box-sizeY)
    (q/rect x y box-sizeX box-sizeY)))



(q/defsketch game-project
             :title "You spin my circle right round"
             :size [screen-sizeX screen-sizeY]
             ; setup function called only once, during sketch initialization.
             :setup setup
             ; update-state is called on each iteration before draw-state.
             :update update-state
             :draw draw-state
             :features [:keep-on-top]
             ; This sketch uses functional-mode middleware.
             ; Check quil wiki for more info about middlewares and particularly
             ; fun-mode.
             ;:middleware [m/fun-mode]
             )
