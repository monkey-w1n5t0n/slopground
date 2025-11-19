(ns odoyle-rules-extensions.examples.priorities-example
  "Examples demonstrating rule priorities for O'Doyle Rules.

  Priorities solve the problem of controlling rule execution order,
  which is essential for implementing business logic hierarchies."
  (:require [odoyle.rules :as o]
            [odoyle-rules-extensions.priorities :as prio]))

;; Example 1: Exception Handling Hierarchy
;; Specific exception handlers run before generic handlers

(defn example-1-exception-hierarchy []
  (println "\n========== Example 1: Exception Hierarchy ==========")
  (let [*handled (atom [])]
    (-> (prio/enable-priorities (o/->session))
        ;; Highest priority: Handle critical errors first
        (prio/add-rule
          (prio/critical-priority
            (o/->rule ::critical-error-handler
              {:what [['id ::error-type 'type]
                      ['id ::error-severity 'severity]]
               :when (fn [session match] (= (:severity match) :critical))
               :then (fn [session match]
                       (swap! *handled conj [:critical (:type match)])
                       (println "🚨 CRITICAL ERROR:" (:type match) "- Immediate action required!")
                       (o/insert! (:id match) ::handled true))})))
        ;; High priority: Handle specific error types
        (prio/add-rule
          (prio/high-priority
            (o/->rule ::database-error-handler
              {:what [['id ::error-type :database-connection]
                      ['id ::handled 'handled]]
               :when (fn [session match] (not (:handled match)))
               :then (fn [session match]
                       (swap! *handled conj [:database-specific])
                       (println "💾 Database error - Attempting reconnection...")
                       (o/insert! (:id match) ::handled true))})))
        ;; Normal priority: Generic error handler
        (prio/add-rule
          (prio/normal-priority
            (o/->rule ::generic-error-handler
              {:what [['id ::error-type 'type]
                      ['id ::handled 'handled]]
               :when (fn [session match] (not (:handled match)))
               :then (fn [session match]
                       (swap! *handled conj [:generic (:type match)])
                       (println "⚠️  Generic error handler:" (:type match))
                       (o/insert! (:id match) ::handled true))})))
        ;; Low priority: Logging (runs after handling)
        (prio/add-rule
          (prio/low-priority
            (o/->rule ::error-logger
              {:what [['id ::error-type 'type]
                      ['id ::handled 'handled]]
               :when (fn [session match] (:handled match))
               :then (fn [session match]
                       (swap! *handled conj [:logged (:type match)])
                       (println "📝 Logged error:" (:type match)))})))
        ;; Insert various errors
        (o/insert ::error1 {::error-type :database-connection
                            ::error-severity :critical
                            ::handled false})
        (o/insert ::error2 {::error-type :network-timeout
                            ::error-severity :warning
                            ::handled false})
        (o/insert ::error3 {::error-type :validation-failed
                            ::error-severity :info
                            ::handled false})
        prio/fire-rules)
    (println "\nExecution order:" @*handled)))

;; Example 2: Discount Application System
;; Apply discounts in priority order (best discount wins)

(defn example-2-discount-system []
  (println "\n========== Example 2: Discount System ==========")
  (-> (prio/enable-priorities (o/->session))
      ;; Highest: VIP customer discount
      (prio/add-rule
        (prio/with-priority 1000
          (o/->rule ::vip-discount
            {:what [['id ::customer-type :vip]
                    ['id ::purchase-amount 'amount]]
             :then (fn [session match]
                     (let [discount (* 0.30 (:amount match))]
                       (println "🌟 VIP Discount: 30% off - Save $" discount)
                       (o/insert! (:id match) ::discount discount)))})))
      ;; High: Holiday discount
      (prio/add-rule
        (prio/with-priority 500
          (o/->rule ::holiday-discount
            {:what [['id ::purchase-amount 'amount]
                    ['id ::discount 'existing-discount]]
             :when (fn [session match]
                     ;; Only if no better discount applied
                     (or (nil? (:existing-discount match))
                         (< (:existing-discount match) (* 0.20 (:amount match)))))
             :then (fn [session match]
                     (let [discount (* 0.20 (:amount match))]
                       (println "🎄 Holiday Discount: 20% off - Save $" discount)
                       (o/insert! (:id match) ::discount discount)))})))
      ;; Normal: First-time customer discount
      (prio/add-rule
        (prio/with-priority 100
          (o/->rule ::first-time-discount
            {:what [['id ::first-time-customer 'first]
                    ['id ::purchase-amount 'amount]
                    ['id ::discount 'existing-discount]]
             :when (fn [session match]
                     (and (:first match)
                          (or (nil? (:existing-discount match))
                              (< (:existing-discount match) (* 0.10 (:amount match))))))
             :then (fn [session match]
                     (let [discount (* 0.10 (:amount match))]
                       (println "👋 Welcome Discount: 10% off - Save $" discount)
                       (o/insert! (:id match) ::discount discount)))})))
      ;; Low: Calculate final price (after all discounts)
      (prio/add-rule
        (prio/very-low-priority
          (o/->rule ::calculate-final-price
            {:what [['id ::purchase-amount 'amount]
                    ['id ::discount 'discount]]
             :then (fn [session match]
                     (let [final-price (- (:amount match) (or (:discount match) 0))]
                       (println "💰 Final Price: $" final-price)
                       (o/insert! (:id match) ::final-price final-price)))})))
      ;; Test different customers
      (o/insert ::customer1 {::customer-type :vip
                             ::purchase-amount 100.0
                             ::first-time-customer false})
      prio/fire-rules
      ((fn [session]
         (println "\n--- Next Customer ---")
         session))
      (o/insert ::customer2 {::customer-type :regular
                             ::purchase-amount 100.0
                             ::first-time-customer true})
      prio/fire-rules))

;; Example 3: Game Combat System
;; Process combat actions in priority order

(defn example-3-combat-system []
  (println "\n========== Example 3: Combat System ==========")
  (-> (prio/enable-priorities (o/->session))
      ;; Critical: Death/resurrection
      (prio/add-rule
        (prio/with-priority 1000
          (o/->rule ::check-death
            {:what [['id ::health 'health]
                    ['id ::name 'name]]
             :when (fn [session match] (<= (:health match) 0))
             :then (fn [session match]
                     (println "💀" (:name match) "has been defeated!")
                     (o/insert! (:id match) ::state :dead))})))
      ;; High: Healing effects (before damage)
      (prio/add-rule
        (prio/with-priority 500
          (o/->rule ::apply-healing
            {:what [['id ::pending-heal 'heal-amount]
                    ['id ::health 'current-health]
                    ['id ::name 'name]]
             :then (fn [session match]
                     (let [new-health (+ (:current-health match) (:heal-amount match))]
                       (println "💚" (:name match) "healed for" (:heal-amount match)
                               "HP →" new-health "HP")
                       (o/insert! (:id match) ::health new-health)
                       (o/retract! (:id match) ::pending-heal)))})))
      ;; Normal: Damage effects
      (prio/add-rule
        (prio/with-priority 100
          (o/->rule ::apply-damage
            {:what [['id ::pending-damage 'damage-amount]
                    ['id ::health 'current-health]
                    ['id ::name 'name]
                    ['id ::state 'state]]
             :when (fn [session match] (not= (:state match) :dead))
             :then (fn [session match]
                     (let [new-health (- (:current-health match) (:damage-amount match))]
                       (println "⚔️ " (:name match) "takes" (:damage-amount match)
                               "damage →" new-health "HP")
                       (o/insert! (:id match) ::health new-health)
                       (o/retract! (:id match) ::pending-damage)))})))
      ;; Low: Status effects
      (prio/add-rule
        (prio/with-priority 10
          (o/->rule ::poison-damage
            {:what [['id ::poisoned 'is-poisoned]
                    ['id ::health 'health]
                    ['id ::name 'name]
                    ['id ::state 'state]]
             :when (fn [session match]
                     (and (:is-poisoned match)
                          (not= (:state match) :dead)))
             :then (fn [session match]
                     (println "🧪" (:name match) "suffers poison damage")
                     (o/insert! (:id match) ::pending-damage 5))})))
      ;; Setup combatants
      (o/insert ::hero {::name "Hero" ::health 50 ::state :alive})
      (o/insert ::enemy {::name "Enemy" ::health 30 ::state :alive ::poisoned true})
      ;; Hero attacks enemy
      (o/insert ::enemy ::pending-damage 25)
      ;; Enemy attacks hero
      (o/insert ::hero ::pending-damage 15)
      ;; Hero uses healing potion
      (o/insert ::hero ::pending-heal 20)
      prio/fire-rules))

;; Example 4: Data Validation Pipeline
;; Validate data in priority order (fail fast)

(defn example-4-validation-pipeline []
  (println "\n========== Example 4: Validation Pipeline ==========")
  (-> (prio/enable-priorities (o/->session))
      ;; Highest: Check required fields
      (prio/add-rule
        (prio/with-priority 1000
          (o/->rule ::validate-required-fields
            {:what [['id ::validation-type :user-registration]
                    ['id ::data 'data]]
             :then (fn [session match]
                     (let [required [:email :password :username]
                           data (:data match)
                           missing (filter #(nil? (get data %)) required)]
                       (if (seq missing)
                         (do
                           (println "❌ Missing required fields:" missing)
                           (o/insert! (:id match) ::validation-result :failed))
                         (do
                           (println "✓ All required fields present")
                           (o/insert! (:id match) ::has-required true)))))})))
      ;; High: Format validation
      (prio/add-rule
        (prio/with-priority 500
          (o/->rule ::validate-email-format
            {:what [['id ::has-required true]
                    ['id ::data 'data]]
             :when (fn [session match]
                     (not (o/contains? session (:id match) ::validation-result)))
             :then (fn [session match]
                     (let [email (get-in match [:data :email])]
                       (if (re-matches #".+@.+\..+" (str email))
                         (println "✓ Email format valid")
                         (do
                           (println "❌ Invalid email format")
                           (o/insert! (:id match) ::validation-result :failed)))))})))
      ;; Normal: Business rules
      (prio/add-rule
        (prio/with-priority 100
          (o/->rule ::validate-password-strength
            {:what [['id ::has-required true]
                    ['id ::data 'data]]
             :when (fn [session match]
                     (not (o/contains? session (:id match) ::validation-result)))
             :then (fn [session match]
                     (let [password (get-in match [:data :password])]
                       (if (>= (count (str password)) 8)
                         (println "✓ Password strength acceptable")
                         (do
                           (println "❌ Password too weak")
                           (o/insert! (:id match) ::validation-result :failed)))))})))
      ;; Low: Success handler
      (prio/add-rule
        (prio/very-low-priority
          (o/->rule ::validation-success
            {:what [['id ::has-required true]
                    ['id ::validation-result 'result]]
             :when (fn [session match]
                     (not (o/contains? session (:id match) ::validation-result)))
             :then (fn [session match]
                     (println "✅ All validations passed!")
                     (o/insert! (:id match) ::validation-result :success))})))
      ;; Test valid data
      (o/insert ::registration1 {::validation-type :user-registration
                                  ::data {:email "user@example.com"
                                         :password "securepass123"
                                         :username "johndoe"}})
      prio/fire-rules
      ((fn [session]
         (println "\n--- Testing invalid data ---")
         session))
      ;; Test invalid data (missing email)
      (o/insert ::registration2 {::validation-type :user-registration
                                  ::data {:password "pass"
                                         :username "janedoe"}})
      prio/fire-rules))

;; Example 5: Explaining Execution Order

(defn example-5-explain-order []
  (println "\n========== Example 5: Explaining Execution Order ==========")
  (let [session (-> (prio/enable-priorities (o/->session))
                    (prio/add-rule
                      (prio/with-priority 1000
                        (o/->rule ::critical-check
                          {:what [['id ::status 'status]]})))
                    (prio/add-rule
                      (prio/with-priority 500
                        (o/->rule ::important-check
                          {:what [['id ::status 'status]]})))
                    (prio/add-rule
                      (prio/with-priority 100
                        (o/->rule ::normal-check
                          {:what [['id ::status 'status]]})))
                    (prio/add-rule
                      (prio/with-priority 10
                        (o/->rule ::cleanup
                          {:what [['id ::status 'status]]})))
                    (o/insert ::test ::status :active))]
    (println "\nRegistered priorities:")
    (doseq [[rule-name priority] (prio/list-priorities session)]
      (println (format "  %-20s priority: %4d" (name rule-name) priority)))
    (println "\nExecution order (when all rules match):")
    (doseq [{:keys [rule-name priority]} (prio/explain-execution-order session)]
      (println (format "  %2d. %-20s (priority: %4d)"
                      (inc (.indexOf (prio/explain-execution-order session)
                                   {:rule-name rule-name :priority priority}))
                      (name rule-name)
                      priority)))))

;; Run all examples

(defn run-all-examples []
  (println "=" 60)
  (println "RULE PRIORITIES EXAMPLES")
  (println "=" 60)

  (example-1-exception-hierarchy)
  (example-2-discount-system)
  (example-3-combat-system)
  (example-4-validation-pipeline)
  (example-5-explain-order)

  (println "\n" (apply str (repeat 60 "=")))
  (println "All examples completed!")
  (println (apply str (repeat 60 "="))))

;; Uncomment to run when loaded
;; (run-all-examples)
