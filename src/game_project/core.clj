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
(def obstacles (atom []))

(defn setup []
  ; Set frame rate to 30 frames per second.
  (q/frame-rate 60)
  ; Set color mode to HSB (HSV) instead of default RGB.
  (q/color-mode :hsb))





(def key-pressed-trigger (atom true))
(def game-over (atom false))


(defn random-range [min max]
  (+ min (rand-int (- max min 1))))

(defn reset-game []
  (do (reset! game-over false)
      (let [x (:x @player)
            y (:y @player)]
        (swap! player assoc :x (/ screen-sizeX 3))
        (swap! player assoc :y (/ screen-sizeY 3))
            )
      (swap! obstacles
             #(filter
                (fn [param1]
                  (>= (:x param1) (+ 600 box-sizeX))) %))         ;deletes the obstacle if x lover than {0 - box-sizeX}
      ) )

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
      (do (swap! player update :vy + 0.7)))                 ;Gravity Strength


    (swap! player update :y + vy)                           ;player movement

    (if (and @key-pressed-trigger (q/key-pressed?))
      (do
        (swap! player assoc :vy (- 0 jump-strength))        ;jump
        (reset! key-pressed-trigger false)))
    (if (and (not @key-pressed-trigger) (not (q/key-pressed?)))
      (do
        (reset! key-pressed-trigger true)))
    )


  ;(let [x (:x @obstacle)
  ;      y (:y @obstacle)]
  ;  (if (not @game-over)
  ;    (swap! obstacle update :x - 2)))                      ;obstacle moving




  (if (not @game-over)
    (swap! obstacles #(mapv                                 ;obstacles moving
                        (fn [param1]
                          (update param1 :x - obstacle-speed)) %))

    )


  (if (= @game-over true)
    (do (let [key (q/key-as-keyword)]  ; Get the key that was pressed
          (when (#{:r :right :up :down} key)  ; Check if it's an arrow key
            (case key
              :r (do (reset-game)
                     (println "W")))
            )) )
    (do ))




  (doseq [m @obstacles]
    (let [x (:x @player)                                    ;Collision detection
          y (:y @player)
          ox (:x m)
          oy (:y m)]
      (if (or
            (and
              (> (+ x box-sizeX) ox)
              (> (+ y box-sizeY) oy)
              (< x (+ ox obstacle-sizeX))
              )
            (and
              (> (+ x box-sizeX) ox)
              (< y (- oy obstacle-gap))
              (< x (+ ox obstacle-sizeX))))
        (do (q/fill 0)                                      ;set text color
            (q/text-size 100)                               ;set text size
            (q/text-align :center :center)                  ;align text horizontal and vertical
            (q/text "Game over!" (/ screen-sizeX 2) (/ screen-sizeY 2)) ;text
            (swap! player assoc :vy 0)
            (swap! player assoc :vx 0)
            (reset! game-over true)
            )))
    ;(println "Value of a: " (:x m) ", Value of b: " (:y m))
    )



  (if (empty? @obstacles)                                   ;spawning of obstacles
    (do (swap! obstacles conj {:x (+ screen-sizeX box-sizeX)
                               :y (random-range obstacle-gap
                                                screen-sizeY)})
        )
    (do (if (< (:x (last @obstacles)) (- screen-sizeX obstacle-density))
          (do
            (swap! obstacles conj {:x (+ screen-sizeX box-sizeX)
                                   :y (random-range obstacle-gap
                                                    screen-sizeY)})
            )
          )))

  (swap! obstacles
         #(filter
            (fn [param1]
              (>= (:x param1) (- 0 box-sizeX))) %))         ;deletes the obstacle if x lover than {0 - box-sizeX}




  ;(doseq [m v]
  ;  (println "Value of a: " (:a m) ", Value of b: " (:b m)))
  )



(defn draw-state []
  ; Clear the sketch by filling it with light-grey color.
  (q/background 240)
  (update-state)
  (let [x (:x @player)                                      ;player drawing
        y (:y @player)]
    (q/fill 200)
    (q/rect x y box-sizeX box-sizeY))


  (doseq [m @obstacles]                                     ;obstacle drawing
    (let [x (:x m)
          y (:y m)]
      (q/fill 100)
      (q/rect x (- y obstacle-sizeY obstacle-gap) obstacle-sizeX obstacle-sizeY)
      (q/rect x y obstacle-sizeX obstacle-sizeY))
    ;(println "Value of a: " (:x m) ", Value of b: " (:y m))
    )

  )



(q/defsketch game-project
             :title "Game"
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
