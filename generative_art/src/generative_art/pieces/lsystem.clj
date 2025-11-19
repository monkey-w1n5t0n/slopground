(ns generative-art.pieces.lsystem
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(def canvas-width 3000)
(def canvas-height 3000)

;; L-system rules for a plant-like structure
(def axiom "F")
(def rules {"F" "FF+[+F-F-F]-[-F+F+F]"})
(def iterations 4)
(def angle 25) ;; degrees
(def segment-length 8)

(defn apply-rules [ch]
  (get rules (str ch) (str ch)))

(defn generate-lsystem [axiom iterations]
  (if (zero? iterations)
    axiom
    (generate-lsystem (apply str (map apply-rules axiom)) (dec iterations))))

(defn setup [seed]
  (q/random-seed seed)
  (q/noise-seed seed)
  (q/frame-rate 1)
  (let [lstring (generate-lsystem axiom iterations)]
    {:seed seed
     :lstring lstring
     :ready false}))

(defn draw-lsystem [lstring]
  (doseq [ch lstring]
    (case ch
      \F (do
           ;; Draw forward
           (q/line 0 0 0 (- segment-length))
           (q/translate 0 (- segment-length)))

      \+ (q/rotate (q/radians angle))

      \- (q/rotate (q/radians (- angle)))

      \[ (q/push-matrix)

      \] (q/pop-matrix)

      nil)))

(defn update-state [state]
  (if (:ready state)
    (do
      (q/save (str "output/lsystem_seed_" (:seed state) ".jpg"))
      (println (str "L-system fractal artwork saved to output/lsystem_seed_" (:seed state) ".jpg"))
      (q/exit)
      state)
    (assoc state :ready true)))

(defn draw-state [state]
  (q/background 250 248 240)
  (q/color-mode :hsb 360 100 100 100)

  (q/push-matrix)
  ;; Position at bottom center
  (q/translate (/ canvas-width 2) (* canvas-height 0.95))

  ;; Draw with color variation
  (q/stroke 100 65 50)
  (q/stroke-weight 2.5)
  (draw-lsystem (:lstring state))

  (q/pop-matrix))

(defn run [seed]
  (q/sketch
   :title "L-System Fractal"
   :size [canvas-width canvas-height]
   :setup #(setup seed)
   :update update-state
   :draw draw-state
   :middleware [m/fun-mode]
   :renderer :java2d))
