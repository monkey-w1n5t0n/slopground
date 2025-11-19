(ns odoyle-rules-extensions.examples.character-example
  "Example demonstrating negative fact matching for a character/game system.

  This example shows how to detect missing attributes and incomplete entities."
  (:require [odoyle.rules :as o]
            [odoyle-rules-extensions.negative-facts :as nf]))

;; Example 1: Basic character with missing attributes
;; This matches your original request: detect when x and y exist but z doesn't

(def basic-rules
  (o/ruleset
    {:character
     [:what
      [id ::x x]
      [id ::y y]
      :when
      ;; Only match if ::z is NOT defined
      (nf/not-defined? session id ::z)
      :then
      (println "Character" id "has coordinates (" x "," y ") but no ::z!")
      (o/insert! id ::incomplete true)]}))

(defn example-1-basic []
  (println "\n=== Example 1: Basic Negative Matching ===")
  (-> (reduce o/add-rule (o/->session) basic-rules)
      (o/insert ::char1 ::x 10)
      (o/insert ::char1 ::y 20)
      ;; char1 has x and y, but not z - should match

      (o/insert ::char2 ::x 30)
      (o/insert ::char2 ::y 40)
      (o/insert ::char2 ::z 50)
      ;; char2 has x, y, AND z - should NOT match

      o/fire-rules))

;; Example 2: Game character system with equipment checking

(def equipment-rules
  (o/ruleset
    {:unarmed-character
     [:what
      [char ::name name]
      [char ::type :player]
      :when
      (nf/not-defined? session char ::weapon)
      :then
      (println name "is unarmed!")
      (o/insert! char ::damage-multiplier 0.5)]}

    {:unarmored-character
     [:what
      [char ::name name]
      [char ::type :player]
      :when
      (nf/all-not-defined? session char [::helmet ::chestplate ::boots])
      :then
      (println name "has no armor!")
      (o/insert! char ::defense-multiplier 0.3)]}

    {:well-equipped
     [:what
      [char ::name name]
      [char ::weapon weapon]
      [char ::helmet helmet]
      :then
      (println name "is well-equipped with" weapon "and" helmet)]}))

(defn example-2-equipment []
  (println "\n=== Example 2: Equipment System ===")
  (-> (reduce o/add-rule (o/->session) equipment-rules)
      ;; Create a fully equipped character
      (o/insert ::player1 {::name "Alice"
                           ::type :player
                           ::weapon :sword
                           ::helmet :iron-helm
                           ::chestplate :chainmail
                           ::boots :leather-boots})

      ;; Create an unarmed character
      (o/insert ::player2 {::name "Bob"
                           ::type :player
                           ::helmet :bronze-helm})

      ;; Create a completely unequipped character
      (o/insert ::player3 {::name "Charlie"
                           ::type :player})

      o/fire-rules))

;; Example 3: Progressive tutorial system

(def tutorial-rules
  (o/ruleset
    {:new-player-tutorial
     [:what
      [player ::name name]
      [player ::type :player]
      :when
      ;; Brand new player - has no progress indicators
      (nf/all-not-defined? session player
        [::completed-tutorial ::level ::experience])
      :then
      (println "Showing tutorial to new player:" name)
      (o/insert! player ::show-tutorial :basics)]}

    {:movement-tutorial
     [:what
      [player ::name name]
      [player ::completed-tutorial :basics]
      :when
      (nf/not-defined? session player ::first-movement)
      :then
      (println "Showing movement tutorial to" name)
      (o/insert! player ::show-tutorial :movement)]}

    {:combat-tutorial
     [:what
      [player ::name name]
      [player ::first-movement true]
      :when
      (nf/not-defined? session player ::first-combat)
      :then
      (println "Showing combat tutorial to" name)
      (o/insert! player ::show-tutorial :combat)]}))

(defn example-3-tutorial []
  (println "\n=== Example 3: Progressive Tutorial System ===")
  (-> (reduce o/add-rule (o/->session) tutorial-rules)
      ;; Brand new player
      (o/insert ::player1 {::name "David"
                           ::type :player})
      o/fire-rules

      ;; Player who completed basics but hasn't moved yet
      ((fn [session]
         (println "\nPlayer completes basic tutorial...")
         session))
      (o/insert ::player1 ::completed-tutorial :basics)
      o/fire-rules

      ;; Player who has moved but not fought
      ((fn [session]
         (println "\nPlayer makes first movement...")
         session))
      (o/insert ::player1 ::first-movement true)
      o/fire-rules))

;; Example 4: Configuration with custom marker

(defn example-4-custom-marker []
  (println "\n=== Example 4: Custom Not-Defined Marker ===")

  ;; Set a custom marker for this application
  (nf/set-not-defined-marker! ::absent)

  (println "Current marker:" (nf/get-not-defined-marker))
  (println "Is ::absent the marker?" (nf/is-marker? ::absent))
  (println "Is :something-else the marker?" (nf/is-marker? :something-else))

  ;; Reset to default
  (nf/set-not-defined-marker! :odoyle/not-defined))

;; Example 5: Using negated patterns (more declarative)

(def negated-pattern-rules
  (let [no-health (nf/->negated-pattern 'entity ::health)
        no-position (nf/->negated-pattern 'entity ::position)]
    (o/ruleset
      {:needs-initialization
       [:what
        [entity ::id id]
        :when
        (or (nf/matches-negated? session match no-health)
            (nf/matches-negated? session match no-position))
        :then
        (println "Entity" id "needs initialization")
        ;; Initialize with defaults
        (when (nf/not-defined? session entity ::health)
          (o/insert! entity ::health 100))
        (when (nf/not-defined? session entity ::position)
          (o/insert! entity ::position [0 0]))]})))

(defn example-5-negated-patterns []
  (println "\n=== Example 5: Negated Patterns ===")
  (-> (reduce o/add-rule (o/->session) negated-pattern-rules)
      (o/insert ::entity1 {::id "entity-1"})
      (o/insert ::entity2 {::id "entity-2" ::health 50})
      (o/insert ::entity3 {::id "entity-3" ::position [10 20]})
      o/fire-rules))

;; Example 6: Dynamic fact checking (facts come and go)

(def dynamic-rules
  (o/ruleset
    {:check-status
     [:what
      [player ::name name]
      :when
      (nf/not-defined? session player ::status-effect)
      :then
      (println name "has no status effects - applying default health regen")
      (o/insert! player ::health-regen 1)]}))

(defn example-6-dynamic []
  (println "\n=== Example 6: Dynamic Fact Checking ===")
  (-> (reduce o/add-rule (o/->session) dynamic-rules)
      (o/insert ::player1 ::name "Eve")
      o/fire-rules

      ((fn [session]
         (println "\nApplying poison status effect...")
         session))
      (o/insert ::player1 ::status-effect :poisoned)
      (o/retract ::player1 ::health-regen)
      o/fire-rules

      ((fn [session]
         (println "\nRemoving poison status effect...")
         session))
      (o/retract ::player1 ::status-effect)
      o/fire-rules))

;; Run all examples

(defn run-all-examples []
  (println "========================================")
  (println "NEGATIVE FACT MATCHING EXAMPLES")
  (println "========================================")

  (example-1-basic)
  (example-2-equipment)
  (example-3-tutorial)
  (example-4-custom-marker)
  (example-5-negated-patterns)
  (example-6-dynamic)

  (println "\n========================================")
  (println "All examples completed!")
  (println "========================================"))

;; Uncomment to run when loaded
;; (run-all-examples)
