(ns game-project.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(def screen-sizeX 1000)
(def screen-sizeY 600)

(def box-sizeX 50)
(def box-sizeY 50)

(def obstacle-gap 200)
(def obstacle-density 200)
(def obstacle-speed 4)

(def obstacle-size screen-sizeX)

(defn setup []
  ; Set frame rate to 30 frames per second.
  (q/frame-rate 60)
  ; Set color mode to HSB (HSV) instead of default RGB.
  (q/color-mode :hsb))

(def player (atom {:x (/ screen-sizeX 3) :y 100 :vx 0 :vy 0}))
(def obstacle (atom {:x (+ screen-sizeX box-sizeX) :y (/ screen-sizeY 2)}))
(def obstacles (atom []))

(def v [{:a 1 :b 2} {:c 3 :d 4} {:e 5 :f 6}])



(def key-pressed-trigger (atom true))
(def game-over (atom false))


(defn random-range [min max]
  (+ min (rand-int (- max min 1))))

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
    (if (< y 0)
      (do (swap! player assoc :vy 0)
          (swap! player assoc :y 0)))

    (swap! player update :y + vy)                           ;player movement

    (if (and @key-pressed-trigger (q/key-pressed?))
      (do
        (swap! player assoc :vy -7)                         ;jump
        (reset! key-pressed-trigger false)))
    (if (and (not @key-pressed-trigger) (not (q/key-pressed?)))
      (do
        (reset! key-pressed-trigger true)))
    )
  (let [x (:x @obstacle)
        y (:y @obstacle)]
    (if (not @game-over)
      (swap! obstacle update :x - 2)))                      ;obstacle moving



  (swap! obstacles #(mapv                                   ;obstacles moving
                      (fn [param1]
                        (update param1 :x - obstacle-speed)) %))

  (for [[k v] @obstacles]
    (if (not @game-over)
      ;(swap! obstacleX update :x - 2 )

      (println (str k ": " v))
      )
    )

  ;(let [x (:x @player)                                      ;Collision detection
  ;      y (:y @player)
  ;      ox (:x @obstacle)
  ;      oy (:y @obstacle)]
  ;  (if (or
  ;        (and
  ;          (> (+ x box-sizeX) ox)
  ;          (> (+ y box-sizeY) oy)
  ;          (< x (+ ox box-sizeY))
  ;          )
  ;        (and
  ;          (> (+ x box-sizeX) ox)
  ;          (< y (- oy 200))
  ;          (< x (+ ox box-sizeY))))
  ;    (do (q/fill 0)                                        ;set text color
  ;        (q/text-size 100)                                 ;set text size
  ;        (q/text-align :center :center)                    ;align text horizontal and vertical
  ;        (q/text "Game over!" (/ screen-sizeX 2) (/ screen-sizeY 2)) ;text
  ;        (swap! player assoc :vy 0)
  ;        (swap! player assoc :vx 0)
  ;        (reset! game-over true)
  ;        )))
  (doseq [m @obstacles]
    (let [x (:x @player)                                      ;Collision detection
          y (:y @player)
          ox (:x m)
          oy (:y m)]
      (if (or
            (and
              (> (+ x box-sizeX) ox)
              (> (+ y box-sizeY) oy)
              (< x (+ ox box-sizeY))
              )
            (and
              (> (+ x box-sizeX) ox)
              (< y (- oy obstacle-gap))
              (< x (+ ox box-sizeY))))
        (do (q/fill 0)                                        ;set text color
            (q/text-size 100)                                 ;set text size
            (q/text-align :center :center)                    ;align text horizontal and vertical
            (q/text "Game over!" (/ screen-sizeX 2) (/ screen-sizeY 2)) ;text
            (swap! player assoc :vy 0)
            (swap! player assoc :vx 0)
            (reset! game-over true)
            )))
    ;(println "Value of a: " (:x m) ", Value of b: " (:y m))
    )



  (if (empty? @obstacles)
    (do (swap! obstacles conj {:x (+ screen-sizeX box-sizeX)
                               :y (random-range obstacle-gap
                                                (- screen-sizeY obstacle-gap))})
        )
    (do (if (< (:x (last @obstacles)) (- screen-sizeX obstacle-density ))
          (do
            (swap! obstacles conj {:x (+ screen-sizeX box-sizeX)
                                   :y (random-range obstacle-gap
                                                    (- screen-sizeY obstacle-gap))}) ;working
            )
          )))

  (swap! obstacles #(filter (fn [param1] (>= (:x param1) (- 0 box-sizeX))) %)) ;deletes the obstacle if x lover than {0 - box-sizeX}




  ;(doseq [m v]
  ;  (println "Value of a: " (:a m) ", Value of b: " (:b m)))
  )



(defn draw-state []
  ; Clear the sketch by filling it with light-grey color.
  (q/background 240)
  (update-state)
  (let [x (:x @player)
        y (:y @player)]
    (q/fill 200)
    (q/rect x y box-sizeX box-sizeY))
  ;(let [x (:x @obstacle)
  ;      y (:y @obstacle)]
  ;  (q/fill 100)
  ;  (q/rect x (- y obstacle-size 200) box-sizeX obstacle-size)
  ;  (q/rect x y box-sizeX obstacle-size))


  (doseq [m @obstacles]
    (let [x (:x m)
          y (:y m)]
      (q/fill 100)
      (q/rect x (- y obstacle-size obstacle-gap) box-sizeX obstacle-size)
      (q/rect x y box-sizeX obstacle-size))
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
