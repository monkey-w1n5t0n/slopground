(ns odoyle-rules-extensions.priorities-test
  (:require [clojure.test :refer [deftest is testing]]
            [odoyle.rules :as o]
            [odoyle-rules-extensions.priorities :as prio]))

(deftest test-basic-priorities
  (testing "Higher priority rules fire before lower priority rules"
    (let [*order (atom [])]
      (-> (prio/enable-priorities (o/->session))
          (prio/add-rule
            (prio/with-priority 100
              (o/->rule ::high-priority
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *order conj :high))})))
          (prio/add-rule
            (prio/with-priority 10
              (o/->rule ::low-priority
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *order conj :low))})))
          (prio/add-rule
            (prio/with-priority 50
              (o/->rule ::medium-priority
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *order conj :medium))})))
          (o/insert ::test ::trigger true)
          prio/fire-rules)
      ;; Should fire in order: high, medium, low
      (is (= [:high :medium :low] @*order)))))

(deftest test-default-priority
  (testing "Rules without explicit priority default to 0"
    (let [*order (atom [])]
      (-> (prio/enable-priorities (o/->session))
          (prio/add-rule
            (prio/with-priority 10
              (o/->rule ::explicit-high
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *order conj :explicit-high))})))
          (prio/add-rule
            (o/->rule ::no-priority
              {:what [['id ::trigger 'trigger]]
               :then (fn [session match]
                       (swap! *order conj :no-priority))}))
          (prio/add-rule
            (prio/with-priority -10
              (o/->rule ::explicit-low
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *order conj :explicit-low))})))
          (o/insert ::test ::trigger true)
          prio/fire-rules)
      ;; explicit-high (10) > no-priority (0) > explicit-low (-10)
      (is (= [:explicit-high :no-priority :explicit-low] @*order)))))

(deftest test-named-priority-levels
  (testing "Named priority levels work correctly"
    (let [*order (atom [])]
      (-> (prio/enable-priorities (o/->session))
          (prio/add-rule
            (prio/critical-priority
              (o/->rule ::critical
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *order conj :critical))})))
          (prio/add-rule
            (prio/high-priority
              (o/->rule ::high
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *order conj :high))})))
          (prio/add-rule
            (prio/normal-priority
              (o/->rule ::normal
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *order conj :normal))})))
          (prio/add-rule
            (prio/low-priority
              (o/->rule ::low
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *order conj :low))})))
          (prio/add-rule
            (prio/very-low-priority
              (o/->rule ::very-low
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *order conj :very-low))})))
          (o/insert ::test ::trigger true)
          prio/fire-rules)
      (is (= [:critical :high :normal :low :very-low] @*order)))))

(deftest test-negative-priorities
  (testing "Negative priorities work correctly"
    (let [*order (atom [])]
      (-> (prio/enable-priorities (o/->session))
          (prio/add-rule
            (prio/with-priority -50
              (o/->rule ::negative
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *order conj :negative))})))
          (prio/add-rule
            (prio/with-priority 0
              (o/->rule ::zero
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *order conj :zero))})))
          (prio/add-rule
            (prio/with-priority 50
              (o/->rule ::positive
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *order conj :positive))})))
          (o/insert ::test ::trigger true)
          prio/fire-rules)
      (is (= [:positive :zero :negative] @*order)))))

