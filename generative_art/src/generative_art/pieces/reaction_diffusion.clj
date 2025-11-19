(ns generative-art.pieces.reaction-diffusion
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(def canvas-width 800)
(def canvas-height 800)
(def grid-size 400)
(def iterations 5000)

;; Gray-Scott parameters
(def feed-rate 0.055)
(def kill-rate 0.062)
(def diffusion-a 1.0)
(def diffusion-b 0.5)
(def delta-t 1.0)

(defn create-grid [size]
  (vec (repeatedly size #(vec (repeatedly size (fn [] {:a 1.0 :b 0.0}))))))

(defn setup [seed]
  (q/random-seed seed)
  (q/frame-rate 60)
  (let [grid (create-grid grid-size)
        ;; Seed with some B in the center
        seeded-grid (reduce
                     (fn [g _]
                       (let [x (+ (/ grid-size 2) (- (q/random 20) 10))
                             y (+ (/ grid-size 2) (- (q/random 20) 10))]
                         (if (and (>= x 0) (< x grid-size) (>= y 0) (< y grid-size))
                           (assoc-in g [y x :b] 1.0)
                           g)))
                     grid
                     (range 200))]
    {:seed seed
     :grid seeded-grid
     :step 0}))

(defn laplacian [grid x y key]
  (let [center (get-in grid [y x key] 0.0)
        sum-neighbors (+ (get-in grid [(mod (dec y) grid-size) x key] 0.0)
                        (get-in grid [(mod (inc y) grid-size) x key] 0.0)
                        (get-in grid [y (mod (dec x) grid-size) key] 0.0)
                        (get-in grid [y (mod (inc x) grid-size) key] 0.0))
        sum-corners (* 0.05 (+ (get-in grid [(mod (dec y) grid-size) (mod (dec x) grid-size) key] 0.0)
                                (get-in grid [(mod (dec y) grid-size) (mod (inc x) grid-size) key] 0.0)
                                (get-in grid [(mod (inc y) grid-size) (mod (dec x) grid-size) key] 0.0)
                                (get-in grid [(mod (inc y) grid-size) (mod (inc x) grid-size) key] 0.0)))]
    (- (+ sum-neighbors sum-corners) (* 4.2 center))))

(defn update-cell [grid x y]
  (let [a (get-in grid [y x :a])
        b (get-in grid [y x :b])
        lap-a (laplacian grid x y :a)
        lap-b (laplacian grid x y :b)
        abb (* a b b)
        new-a (+ a (* delta-t (- (* diffusion-a lap-a) abb (* feed-rate (- 1 a)))))
        new-b (+ b (* delta-t (- (* diffusion-b lap-b) (* -1 abb) (* (+ kill-rate feed-rate) b))))]
    {:a (max 0 (min 1 new-a))
     :b (max 0 (min 1 new-b))}))

(defn update-state [state]
  (if (>= (:step state 0) iterations)
    (do
      (q/save (str "output/reaction_diffusion_seed_" (:seed state) ".jpg"))
      (println (str "Reaction-diffusion artwork saved to output/reaction_diffusion_seed_" (:seed state) ".jpg"))
      (q/exit)
      state)
    (let [new-grid (vec (for [y (range grid-size)]
                          (vec (for [x (range grid-size)]
                                 (update-cell (:grid state) x y)))))]
      (assoc state
             :grid new-grid
             :step (inc (:step state 0))))))

(defn draw-state [state]
  (q/background 0)
  (q/no-stroke)
  (q/color-mode :hsb 360 100 100)
  (let [cell-size (/ canvas-width grid-size)]
    (doseq [y (range grid-size)
            x (range grid-size)]
      (let [b (get-in state [:grid y x :b])
            a (get-in state [:grid y x :a])
            val (- a b)
            hue (q/map-range val 0 1 200 320)
            saturation (q/map-range b 0 1 20 90)
            brightness (q/map-range val 0 1 30 95)]
        (q/fill hue saturation brightness)
        (q/rect (* x cell-size) (* y cell-size) cell-size cell-size)))))

(defn run [seed]
  (q/sketch
   :title "Reaction-Diffusion"
   :size [canvas-width canvas-height]
   :setup #(setup seed)
   :update update-state
   :draw draw-state
   :middleware [m/fun-mode]
   :renderer :java2d))
