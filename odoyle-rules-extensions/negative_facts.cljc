(ns odoyle-rules-extensions.negative-facts
  "Extension for O'Doyle Rules to support matching on the absence of facts.

  This extension provides a way to match rules when certain facts are NOT present.

  Usage example:

  (require '[odoyle.rules :as o]
           '[odoyle-rules-extensions.negative-facts :as nf])

  ;; Set a custom marker (optional, defaults to :odoyle/not-defined)
  (nf/set-not-defined-marker! :my-app/absent)

  ;; Use in a rule:
  (def rules
    (o/ruleset
      {:character
       [:what
        [id ::x x]
        [id ::y y]
        :when
        (nf/not-defined? session id ::z)  ; Match when ::z is NOT defined for this id
        :then
        (o/insert! id ::character match)]}))

  ;; Alternative approach using a marker value:
  (def rules
    (o/ruleset
      {:character
       [:what
        [id ::x x]
        [id ::y y]
        [id ::z z]
        :when
        (nf/is-marker? z)  ; Check if z equals the not-defined marker
        :then
        (o/insert! id ::character match)]}))
  "
  (:require [odoyle.rules :as o]))

;; Configuration

(def ^{:dynamic true
       :doc "The marker value used to indicate a fact should not be defined.
            Can be rebound dynamically or set globally using set-not-defined-marker!"}
  *not-defined-marker* :odoyle/not-defined)

(def ^:private global-marker (atom :odoyle/not-defined))

(defn set-not-defined-marker!
  "Sets the global marker value used to indicate a fact should not be defined.
  Default is :odoyle/not-defined.

  Example:
    (set-not-defined-marker! :my-app/absent)"
  [marker]
  (reset! global-marker marker))

(defn get-not-defined-marker
  "Returns the current not-defined marker value."
  []
  (if (= *not-defined-marker* :odoyle/not-defined)
    @global-marker
    *not-defined-marker*))

;; Helper functions for use in rules

(defn not-defined?
  "Returns true if the session does NOT contain a fact with the given id and attribute.
  Use this function in the :when block of a rule.

  Example:
    [:what
     [id ::x x]
     [id ::y y]
     :when
     (not-defined? session id ::z)
     :then
     ...]"
  [session id attr]
  (not (o/contains? session id attr)))

(defn is-marker?
  "Returns true if the value equals the not-defined marker.
  Use this function in the :when block when you have a binding for the value.

  Example:
    [:what
     [id ::x x]
     [id ::y y]
     [id ::z z]  ; Bind z even if it might not exist
     :when
     (is-marker? z)  ; Check if it's marked as not-defined
     :then
     ...]"
  [value]
  (= value (get-not-defined-marker)))

(defn all-not-defined?
  "Returns true if NONE of the given attributes are defined for the given id.

  Example:
    [:what
     [id ::x x]
     :when
     (all-not-defined? session id [::y ::z ::w])
     :then
     ...]"
  [session id attrs]
  (not-any? #(o/contains? session id %) attrs))

(defn any-not-defined?
  "Returns true if AT LEAST ONE of the given attributes is not defined for the given id.

  Example:
    [:what
     [id ::x x]
     :when
     (any-not-defined? session id [::y ::z])
     :then
     ...]"
  [session id attrs]
  (some #(not (o/contains? session id %)) attrs))

;; Advanced: Negation in pattern matching
;; This approach allows you to specify negated patterns more declaratively

(defrecord NegatedPattern [id attr])

(defn ->negated-pattern
  "Creates a negated pattern that can be checked in :when blocks.

  Example:
    (def neg-z (->negated-pattern 'id ::z))

    [:what
     [id ::x x]
     [id ::y y]
     :when
     (matches-negated? session match neg-z)
     :then
     ...]"
  [id-binding attr]
  (->NegatedPattern id-binding attr))

(defn matches-negated?
  "Checks if a negated pattern matches (i.e., the fact does NOT exist).

  Arguments:
    session - The current session
    match   - The match map from the rule
    pattern - A NegatedPattern created with ->negated-pattern"
  [session match pattern]
  (let [id-value (get match (keyword (:id pattern)))]
    (not (o/contains? session id-value (:attr pattern)))))

;; Macro for more ergonomic syntax (Clojure only)

#?(:clj
   (defmacro with-not-defined-marker
     "Temporarily sets the not-defined marker for the body of code.

     Example:
       (with-not-defined-marker :custom/absent
         ;; code that uses the custom marker
         )"
     [marker & body]
     `(binding [*not-defined-marker* ~marker]
        ~@body)))

;; Integration helpers

(defn create-negative-condition
  "Helper to create a :when clause that checks for absence of a fact.
  Returns a function suitable for use in a :when block.

  Example:
    (def check-no-z (create-negative-condition 'id ::z))

    [:what
     [id ::x x]
     [id ::y y]
     :when
     (check-no-z session match)
     :then
     ...]"
  [id-binding attr]
  (fn [session match]
    (let [id-value (get match (keyword id-binding))]
      (not (o/contains? session id-value attr)))))
