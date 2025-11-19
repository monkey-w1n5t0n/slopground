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
  (let [stack (atom [])]
    (q/push-matrix)
    (doseq [ch lstring]
      (case ch
        \F (do
             ;; Draw forward
             (let [noise-offset (* (q/noise (/ (q/frame-count) 100.0)) 3)]
               (q/line 0 0 0 (- (+ segment-length noise-offset))))
             (q/translate 0 (- segment-length)))

        \+ (q/rotate (q/radians (+ angle (* (q/noise (/ (q/frame-count) 50.0)) 5))))

        \- (q/rotate (q/radians (- (+ angle (* (q/noise (/ (q/frame-count) 50.0)) 5)))))

        \[ (do
             (swap! stack conj {:x (q/screen-x 0 0)
                               :y (q/screen-y 0 0)
                               :angle (aget (q/current-graphics) "curMatrix" "m02")})
             (q/push-matrix))

        \] (do
             (q/pop-matrix)
             (swap! stack pop))

        nil))
    (q/pop-matrix)))

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

  ;; Position at bottom center
  (q/translate (/ canvas-width 2) (* canvas-height 0.95))

  ;; Draw multiple slightly offset versions for depth
  (doseq [i (range 5)]
    (q/push-matrix)
    (let [offset (* i 15)
          hue (q/map-range i 0 5 80 140)
          alpha (q/map-range i 0 5 80 30)]
      (q/translate offset 0)
      (q/stroke hue 70 60 alpha)
      (q/stroke-weight (- 4 (* i 0.5)))
      (draw-lsystem (:lstring state)))
    (q/pop-matrix)))

(defn run [seed]
  (q/sketch
   :title "L-System Fractal"
   :size [canvas-width canvas-height]
   :setup #(setup seed)
   :update update-state
   :draw draw-state
   :middleware [m/fun-mode]
   :renderer :java2d))
