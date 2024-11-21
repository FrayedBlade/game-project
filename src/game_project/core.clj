(ns game-project.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(defn setup []
  ; Set frame rate to 30 frames per second.
  (q/frame-rate 30)
  ; Set color mode to HSB (HSV) instead of default RGB.
  (q/color-mode :hsb))

(def player (atom {:x 100 :y 100 :vx 0 :vy 0}))

(defn update-state []
  (let [x (:x @player)
        y (:y @player)
        vy (:vy @player)]
    (if (< y 400) (swap! player update :vy + 1) (do (swap! player assoc :y 400) (swap! player assoc :vy 0) ))
    (swap! player update :y + vy)

    (if (q/key-pressed?) (swap! player assoc :vy -10))
    )



  (defn draw-state []
    ; Clear the sketch by filling it with light-grey color.
    (q/background 240)
    (update-state)

    (let [x (:x @player)
          y (:y @player)]
      (q/rect x y 100 100))))


(q/defsketch game-project
             :title "You spin my circle right round"
             :size [500 500]
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