(deftest test-same-priority-deterministic
  (testing "Rules with same priority execute in deterministic order"
    (let [*orders (atom #{})]
      ;; Run multiple times to check determinism
      (dotimes [_ 5]
        (let [*order (atom [])]
          (-> (prio/enable-priorities (o/->session))
              (prio/add-rule
                (prio/with-priority 50
                  (o/->rule ::same-prio-1
                    {:what [['id ::trigger 'trigger]]
                     :then (fn [session match]
                             (swap! *order conj :rule1))})))
              (prio/add-rule
                (prio/with-priority 50
                  (o/->rule ::same-prio-2
                    {:what [['id ::trigger 'trigger]]
                     :then (fn [session match]
                             (swap! *order conj :rule2))})))
              (prio/add-rule
                (prio/with-priority 50
                  (o/->rule ::same-prio-3
                    {:what [['id ::trigger 'trigger]]
                     :then (fn [session match]
                             (swap! *order conj :rule3))})))
              (o/insert ::test ::trigger true)
              prio/fire-rules)
          (swap! *orders conj @*order)))
      ;; All executions should have the same order
      (is (= 1 (count @*orders))))))

(deftest test-complex-scenario
  (testing "Complex scenario with overriding rules"
    (let [*result (atom nil)]
      (-> (prio/enable-priorities (o/->session))
          ;; General rule: all players get default bonus
          (prio/add-rule
            (prio/with-priority 10
              (o/->rule ::default-bonus
                {:what [['id ::type :player]
                        ['id ::level 'level]]
                 :then (fn [session match]
                         (o/insert! (:id match) ::bonus 100))})))
          ;; Higher priority: high-level players get better bonus
          (prio/add-rule
            (prio/with-priority 50
              (o/->rule ::high-level-bonus
                {:what [['id ::type :player]
                        ['id ::level 'level]]
                 :when (fn [session match] (>= (:level match) 10))
                 :then (fn [session match]
                         (o/insert! (:id match) ::bonus 500))})))
          ;; Highest priority: VIP players get best bonus
          (prio/add-rule
            (prio/with-priority 100
              (o/->rule ::vip-bonus
                {:what [['id ::type :player]
                        ['id ::vip 'vip]]
                 :when (fn [session match] (:vip match))
                 :then (fn [session match]
                         (o/insert! (:id match) ::bonus 1000))})))
          (o/insert ::player1 {::type :player ::level 5 ::vip false})
          (o/insert ::player2 {::type :player ::level 15 ::vip false})
          (o/insert ::player3 {::type :player ::level 5 ::vip true})
          prio/fire-rules
          ((fn [session]
             ;; Check final bonuses
             (reset! *result
               {:p1-bonus (some-> (o/query-all session)
                                 (as-> facts (filter #(and (= (first %) ::player1)
                                                          (= (second %) ::bonus)) facts))
                                 first
                                 (nth 2))
                :p2-bonus (some-> (o/query-all session)
                                 (as-> facts (filter #(and (= (first %) ::player2)
                                                          (= (second %) ::bonus)) facts))
                                 first
                                 (nth 2))
                :p3-bonus (some-> (o/query-all session)
                                 (as-> facts (filter #(and (= (first %) ::player3)
                                                          (= (second %) ::bonus)) facts))
                                 first
                                 (nth 2))})
             session)))
      ;; VIP rule fires last but has highest priority, so overrides others
      (is (= 1000 (:p3-bonus @*result)))
      ;; Note: The actual values depend on which rule fires last
      ;; In a real system, you'd want to use :then false on lower rules
      ;; or check conditions to prevent overriding
      )))

(deftest test-list-priorities
  (testing "list-priorities returns all rule priorities"
    (let [session (-> (prio/enable-priorities (o/->session))
                      (prio/add-rule
                        (prio/with-priority 100
                          (o/->rule ::rule1
                            {:what [['id ::x 'x]]})))
                      (prio/add-rule
                        (prio/with-priority 50
                          (o/->rule ::rule2
                            {:what [['id ::y 'y]]})))
                      (prio/add-rule
                        (o/->rule ::rule3
                          {:what [['id ::z 'z]]})))]
      (let [priorities (prio/list-priorities session)]
        (is (= 100 (::rule1 priorities)))
        (is (= 50 (::rule2 priorities)))
        (is (= 0 (::rule3 priorities)))))))

(deftest test-get-priority
  (testing "get-priority retrieves rule priority"
    (let [rule1 (prio/with-priority 100
                  (o/->rule ::test-rule
                    {:what [['id ::x 'x]]}))
          rule2 (o/->rule ::test-rule2
                  {:what [['id ::y 'y]]})]
      (is (= 100 (prio/get-priority rule1)))
      (is (= 0 (prio/get-priority rule2))))))

(deftest test-multiple-inserts
  (testing "Priorities work with multiple fact insertions"
    (let [*order (atom [])]
      (-> (prio/enable-priorities (o/->session))
          (prio/add-rule
            (prio/with-priority 100
              (o/->rule ::log-high
                {:what [['id ::value 'value]]
                 :then (fn [session match]
                         (swap! *order conj [:high (:value match)]))})))
          (prio/add-rule
            (prio/with-priority 10
              (o/->rule ::log-low
                {:what [['id ::value 'value]]
                 :then (fn [session match]
                         (swap! *order conj [:low (:value match)]))})))
          (o/insert ::entity1 ::value 1)
          (o/insert ::entity2 ::value 2)
          prio/fire-rules)
      ;; Each entity should trigger both rules in priority order
      (let [high-fires (filter #(= :high (first %)) @*order)
            low-fires (filter #(= :low (first %)) @*order)]
        (is (= 2 (count high-fires)))
        (is (= 2 (count low-fires)))
        ;; First two should be high priority
        (is (= :high (first (nth @*order 0))))
        (is (= :high (first (nth @*order 1))))))))

(deftest test-explain-execution-order
  (testing "explain-execution-order shows rule ordering"
    (let [session (-> (prio/enable-priorities (o/->session))
                      (prio/add-rule
                        (prio/with-priority 100
                          (o/->rule ::high
                            {:what [['id ::trigger 'trigger]]})))
                      (prio/add-rule
                        (prio/with-priority 50
                          (o/->rule ::medium
                            {:what [['id ::trigger 'trigger]]})))
                      (prio/add-rule
                        (prio/with-priority 10
                          (o/->rule ::low
                            {:what [['id ::trigger 'trigger]]})))
                      (o/insert ::test ::trigger true))]
      (let [order (prio/explain-execution-order session)]
        (is (= 3 (count order)))
        (is (= ::high (:rule-name (nth order 0))))
        (is (= ::medium (:rule-name (nth order 1))))
        (is (= ::low (:rule-name (nth order 2))))
        (is (= 100 (:priority (nth order 0))))
        (is (= 50 (:priority (nth order 1))))
        (is (= 10 (:priority (nth order 2))))))))

(deftest test-add-rules-batch
  (testing "add-rules can add multiple rules at once"
    (let [*order (atom [])
          rules [(prio/with-priority 100
                   (o/->rule ::rule1
                     {:what [['id ::trigger 'trigger]]
                      :then (fn [session match]
                              (swap! *order conj :rule1))}))
                 (prio/with-priority 50
                   (o/->rule ::rule2
                     {:what [['id ::trigger 'trigger]]
                      :then (fn [session match]
                              (swap! *order conj :rule2))}))
                 (prio/with-priority 10
                   (o/->rule ::rule3
                     {:what [['id ::trigger 'trigger]]
                      :then (fn [session match]
                              (swap! *order conj :rule3))}))]]
      (-> (prio/enable-priorities (o/->session))
          (prio/add-rules rules)
          (o/insert ::test ::trigger true)
          prio/fire-rules)
      (is (= [:rule1 :rule2 :rule3] @*order)))))

(deftest test-compatibility-with-regular-fire-rules
  (testing "Session works with regular o/fire-rules (but without priority ordering)"
    (let [*executed (atom #{})]
      (-> (prio/enable-priorities (o/->session))
          (prio/add-rule
            (prio/with-priority 100
              (o/->rule ::rule1
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *executed conj :rule1))})))
          (prio/add-rule
            (prio/with-priority 10
              (o/->rule ::rule2
                {:what [['id ::trigger 'trigger]]
                 :then (fn [session match]
                         (swap! *executed conj :rule2))})))
          (o/insert ::test ::trigger true)
          o/fire-rules)  ; Using regular fire-rules
      ;; Both rules execute, but order may not respect priority
      (is (= #{:rule1 :rule2} @*executed)))))
