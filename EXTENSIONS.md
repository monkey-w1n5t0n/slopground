# O'Doyle Rules Extensions

This document describes useful extensions built on top of the O'Doyle Rules library, including both implemented and proposed features.

## Table of Contents

1. [Implemented Extensions](#implemented-extensions)
2. [Proposed Extensions](#proposed-extensions)
3. [Implementation Guide](#implementation-guide)

---

## Implemented Extensions

### 1. Negative Fact Matching (Absence Detection)

**Status:** ✅ Implemented
**Location:** `odoyle-rules-extensions/negative_facts.cljc`

Allows rules to match when certain facts are NOT present in the session.

**Use Cases:**
- Detect missing attributes (e.g., incomplete profiles)
- Implement closed-world assumptions
- Match on absence of relationships
- Trigger rules when prerequisites are missing

**Example:**
```clojure
(require '[odoyle.rules :as o]
         '[odoyle-rules-extensions.negative-facts :as nf])

;; Rule that fires only when an entity has x and y, but NOT z
(def rules
  (o/ruleset
    {:incomplete-entity
     [:what
      [id ::x x]
      [id ::y y]
      :when
      (nf/not-defined? session id ::z)
      :then
      (println "Entity" id "is missing ::z!")]}))
```

**Features:**
- `not-defined?` - Check if a fact doesn't exist
- `all-not-defined?` - Check if multiple facts are all absent
- `any-not-defined?` - Check if at least one fact is absent
- `is-marker?` - Check if a value equals the not-defined marker
- Configurable marker value (default: `:odoyle/not-defined`)
- Negated pattern abstraction for cleaner code

**Configuration:**
```clojure
;; Set a custom marker
(nf/set-not-defined-marker! :my-app/absent)
```

---

## Proposed Extensions

### 2. Accumulator Functions

**Status:** 🔨 Proposed

Aggregate values across multiple matches (count, sum, average, min, max, etc.).

**Use Cases:**
- Count entities matching criteria
- Calculate totals, averages
- Find min/max values
- Collect values into data structures

**Proposed API:**
```clojure
(o/ruleset
  {:total-health
   [:what
    [id ::type :player]
    [id ::health health]
    :then-finally
    (let [total (accumulate session ::total-health
                  (acc/sum :health))]
      (o/insert! ::game ::total-player-health total))]})
```

**Accumulator Types:**
- `(acc/count)` - Count matches
- `(acc/sum key)` - Sum values
- `(acc/avg key)` - Average values
- `(acc/min key)` / `(acc/max key)` - Min/max values
- `(acc/collect key)` - Collect into vector
- `(acc/group-by key-fn value-fn)` - Group by key
- Custom accumulators with `(acc/custom init-fn reduce-fn)`

### 3. Temporal Reasoning / Time-Based Rules

**Status:** 🔨 Proposed

Match patterns based on time windows, event sequences, and temporal relationships.

**Use Cases:**
- Detect events within time windows
- Match event sequences (A followed by B within 5 seconds)
- Implement timeouts and expirations
- Sliding window aggregations

**Proposed API:**
```clojure
(o/ruleset
  {:rapid-fire
   [:what
    [id ::shot-fired time]
    :when
    (temporal/within-window? session id ::shot-fired
      {:duration 5000  ; 5 seconds
       :min-count 3})  ; at least 3 shots
    :then
    (o/insert! id ::rapid-fire true)]

  {:attack-combo
   [:what
    [player ::action action]
    :when
    (temporal/sequence? session player ::action
      [:punch :kick :uppercut]
      {:max-gap 2000})  ; each within 2 seconds
    :then
    (o/insert! player ::combo-performed :triple-strike)]})
```

**Features:**
- Time windows with sliding/tumbling support
- Event sequence matching
- Timeout detection
- Temporal joins (events within time delta)
- Clock abstraction (real-time, logical, simulation)

### 4. Rule Priorities / Salience

**Status:** 🔨 Proposed

Control the order in which rules fire when multiple rules are triggered.

**Use Cases:**
- Implement decision hierarchies
- Override general rules with specific ones
- Ensure critical rules fire first
- Control rule execution order deterministically

**Proposed API:**
```clojure
(o/ruleset
  {:critical-rule
   [:priority 100  ; Higher numbers = higher priority
    :what
    [id ::health health]
    :when
    (<= health 0)
    :then
    (o/insert! id ::state :dead)]

  {:normal-rule
   [:priority 10
    :what
    [id ::health health]
    :when
    (< health 20)
    :then
    (o/insert! id ::status :critical)]})
```

### 5. Logical Retraction / Truth Maintenance

**Status:** 🔨 Proposed

Automatically retract facts when their supporting facts are removed (dependency tracking).

**Use Cases:**
- Maintain derived facts automatically
- Implement logical dependencies
- Clean up inferred data automatically
- Prevent orphaned facts

**Proposed API:**
```clojure
(o/ruleset
  {:derive-speed
   [:what
    [id ::distance distance]
    [id ::time time]
    :then
    (o/insert-logical! id ::speed (/ distance time))]})

;; When ::distance or ::time is retracted, ::speed is automatically retracted
```

**Features:**
- `insert-logical!` - Insert facts with automatic retraction
- Justification tracking
- Multiple justifications (fact persists while any justification exists)
- Transitive retraction

### 6. Pattern Matching on Collections

**Status:** 🔨 Proposed

Match patterns within collection values (vectors, sets, maps).

**Use Cases:**
- Match on vector elements
- Check set membership
- Query nested data structures
- Pattern match on sequences

**Proposed API:**
```clojure
(o/ruleset
  {:has-skill
   [:what
    [player ::skills skills]
    :when
    (coll/contains? skills :fire-magic)
    :then
    (o/insert! player ::can-use-fire true)]

  {:inventory-check
   [:what
    [player ::inventory items]
    :when
    (coll/has-all? items [:sword :shield :potion])
    :then
    (o/insert! player ::equipment-ready true)]

  {:find-in-nested
   [:what
    [game ::state state]
    :when
    (coll/get-in? state [:players player-id :health] health)
    (< health 50)
    :then
    (o/insert! player-id ::needs-healing true)]}})
```

### 7. Fact Versioning / History

**Status:** 🔨 Proposed

Track historical values of facts and query fact history.

**Use Cases:**
- Audit trail
- Undo/redo functionality
- Temporal queries (what was the value at time T?)
- Change detection and deltas

**Proposed API:**
```clojure
;; Enable history tracking
(def session
  (-> (o/->session)
      (history/enable-for #{::position ::health})))

;; Query history
(history/get-history session ::player1 ::health)
;; => [{:value 100 :timestamp 1000}
;;     {:value 80 :timestamp 1050}
;;     {:value 60 :timestamp 1100}]

;; Query at specific time
(history/get-at session ::player1 ::health 1075)
;; => 80

;; Rules can access previous values
(o/ruleset
  {:health-decreased
   [:what
    [id ::health current-health]
    :when
    (let [prev-health (history/previous session id ::health)]
      (and prev-health (< current-health prev-health)))
    :then
    (println id "took damage!")]})
```

### 8. Existential Quantification (Exists/Forall)

**Status:** 🔨 Proposed

Match based on existence conditions or universal quantification.

**Use Cases:**
- "At least one enemy exists"
- "All players are ready"
- "No obstacles blocking path"
- Quantified pattern matching

**Proposed API:**
```clojure
(o/ruleset
  {:all-players-ready
   [:what
    [game-id ::type :game]
    :when
    (quant/forall? session
      [id ::player-type :player]
      (o/contains? session id ::ready))
    :then
    (o/insert! game-id ::game-state :starting)]

  {:any-enemy-nearby
   [:what
    [player ::position pos]
    :when
    (quant/exists? session
      [enemy ::enemy-type _]
      (fn [match]
        (< (distance pos (:position match)) 10)))
    :then
    (o/insert! player ::alert-state :combat)]})
```

### 9. Rule Scheduling / Delayed Actions

**Status:** 🔨 Proposed

Schedule rules to fire at specific times or after delays.

**Use Cases:**
- Delayed effects (poison damage over time)
- Scheduled events
- Cooldowns and timers
- Deferred execution

**Proposed API:**
```clojure
(o/ruleset
  {:apply-poison
   [:what
    [id ::poisoned? true]
    :then
    (schedule/after 1000 ; After 1 second
      (o/insert! id ::health (- health 5)))
    (schedule/repeat 1000 3 ; Every second, 3 times
      (o/insert! id ::health (- health 5)))]

  {:cooldown-expiry
   [:what
    [id ::cooldown-started time]
    :when
    (schedule/elapsed? time 5000)
    :then
    (o/retract! id ::cooldown-started)
    (o/insert! id ::ability-ready true)]})
```

### 10. Rule Groups / Modules

**Status:** 🔨 Proposed

Organize rules into logical groups that can be enabled/disabled together.

**Use Cases:**
- Feature toggles
- Different rule sets for different game modes
- Conditional rule activation
- Plugin-style rule modules

**Proposed API:**
```clojure
;; Define rule groups
(def combat-rules
  (o/ruleset
    {:deal-damage [...]}
    {:apply-defense [...]}))

(def trading-rules
  (o/ruleset
    {:exchange-items [...]}
    {:update-gold [...]}))

;; Session with module support
(def session
  (-> (o/->session)
      (modules/add-group :combat combat-rules)
      (modules/add-group :trading trading-rules)
      (modules/enable :combat)
      (modules/disable :trading)))

;; Toggle modules dynamically
(modules/toggle session :trading true)
```

### 11. Fuzzy Matching / Similarity

**Status:** 🔨 Proposed

Match patterns based on similarity rather than exact equality.

**Use Cases:**
- Approximate matching
- Threshold-based comparisons
- Pattern recognition
- Probabilistic rules

**Proposed API:**
```clojure
(o/ruleset
  {:similar-color
   [:what
    [id1 ::color c1]
    [id2 ::color c2]
    :when
    (fuzzy/similar? c1 c2 {:threshold 0.9})
    :then
    (o/insert! id1 ::similar-to id2)]})
```

### 12. Conflict Resolution Strategies

**Status:** 🔨 Proposed

Customize how conflicts are resolved when multiple rules can fire.

**Use Cases:**
- Implement different execution strategies
- Control non-determinism
- Optimize rule execution
- Domain-specific conflict resolution

**Proposed Strategies:**
- Depth-first vs Breadth-first
- Recency (prefer recently inserted facts)
- Specificity (prefer more specific rules)
- Random selection
- Custom conflict resolution functions

### 13. Performance Profiling / Debugging Tools

**Status:** 🔨 Proposed

Tools to understand and optimize rule performance.

**Features:**
- Rule execution statistics
- Match count tracking
- Performance bottleneck detection
- Visual rule network explorer
- Fact dependency graph
- Rule fire history and timeline

**Proposed API:**
```clojure
;; Enable profiling
(def session
  (-> (o/->session)
      (profile/enable)))

;; Get statistics
(profile/stats session)
;; => {:rules-fired 42
;;     :total-matches 156
;;     :rule-stats {::damage-calc {:fires 10 :avg-time-ms 0.5}}}

;; Trace rule execution
(profile/trace session)
;; Prints: ::rule1 fired -> ::rule2 fired -> ...
```

### 14. Reactive Streams Integration

**Status:** 🔨 Proposed

Integrate with reactive streams (e.g., core.async, manifold) for event-driven architectures.

**Use Cases:**
- Event sourcing
- Message queue integration
- Real-time data processing
- Asynchronous fact insertion

**Proposed API:**
```clojure
(require '[clojure.core.async :as async])

;; Pipe facts from a channel
(reactive/pipe-facts session fact-channel)

;; Emit rule matches to a channel
(reactive/emit-matches session ::my-rule output-channel)
```

### 15. Persistence / Serialization

**Status:** 🔨 Proposed

Save and restore session state.

**Use Cases:**
- Save game state
- Distributed rules engine
- Session snapshots
- State migration

**Proposed API:**
```clojure
;; Serialize session
(def saved-state (persist/save session))

;; Restore session
(def restored-session (persist/load saved-state rules))

;; Incremental saves
(persist/checkpoint session "checkpoint-1")
(persist/rollback session "checkpoint-1")
```

---

## Implementation Guide

### Architecture Considerations

When implementing extensions for O'Doyle Rules, consider:

1. **Compatibility:** Ensure extensions work with the core library without modifications
2. **Performance:** Avoid adding overhead to the Rete network when features aren't used
3. **Composability:** Extensions should work together when possible
4. **Testing:** Comprehensive test coverage for edge cases

### Extension Patterns

#### 1. Helper Functions (Easiest)
Add utility functions for use in `:when` blocks (like negative fact matching).

#### 2. Session Wrappers
Wrap the session with additional metadata and functionality.

#### 3. Rule Transformations
Transform rules before adding them to the session.

#### 4. Custom Node Types
Extend the Rete network with new node types (most complex).

### Testing Extensions

Each extension should include:
- Unit tests for core functionality
- Integration tests with O'Doyle Rules
- Performance benchmarks
- Usage examples
- Documentation

---

## Contributing

To add a new extension:

1. Create a new namespace in `odoyle-rules-extensions/`
2. Implement the extension
3. Add comprehensive tests in `odoyle-rules-extensions/test/`
4. Document the extension in this file
5. Add usage examples
6. Submit a pull request

## Configuration

### Negative Fact Matching

```clojure
(require '[odoyle-rules-extensions.negative-facts :as nf])

;; Set custom marker
(nf/set-not-defined-marker! :my-app/absent)

;; Use in rules
(nf/not-defined? session id attr)
```

---

## License

These extensions follow the same license as O'Doyle Rules (see UNLICENSE).

## Credits

- Original O'Doyle Rules by Zach Oakes
- Extensions and additional features by contributors
