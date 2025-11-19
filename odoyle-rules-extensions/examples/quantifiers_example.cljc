(ns odoyle-rules-extensions.examples.quantifiers-example
  "Examples demonstrating existential quantification for O'Doyle Rules."
  (:require [odoyle.rules :as o]
            [odoyle-rules-extensions.quantifiers :as quant]))

;; Example 1: Game Start Condition - All Players Ready

(def game-start-rules
  (o/ruleset
    {:all-players
     [:what
      [id ::player-type :player]
      [id ::ready ready]
      [id ::name name]]}

    {:start-game
     [:what
      [game ::type :game]
      [game ::status status]
      :when
      (= status :waiting)
      (quant/forall? session ::all-players :ready)
      (quant/exists-at-least? session ::all-players 2)
      :then
      (println "✓ All players ready! Starting game...")
      (o/insert! game ::status :starting)]}))

(defn example-1-game-start []
  (println "\n========== Example 1: Game Start Condition ==========")
  (-> (reduce o/add-rule (o/->session) game-start-rules)
      (o/insert ::game1 {::type :game ::status :waiting})
      (o/insert ::p1 {::player-type :player ::ready true ::name "Alice"})
      (o/insert ::p2 {::player-type :player ::ready true ::name "Bob"})
      (o/insert ::p3 {::player-type :player ::ready false ::name "Charlie"})
      o/fire-rules
      ((fn [session]
         (println "\nCharlie becomes ready...")
         session))
      (o/insert ::p3 ::ready true)
      o/fire-rules))

;; Example 2: Alert System - Majority Vote

(def voting-rules
  (o/ruleset
    {:voters
     [:what
      [id ::voter true]
      [id ::vote vote]
      [id ::name name]]}

    {:motion-passed
     [:what
      [motion ::type :motion]
      :when
      (quant/majority? session ::voters #(= (:vote %) :yes))
      :then
      (let [pct (quant/percentage-where session ::voters #(= (:vote %) :yes))]
        (println "✓ Motion passed with" (format "%.1f%%" pct) "approval")
        (o/insert! motion ::result :passed))]}))

(defn example-2-voting []
  (println "\n========== Example 2: Majority Voting ==========")
  (-> (reduce o/add-rule (o/->session) voting-rules)
      (o/insert ::motion1 {::type :motion})
      (o/insert ::v1 {::voter true ::vote :yes ::name "Alice"})
      (o/insert ::v2 {::voter true ::vote :yes ::name "Bob"})
      (o/insert ::v3 {::voter true ::vote :yes ::name "Charlie"})
      (o/insert ::v4 {::voter true ::vote :no ::name "David"})
      (o/insert ::v5 {::voter true ::vote :yes ::name "Eve"})
      o/fire-rules))

;; Example 3: Team Balance - Relative Quantifiers

(def team-balance-rules
  (o/ruleset
    {:team-members
     [:what
      [id ::team-member true]
      [id ::team team]
      [id ::role role]]}

    {:unbalanced-teams
     [:what
      [game ::type :game]
      :when
      (quant/exists-more-than? session ::team-members
        #(= (:team %) :red)
        #(= (:team %) :blue))
      :then
      (println "⚠ Teams are unbalanced!")
      (let [red-count (quant/count-where session ::team-members #(= (:team %) :red))
            blue-count (quant/count-where session ::team-members #(= (:team %) :blue))]
        (println "  Red team:" red-count "Blue team:" blue-count))
      (o/insert! game ::teams-balanced false)]}))

(defn example-3-team-balance []
  (println "\n========== Example 3: Team Balance ==========")
  (-> (reduce o/add-rule (o/->session) team-balance-rules)
      (o/insert ::game1 {::type :game})
      (o/insert ::p1 {::team-member true ::team :red ::role :tank})
      (o/insert ::p2 {::team-member true ::team :red ::role :healer})
      (o/insert ::p3 {::team-member true ::team :red ::role :damage})
      (o/insert ::p4 {::team-member true ::team :blue ::role :tank})
      o/fire-rules))

;; Example 4: Safety Check - No Enemies Nearby

(def safety-rules
  (o/ruleset
    {:nearby-enemies
     [:what
      [id ::type :enemy]
      [id ::position pos]]}

    {:safe-to-rest
     [:what
      [player ::type :player]
      [player ::wants-to-rest true]
      :when
      (quant/none? session ::nearby-enemies
        (fn [enemy]
          (let [enemy-pos (:position enemy)]
            (< (+ (abs (- (:x enemy-pos) 100))
                  (abs (- (:y enemy-pos) 100)))
               50))))
      :then
      (println "✓ Area is safe. Player can rest.")
      (o/insert! player ::can-rest true)]}))

(defn abs [x] (if (neg? x) (- x) x))

(defn example-4-safety-check []
  (println "\n========== Example 4: Safety Check ==========")
  (-> (reduce o/add-rule (o/->session) safety-rules)
      (o/insert ::player1 {::type :player ::wants-to-rest true})
      (o/insert ::enemy1 {::type :enemy ::position {:x 200 :y 200}})  ; Far away
      o/fire-rules))

;; Run all examples

(defn run-all-examples []
  (println "=" 60)
  (println "EXISTENTIAL QUANTIFICATION EXAMPLES")
  (println "=" 60)

  (example-1-game-start)
  (example-2-voting)
  (example-3-team-balance)
  (example-4-safety-check)

  (println "\n" (apply str (repeat 60 "=")))
  (println "All examples completed!")
  (println (apply str (repeat 60 "="))))

;; (run-all-examples)
