# O'Doyle Rules Extensions

Extensions and enhancements for the [O'Doyle Rules](https://github.com/oakes/odoyle-rules) forward-chaining rules engine.

## Overview

This project provides a collection of extensions that enhance O'Doyle Rules with additional functionality while maintaining compatibility with the core library.

## Currently Implemented

### Negative Fact Matching

Match on the **absence** of facts, enabling rules to fire when certain data is NOT present.

**Quick Example:**
```clojure
(require '[odoyle.rules :as o]
         '[odoyle-rules-extensions.negative-facts :as nf])

(def rules
  (o/ruleset
    {:incomplete-profile
     [:what
      [user ::name name]
      [user ::email email]
      :when
      ;; This rule fires when user has name and email, but NOT avatar
      (nf/not-defined? session user ::avatar)
      :then
      (println "User" name "needs to upload an avatar!")]}))

(-> (reduce o/add-rule (o/->session) rules)
    (o/insert ::user1 ::name "Alice")
    (o/insert ::user1 ::email "alice@example.com")
    ;; Note: no ::avatar inserted
    o/fire-rules)
;; Prints: "User Alice needs to upload an avatar!"
```

## Installation

### As a Local Dependency

Add to your `deps.edn`:

```clojure
{:deps {odoyle-rules-extensions {:local/root "../path/to/odoyle-rules-extensions"}
        odoyle-rules {:local/root "../path/to/odoyle-rules"}}}
```

## Usage

### Negative Fact Matching API

#### Basic Functions

**`not-defined?`** - Check if a fact doesn't exist
```clojure
[:what
 [id ::x x]
 :when
 (nf/not-defined? session id ::y)
 :then
 (println id "does not have ::y")]
```

**`all-not-defined?`** - Check multiple attributes are all absent
```clojure
[:when
 (nf/all-not-defined? session player [::weapon ::armor ::shield])
 :then
 (println "Player is unarmed and unarmored!")]
```

**`any-not-defined?`** - Check if at least one attribute is missing
```clojure
[:when
 (nf/any-not-defined? session entity [::x ::y ::z])
 :then
 (println "Entity is missing at least one coordinate")]
```

**`is-marker?`** - Check if a value equals the not-defined marker
```clojure
[:what
 [id ::status status]
 :when
 (nf/is-marker? status)
 :then
 (println id "has no status")]
```

#### Advanced Patterns

**Negated Patterns** - Declarative approach:
```clojure
(def neg-weapon (nf/->negated-pattern 'player ::weapon))

[:what
 [player ::type :player]
 :when
 (nf/matches-negated? session match neg-weapon)
 :then
 (o/insert! player ::status :unarmed)]
```

**Condition Factories** - Pre-built condition functions:
```clojure
(def check-no-shield (nf/create-negative-condition 'knight ::shield))

[:what
 [knight ::type :knight]
 :when
 (check-no-shield session match)
 :then
 (println "Knight needs a shield!")]
```

#### Configuration

**Custom Markers** - Configure the sentinel value:
```clojure
;; Set globally
(nf/set-not-defined-marker! :my-app/absent)

;; Or use dynamically (Clojure only)
(nf/with-not-defined-marker :custom/missing
  ;; code here uses :custom/missing as the marker
  )
```

### Complete Example: Game Character System

```clojure
(require '[odoyle.rules :as o]
         '[odoyle-rules-extensions.negative-facts :as nf])

(def character-rules
  (o/ruleset
    ;; Character is incomplete if missing coordinates
    {:incomplete-position
     [:what
      [char ::name name]
      :when
      (nf/any-not-defined? session char [::x ::y])
      :then
      (println "Character" name "needs position!")
      (o/insert! char ::status :needs-position)]}

    ;; Character is ready if has all required attributes
    {:character-ready
     [:what
      [char ::name name]
      [char ::x x]
      [char ::y y]
      [char ::health health]
      :when
      (> health 0)
      :then
      (o/insert! char ::status :active)]}

    ;; Unarmed character detection
    {:unarmed
     [:what
      [char ::type :player]
      :when
      (nf/not-defined? session char ::weapon)
      :then
      (o/insert! char ::combat-modifier 0.5)]}  ; Half damage when unarmed

    ;; New player tutorial trigger
    {:new-player
     [:what
      [player ::type :player]
      :when
      (nf/all-not-defined? session player
        [::completed-tutorial ::level ::experience])
      :then
      (o/insert! player ::show-tutorial true)]}))

;; Use the rules
(-> (reduce o/add-rule (o/->session) character-rules)
    (o/insert ::player1 {::type :player
                         ::name "Alice"
                         ::x 100
                         ::y 200
                         ::health 100})
    ;; Note: no ::weapon, ::completed-tutorial, etc.
    o/fire-rules)
```

## Use Cases

Negative fact matching is useful for:

1. **Validation & Completeness Checks**
   - Detect incomplete profiles, missing required fields
   - Form validation

2. **Default Behavior**
   - Apply defaults when specific configuration is absent
   - Fallback rules

3. **State Transitions**
   - Detect when prerequisites are missing
   - Guard conditions

4. **Game Logic**
   - Detect unarmed/unequipped characters
   - Trigger tutorials for new players
   - Check missing status effects

5. **Business Rules**
   - Detect missing documentation
   - Flag incomplete transactions
   - Identify missing approvals

## Testing

Run tests:
```bash
clojure -M:test:odoyle -m clojure.test.runner \
  odoyle-rules-extensions.negative-facts-test
```

## Documentation

For a complete list of proposed extensions and implementation details, see [EXTENSIONS.md](../EXTENSIONS.md).

## Performance Considerations

- Negative fact checking uses `o/contains?` which is O(1) lookup
- No overhead added to normal fact insertion
- Minimal impact on rule execution
- Scales with the number of facts checked, not total facts

## Comparison with Alternatives

### Using `:when` with `not`
```clojure
;; Manual approach (what you'd do without this extension)
[:what
 [id ::x x]
 :when
 (not (o/contains? session id ::y))
 :then
 ...]

;; With extension - more declarative and reusable
[:when
 (nf/not-defined? session id ::y)
 :then
 ...]
```

The extension provides:
- ✅ Better readability and intent
- ✅ Reusable helper functions
- ✅ Consistent API across projects
- ✅ Advanced patterns (all-not-defined, any-not-defined, etc.)

## Contributing

Contributions welcome! Please:
1. Add tests for new functionality
2. Update documentation
3. Follow existing code style
4. Add usage examples

## License

This project is released under the [UNLICENSE](../LICENSE), same as O'Doyle Rules.

## Credits

- Original O'Doyle Rules by [Zach Oakes](https://github.com/oakes)
- Negative fact matching extension by contributors

## See Also

- [O'Doyle Rules](https://github.com/oakes/odoyle-rules)
- [EXTENSIONS.md](../EXTENSIONS.md) - Full list of proposed extensions
- [Rete Algorithm](https://en.wikipedia.org/wiki/Rete_algorithm)
