(ns odoyle-rules-extensions.profiling-test
  (:require [clojure.test :refer [deftest is testing]]
            [odoyle.rules :as o]
            [odoyle-rules-extensions.profiling :as prof]))

(deftest test-basic-profiling
  (testing "Profiling tracks rule firings"
    (let [session (-> (prof/enable-profiling (o/->session))
                      (reduce o/add-rule
                        (o/ruleset
                          {::test-rule
                           [:what
                            [id ::x x]]}))
                      (prof/insert ::e1 ::x 1)
                      prof/fire-rules
                      (prof/insert ::e2 ::x 2)
                      prof/fire-rules)]
      (is (prof/enabled? session))
      (is (= 2 (:fire-count (prof/stats session)))))))

(deftest test-fact-tracking
  (testing "Profiling tracks fact operations"
    (let [session (-> (prof/enable-profiling (o/->session))
                      (prof/insert ::e1 ::x 1)
                      (prof/insert ::e1 ::y 2)
                      (prof/retract ::e1 ::x))]
      (is (= 2 (get-in (prof/stats session) [:fact-stats [::e1 ::x] :inserts])))
      (is (= 1 (get-in (prof/stats session) [:fact-stats [::e1 ::x] :retracts]))))))
