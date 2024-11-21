(ns game-project.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(def screen-sizeX 1000)
(def screen-sizeY 600)

(def box-sizeX 50)
(def box-sizeY 50)

(defn setup []
  ; Set frame rate to 30 frames per second.
  (q/frame-rate 30)
  ; Set color mode to HSB (HSV) instead of default RGB.
  (q/color-mode :hsb))

(def player (atom {:x (/ screen-sizeX 3) :y 100 :vx 0 :vy 0}))



(defn update-state []
  (let [x (:x @player)
        y (:y @player)
        vy (:vy @player)]
    (if (> y (- screen-sizeY box-sizeY 1))
      (do (swap! player assoc :vy 0)
          (swap! player assoc :y (- screen-sizeY box-sizeY))
          (q/fill 0)
          (q/text-size 100)
          (q/text-align :center :center)
          (q/text "Game over!" (/ screen-sizeX 2) (/ screen-sizeY 2))
          )
      (do (swap! player update :vy + 1)))
    (swap! player update :y + vy)

    (if (q/key-pressed?) (swap! player assoc :vy -10))
    )



  (defn draw-state []
    ; Clear the sketch by filling it with light-grey color.
    (q/background 240)
    (update-state)

    (let [x (:x @player)
          y (:y @player)]
      (q/fill 200)
      (q/rect x y box-sizeX box-sizeY))))


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
