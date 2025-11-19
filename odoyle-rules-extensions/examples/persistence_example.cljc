(ns odoyle-rules-extensions.examples.persistence-example
  (:require [odoyle.rules :as o]
            [odoyle-rules-extensions.persistence :as persist]))

(defn example-save-game []
  (println "\n========== Save/Load Example ==========")
  (let [rules (o/ruleset
                {::player-stats
                 [:what
                  [id ::health health]
                  [id ::level level]]})
        
        ;; Create game state
        session (-> (reduce o/add-rule (o/->session) rules)
                    (o/insert ::player {::health 100 ::level 5 ::gold 1000}))
        
        ;; Save game
        saved (persist/save session)]
    
    (println "Saved game state:" saved)
    
    ;; Load game
    (let [loaded (persist/load saved rules)]
      (println "Loaded successfully!")
      (println "Player health:" (some #(when (and (= (first %) ::player)
                                                   (= (second %) ::health))
                                         (nth % 2))
                                       (o/query-all loaded))))))

;; (example-save-game)
