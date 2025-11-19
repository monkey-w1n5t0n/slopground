(ns odoyle-rules-extensions.examples.collections-example
  "Examples demonstrating collection pattern matching for O'Doyle Rules."
  (:require [odoyle.rules :as o]
            [odoyle-rules-extensions.collections :as coll]))

;; Example 1: RPG Inventory and Equipment System

(def inventory-rules
  (o/ruleset
    {:check-quest-requirements
     [:what
      [player ::name name]
      [player ::inventory inv]
      [player ::level level]
      :when
      (coll/contains-all? inv [:ancient-key :magic-scroll :healing-potion])
      (>= level 10)
      :then
      (println "✓" name "meets quest requirements!")
      (o/insert! player ::quest-ready true)]}

    {:check-crafting-materials
     [:what
      [player ::name name]
      [player ::materials mats]
      :when
      (let [required [:iron-ore :crystal-shard :enchanted-wood]]
        (and (coll/contains-all? mats required)
             (coll/count-gte? mats 10)))
      :then
      (println "✓" name "can craft legendary sword!")
      (o/insert! player ::can-craft-legendary true)]}

    {:detect-cursed-items
     [:what
      [player ::name name]
      [player ::inventory inv]
      :when
      (coll/contains-any? inv [:cursed-ring :poisoned-dagger :dark-amulet])
      :then
      (println "⚠" name "is carrying cursed items!")
      (o/insert! player ::status :cursed)]}))

(defn example-1-inventory []
  (println "\n========== Example 1: RPG Inventory System ==========")
  (-> (reduce o/add-rule (o/->session) inventory-rules)
      (o/insert ::player1 {::name "Gandalf"
                           ::level 15
                           ::inventory [:ancient-key :magic-scroll :healing-potion :staff]
                           ::materials [:iron-ore :crystal-shard :enchanted-wood
                                       :silver :gold :mithril :ruby :sapphire
                                       :dragon-scale :phoenix-feather :moonstone]})
      (o/insert ::player2 {::name "Frodo"
                           ::level 5
                           ::inventory [:ancient-key :bread :rope]
                           ::materials []})
      (o/insert ::player3 {::name "Saruman"
                           ::level 20
                           ::inventory [:staff :cursed-ring :spellbook]
                           ::materials []})
      o/fire-rules))

;; Example 2: Permission and Role Management

