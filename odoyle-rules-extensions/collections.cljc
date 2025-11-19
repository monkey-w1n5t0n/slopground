(ns odoyle-rules-extensions.collections
  "Extension for O'Doyle Rules to support pattern matching on collections.

  Rationale:
  ---------
  Many facts have collection values (vectors, sets, maps) that need
  to be queried:
  - Player inventory (vector of items)
  - Entity tags/capabilities (set of keywords)
  - Configuration maps
  - Nested data structures

  Without this extension, you'd need verbose :when blocks to check
  collection contents, making rules harder to read and maintain.

  Design Decisions:
  ----------------
  1. All functions work in :when blocks
  2. Designed for common collection operations
  3. Works with any Clojure collection type
  4. Composable - combine multiple checks
  5. Performance-conscious - avoid expensive operations

  How It Works:
  ------------
  These are helper functions for use in :when blocks that make
  collection checking more expressive and declarative.

  Usage example:

  (require '[odoyle.rules :as o]
           '[odoyle-rules-extensions.collections :as coll])

  (def rules
    (o/ruleset
      {:equipped-player
       [:what
        [player ::inventory inv]
        [player ::skills skills]
        :when
        ;; Check vector contains specific items
        (coll/contains-all? inv [:sword :shield :potion])
        ;; Check set membership
        (coll/has-any? skills #{:fire-magic :ice-magic})
        :then
        (println player \"is ready for combat!\")]}))
  "
  (:require [odoyle.rules :as o]
            [clojure.set :as set]))

;; Basic collection operations

(defn contains?
  "Returns true if collection contains the value.

  Works with any collection type that supports clojure.core/some.

  Arguments:
    coll  - collection to search
    value - value to find

  Example:
    (coll/contains? [:sword :shield] :sword)  ; => true
    (coll/contains? #{:a :b :c} :b)           ; => true"
  [coll value]
  (boolean (some #(= % value) coll)))

(defn contains-all?
  "Returns true if collection contains all specified values.

  Arguments:
    coll   - collection to search
    values - collection of values that must all be present

  Example:
    (coll/contains-all? [:a :b :c :d] [:a :c])        ; => true
    (coll/contains-all? #{:x :y :z} [:x :w])          ; => false"
  [coll values]
  (every? #(contains? coll %) values))

(defn contains-any?
  "Returns true if collection contains at least one of the specified values.

  Arguments:
    coll   - collection to search
    values - collection of values, at least one must be present

  Example:
    (coll/contains-any? [:a :b :c] [:x :y :a])  ; => true
    (coll/contains-any? #{:red :blue} [:green :yellow])  ; => false"
  [coll values]
  (boolean (some #(contains? coll %) values)))

(defn contains-none?
  "Returns true if collection contains none of the specified values.

  Arguments:
    coll   - collection to search
    values - collection of values, none should be present

  Example:
    (coll/contains-none? [:a :b :c] [:x :y :z])  ; => true
    (coll/contains-none? #{:red :blue} [:blue :green])  ; => false"
  [coll values]
  (not (contains-any? coll values)))

(defn has-all?
  "Alias for contains-all?. More readable for sets.

  Example:
    (coll/has-all? player-skills #{:attack :defend})"
  [coll values]
  (contains-all? coll values))

(defn has-any?
  "Alias for contains-any?. More readable for sets.

  Example:
    (coll/has-any? player-roles #{:admin :moderator})"
  [coll values]
  (contains-any? coll values))

(defn has-none?
  "Alias for contains-none?. More readable for sets.

  Example:
    (coll/has-none? player-status #{:poisoned :stunned})"
  [coll values]
  (contains-none? coll values))

;; Size/count operations

(defn empty?
  "Returns true if collection is empty.

  Example:
    (coll/empty? [])  ; => true
    (coll/empty? #{:a})  ; => false"
  [coll]
  (clojure.core/empty? coll))

(defn not-empty?
  "Returns true if collection is not empty.

  Example:
    (coll/not-empty? [:a])  ; => true"
  [coll]
  (not (clojure.core/empty? coll)))

(defn count-eq?
  "Returns true if collection has exactly n elements.

  Arguments:
    coll - collection to check
    n    - expected count

  Example:
    (coll/count-eq? [:a :b :c] 3)  ; => true"
  [coll n]
  (= (count coll) n))

(defn count-gt?
  "Returns true if collection has more than n elements.

  Arguments:
    coll - collection to check
    n    - minimum count (exclusive)

  Example:
    (coll/count-gt? [:a :b :c] 2)  ; => true"
  [coll n]
  (> (count coll) n))

(defn count-gte?
  "Returns true if collection has n or more elements.

  Arguments:
    coll - collection to check
    n    - minimum count (inclusive)

  Example:
    (coll/count-gte? [:a :b :c] 3)  ; => true"
  [coll n]
  (>= (count coll) n))

(defn count-lt?
  "Returns true if collection has fewer than n elements.

  Arguments:
    coll - collection to check
    n    - maximum count (exclusive)

  Example:
    (coll/count-lt? [:a :b] 3)  ; => true"
  [coll n]
  (< (count coll) n))

(defn count-lte?
  "Returns true if collection has n or fewer elements.

  Arguments:
    coll - collection to check
    n    - maximum count (inclusive)

  Example:
    (coll/count-lte? [:a :b :c] 3)  ; => true"
  [coll n]
  (<= (count coll) n))

;; Set operations

(defn subset?
  "Returns true if set1 is a subset of set2.

  Arguments:
    set1 - potential subset
    set2 - potential superset

  Example:
    (coll/subset? #{:a :b} #{:a :b :c})  ; => true"
  [set1 set2]
  (set/subset? (set set1) (set set2)))

(defn superset?
  "Returns true if set1 is a superset of set2.

  Arguments:
    set1 - potential superset
    set2 - potential subset

  Example:
    (coll/superset? #{:a :b :c} #{:a :b})  ; => true"
  [set1 set2]
  (set/superset? (set set1) (set set2)))

(defn disjoint?
  "Returns true if the two sets have no common elements.

  Arguments:
    set1 - first set
    set2 - second set

  Example:
    (coll/disjoint? #{:a :b} #{:c :d})  ; => true"
  [set1 set2]
  (clojure.core/empty? (set/intersection (set set1) (set set2))))

(defn intersects?
  "Returns true if the two sets have at least one common element.

  Arguments:
    set1 - first set
    set2 - second set

  Example:
    (coll/intersects? #{:a :b :c} #{:c :d})  ; => true"
  [set1 set2]
  (not (disjoint? set1 set2)))

;; Map operations

(defn has-key?
  "Returns true if map contains the specified key.

  Arguments:
    m   - map to check
    key - key to look for

  Example:
    (coll/has-key? {:a 1 :b 2} :a)  ; => true"
  [m key]
  (clojure.core/contains? m key))

(defn has-keys?
  "Returns true if map contains all specified keys.

  Arguments:
    m    - map to check
    keys - collection of keys that must all be present

  Example:
    (coll/has-keys? {:a 1 :b 2 :c 3} [:a :c])  ; => true"
  [m keys]
  (every? #(clojure.core/contains? m %) keys))

(defn has-any-key?
  "Returns true if map contains at least one of the specified keys.

  Arguments:
    m    - map to check
    keys - collection of keys, at least one must be present

  Example:
    (coll/has-any-key? {:a 1 :b 2} [:c :a])  ; => true"
  [m keys]
  (boolean (some #(clojure.core/contains? m %) keys)))

(defn has-value?
  "Returns true if map contains the specified value.

  Arguments:
    m     - map to check
    value - value to look for

  Example:
    (coll/has-value? {:a 1 :b 2} 2)  ; => true"
  [m value]
  (boolean (some #(= % value) (vals m))))

(defn get-in?
  "Returns true if nested path exists in map and optionally matches predicate.

  Arguments:
    m         - map to check
    path      - path vector (like clojure.core/get-in)
    pred-or-val - (optional) predicate function or value to match

  Example:
    (coll/get-in? {:a {:b {:c 10}}} [:a :b :c])       ; => true
    (coll/get-in? {:a {:b {:c 10}}} [:a :b :c] 10)    ; => true
    (coll/get-in? {:a {:b {:c 10}}} [:a :b :c] #(> % 5))  ; => true"
  ([m path]
   (not= ::not-found (get-in m path ::not-found)))
  ([m path pred-or-val]
   (let [v (get-in m path ::not-found)]
     (and (not= ::not-found v)
          (if (fn? pred-or-val)
            (pred-or-val v)
            (= v pred-or-val))))))

;; Vector/sequence operations

(defn nth?
  "Returns true if nth element exists and optionally matches predicate.

  Arguments:
    coll        - collection to check
    n           - index
    pred-or-val - (optional) predicate function or value to match

  Example:
    (coll/nth? [:a :b :c] 1)        ; => true
    (coll/nth? [:a :b :c] 1 :b)     ; => true
    (coll/nth? [:a :b :c] 10)       ; => false"
  ([coll n]
   (< n (count coll)))
  ([coll n pred-or-val]
   (and (< n (count coll))
        (let [v (nth coll n)]
          (if (fn? pred-or-val)
            (pred-or-val v)
            (= v pred-or-val))))))

(defn first?
  "Returns true if first element exists and optionally matches predicate.

  Arguments:
    coll        - collection to check
    pred-or-val - (optional) predicate function or value to match

  Example:
    (coll/first? [:a :b :c] :a)  ; => true
    (coll/first? [:a :b :c] #(= % :a))  ; => true"
  ([coll]
   (not-empty? coll))
  ([coll pred-or-val]
   (and (not-empty? coll)
        (let [v (first coll)]
          (if (fn? pred-or-val)
            (pred-or-val v)
            (= v pred-or-val))))))

(defn last?
  "Returns true if last element exists and optionally matches predicate.

  Arguments:
    coll        - collection to check
    pred-or-val - (optional) predicate function or value to match

  Example:
    (coll/last? [:a :b :c] :c)  ; => true"
  ([coll]
   (not-empty? coll))
  ([coll pred-or-val]
   (and (not-empty? coll)
        (let [v (last coll)]
          (if (fn? pred-or-val)
            (pred-or-val v)
            (= v pred-or-val))))))

;; Filtering and finding

(defn any?
  "Returns true if any element satisfies the predicate.

  Arguments:
    pred - predicate function
    coll - collection to check

  Example:
    (coll/any? even? [1 3 5 6 7])  ; => true"
  [pred coll]
  (boolean (some pred coll)))

(defn all?
  "Returns true if all elements satisfy the predicate.

  Arguments:
    pred - predicate function
    coll - collection to check

  Example:
    (coll/all? even? [2 4 6 8])  ; => true"
  [pred coll]
  (every? pred coll))

(defn none?
  "Returns true if no elements satisfy the predicate.

  Arguments:
    pred - predicate function
    coll - collection to check

  Example:
    (coll/none? even? [1 3 5 7])  ; => true"
  [pred coll]
  (not (some pred coll)))

(defn count-where
  "Returns the count of elements satisfying the predicate.

  Arguments:
    pred - predicate function
    coll - collection to check

  Example:
    (coll/count-where even? [1 2 3 4 5 6])  ; => 3"
  [pred coll]
  (count (filter pred coll)))

(defn distinct-values?
  "Returns true if all elements in collection are distinct.

  Example:
    (coll/distinct-values? [:a :b :c])    ; => true
    (coll/distinct-values? [:a :b :a])    ; => false"
  [coll]
  (= (count coll) (count (distinct coll))))

;; Composite operations for common patterns

(defn has-required-items?
  "Checks if a collection (like inventory) has all required items.

  Useful for quest/crafting requirements.

  Arguments:
    inventory - collection of items
    required  - collection of required items

  Example:
    (coll/has-required-items? [:sword :shield :potion] [:sword :shield])
    ; => true"
  [inventory required]
  (contains-all? inventory required))

(defn has-capability?
  "Checks if entity capabilities/tags include all required ones.

  Arguments:
    capabilities - set of capabilities
    required     - collection of required capabilities

  Example:
    (coll/has-capability? #{:can-fly :can-swim} [:can-fly])
    ; => true"
  [capabilities required]
  (has-all? capabilities required))

(defn missing-requirements?
  "Returns collection of missing requirements.

  Arguments:
    current  - collection of current items/capabilities
    required - collection of required items/capabilities

  Returns:
    Collection of missing requirements (empty if all present)

  Example:
    (coll/missing-requirements? [:sword] [:sword :shield :potion])
    ; => (:shield :potion)"
  [current required]
  (remove (set current) required))
