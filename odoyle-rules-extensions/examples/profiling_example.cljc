(ns odoyle-rules-extensions.examples.profiling-example
  (:require [odoyle.rules :as o]
            [odoyle-rules-extensions.profiling :as prof]))

(defn example-basic-profiling []
  (println "\n========== Profiling Example ==========")
  (let [session (-> (prof/enable-profiling (o/->session))
                    (reduce o/add-rule
                      (o/ruleset
                        {::calculate-bonus
                         [:what
                          [id ::score score]
                          [id ::level level]
                          :then
                          (o/insert! id ::bonus (* score level))]}))
                    (prof/insert ::p1 {::score 100 ::level 5})
                    (prof/insert ::p2 {::score 200 ::level 3})
                    prof/fire-rules)]
    (prof/print-report session)))

;; (example-basic-profiling)
