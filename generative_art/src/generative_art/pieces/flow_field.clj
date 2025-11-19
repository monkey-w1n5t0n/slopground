(ns generative-art.pieces.flow-field
  (:require [quil.core :as q]
            [quil.middleware :as m]))

;; Configuration
(def canvas-width 3000)
(def canvas-height 3000)
(def num-particles 8000)
(def noise-scale 0.003)
(def noise-strength 4)
(def particle-alpha 8)
(def max-steps 500)

;; Particle record
(defrecord Particle [x y prev-x prev-y steps])

(defn create-particle []
  (let [x (q/random canvas-width)
        y (q/random canvas-height)]
    (->Particle x y x y 0)))

(defn setup [seed]
  (q/random-seed seed)
  (q/noise-seed seed)
  (q/frame-rate 60)
  (q/background 250 248 245)
  (q/stroke-weight 1.5)
  {:particles (repeatedly num-particles create-particle)
   :step 0
   :seed seed})

(defn get-flow-field-angle [x y]
  (let [noise-val (q/noise (* x noise-scale) (* y noise-scale))
        angle (* noise-val q/TWO-PI noise-strength)]
    angle))

(defn update-particle [particle]
  (if (>= (:steps particle) max-steps)
    (create-particle)
    (let [angle (get-flow-field-angle (:x particle) (:y particle))
          velocity-x (q/cos angle)
          velocity-y (q/sin angle)
          new-x (+ (:x particle) velocity-x)
          new-y (+ (:y particle) velocity-y)]
      (if (and (< 0 new-x canvas-width)
               (< 0 new-y canvas-height))
        (->Particle new-x new-y (:x particle) (:y particle) (inc (:steps particle)))
        (create-particle)))))

(defn draw-particle [particle]
  (when (> (:steps particle) 1)
    (let [hue (mod (+ (* (q/noise (* (:x particle) 0.001)
                                   (* (:y particle) 0.001)) 360) 180) 360)
          saturation (q/map-range (q/noise (* (:x particle) 0.002)) 0 1 30 70)
          brightness (q/map-range (q/noise (* (:y particle) 0.002)) 0 1 40 90)]
      (q/stroke hue saturation brightness particle-alpha)
      (q/line (:prev-x particle) (:prev-y particle)
              (:x particle) (:y particle)))))

(defn update-state [state]
  (let [new-step (inc (:step state))]
    (if (>= new-step (* max-steps 1.5))
      (do
        (q/save (str "output/flow_field_seed_" (:seed state) ".jpg"))
        (println (str "Flow field artwork saved to output/flow_field_seed_" (:seed state) ".jpg"))
        (q/exit)
        state)
      {:particles (map update-particle (:particles state))
       :step new-step
       :seed (:seed state)})))

(defn draw-state [state]
  (q/color-mode :hsb 360 100 100 255)
  (doseq [particle (:particles state)]
    (draw-particle particle)))

(defn run [seed]
  (q/sketch
   :title "Flow Field Art"
   :size [canvas-width canvas-height]
   :setup #(setup seed)
   :update update-state
   :draw draw-state
   :middleware [m/fun-mode]
   :renderer :java2d))
