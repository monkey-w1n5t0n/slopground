(ns odoyle-rules-extensions.examples.modules-example
  "Examples demonstrating rule modules for O'Doyle Rules."
  (:require [odoyle.rules :as o]
            [odoyle-rules-extensions.modules :as mod]))

(def combat-rules
  (o/ruleset
    {:attack-damage
     [:what
      [attacker ::attacks target]
      [target ::health health]
      :then
      (println "⚔ Attack!" attacker "->" target)
      (o/insert! target ::health (- health 10))]}))

(def trading-rules
  (o/ruleset
    {:execute-trade
     [:what
      [trader ::wants-to-trade item]
      [trader ::gold gold]
      :when
      (>= gold 100)
      :then
      (println "💰 Trade executed:" item)
      (o/insert! trader ::gold (- gold 100))
      (o/insert! trader ::inventory item)]}))

(def debug-rules
  (o/ruleset
    {:log-all-facts
     [:what
      [id attr val]
      :then
      (println "🐛 DEBUG:" id attr val)]}))

(defn example-feature-toggles []
  (println "\n========== Example: Feature Toggles ==========")

  (let [session (-> (mod/enable-modules (o/->session))
                    (mod/register-module :combat combat-rules)
                    (mod/register-module :trading trading-rules)
                    (mod/register-module :debug debug-rules)
                    (mod/enable :combat)
                    (mod/enable :trading))]

    (println "\nCombat enabled, trading enabled:")
    (mod/print-status session)

    (-> session
        (o/insert ::player1 ::attacks ::enemy1)
        (o/insert ::enemy1 ::health 100)
        (o/insert ::player1 ::wants-to-trade :sword)
        (o/insert ::player1 ::gold 150)
        o/fire-rules)

    (println "\n\nEnabling debug mode:")
    (-> session
        (mod/enable :debug)
        mod/print-status
        (o/insert ::player2 ::test-fact :value)
        o/fire-rules)))

(defn example-game-modes []
  (println "\n\n========== Example: Game Modes ==========")

  (let [session (-> (mod/enable-modules (o/->session))
                    (mod/register-module :combat combat-rules)
                    (mod/register-module :trading trading-rules))]

    (println "\nPeaceful Mode (only trading):")
    (-> session
        (mod/enable-set [:trading])
        mod/print-status)

    (println "\nCombat Mode (only combat):")
    (-> session
        (mod/enable-set [:combat])
        mod/print-status)

    (println "\nFull Game Mode (both):")
    (-> session
        (mod/enable-set [:combat :trading])
        mod/print-status)))

(defn run-all-examples []
  (println "=" 60)
  (println "RULE MODULES EXAMPLES")
  (println "=" 60)
  (example-feature-toggles)
  (example-game-modes)
  (println "\n" (apply str (repeat 60 "="))))

;; (run-all-examples)
