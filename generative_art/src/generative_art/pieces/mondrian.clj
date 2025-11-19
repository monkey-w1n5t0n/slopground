(ns generative-art.pieces.mondrian
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(def canvas-width 3000)
(def canvas-height 3000)
(def min-size 150)

(defn setup [seed]
  (q/random-seed seed)
  (q/frame-rate 1)
  {:seed seed
   :splits []
   :ready false})

(defn can-split-h? [rect]
  (> (- (:y2 rect) (:y1 rect)) (* 2 min-size)))

(defn can-split-v? [rect]
  (> (- (:x2 rect) (:x1 rect)) (* 2 min-size)))

(defn split-rect [rect]
  (let [can-h (can-split-h? rect)
        can-v (can-split-v? rect)]
    (cond
      (and can-h can-v)
      (if (< (q/random 1) 0.5)
        ;; Split horizontally
        (let [split-y (+ (:y1 rect) min-size (q/random (- (:y2 rect) (:y1 rect) (* 2 min-size))))]
          [{:x1 (:x1 rect) :y1 (:y1 rect) :x2 (:x2 rect) :y2 split-y}
           {:x1 (:x1 rect) :y1 split-y :x2 (:x2 rect) :y2 (:y2 rect)}])
        ;; Split vertically
        (let [split-x (+ (:x1 rect) min-size (q/random (- (:x2 rect) (:x1 rect) (* 2 min-size))))]
          [{:x1 (:x1 rect) :y1 (:y1 rect) :x2 split-x :y2 (:y2 rect)}
           {:x1 split-x :y1 (:y1 rect) :x2 (:x2 rect) :y2 (:y2 rect)}]))

      can-h
      (let [split-y (+ (:y1 rect) min-size (q/random (- (:y2 rect) (:y1 rect) (* 2 min-size))))]
        [{:x1 (:x1 rect) :y1 (:y1 rect) :x2 (:x2 rect) :y2 split-y}
         {:x1 (:x1 rect) :y1 split-y :x2 (:x2 rect) :y2 (:y2 rect)}])

      can-v
      (let [split-x (+ (:x1 rect) min-size (q/random (- (:x2 rect) (:x1 rect) (* 2 min-size))))]
        [{:x1 (:x1 rect) :y1 (:y1 rect) :x2 split-x :y2 (:y2 rect)}
         {:x1 split-x :y1 (:y1 rect) :x2 (:x2 rect) :y2 (:y2 rect)}])

      :else
      [rect])))

(defn subdivide [rects depth]
  (if (zero? depth)
    rects
    (let [new-rects (mapcat split-rect rects)]
      (subdivide new-rects (dec depth)))))

(defn choose-color []
  (let [r (q/random 1)]
    (cond
      (< r 0.08) [224 24 45]      ;; Red
      (< r 0.16) [36 72 175]      ;; Blue
      (< r 0.24) [249 222 61]     ;; Yellow
      :else      [250 248 245]))) ;; White/off-white

(defn update-state [state]
  (if (:ready state)
    (do
      (q/save (str "output/mondrian_seed_" (:seed state) ".jpg"))
      (println (str "Mondrian artwork saved to output/mondrian_seed_" (:seed state) ".jpg"))
      (q/exit)
      state)
    (let [initial-rect {:x1 0 :y1 0 :x2 canvas-width :y2 canvas-height}
          splits (subdivide [initial-rect] 7)]
      (assoc state :splits splits :ready true))))

(defn draw-state [state]
  (q/background 250 248 245)
  (q/stroke-weight 18)
  (q/stroke 20 18 15)
  (doseq [rect (:splits state)]
    (let [[r g b] (choose-color)]
      (q/fill r g b)
      (q/rect (:x1 rect) (:y1 rect)
              (- (:x2 rect) (:x1 rect))
              (- (:y2 rect) (:y1 rect))))))

(defn run [seed]
  (q/sketch
   :title "Mondrian Subdivision"
   :size [canvas-width canvas-height]
   :setup #(setup seed)
   :update update-state
   :draw draw-state
   :middleware [m/fun-mode]
   :renderer :java2d))
