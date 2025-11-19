(ns generative-art.pieces.triangular-mesh
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [generative-art.util :refer [poisson-disc-sample]]))

(def canvas-width 3000)
(def canvas-height 3000)

(defn setup [seed]
  (q/random-seed seed)
  (q/noise-seed seed)
  (q/frame-rate 1)
  ;; Generate points with Poisson disc sampling
  (let [interior-points (poisson-disc-sample 120 12 200 (- canvas-width 200) 200 (- canvas-height 200) seed)
        ;; Add border points for better triangulation
        border-points (concat
                       (for [x (range 0 canvas-width 150)] [x 0])
                       (for [x (range 0 canvas-width 150)] [x canvas-height])
                       (for [y (range 0 canvas-height 150)] [0 y])
                       (for [y (range 0 canvas-height 150)] [canvas-width y]))
        all-points (concat interior-points border-points)
        ;; Simple triangulation: connect nearby points
        triangles (for [[x1 y1] all-points
                       [x2 y2] all-points
                       [x3 y3] all-points
                       :when (and (not= [x1 y1] [x2 y2])
                                 (not= [x2 y2] [x3 y3])
                                 (not= [x1 y1] [x3 y3]))]
                   (let [dist12 (q/dist x1 y1 x2 y2)
                         dist23 (q/dist x2 y2 x3 y3)
                         dist31 (q/dist x3 y3 x1 y1)]
                     (when (and (< dist12 250) (< dist23 250) (< dist31 250))
                       [[x1 y1] [x2 y2] [x3 y3]])))]
    {:seed seed
     :triangles (filter some? (take 2000 triangles))
     :ready false}))

(defn update-state [state]
  (if (:ready state)
    (do
      (q/save (str "output/triangular_mesh_seed_" (:seed state) ".jpg"))
      (println (str "Triangular mesh artwork saved to output/triangular_mesh_seed_" (:seed state) ".jpg"))
      (q/exit)
      state)
    (assoc state :ready true)))

(defn draw-state [state]
  (q/background 245 242 235)
  (q/stroke-weight 1.5)
  (q/color-mode :hsb 360 100 100 100)

  (doseq [[[x1 y1] [x2 y2] [x3 y3]] (:triangles state)]
    (let [cx (/ (+ x1 x2 x3) 3)
          cy (/ (+ y1 y2 y3) 3)
          hue (mod (* (q/noise (* cx 0.002) (* cy 0.002)) 360) 360)
          saturation (q/map-range (q/noise (* cx 0.001) (* cy 0.001)) 0 1 30 65)
          brightness (q/map-range (q/noise (* cx 0.0015) (* cy 0.0015)) 0 1 70 90)
          alpha 85]
      (q/fill hue saturation brightness alpha)
      (q/stroke hue saturation (- brightness 20) 60)
      (q/triangle x1 y1 x2 y2 x3 y3))))

(defn run [seed]
  (q/sketch
   :title "Triangular Mesh"
   :size [canvas-width canvas-height]
   :setup #(setup seed)
   :update update-state
   :draw draw-state
   :middleware [m/fun-mode]
   :renderer :java2d))
