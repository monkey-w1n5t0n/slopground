(ns odoyle-rules-extensions.accumulators
  "Extension for O'Doyle Rules to support accumulator functions.

  Accumulators aggregate values across multiple rule matches, enabling
  operations like counting, summing, averaging, and collecting values.

  Rationale:
  ---------
  Many real-world scenarios require aggregating data across multiple facts:
  - Calculating totals (e.g., total health of all players)
  - Finding extremes (e.g., highest score)
  - Counting entities (e.g., number of enemies in range)
  - Grouping data (e.g., players by team)

  Without accumulators, you'd need to manually iterate through query results
  in :then-finally blocks, which is verbose and error-prone.

  Design Decisions:
  ----------------
  1. Use :then-finally blocks - Accumulators need to see all matches
  2. Functional composition - Accumulators are composable functions
  3. Lazy evaluation - Process matches efficiently
  4. Type safety - Validate accumulator inputs
  5. Extensibility - Easy to create custom accumulators

  Usage example:

  (require '[odoyle.rules :as o]
           '[odoyle-rules-extensions.accumulators :as acc])

  (def rules
    (o/ruleset
      {:player-stats
       [:what
        [id ::type :player]
        [id ::health health]
        :then-finally
        (let [total (acc/accumulate session ::player-stats
                      (acc/sum :health))
              count (acc/accumulate session ::player-stats
                      (acc/count))
              avg (acc/accumulate session ::player-stats
                    (acc/avg :health))]
          (o/insert! ::game ::total-health total)
          (o/insert! ::game ::player-count count)
          (o/insert! ::game ::avg-health avg))]}))
  "
  (:require [odoyle.rules :as o]))

;; Core accumulator protocol

(defprotocol Accumulator
  "Protocol for accumulator functions."
  (init [this]
    "Returns the initial accumulator state.")
  (step [this state match]
    "Updates the accumulator state with a new match.")
  (finish [this state]
    "Returns the final accumulated value from the state."))

;; Built-in accumulator implementations

(defrecord CountAccumulator []
  Accumulator
  (init [_] 0)
  (step [_ state _] (inc state))
  (finish [_ state] state))

(defrecord SumAccumulator [key-fn]
  Accumulator
  (init [_] 0)
  (step [_ state match]
    (+ state (key-fn match)))
  (finish [_ state] state))

(defrecord AvgAccumulator [key-fn]
  Accumulator
  (init [_] {:sum 0 :count 0})
  (step [_ state match]
    (-> state
        (update :sum + (key-fn match))
        (update :count inc)))
  (finish [_ state]
    (if (zero? (:count state))
      nil
      (/ (:sum state) (:count state)))))

(defrecord MinAccumulator [key-fn]
  Accumulator
  (init [_] nil)
  (step [_ state match]
    (let [v (key-fn match)]
      (if (nil? state)
        v
        (min state v))))
  (finish [_ state] state))

(defrecord MaxAccumulator [key-fn]
  Accumulator
  (init [_] nil)
  (step [_ state match]
    (let [v (key-fn match)]
      (if (nil? state)
        v
        (max state v))))
  (finish [_ state] state))

(defrecord CollectAccumulator [key-fn]
  Accumulator
  (init [_] [])
  (step [_ state match]
    (conj state (key-fn match)))
  (finish [_ state] state))

(defrecord CollectSetAccumulator [key-fn]
  Accumulator
  (init [_] #{})
  (step [_ state match]
    (conj state (key-fn match)))
  (finish [_ state] state))

(defrecord GroupByAccumulator [key-fn value-fn]
  Accumulator
  (init [_] {})
  (step [_ state match]
    (let [k (key-fn match)
          v (value-fn match)]
      (update state k (fn [existing]
                        (if existing
                          (conj existing v)
                          [v])))))
  (finish [_ state] state))

(defrecord FirstAccumulator [key-fn]
  Accumulator
  (init [_] ::none)
  (step [_ state match]
    (if (= state ::none)
      (key-fn match)
      state))
  (finish [_ state]
    (when-not (= state ::none)
      state)))

(defrecord LastAccumulator [key-fn]
  Accumulator
  (init [_] nil)
  (step [_ state match]
    (key-fn match))
  (finish [_ state] state))

(defrecord CustomAccumulator [init-fn step-fn finish-fn]
  Accumulator
  (init [_] (init-fn))
  (step [_ state match] (step-fn state match))
  (finish [_ state] (finish-fn state)))

;; Accumulator factory functions

(defn count
  "Returns an accumulator that counts matches.

  Example:
    (acc/accumulate session ::my-rule (acc/count))
    ;; => 5"
  []
  (->CountAccumulator))

(defn sum
  "Returns an accumulator that sums values by key.

  Arguments:
    key - keyword or function to extract numeric values from matches

  Example:
    (acc/accumulate session ::player-stats (acc/sum :health))
    ;; => 450"
  [key]
  (->SumAccumulator
    (if (keyword? key)
      key
      key)))

(defn avg
  "Returns an accumulator that averages values by key.

  Arguments:
    key - keyword or function to extract numeric values from matches

  Example:
    (acc/accumulate session ::player-stats (acc/avg :score))
    ;; => 87.5"
  [key]
  (->AvgAccumulator
    (if (keyword? key)
      key
      key)))

(defn min
  "Returns an accumulator that finds the minimum value.

  Arguments:
    key - keyword or function to extract comparable values from matches

  Example:
    (acc/accumulate session ::enemies (acc/min :health))
    ;; => 15"
  [key]
  (->MinAccumulator
    (if (keyword? key)
      key
      key)))

(defn max
  "Returns an accumulator that finds the maximum value.

  Arguments:
    key - keyword or function to extract comparable values from matches

  Example:
    (acc/accumulate session ::players (acc/max :level))
    ;; => 42"
  [key]
  (->MaxAccumulator
    (if (keyword? key)
      key
      key)))

(defn collect
  "Returns an accumulator that collects values into a vector.

  Arguments:
    key - keyword or function to extract values from matches (default: identity)

  Example:
    (acc/accumulate session ::players (acc/collect :name))
    ;; => [\"Alice\" \"Bob\" \"Charlie\"]"
  ([]
   (->CollectAccumulator identity))
  ([key]
   (->CollectAccumulator
     (if (keyword? key)
       key
       key))))

(defn collect-set
  "Returns an accumulator that collects unique values into a set.

  Arguments:
    key - keyword or function to extract values from matches (default: identity)

  Example:
    (acc/accumulate session ::items (acc/collect-set :type))
    ;; => #{:weapon :armor :potion}"
  ([]
   (->CollectSetAccumulator identity))
  ([key]
   (->CollectSetAccumulator
     (if (keyword? key)
       key
       key))))

(defn group-by
  "Returns an accumulator that groups matches by a key function.

  Arguments:
    key-fn   - function to extract grouping key from matches
    value-fn - function to extract values (default: identity)

  Example:
    (acc/accumulate session ::players
      (acc/group-by :team :name))
    ;; => {:red [\"Alice\" \"Bob\"], :blue [\"Charlie\" \"David\"]}"
  ([key-fn]
   (->GroupByAccumulator
     (if (keyword? key-fn) key-fn key-fn)
     identity))
  ([key-fn value-fn]
   (->GroupByAccumulator
     (if (keyword? key-fn) key-fn key-fn)
     (if (keyword? value-fn) value-fn value-fn))))

(defn first
  "Returns an accumulator that returns the first match's value.

  Arguments:
    key - keyword or function to extract value (default: identity)

  Example:
    (acc/accumulate session ::high-scores (acc/first :player-name))
    ;; => \"Alice\""
  ([]
   (->FirstAccumulator identity))
  ([key]
   (->FirstAccumulator
     (if (keyword? key)
       key
       key))))

(defn last
  "Returns an accumulator that returns the last match's value.

  Arguments:
    key - keyword or function to extract value (default: identity)

  Example:
    (acc/accumulate session ::recent-events (acc/last :timestamp))
    ;; => 1234567890"
  ([]
   (->LastAccumulator identity))
  ([key]
   (->LastAccumulator
     (if (keyword? key)
       key
       key))))

(defn custom
  "Creates a custom accumulator.

  Arguments:
    init-fn   - function returning initial state
    step-fn   - function (state, match) => new-state
    finish-fn - function state => final-value

  Example:
    (def median-acc
      (acc/custom
        (fn [] [])
        (fn [state match] (conj state (:value match)))
        (fn [state]
          (let [sorted (sort state)
                n (count sorted)]
            (if (zero? n)
              nil
              (nth sorted (quot n 2)))))))

    (acc/accumulate session ::values median-acc)"
  [init-fn step-fn finish-fn]
  (->CustomAccumulator init-fn step-fn finish-fn))

;; Core accumulation function

(defn accumulate
  "Accumulates values from rule matches using the given accumulator.

  Arguments:
    session     - O'Doyle session
    rule-name   - keyword identifying the rule
    accumulator - accumulator created with acc/count, acc/sum, etc.

  Returns:
    The accumulated value

  Example:
    (acc/accumulate session ::my-rule (acc/sum :points))

  This should be called in a :then-finally block to aggregate all matches."
  [session rule-name accumulator]
  (let [matches (o/query-all session rule-name)
        initial (init accumulator)]
    (finish accumulator
            (reduce (fn [state match]
                      (step accumulator state match))
                    initial
                    matches))))

;; Convenience functions for common patterns

(defn accumulate-all
  "Accumulates multiple values at once using different accumulators.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule
    acc-map   - map of result-key => accumulator

  Returns:
    Map of result-key => accumulated value

  Example:
    (acc/accumulate-all session ::player-stats
      {:total-health (acc/sum :health)
       :avg-score    (acc/avg :score)
       :count        (acc/count)
       :players      (acc/collect :name)})
    ;; => {:total-health 450, :avg-score 87.5, :count 5, :players [...]}"
  [session rule-name acc-map]
  (reduce-kv
    (fn [result k accumulator]
      (assoc result k (accumulate session rule-name accumulator)))
    {}
    acc-map))

(defn top-n
  "Returns an accumulator that collects the top N values by some key.

  Arguments:
    n      - number of top values to keep
    key-fn - function to extract comparison value

  Example:
    (acc/accumulate session ::players (acc/top-n 3 :score))
    ;; => [{:name \"Alice\" :score 100}
    ;;     {:name \"Bob\" :score 95}
    ;;     {:name \"Charlie\" :score 90}]"
  [n key-fn]
  (custom
    (fn [] [])
    (fn [state match]
      (let [with-new (conj state match)
            sorted (sort-by (if (keyword? key-fn) key-fn key-fn)
                           #(compare %2 %1)  ; reverse for descending
                           with-new)]
        (vec (take n sorted))))
    identity))

(defn bottom-n
  "Returns an accumulator that collects the bottom N values by some key.

  Arguments:
    n      - number of bottom values to keep
    key-fn - function to extract comparison value

  Example:
    (acc/accumulate session ::players (acc/bottom-n 3 :health))
    ;; => [{:name \"Bob\" :health 10}
    ;;     {:name \"Charlie\" :health 15}
    ;;     {:name \"David\" :health 20}]"
  [n key-fn]
  (custom
    (fn [] [])
    (fn [state match]
      (let [with-new (conj state match)
            sorted (sort-by (if (keyword? key-fn) key-fn key-fn)
                           with-new)]
        (vec (take n sorted))))
    identity))

(defn distinct-count
  "Returns an accumulator that counts distinct values.

  Arguments:
    key-fn - function to extract values for uniqueness check

  Example:
    (acc/accumulate session ::players (acc/distinct-count :team))
    ;; => 3  ; 3 different teams"
  [key-fn]
  (custom
    (fn [] #{})
    (fn [state match]
      (conj state ((if (keyword? key-fn) key-fn key-fn) match)))
    count))

;; Statistical accumulators

(defn variance
  "Returns an accumulator that calculates variance.

  Arguments:
    key-fn - function to extract numeric values

  Example:
    (acc/accumulate session ::measurements (acc/variance :value))"
  [key-fn]
  (custom
    (fn [] {:sum 0 :sum-sq 0 :count 0})
    (fn [state match]
      (let [v ((if (keyword? key-fn) key-fn key-fn) match)]
        (-> state
            (update :sum + v)
            (update :sum-sq + (* v v))
            (update :count inc))))
    (fn [{:keys [sum sum-sq count]}]
      (when (pos? count)
        (let [mean (/ sum count)
              mean-sq (/ sum-sq count)]
          (- mean-sq (* mean mean)))))))

(defn std-dev
  "Returns an accumulator that calculates standard deviation.

  Arguments:
    key-fn - function to extract numeric values

  Example:
    (acc/accumulate session ::measurements (acc/std-dev :value))"
  [key-fn]
  (custom
    (fn [] {:sum 0 :sum-sq 0 :count 0})
    (fn [state match]
      (let [v ((if (keyword? key-fn) key-fn key-fn) match)]
        (-> state
            (update :sum + v)
            (update :sum-sq + (* v v))
            (update :count inc))))
    (fn [{:keys [sum sum-sq count]}]
      (when (pos? count)
        (let [mean (/ sum count)
              mean-sq (/ sum-sq count)
              variance (- mean-sq (* mean mean))]
          #?(:clj (Math/sqrt variance)
             :cljs (js/Math.sqrt variance)))))))