(def permission-rules
  (o/ruleset
    {:admin-access
     [:what
      [user ::username username]
      [user ::roles roles]
      :when
      (coll/has-capability? roles [:admin])
      :then
      (println "🔐" username "has full admin access")
      (o/insert! user ::access-level :full)]}

    {:moderator-access
     [:what
      [user ::username username]
      [user ::roles roles]
      [user ::access-level level]
      :when
      (coll/has-any? roles #{:moderator :admin})
      (nil? level)
      :then
      (println "🔐" username "has moderator access")
      (o/insert! user ::access-level :moderate)]}

    {:check-required-permissions
     [:what
      [user ::username username]
      [user ::permissions perms]
      :when
      (coll/has-all? perms #{:read :write :delete})
      :then
      (println "✓" username "has all CRUD permissions")
      (o/insert! user ::can-manage-content true)]}

    {:flag-dangerous-permissions
     [:what
      [user ::username username]
      [user ::permissions perms]
      :when
      (coll/has-any? perms #{:delete-users :modify-system :access-logs})
      (not (coll/has-capability? (or (:roles (o/query-all session)) #{}) [:admin]))
      :then
      (println "⚠" username "has dangerous permissions without admin role!")
      (o/insert! user ::security-risk true)]}))

(defn example-2-permissions []
  (println "\n========== Example 2: Permission Management ==========")
  (-> (reduce o/add-rule (o/->session) permission-rules)
      (o/insert ::user1 {::username "alice"
                         ::roles #{:admin :developer}
                         ::permissions #{:read :write :delete :deploy}})
      (o/insert ::user2 {::username "bob"
                         ::roles #{:moderator :support}
                         ::permissions #{:read :write :ban-users}})
      (o/insert ::user3 {::username "charlie"
                         ::roles #{:developer}
                         ::permissions #{:read :write :delete :delete-users}})
      o/fire-rules))

;; Example 3: Configuration Validation

(def config-rules
  (o/ruleset
    {:validate-database-config
     [:what
      [app ::name name]
      [app ::config cfg]
      :when
      (coll/has-keys? cfg [:database :cache :logging])
      (coll/get-in? cfg [:database :port] #(and (> % 1000) (< % 65536)))
      (coll/get-in? cfg [:database :pool-size] #(>= % 5))
      :then
      (println "✓" name "database config is valid")
      (o/insert! app ::db-config-valid true)]}

    {:warn-missing-config
     [:what
      [app ::name name]
      [app ::config cfg]
      :when
      (let [missing (coll/missing-requirements?
                      (keys cfg)
                      [:database :cache :logging :monitoring])]
        (seq missing))
      :then
      (let [missing (coll/missing-requirements?
                      (keys (::config (first (o/query-all session))))
                      [:database :cache :logging :monitoring])]
        (println "⚠" name "missing config sections:" missing)
        (o/insert! app ::config-incomplete missing))]}

    {:check-feature-flags
     [:what
      [app ::name name]
      [app ::features features]
      :when
      (coll/intersects? features #{:experimental-ai :beta-features})
      :then
      (println "🧪" name "has experimental features enabled")
      (o/insert! app ::experimental-mode true)]}))

(defn example-3-configuration []
  (println "\n========== Example 3: Configuration Validation ==========")
  (-> (reduce o/add-rule (o/->session) config-rules)
      (o/insert ::app1 {::name "Production App"
                        ::config {:database {:host "localhost"
                                            :port 5432
                                            :pool-size 10}
                                 :cache {:ttl 300}
                                 :logging {:level :info}}
                        ::features #{:caching :logging}})
      (o/insert ::app2 {::name "Dev App"
                        ::config {:database {:host "localhost"
                                            :port 5433
                                            :pool-size 3}
                                 :cache {:ttl 60}}
                        ::features #{:experimental-ai :debugging}})
      o/fire-rules))

;; Example 4: Team Composition and Balance

(def team-rules
  (o/ruleset
    {:balanced-team
     [:what
      [team ::name name]
      [team ::members members]
      [team ::roles roles]
      :when
      (coll/count-gte? members 3)
      (coll/count-lte? members 6)
      (coll/has-all? roles #{:tank :healer :damage})
      :then
      (println "✓" name "has a balanced team composition")
      (o/insert! team ::balanced true)]}

    {:understaffed-team
     [:what
      [team ::name name]
      [team ::members members]
      :when
      (coll/count-lt? members 3)
      :then
      (println "⚠" name "is understaffed (only" (count members) "members)")
      (o/insert! team ::status :understaffed)]}

    {:duplicate-roles-warning
     [:what
      [team ::name name]
      [team ::roles roles]
      :when
      (not (coll/distinct-values? roles))
      :then
      (println "⚠" name "has duplicate roles")
      (o/insert! team ::has-duplicates true)]}))

(defn example-4-team-composition []
  (println "\n========== Example 4: Team Composition ==========")
  (-> (reduce o/add-rule (o/->session) team-rules)
      (o/insert ::team1 {::name "Alpha Squad"
                         ::members [:alice :bob :charlie :david]
                         ::roles [:tank :healer :damage :damage]})
      (o/insert ::team2 {::name "Beta Team"
                         ::members [:eve :frank]
                         ::roles [:tank :healer]})
      (o/insert ::team3 {::name "Gamma Force"
                         ::members [:grace :henry :irene :jack :kelly]
                         ::roles [:tank :healer :damage :support :damage]})
      o/fire-rules))

;; Example 5: Sequence and Position Checks

(def sequence-rules
  (o/ruleset
    {:check-action-sequence
     [:what
      [player ::name name]
      [player ::recent-actions actions]
      :when
      (coll/nth? actions 0 :attack)
      (coll/nth? actions 1 :defend)
      (coll/nth? actions 2 :attack)
      (coll/count-eq? actions 3)
      :then
      (println "🎯" name "executed attack-defend-attack combo!")
      (o/insert! player ::combo-bonus 1.5)]}

    {:check-resource-depletion
     [:what
      [player ::name name]
      [player ::resource-history history]
      :when
      (coll/last? history #(< % 10))
      (coll/count-gte? history 5)
      :then
      (println "⚠" name "resources are critically low!")
      (o/insert! player ::resource-warning true)]}

    {:achievement-first-completion
     [:what
      [player ::name name]
      [player ::achievements achievements]
      :when
      (coll/first? achievements :tutorial-complete)
      (coll/count-eq? achievements 1)
      :then
      (println "🎉" name "completed their first achievement!")
      (o/insert! player ::newbie-bonus 100)]}))

(defn example-5-sequences []
  (println "\n========== Example 5: Sequence and Position Checks ==========")
  (-> (reduce o/add-rule (o/->session) sequence-rules)
      (o/insert ::p1 {::name "Alice"
                      ::recent-actions [:attack :defend :attack]})
      (o/insert ::p2 {::name "Bob"
                      ::resource-history [100 80 50 30 8]})
      (o/insert ::p3 {::name "Charlie"
                      ::achievements [:tutorial-complete]})
      o/fire-rules))

;; Example 6: Complex Nested Data

(def nested-data-rules
  (o/ruleset
    {:validate-player-state
     [:what
      [player ::name name]
      [player ::state state]
      :when
      (coll/get-in? state [:position :x] #(> % 100))
      (coll/get-in? state [:stats :health] #(> % 0))
      (coll/get-in? state [:inventory :equipped :weapon])
      :then
      (println "✓" name "is in valid combat state")
      (o/insert! player ::combat-ready true)]}

    {:check-environment-hazards
     [:what
      [zone ::name name]
      [zone ::environment env]
      :when
      (coll/get-in? env [:hazards] #(coll/has-any? % [:fire :poison :radiation]))
      (coll/get-in? env [:protection-required] true)
      :then
      (println "☢" name "zone requires special protection!")
      (o/insert! zone ::hazard-level :high)]}))

(defn example-6-nested-data []
  (println "\n========== Example 6: Nested Data Structures ==========")
  (-> (reduce o/add-rule (o/->session) nested-data-rules)
      (o/insert ::player1 {::name "Warrior"
                           ::state {:position {:x 150 :y 200}
                                   :stats {:health 100 :mana 50}
                                   :inventory {:equipped {:weapon :sword
                                                         :armor :platemail}}}})
      (o/insert ::zone1 {::name "Toxic Swamp"
                         ::environment {:hazards [:poison :quicksand]
                                       :protection-required true
                                       :difficulty :hard}})
      o/fire-rules))

;; Run all examples

(defn run-all-examples []
  (println "=" 60)
  (println "COLLECTION PATTERN MATCHING EXAMPLES")
  (println "=" 60)

  (example-1-inventory)
  (example-2-permissions)
  (example-3-configuration)
  (example-4-team-composition)
  (example-5-sequences)
  (example-6-nested-data)

  (println "\n" (apply str (repeat 60 "=")))
  (println "All examples completed!")
  (println (apply str (repeat 60 "="))))

;; Uncomment to run when loaded
;; (run-all-examples)
