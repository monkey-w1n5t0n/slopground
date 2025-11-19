(ns generative-art.pieces.grid-deformation
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [generative-art.util :refer [chaikin-curve]]))

(def canvas-width 3000)
(def canvas-height 3000)
(def grid-spacing 80)
(def noise-scale 0.004)
(def displacement-strength 60)

(defn setup [seed]
  (q/random-seed seed)
  (q/noise-seed seed)
  (q/frame-rate 1)
  {:seed seed
   :ready false})

(defn create-deformed-grid [seed]
  (let [rows (range 0 (+ canvas-height grid-spacing) grid-spacing)
        cols (range 0 (+ canvas-width grid-spacing) grid-spacing)]
    {:horizontal-lines
     (for [y rows]
       (let [points (for [x cols]
                      (let [noise-x (q/noise (* x noise-scale) (* y noise-scale) 0)
                            noise-y (q/noise (* x noise-scale) (* y noise-scale) 100)
                            dx (* (- noise-x 0.5) displacement-strength)
                            dy (* (- noise-y 0.5) displacement-strength)]
                        [(+ x dx) (+ y dy)]))]
         (chaikin-curve points 2)))

     :vertical-lines
     (for [x cols]
       (let [points (for [y rows]
                      (let [noise-x (q/noise (* x noise-scale) (* y noise-scale) 0)
                            noise-y (q/noise (* x noise-scale) (* y noise-scale) 100)
                            dx (* (- noise-x 0.5) displacement-strength)
                            dy (* (- noise-y 0.5) displacement-strength)]
                        [(+ x dx) (+ y dy)]))]
         (chaikin-curve points 2)))}))

(defn update-state [state]
  (if (:ready state)
    (do
      (q/save (str "output/grid_deformation_seed_" (:seed state) ".jpg"))
      (println (str "Grid deformation artwork saved to output/grid_deformation_seed_" (:seed state) ".jpg"))
      (q/exit)
      state)
    (assoc state
           :grid (create-deformed-grid (:seed state))
           :ready true)))

(defn draw-line-with-gradient [points]
  (q/begin-shape)
  (doseq [[[x1 y1] [x2 y2]] (partition 2 1 points)]
    (q/vertex x1 y1))
  (q/end-shape))

(defn draw-state [state]
  (q/background 248 245 238)
  (q/no-fill)
  (q/stroke-weight 2.5)
  (q/color-mode :hsb 360 100 100 100)

  (when-let [grid (:grid state)]
    ;; Draw horizontal lines
    (doseq [[idx line] (map-indexed vector (:horizontal-lines grid))]
      (let [hue (mod (+ 200 (* idx 1.5)) 360)
            saturation 55
            brightness 45
            alpha 70]
        (q/stroke hue saturation brightness alpha)
        (draw-line-with-gradient line)))

    ;; Draw vertical lines
    (doseq [[idx line] (map-indexed vector (:vertical-lines grid))]
      (let [hue (mod (+ 200 (* idx 1.5)) 360)
            saturation 55
            brightness 45
            alpha 70]
        (q/stroke hue saturation brightness alpha)
        (draw-line-with-gradient line)))))

(defn run [seed]
  (q/sketch
   :title "Grid Deformation"
   :size [canvas-width canvas-height]
   :setup #(setup seed)
   :update update-state
   :draw draw-state
   :middleware [m/fun-mode]
   :renderer :java2d))
