(ns generative-art.pieces.phyllotaxis
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(def canvas-width 3000)
(def canvas-height 3000)
(def num-elements 5000)
(def golden-angle (* Math/PI (- 3 (Math/sqrt 5)))) ;; ~137.5 degrees

(defn setup [seed]
  (q/random-seed seed)
  (q/noise-seed seed)
  (q/frame-rate 1)
  {:seed seed
   :ready false})

(defn update-state [state]
  (if (:ready state)
    (do
      (q/save (str "output/phyllotaxis_seed_" (:seed state) ".jpg"))
      (println (str "Phyllotaxis artwork saved to output/phyllotaxis_seed_" (:seed state) ".jpg"))
      (q/exit)
      state)
    (assoc state :ready true)))

(defn draw-state [state]
  (q/background 15 12 20)
  (q/translate (/ canvas-width 2) (/ canvas-height 2))
  (q/color-mode :hsb 360 100 100 100)

  (doseq [i (range num-elements)]
    (let [angle (* i golden-angle)
          radius (* 8 (Math/sqrt i))
          x (* radius (Math/cos angle))
          y (* radius (Math/sin angle))
          size (q/map-range (Math/sqrt i) 0 (Math/sqrt num-elements) 1 18)
          hue (mod (+ 180 (* i 0.5)) 360)
          saturation (q/map-range (q/noise (* i 0.01)) 0 1 60 85)
          brightness (q/map-range radius 0 1200 90 60)
          alpha (q/map-range (q/noise (* i 0.008)) 0 1 40 85)]
      (q/no-stroke)
      (q/fill hue saturation brightness alpha)
      (q/ellipse x y size size))))

(defn run [seed]
  (q/sketch
   :title "Phyllotaxis Pattern"
   :size [canvas-width canvas-height]
   :setup #(setup seed)
   :update update-state
   :draw draw-state
   :middleware [m/fun-mode]
   :renderer :java2d))
