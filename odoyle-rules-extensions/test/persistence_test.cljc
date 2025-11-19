(ns odoyle-rules-extensions.persistence-test
  (:require [clojure.test :refer [deftest is testing]]
            [odoyle.rules :as o]
            [odoyle-rules-extensions.persistence :as persist]))

(deftest test-save-load
  (testing "Can save and restore session"
    (let [rules (o/ruleset {::test [:what [id ::x x]]})
          session (-> (reduce o/add-rule (o/->session) rules)
                      (o/insert ::e1 ::x 1)
                      (o/insert ::e2 ::y 2))
          saved (persist/save session)
          restored (persist/load saved rules)]
      (is (= 2 (count (:facts saved))))
      (is (o/contains? restored ::e1 ::x))
      (is (o/contains? restored ::e2 ::y)))))

(deftest test-checkpoints
  (testing "Can create and rollback to checkpoints"
    (let [rules (o/ruleset {::test [:what [id ::x x]]})
          session (-> (reduce o/add-rule (o/->session) rules)
                      (o/insert ::e1 ::x 1)
                      (persist/checkpoint :point1)
                      (o/insert ::e2 ::y 2))
          restored (persist/rollback session :point1 rules)]
      (is (contains? (set (persist/list-checkpoints session)) :point1))
      (is (o/contains? restored ::e1 ::x))
      (is (not (o/contains? restored ::e2 ::y))))))
