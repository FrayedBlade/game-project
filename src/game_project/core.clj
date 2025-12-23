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
  (do (reset! game-over false)
      (reset! score 0)
      (let [x (:x @player)
            y (:y @player)]
        (swap! player assoc :x (/ screen-sizeX 3))
        (swap! player assoc :y (/ screen-sizeY 3))
            )
      (swap! obstacles
             #(filter
                (fn [param1]
                  (>= (:x param1) (+ 600 box-sizeX))) %))         ;deletes the obstacle if x lover than {value}
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
                     ))
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



  (def filtered-obstacle
    (first (filter #(> (:x %) (:x @player)) @obstacles)))
  ;(println filtered-obstacle)

  (if (< (:x filtered-obstacle) (+ (:x @player) 1))
    (do
      (swap! score + 1)))
  ;(println @score)



  (swap! player-rotation + 0.01)




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

    (q/translate (/ screen-sizeX 2) (/ screen-sizeY 7))
    (q/rotate @player-rotation)
    (q/rect x y box-sizeX box-sizeY)
    )



  (doseq [m @obstacles]                                     ;obstacle drawing
    (let [x (:x m)
          y (:y m)]
      (q/fill 100)
      (q/rect x (- y obstacle-sizeY obstacle-gap) obstacle-sizeX obstacle-sizeY)
      (q/rect x y obstacle-sizeX obstacle-sizeY))
    ;(println "Value of a: " (:x m) ", Value of b: " (:y m))
    )

  (q/fill 0)                                      ;set text color
  (q/text-size 100)                               ;set text size
  (q/text-align :center :center)                  ;align text horizontal and vertical
  (q/text (str @score) (/ screen-sizeX 2) (/ screen-sizeY 7)) ;text


  (let [im (q/state :image)]
    ; check if image is loaded using function loaded?
    (when (q/loaded? im)
      (q/rotate 0.1)
      (q/image-mode :corners)
      (q/image im (:x @player) (:y @player)(+(:x @player) box-sizeX) (+(:y @player) box-sizeX))
      ))

  )

; a1 a2



(defn -main []
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
               ))


