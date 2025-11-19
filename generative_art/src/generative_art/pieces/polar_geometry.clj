(ns generative-art.pieces.polar-geometry
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [genartlib.curves :refer [chaikin-curve]]))

(def canvas-width 3000)
(def canvas-height 3000)
(def num-layers 120)
(def num-segments 12)

(defn setup [seed]
  (q/random-seed seed)
  (q/noise-seed seed)
  (q/frame-rate 1)
  {:seed seed
   :ready false})

(defn polar-to-cartesian [r theta]
  [(* r (q/cos theta))
   (* r (q/sin theta))])

(defn create-radial-shape [radius layer-idx seed-val]
  (let [angle-step (/ q/TWO-PI num-segments)]
    (for [i (range num-segments)]
      (let [theta (* i angle-step)
            noise-val (q/noise (* (q/cos theta) 0.5)
                              (* (q/sin theta) 0.5)
                              (* layer-idx 0.05))
            variation (* radius 0.3 noise-val)
            r (+ radius variation)]
        (polar-to-cartesian r theta)))))

(defn update-state [state]
  (if (:ready state)
    (do
      (q/save (str "output/polar_geometry_seed_" (:seed state) ".jpg"))
      (println (str "Polar geometry artwork saved to output/polar_geometry_seed_" (:seed state) ".jpg"))
      (q/exit)
      state)
    (assoc state :ready true)))

(defn draw-state [state]
  (q/background 250 248 242)
  (q/translate (/ canvas-width 2) (/ canvas-height 2))
  (q/color-mode :hsb 360 100 100 100)
  (q/stroke-weight 2)

  (doseq [layer (range num-layers)]
    (let [radius (* layer (/ 1100 num-layers))
          points (create-radial-shape radius layer (:seed state))
          smoothed (chaikin-curve (conj (vec points) (first points)) 3)
          hue (mod (+ 30 (* layer 2.5)) 360)
          saturation (q/map-range (q/noise (* layer 0.05)) 0 1 35 70)
          brightness (q/map-range layer 0 num-layers 90 60)
          alpha (q/map-range layer 0 num-layers 80 40)]

      ;; Fill
      (q/fill hue saturation brightness alpha)
      (q/stroke hue saturation (- brightness 15) alpha)

      (q/begin-shape)
      (doseq [[x y] smoothed]
        (q/vertex x y))
      (q/end-shape :close))))

(defn run [seed]
  (q/sketch
   :title "Polar Geometry"
   :size [canvas-width canvas-height]
   :setup #(setup seed)
   :update update-state
   :draw draw-state
   :middleware [m/fun-mode]
   :renderer :java2d))
