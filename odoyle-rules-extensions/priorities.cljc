(ns odoyle-rules-extensions.priorities
  "Extension for O'Doyle Rules to support rule priorities (salience).

  Rationale:
  ---------
  By default, O'Doyle Rules fires rules in a deterministic but implementation-
  defined order. In many scenarios, you need explicit control over rule
  execution order:

  1. Override general rules with specific exceptions
  2. Ensure critical rules fire before optional ones
  3. Implement decision hierarchies
  4. Optimize performance by running cheap rules first
  5. Model business rule precedence

  Design Decisions:
  ----------------
  1. Priorities are integers (higher = higher priority)
  2. Default priority is 0
  3. Priorities control :then block execution order
  4. Within same priority, order is deterministic but unspecified
  5. Compatible with existing O'Doyle features

  How It Works:
  ------------
  Since O'Doyle manages rule ordering internally via the Rete network,
  we implement priorities at the fire-rules level by:

  1. Wrapping sessions with priority metadata
  2. Sorting the :then-queue by priority before execution
  3. Preserving all other O'Doyle semantics

  Limitations:
  -----------
  - Priorities only affect :then block order, not :when evaluation
  - Cannot change priorities after rules are added
  - All rules in a session share the same priority system

  Usage example:

  (require '[odoyle.rules :as o]
           '[odoyle-rules-extensions.priorities :as prio])

  ;; Create a session with priority support
  (def session
    (-> (o/->session)
        prio/enable-priorities))

  ;; Add rules with priorities
  (def rules
    [(prio/with-priority 100  ; High priority - fires first
       (o/->rule ::critical-rule
         {:what [['id ::health 'health]]
          :when (fn [session match] (< (:health match) 10))
          :then (fn [session match]
                  (println \"CRITICAL: Low health!\"))}))

     (prio/with-priority 50   ; Medium priority
       (o/->rule ::warning-rule
         {:what [['id ::health 'health]]
          :when (fn [session match] (< (:health match) 30))
          :then (fn [session match]
                  (println \"Warning: Health low\"))}))

     (prio/with-priority 10   ; Low priority - fires last
       (o/->rule ::info-rule
         {:what [['id ::health 'health]]
          :then (fn [session match]
                  (println \"Health update:\", (:health match)))}))])

  (-> (reduce o/add-rule session rules)
      (o/insert ::player ::health 5)
      prio/fire-rules)  ; Use prio/fire-rules instead of o/fire-rules
  "
  (:require [odoyle.rules :as o]))

;; Priority metadata storage

(def ^:private priority-key ::rule-priorities)
(def ^:private default-priority 0)

(defn enable-priorities
  "Enables priority support for a session.

  This wraps the session with priority tracking metadata.
  You must use this on a session before adding rules with priorities.

  Example:
    (def session
      (-> (o/->session)
          prio/enable-priorities))"
  [session]
  (vary-meta session assoc priority-key {}))

(defn- get-priorities [session]
  (get (meta session) priority-key {}))

(defn- set-priority [session rule-name priority]
  (vary-meta session update priority-key assoc rule-name priority))

(defn with-priority
  "Wraps a rule with a priority value.

  Higher priorities fire before lower priorities.
  Default priority is 0.

  Arguments:
    priority - integer, higher values = higher priority
    rule     - O'Doyle Rule instance

  Returns:
    Modified rule with priority metadata

  Example:
    (prio/with-priority 100
      (o/->rule ::high-priority-rule
        {:what [['id ::x 'x]]
         :then (fn [session match] ...)}))"
  [priority rule]
  (vary-meta rule assoc ::priority priority))

(defn get-priority
  "Gets the priority of a rule.

  Arguments:
    rule - O'Doyle Rule instance or rule name (keyword)
    session - (optional) session to look up rule name

  Returns:
    Priority integer, or default-priority if not set"
  ([rule]
   (get (meta rule) ::priority default-priority))
  ([rule session]
   (if (keyword? rule)
     (get (get-priorities session) rule default-priority)
     (get-priority rule))))

(defn add-rule
  "Adds a rule to the session, recording its priority.

  This is a drop-in replacement for o/add-rule that tracks priorities.

  Arguments:
    session - priority-enabled session
    rule    - rule (optionally wrapped with with-priority)

  Returns:
    Updated session

  Example:
    (-> session
        (prio/add-rule (prio/with-priority 100 rule1))
        (prio/add-rule (prio/with-priority 50 rule2)))"
  [session rule]
  (let [priority (get-priority rule)
        session (o/add-rule session rule)
        rule-name (:name rule)]
    (set-priority session rule-name priority)))

(defn add-rules
  "Adds multiple rules to the session, recording their priorities.

  Arguments:
    session - priority-enabled session
    rules   - collection of rules

  Returns:
    Updated session

  Example:
    (prio/add-rules session [rule1 rule2 rule3])"
  [session rules]
  (reduce add-rule session rules))

(defn- sort-then-queue-by-priority
  "Sorts the :then-queue by priority (highest first).

  Within the same priority, maintains original order for determinism."
  [session]
  (let [priorities (get-priorities session)
        then-queue (:then-queue session)]
    (if (empty? then-queue)
      session
      (let [;; Convert set to vec with priorities
            queue-with-prio
            (mapv (fn [[node-id _ :as entry]]
                    (let [rule-name (get-in session [:node-id->rule-name node-id])
                          priority (get priorities rule-name default-priority)]
                      [priority entry]))
                  then-queue)
            ;; Sort by priority (descending) then by original position
            sorted-queue
            (mapv second
                  (sort-by (fn [[priority idx]]
                             [(- priority) idx])  ; negative for descending
                           (map-indexed (fn [idx [prio entry]]
                                         [prio idx entry])
                                       queue-with-prio)))]
        ;; Replace then-queue with sorted version (as a set)
        (assoc session :then-queue (set sorted-queue))))))

(defn fire-rules
  "Fires rules in priority order.

  This is a drop-in replacement for o/fire-rules that respects priorities.

  Higher priority rules fire before lower priority rules.
  Within the same priority level, order is deterministic but unspecified.

  Arguments:
    session - priority-enabled session
    opts    - (optional) options map passed to o/fire-rules

  Returns:
    Updated session

  Example:
    (-> session
        (o/insert ::player ::health 10)
        prio/fire-rules)"
  ([session]
   (fire-rules session {}))
  ([session opts]
   (let [session-with-sorted-queue (sort-then-queue-by-priority session)]
     (o/fire-rules session-with-sorted-queue opts))))

;; Convenience functions

(defn list-priorities
  "Lists all rules and their priorities.

  Arguments:
    session - priority-enabled session

  Returns:
    Map of rule-name -> priority

  Example:
    (prio/list-priorities session)
    ;; => {::critical-rule 100
    ;;     ::warning-rule 50
    ;;     ::info-rule 10}"
  [session]
  (get-priorities session))

(defn set-default-priority
  "Creates a function that wraps rules with a default priority.

  Useful for creating groups of rules with the same priority.

  Arguments:
    priority - default priority value

  Returns:
    Function that takes a rule and returns it with the priority

  Example:
    (def high-priority (prio/set-default-priority 100))

    (prio/add-rule session
      (high-priority my-rule))"
  [priority]
  (fn [rule]
    (with-priority priority rule)))

;; Priority-aware ruleset macro helper

#?(:clj
   (defmacro ruleset-with-priorities
     "Creates a ruleset where each rule can specify its priority.

     Rules are specified as maps with :priority and :rule keys.

     Arguments:
       rules - vector of {:priority N :rule rule-def} maps

     Returns:
       Vector of rules with priorities applied

     Example:
       (prio/ruleset-with-priorities
         [{:priority 100
           :name ::critical
           :rule [:what [id ::health health]
                  :when (< health 10)
                  :then (println \"Critical!\")]}
          {:priority 50
           :name ::warning
           :rule [:what [id ::health health]
                  :when (< health 30)
                  :then (println \"Warning!\")]}])"
     [rules]
     `(mapv (fn [{:keys [~'priority ~'name ~'rule]}]
              (prio/with-priority
                (or ~'priority prio/default-priority)
                (o/->rule ~'name ~'rule)))
            ~rules)))

;; Priority levels - Common named priorities for convenience

(def critical 1000)
(def high 100)
(def normal 0)
(def low -100)
(def very-low -1000)

(defn critical-priority
  "Marks a rule as critical priority (1000).

  Critical rules fire before all other rules.

  Example:
    (prio/add-rule session
      (prio/critical-priority rule))"
  [rule]
  (with-priority critical rule))

(defn high-priority
  "Marks a rule as high priority (100)."
  [rule]
  (with-priority high rule))

(defn normal-priority
  "Marks a rule as normal priority (0).

  This is the default, so usually not needed explicitly."
  [rule]
  (with-priority normal rule))

(defn low-priority
  "Marks a rule as low priority (-100)."
  [rule]
  (with-priority low rule))

(defn very-low-priority
  "Marks a rule as very low priority (-1000).

  Very low priority rules fire after all other rules."
  [rule]
  (with-priority very-low rule))

;; Debugging and introspection

(defn explain-execution-order
  "Returns a list of rules in the order they would execute.

  This examines the current :then-queue and returns rules
  sorted by priority for debugging purposes.

  Arguments:
    session - priority-enabled session

  Returns:
    Vector of {:rule-name name :priority N} maps in execution order

  Example:
    (prio/explain-execution-order session)
    ;; => [{:rule-name ::critical :priority 100}
    ;;     {:rule-name ::normal :priority 0}
    ;;     {:rule-name ::cleanup :priority -100}]"
  [session]
  (let [priorities (get-priorities session)
        then-queue (:then-queue session)]
    (->> then-queue
         (map (fn [[node-id _]]
                (let [rule-name (get-in session [:node-id->rule-name node-id])
                      priority (get priorities rule-name default-priority)]
                  {:rule-name rule-name
                   :priority priority})))
         (sort-by :priority #(compare %2 %1))  ; descending
         vec)))
