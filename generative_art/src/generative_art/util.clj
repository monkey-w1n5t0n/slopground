(ns generative-art.util
  "Pure Clojure utilities to replace genartlib Java dependencies"
  (:require [quil.core :as q]))

;; Poisson Disc Sampling - Pure Clojure Implementation
;; Based on Bridson's algorithm

(defn poisson-disc-sample
  "Returns a seq of points sampled from a Poisson disc set. The points have
   a minimum distance specified by the `spacing` parameter."
  [spacing max-attempts left-x right-x top-y bot-y seed]
  (q/random-seed seed)
  (let [cell-size (/ spacing (Math/sqrt 2))
        grid-width (int (Math/ceil (/ (- right-x left-x) cell-size)))
        grid-height (int (Math/ceil (/ (- top-y bot-y) cell-size)))
        grid (atom {})
        active (atom [])
        points (atom [])

        grid-coords (fn [x y]
                     [(int (/ (- x left-x) cell-size))
                      (int (/ (- y top-y) cell-size))])

        in-neighborhood? (fn [x y]
                          (let [[gx gy] (grid-coords x y)]
                            (some (fn [dx]
                                    (some (fn [dy]
                                            (when-let [p (get @grid [(+ gx dx) (+ gy dy)])]
                                              (let [[px py] p
                                                    dist (q/dist x y px py)]
                                                (< dist spacing))))
                                          (range -2 3)))
                                  (range -2 3))))

        add-point (fn [x y]
                   (let [p [x y]
                         gc (grid-coords x y)]
                     (swap! grid assoc gc p)
                     (swap! active conj p)
                     (swap! points conj p)))

        ;; Start with random point
        initial-x (q/random left-x right-x)
        initial-y (q/random top-y bot-y)]

    (add-point initial-x initial-y)

    (while (seq @active)
      (let [idx (int (q/random (count @active)))
            [px py] (nth @active idx)
            found-point? (atom false)]

        (dotimes [_ max-attempts]
          (when-not @found-point?
            (let [angle (q/random 0 q/TWO-PI)
                  radius (q/random spacing (* 2 spacing))
                  new-x (+ px (* radius (q/cos angle)))
                  new-y (+ py (* radius (q/sin angle)))]
              (when (and (>= new-x left-x) (< new-x right-x)
                        (>= new-y top-y) (< new-y bot-y)
                        (not (in-neighborhood? new-x new-y)))
                (add-point new-x new-y)
                (reset! found-point? true)))))

        (when-not @found-point?
          (swap! active
                 (fn [a] (vec (concat (subvec a 0 idx)
                                     (subvec a (inc idx)))))))))

    @points))

;; Chaikin Curve - Pure Clojure Implementation

(defn interpolate [a b t]
  (+ a (* t (- b a))))

(defn chaikin-step [points tightness]
  (mapcat (fn [[[x1 y1] [x2 y2]]]
            (let [qx (interpolate x1 x2 tightness)
                  qy (interpolate y1 y2 tightness)
                  rx (interpolate x1 x2 (- 1.0 tightness))
                  ry (interpolate y1 y2 (- 1.0 tightness))]
              [[qx qy] [rx ry]]))
          (partition 2 1 points)))

(defn chaikin-curve
  "Forms a Chaikin curve from a seq of points, returning a new seq of points.
   The tightness parameter controls how sharp the corners will be (0.0-0.5).
   The depth parameter controls how many recursive steps will occur."
  ([points] (chaikin-curve points 4))
  ([points depth] (chaikin-curve points depth 0.25))
  ([points depth tightness]
   (nth (iterate #(chaikin-step % tightness) points) depth)))
