(ns generative-art.pieces.strange-attractor
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(def canvas-width 3000)
(def canvas-height 3000)
(def num-iterations 1000000)

;; Lorenz attractor parameters
(def sigma 10.0)
(def rho 28.0)
(def beta (/ 8.0 3.0))
(def dt 0.005)

(defn setup [seed]
  (q/random-seed seed)
  (q/frame-rate 60)
  (q/background 8 5 15)
  {:seed seed
   :x 0.1
   :y 0.0
   :z 0.0
   :points []
   :step 0})

(defn lorenz-step [x y z]
  (let [dx (* sigma (- y x))
        dy (- (* x (- rho z)) y)
        dz (- (* x y) (* beta z))
        new-x (+ x (* dt dx))
        new-y (+ y (* dt dy))
        new-z (+ z (* dt dz))]
    [new-x new-y new-z]))

(defn update-state [state]
  (if (>= (:step state) num-iterations)
    (do
      (q/save (str "output/strange_attractor_seed_" (:seed state) ".jpg"))
      (println (str "Strange attractor artwork saved to output/strange_attractor_seed_" (:seed state) ".jpg"))
      (q/exit)
      state)
    (let [[new-x new-y new-z] (lorenz-step (:x state) (:y state) (:z state))]
      (assoc state
             :x new-x
             :y new-y
             :z new-z
             :step (inc (:step state))))))

(defn draw-state [state]
  (when (> (:step state) 100) ;; Skip initial transient
    (q/color-mode :hsb 360 100 100 100)
    (let [scale 50
          x (+ (/ canvas-width 2) (* (:x state) scale))
          y (+ (/ canvas-height 2.5) (* (:y state) scale))
          hue (mod (+ 200 (* (:z state) 3)) 360)
          saturation (q/map-range (:z state) 0 50 60 90)
          brightness (q/map-range (Math/abs (:y state)) 0 30 80 95)
          alpha 3]
      (q/stroke hue saturation brightness alpha)
      (q/stroke-weight 1.5)
      (q/point x y))))

(defn run [seed]
  (q/sketch
   :title "Strange Attractor"
   :size [canvas-width canvas-height]
   :setup #(setup seed)
   :update update-state
   :draw draw-state
   :middleware [m/fun-mode]
   :renderer :java2d))
