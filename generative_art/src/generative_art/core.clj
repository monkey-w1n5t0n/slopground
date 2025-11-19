(ns generative-art.core
  (:require [generative-art.pieces.flow-field :as flow-field]
            [generative-art.pieces.circle-packing :as circle-packing]
            [generative-art.pieces.mondrian :as mondrian]
            [generative-art.pieces.voronoi :as voronoi]
            [generative-art.pieces.phyllotaxis :as phyllotaxis]
            [generative-art.pieces.reaction-diffusion :as reaction-diffusion]
            [generative-art.pieces.strange-attractor :as strange-attractor]
            [generative-art.pieces.triangular-mesh :as triangular-mesh]
            [generative-art.pieces.polar-geometry :as polar-geometry]
            [generative-art.pieces.lsystem :as lsystem]
            [generative-art.pieces.grid-deformation :as grid-deformation]))

(def pieces
  {"flow-field" flow-field/run
   "circle-packing" circle-packing/run
   "mondrian" mondrian/run
   "voronoi" voronoi/run
   "phyllotaxis" phyllotaxis/run
   "reaction-diffusion" reaction-diffusion/run
   "strange-attractor" strange-attractor/run
   "triangular-mesh" triangular-mesh/run
   "polar-geometry" polar-geometry/run
   "lsystem" lsystem/run
   "grid-deformation" grid-deformation/run})

(defn list-pieces []
  (println "Available generative art pieces:")
  (doseq [[name _] (sort pieces)]
    (println (str "  - " name))))

(defn -main [& args]
  (if (< (count args) 2)
    (do
      (println "Usage: clojure -M:run <piece-name> <seed>")
      (println "")
      (list-pieces)
      (println "")
      (println "Example: clojure -M:run flow-field 12345"))
    (let [piece-name (first args)
          seed (Integer/parseInt (second args))
          piece-fn (get pieces piece-name)]
      (if piece-fn
        (do
          (println (str "Running " piece-name " with seed " seed))
          (piece-fn seed))
        (do
          (println (str "Unknown piece: " piece-name))
          (list-pieces))))))
