(ns generative-art.pieces.voronoi
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [generative-art.util :refer [poisson-disc-sample]]))

(def canvas-width 3000)
(def canvas-height 3000)
(def sample-resolution 6)

(defn setup [seed]
  (q/random-seed seed)
  (q/noise-seed seed)
  (q/frame-rate 1)
  ;; Generate points using Poisson disc sampling
  (let [points (poisson-disc-sample 100 12 0 canvas-width 0 canvas-height seed)]
    {:seed seed
     :points points
     :ready false}))

(defn closest-point-index [x y points]
  (let [distances (map-indexed
                   (fn [idx [px py]]
                     (let [dx (- x px)
                           dy (- y py)]
                       [idx (+ (* dx dx) (* dy dy))]))
                   points)
        [idx _] (apply min-key second distances)]
    idx))

(defn update-state [state]
  (if (:ready state)
    (do
      (q/save (str "output/voronoi_seed_" (:seed state) ".jpg"))
      (println (str "Voronoi diagram saved to output/voronoi_seed_" (:seed state) ".jpg"))
      (q/exit)
      state)
    (assoc state :ready true)))

(defn draw-state [state]
  (q/background 245 242 235)
  (q/no-stroke)
  (q/color-mode :hsb 360 100 100)

  ;; Draw Voronoi cells
  (doseq [x (range 0 canvas-width sample-resolution)
          y (range 0 canvas-height sample-resolution)]
    (let [idx (closest-point-index x y (:points state))
          [px py] (nth (:points state) idx)
          hue (mod (* idx 37) 360)
          saturation (q/map-range (q/noise (* px 0.002) (* py 0.002)) 0 1 45 75)
          brightness (q/map-range (q/noise (* px 0.001) (* py 0.001)) 0 1 70 90)]
      (q/fill hue saturation brightness)
      (q/rect x y sample-resolution sample-resolution)))

  ;; Draw points
  (q/fill 0 0 20)
  (doseq [[x y] (:points state)]
    (q/ellipse x y 12 12)))

(defn run [seed]
  (q/sketch
   :title "Voronoi Diagram"
   :size [canvas-width canvas-height]
   :setup #(setup seed)
   :update update-state
   :draw draw-state
   :middleware [m/fun-mode]
   :renderer :java2d))
