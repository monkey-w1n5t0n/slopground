(ns odoyle-rules-extensions.modules-test
  (:require [clojure.test :refer [deftest is testing]]
            [odoyle.rules :as o]
            [odoyle-rules-extensions.modules :as mod]))

(deftest test-register-and-enable
  (testing "Can register and enable modules"
    (let [*combat-fired (atom false)
          *trading-fired (atom false)
          combat-rules [(o/->rule ::attack {:what [['id ::trigger 'x]]
                                            :then (fn [s m] (reset! *combat-fired true))})]
          trading-rules [(o/->rule ::trade {:what [['id ::trigger 'x]]
                                            :then (fn [s m] (reset! *trading-fired true))})]
          session (-> (mod/enable-modules (o/->session))
                      (mod/register-module :combat combat-rules)
                      (mod/register-module :trading trading-rules)
                      (mod/enable :combat)
                      (o/insert ::test ::trigger true)
                      o/fire-rules)]
      (is @*combat-fired)
      (is (not @*trading-fired)))))

(deftest test-toggle
  (testing "Can toggle modules on/off"
    (let [*fired (atom 0)
          rules [(o/->rule ::test {:what [['id ::x 'x]]
                                   :then (fn [s m] (swap! *fired inc))})]
          session (-> (mod/enable-modules (o/->session))
                      (mod/register-module :test rules)
                      (mod/enable :test)
                      (o/insert ::e1 ::x 1)
                      o/fire-rules
                      (mod/toggle :test)
                      (o/insert ::e2 ::x 2)
                      o/fire-rules)]
      (is (= 1 @*fired)))))

(deftest test-list-modules
  (testing "Can list all modules"
    (let [session (-> (mod/enable-modules (o/->session))
                      (mod/register-module :m1 [(o/->rule ::r1 {:what [['id ::x 'x]]})])
                      (mod/register-module :m2 [(o/->rule ::r2 {:what [['id ::y 'y]]})])
                      (mod/enable :m1))
          modules (mod/list-modules session)]
      (is (get-in modules [:m1 :enabled]))
      (is (not (get-in modules [:m2 :enabled])))
      (is (= 1 (get-in modules [:m1 :rule-count]))))))

(deftest test-enable-set
  (testing "Can enable a set of modules, disabling others"
    (let [session (-> (mod/enable-modules (o/->session))
                      (mod/register-module :m1 [(o/->rule ::r1 {:what [['id ::x 'x]]})])
                      (mod/register-module :m2 [(o/->rule ::r2 {:what [['id ::y 'y]]})])
                      (mod/register-module :m3 [(o/->rule ::r3 {:what [['id ::z 'z]]})])
                      (mod/enable :m1)
                      (mod/enable :m2)
                      (mod/enable-set [:m2 :m3]))]
      (is (not (mod/enabled? session :m1)))
      (is (mod/enabled? session :m2))
      (is (mod/enabled? session :m3)))))
