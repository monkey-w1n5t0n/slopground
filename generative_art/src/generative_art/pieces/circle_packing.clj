(ns generative-art.pieces.circle-packing
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [generative-art.util :refer [poisson-disc-sample]]))

(def canvas-width 3000)
(def canvas-height 3000)
(def max-radius 120)
(def min-radius 3)

(defn setup [seed]
  (q/random-seed seed)
  (q/noise-seed seed)
  (q/frame-rate 60)
  (q/background 245 242 235)
  (q/no-stroke)
  ;; Generate initial points using Poisson disc sampling
  (let [points (poisson-disc-sample 40 12 0 canvas-width 0 canvas-height seed)
        circles (mapv (fn [[x y]] {:x x :y y :r min-radius :growing true}) points)]
    {:circles circles
     :step 0
     :seed seed}))

(defn circles-overlap? [c1 c2]
  (let [dx (- (:x c1) (:x c2))
        dy (- (:y c1) (:y c2))
        dist (Math/sqrt (+ (* dx dx) (* dy dy)))
        min-dist (+ (:r c1) (:r c2))]
    (< dist min-dist)))

(defn can-grow? [circle circles]
  (and (:growing circle)
       (< (:r circle) max-radius)
       (> (:x circle) (:r circle))
       (< (:x circle) (- canvas-width (:r circle)))
       (> (:y circle) (:r circle))
       (< (:y circle) (- canvas-height (:r circle)))
       (not-any? #(and (not= circle %) (circles-overlap? circle %)) circles)))

(defn update-state [state]
  (if (> (:step state 0) 800)
    (do
      (q/save (str "output/circle_packing_seed_" (:seed state) ".jpg"))
      (println (str "Circle packing artwork saved to output/circle_packing_seed_" (:seed state) ".jpg"))
      (q/exit)
      state)
    (let [updated-circles (mapv (fn [circle]
                                   (if (can-grow? circle (:circles state))
                                     (update circle :r + 0.5)
                                     (assoc circle :growing false)))
                                 (:circles state))]
      (assoc state
             :circles updated-circles
             :step (inc (:step state 0))))))

(defn draw-state [state]
  (q/color-mode :hsb 360 100 100)
  (doseq [circle (:circles state)]
    (let [hue (mod (* (:x circle) 0.08) 360)
          saturation (q/map-range (q/noise (* (:x circle) 0.001) (* (:y circle) 0.001)) 0 1 40 70)
          brightness (q/map-range (:r circle) min-radius max-radius 85 55)]
      (q/fill hue saturation brightness)
      (q/ellipse (:x circle) (:y circle) (* 2 (:r circle)) (* 2 (:r circle))))))

(defn run [seed]
  (q/sketch
   :title "Circle Packing"
   :size [canvas-width canvas-height]
   :setup #(setup seed)
   :update update-state
   :draw draw-state
   :middleware [m/fun-mode]
   :renderer :java2d))
