(ns odoyle-rules-extensions.negative-facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [odoyle.rules :as o]
            [odoyle-rules-extensions.negative-facts :as nf]))

(deftest test-not-defined-basic
  (testing "Basic negative fact matching using not-defined?"
    (let [*matches (atom [])]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::character
               [:what
                [id ::x x]
                [id ::y y]
                :when
                (nf/not-defined? session id ::z)
                :then
                (swap! *matches conj {:id id :x x :y y})]}))
          (o/insert ::alice ::x 10)
          (o/insert ::alice ::y 20)
          (o/insert ::bob ::x 30)
          (o/insert ::bob ::y 40)
          (o/insert ::bob ::z 50)  ; Bob has z, so shouldn't match
          o/fire-rules)
      ;; Only Alice should match (has x and y, but no z)
      (is (= 1 (count @*matches)))
      (is (= ::alice (:id (first @*matches)))))))

(deftest test-marker-value
  (testing "Using a custom marker value"
    (nf/set-not-defined-marker! ::absent)
    (let [marker (nf/get-not-defined-marker)]
      (is (= ::absent marker))
      (is (nf/is-marker? ::absent))
      (is (not (nf/is-marker? ::present))))
    ;; Reset to default
    (nf/set-not-defined-marker! :odoyle/not-defined)))

(deftest test-all-not-defined
  (testing "Check that multiple attributes are all not defined"
    (let [*matches (atom [])]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::minimal-character
               [:what
                [id ::name name]
                :when
                (nf/all-not-defined? session id [::x ::y ::z])
                :then
                (swap! *matches conj {:id id :name name})]}))
          (o/insert ::alice ::name "Alice")
          (o/insert ::bob ::name "Bob")
          (o/insert ::bob ::x 10)  ; Bob has x, so shouldn't match
          (o/insert ::charlie ::name "Charlie")
          (o/insert ::charlie ::y 20)  ; Charlie has y, so shouldn't match
          o/fire-rules)
      ;; Only Alice should match (has only name, no x/y/z)
      (is (= 1 (count @*matches)))
      (is (= ::alice (:id (first @*matches)))))))

(deftest test-any-not-defined
  (testing "Check that at least one attribute is not defined"
    (let [*matches (atom [])]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::incomplete-character
               [:what
                [id ::name name]
                :when
                (nf/any-not-defined? session id [::x ::y ::z])
                :then
                (swap! *matches conj {:id id :name name})]}))
          (o/insert ::alice ::name "Alice")
          (o/insert ::alice ::x 10)
          (o/insert ::alice ::y 20)
          ;; Alice is missing z, so should match
          (o/insert ::bob ::name "Bob")
          (o/insert ::bob ::x 30)
          (o/insert ::bob ::y 40)
          (o/insert ::bob ::z 50)
          ;; Bob has all attributes, so shouldn't match
          o/fire-rules)
      ;; Only Alice should match
      (is (= 1 (count @*matches)))
      (is (= ::alice (:id (first @*matches)))))))

(deftest test-negated-pattern
  (testing "Using negated patterns"
    (let [*matches (atom [])
          neg-z (nf/->negated-pattern 'id ::z)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::character
               [:what
                [id ::x x]
                [id ::y y]
                :when
                (nf/matches-negated? session match neg-z)
                :then
                (swap! *matches conj {:id id :x x :y y})]}))
          (o/insert ::alice ::x 10)
          (o/insert ::alice ::y 20)
          (o/insert ::bob ::x 30)
          (o/insert ::bob ::y 40)
          (o/insert ::bob ::z 50)
          o/fire-rules)
      (is (= 1 (count @*matches)))
      (is (= ::alice (:id (first @*matches)))))))

(deftest test-create-negative-condition
  (testing "Using create-negative-condition helper"
    (let [*matches (atom [])
          check-no-z (nf/create-negative-condition 'id ::z)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::character
               [:what
                [id ::x x]
                [id ::y y]
                :when
                (check-no-z session match)
                :then
                (swap! *matches conj {:id id :x x :y y})]}))
          (o/insert ::alice ::x 10)
          (o/insert ::alice ::y 20)
          (o/insert ::bob ::x 30)
          (o/insert ::bob ::y 40)
          (o/insert ::bob ::z 50)
          o/fire-rules)
      (is (= 1 (count @*matches)))
      (is (= ::alice (:id (first @*matches)))))))

(deftest test-dynamic-negation
  (testing "Adding and removing facts with negation"
    (let [*matches (atom [])]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::character
               [:what
                [id ::x x]
                [id ::y y]
                :when
                (nf/not-defined? session id ::z)
                :then
                (swap! *matches conj {:id id :x x :y y})]}))
          (o/insert ::alice ::x 10)
          (o/insert ::alice ::y 20)
          o/fire-rules
          ((fn [session]
             ;; Should have matched Alice
             (is (= 1 (count @*matches)))
             (reset! *matches [])
             session))
          ;; Now add ::z to Alice
          (o/insert ::alice ::z 30)
          o/fire-rules
          ((fn [session]
             ;; Alice should no longer match
             (is (= 0 (count @*matches)))
             session))
          ;; Remove ::z from Alice
          (o/retract ::alice ::z)
          o/fire-rules
          ((fn [session]
             ;; Alice should match again
             (is (= 1 (count @*matches)))
             session))))))

;; Example from the user's request
(deftest test-user-example
  (testing "Example matching the user's original request"
    (let [*characters (atom [])]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::character
               [:what
                [id ::x x]
                [id ::y y]
                :when
                ;; This rule only triggers if id has x, y, but NOT z
                (nf/not-defined? session id ::z)
                :then
                (swap! *characters conj {:id id :x x :y y})]}))
          (o/insert ::player1 ::x 100)
          (o/insert ::player1 ::y 200)
          ;; player1 has x and y, but no z - should match
          (o/insert ::player2 ::x 150)
          (o/insert ::player2 ::y 250)
          (o/insert ::player2 ::z 350)
          ;; player2 has x, y, and z - should NOT match
          o/fire-rules)

      (is (= 1 (count @*characters)))
      (is (= ::player1 (:id (first @*characters))))
      (is (= 100 (:x (first @*characters))))
      (is (= 200 (:y (first @*characters)))))))
