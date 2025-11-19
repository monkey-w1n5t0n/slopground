(ns odoyle-rules-extensions.quantifiers
  "Extension for O'Doyle Rules to support existential quantification.

  Rationale:
  ---------
  Many business rules require quantified statements:
  - 'At least one enemy exists within range'
  - 'All players are ready'
  - 'No obstacles block the path'
  - 'There exists a player with admin rights'

  Without quantifiers, you need verbose code to iterate through
  query results and check conditions manually.

  Design Decisions:
  ----------------
  1. exists? - Check if at least one match satisfies condition
  2. forall? - Check if all matches satisfy condition
  3. none? - Check if no matches satisfy condition
  4. count-where - Count matches satisfying condition
  5. Works in :when blocks with session access
  6. Leverages O'Doyle's query mechanism

  How It Works:
  ------------
  These functions query the session for matches and apply predicates.
  They're designed for use in :when blocks where you have session access.

  Usage example:

  (require '[odoyle.rules :as o]
           '[odoyle-rules-extensions.quantifiers :as quant])

  (def rules
    (o/ruleset
      {:start-game
       [:what
        [game-id ::type :game]
        :when
        ;; Check that all players are ready
        (quant/forall? session
          ::all-players  ; rule name to query
          (fn [match] (:ready match)))
        :then
        (o/insert! game-id ::state :starting)]}

      {:all-players  ; Helper rule to find all players
       [:what
        [id ::player-type :player]
        [id ::ready ready]]}))
  "
  (:require [odoyle.rules :as o]))

;; Core quantifier functions

(defn exists?
  "Returns true if at least one match from the rule satisfies the predicate.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    pred      - predicate function match => boolean
                if omitted, checks if any matches exist

  Returns:
    true if at least one match satisfies predicate

  Example:
    (quant/exists? session ::enemies
      (fn [enemy] (< (:health enemy) 10)))
    ; Returns true if any enemy has health < 10

    (quant/exists? session ::players)
    ; Returns true if any players exist"
  ([session rule-name]
   (let [matches (o/query-all session rule-name)]
     (seq matches)))
  ([session rule-name pred]
   (let [matches (o/query-all session rule-name)]
     (boolean (some pred matches)))))

(defn forall?
  "Returns true if all matches from the rule satisfy the predicate.

  Returns true for empty result sets (vacuous truth).

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    pred      - predicate function match => boolean

  Returns:
    true if all matches satisfy predicate (or no matches)

  Example:
    (quant/forall? session ::players
      (fn [player] (:ready player)))
    ; Returns true if all players are ready"
  [session rule-name pred]
  (let [matches (o/query-all session rule-name)]
    (every? pred matches)))

(defn none?
  "Returns true if no matches from the rule satisfy the predicate.

  Returns true for empty result sets.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    pred      - predicate function match => boolean

  Returns:
    true if no matches satisfy predicate

  Example:
    (quant/none? session ::players
      (fn [player] (< (:health player) 0)))
    ; Returns true if no players have negative health"
  [session rule-name pred]
  (let [matches (o/query-all session rule-name)]
    (not (some pred matches))))

(defn count-where
  "Returns the count of matches satisfying the predicate.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    pred      - predicate function match => boolean

  Returns:
    integer count of matching results

  Example:
    (quant/count-where session ::players
      (fn [player] (> (:level player) 10)))
    ; Returns count of high-level players"
  [session rule-name pred]
  (let [matches (o/query-all session rule-name)]
    (count (filter pred matches))))

;; Comparison quantifiers

(defn exists-exactly-one?
  "Returns true if exactly one match satisfies the predicate.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    pred      - predicate function match => boolean
                if omitted, checks for exactly one match

  Example:
    (quant/exists-exactly-one? session ::admin-users)
    ; Returns true if exactly one admin exists"
  ([session rule-name]
   (= 1 (count (o/query-all session rule-name))))
  ([session rule-name pred]
   (= 1 (count-where session rule-name pred))))

(defn exists-at-least?
  "Returns true if at least n matches satisfy the predicate.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    n         - minimum count
    pred      - (optional) predicate function match => boolean

  Example:
    (quant/exists-at-least? session ::players 4)
    ; Returns true if at least 4 players exist"
  ([session rule-name n]
   (>= (count (o/query-all session rule-name)) n))
  ([session rule-name n pred]
   (>= (count-where session rule-name pred) n)))

(defn exists-at-most?
  "Returns true if at most n matches satisfy the predicate.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    n         - maximum count
    pred      - (optional) predicate function match => boolean

  Example:
    (quant/exists-at-most? session ::errors 3)
    ; Returns true if 3 or fewer errors exist"
  ([session rule-name n]
   (<= (count (o/query-all session rule-name)) n))
  ([session rule-name n pred]
   (<= (count-where session rule-name pred) n)))

(defn exists-between?
  "Returns true if between min and max matches satisfy the predicate.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    min       - minimum count (inclusive)
    max       - maximum count (inclusive)
    pred      - (optional) predicate function match => boolean

  Example:
    (quant/exists-between? session ::team-members 3 6)
    ; Returns true if team has 3-6 members"
  ([session rule-name min max]
   (let [cnt (count (o/query-all session rule-name))]
     (and (>= cnt min) (<= cnt max))))
  ([session rule-name min max pred]
   (let [cnt (count-where session rule-name pred)]
     (and (>= cnt min) (<= cnt max)))))

;; Aggregate quantifiers

(defn majority?
  "Returns true if more than half of matches satisfy the predicate.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    pred      - predicate function match => boolean

  Example:
    (quant/majority? session ::players
      (fn [player] (:voted player)))
    ; Returns true if majority of players have voted"
  [session rule-name pred]
  (let [matches (o/query-all session rule-name)
        total (count matches)
        satisfied (count-where session rule-name pred)]
    (and (pos? total)
         (> satisfied (/ total 2)))))

(defn minority?
  "Returns true if less than half of matches satisfy the predicate.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    pred      - predicate function match => boolean

  Example:
    (quant/minority? session ::players
      (fn [player] (:premium player)))
    ; Returns true if minority of players are premium"
  [session rule-name pred]
  (let [matches (o/query-all session rule-name)
        total (count matches)
        satisfied (count-where session rule-name pred)]
    (and (pos? total)
         (< satisfied (/ total 2)))))

(defn unanimous?
  "Alias for forall?. Returns true if all matches satisfy predicate.

  More readable name for voting/agreement scenarios.

  Example:
    (quant/unanimous? session ::board-members
      (fn [member] (:approved-motion member)))"
  [session rule-name pred]
  (forall? session rule-name pred))

;; Relative quantifiers

(defn exists-more-than?
  "Returns true if more matches satisfy pred1 than pred2.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    pred1     - first predicate function
    pred2     - second predicate function

  Example:
    (quant/exists-more-than? session ::players
      (fn [p] (= (:team p) :red))
      (fn [p] (= (:team p) :blue)))
    ; Returns true if more red team players than blue"
  [session rule-name pred1 pred2]
  (let [count1 (count-where session rule-name pred1)
        count2 (count-where session rule-name pred2)]
    (> count1 count2)))

(defn exists-fewer-than?
  "Returns true if fewer matches satisfy pred1 than pred2.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    pred1     - first predicate function
    pred2     - second predicate function

  Example:
    (quant/exists-fewer-than? session ::tasks
      (fn [t] (= (:status t) :done))
      (fn [t] (= (:status t) :pending)))"
  [session rule-name pred1 pred2]
  (let [count1 (count-where session rule-name pred1)
        count2 (count-where session rule-name pred2)]
    (< count1 count2)))

;; Convenience helpers for common patterns

(defn any-player?
  "Returns true if any player exists.

  Assumes players are identified by ::type :player.

  Example:
    (quant/any-player? session)"
  [session]
  (exists? session
    (fn [fact]
      (and (= (second fact) ::type)
           (= (nth fact 2) :player)))))

(defn all-players-ready?
  "Returns true if all players have ::ready true.

  Arguments:
    session - O'Doyle session

  Example:
    (quant/all-players-ready? session)"
  [session]
  ;; This requires a helper rule to query
  ;; Users should define their own based on their schema
  false)  ; Placeholder - users implement based on schema

(defn percentage-where
  "Returns the percentage (0-100) of matches satisfying predicate.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    pred      - predicate function match => boolean

  Returns:
    percentage as double (0.0 to 100.0)

  Example:
    (quant/percentage-where session ::players
      (fn [player] (:online player)))
    ; => 75.0  (75% of players are online)"
  [session rule-name pred]
  (let [matches (o/query-all session rule-name)
        total (count matches)]
    (if (zero? total)
      0.0
      (* 100.0 (/ (count-where session rule-name pred) total)))))

;; Comparison with values

(defn all-match-value?
  "Returns true if all matches have the same value for the given key.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    key       - keyword to extract from matches
    value     - expected value

  Example:
    (quant/all-match-value? session ::team-members :team :red)
    ; Returns true if all team members are on red team"
  [session rule-name key value]
  (forall? session rule-name
    (fn [match] (= (get match key) value))))

(defn any-match-value?
  "Returns true if any match has the value for the given key.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    key       - keyword to extract from matches
    value     - expected value

  Example:
    (quant/any-match-value? session ::players :role :admin)
    ; Returns true if any player has admin role"
  [session rule-name key value]
  (exists? session rule-name
    (fn [match] (= (get match key) value))))

(defn none-match-value?
  "Returns true if no matches have the value for the given key.

  Arguments:
    session   - O'Doyle session
    rule-name - keyword identifying the rule to query
    key       - keyword to extract from matches
    value     - value that should not appear

  Example:
    (quant/none-match-value? session ::players :status :banned)
    ; Returns true if no players are banned"
  [session rule-name key value]
  (none? session rule-name
    (fn [match] (= (get match key) value))))
