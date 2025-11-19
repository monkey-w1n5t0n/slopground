(ns odoyle-rules-extensions.quantifiers-test
  (:require [clojure.test :refer [deftest is testing]]
            [odoyle.rules :as o]
            [odoyle-rules-extensions.quantifiers :as quant]))

(deftest test-exists
  (testing "exists? checks if any match exists"
    (let [session (-> (reduce o/add-rule (o/->session)
                        (o/ruleset
                          {::players
                           [:what
                            [id ::type :player]
                            [id ::name name]
                            [id ::health health]]}))
                      (o/insert ::p1 {::type :player ::name "Alice" ::health 100})
                      (o/insert ::p2 {::type :player ::name "Bob" ::health 50})
                      o/fire-rules)]
      (is (quant/exists? session ::players))
      (is (quant/exists? session ::players #(> (:health %) 75)))
      (is (not (quant/exists? session ::players #(< (:health %) 10)))))))

(deftest test-forall
  (testing "forall? checks if all matches satisfy predicate"
    (let [session (-> (reduce o/add-rule (o/->session)
                        (o/ruleset
                          {::players
                           [:what
                            [id ::type :player]
                            [id ::ready ready]]}))
                      (o/insert ::p1 {::type :player ::ready true})
                      (o/insert ::p2 {::type :player ::ready true})
                      o/fire-rules)]
      (is (quant/forall? session ::players :ready))
      (is (not (-> session
                   (o/insert ::p3 {::type :player ::ready false})
                   o/fire-rules
                   (quant/forall? ::players :ready)))))))

(deftest test-none
  (testing "none? checks if no matches satisfy predicate"
    (let [session (-> (reduce o/add-rule (o/->session)
                        (o/ruleset
                          {::players
                           [:what
                            [id ::type :player]
                            [id ::banned banned]]}))
                      (o/insert ::p1 {::type :player ::banned false})
                      (o/insert ::p2 {::type :player ::banned false})
                      o/fire-rules)]
      (is (quant/none? session ::players :banned)))))

(deftest test-count-where
  (testing "count-where counts matches satisfying predicate"
    (let [session (-> (reduce o/add-rule (o/->session)
                        (o/ruleset
                          {::players
                           [:what
                            [id ::type :player]
                            [id ::level level]]}))
                      (o/insert ::p1 {::type :player ::level 5})
                      (o/insert ::p2 {::type :player ::level 15})
                      (o/insert ::p3 {::type :player ::level 25})
                      o/fire-rules)]
      (is (= 2 (quant/count-where session ::players #(> (:level %) 10)))))))

(deftest test-exists-exactly-one
  (testing "exists-exactly-one? checks for single match"
    (let [session (-> (reduce o/add-rule (o/->session)
                        (o/ruleset
                          {::admins
                           [:what
                            [id ::role :admin]
                            [id ::name name]]}))
                      (o/insert ::admin1 {::role :admin ::name "Boss"})
                      o/fire-rules)]
      (is (quant/exists-exactly-one? session ::admins)))))

(deftest test-exists-at-least
  (testing "exists-at-least? checks minimum count"
    (let [session (-> (reduce o/add-rule (o/->session)
                        (o/ruleset
                          {::players
                           [:what
                            [id ::type :player]
                            [id ::name name]]}))
                      (o/insert ::p1 {::type :player ::name "A"})
                      (o/insert ::p2 {::type :player ::name "B"})
                      (o/insert ::p3 {::type :player ::name "C"})
                      o/fire-rules)]
      (is (quant/exists-at-least? session ::players 3))
      (is (quant/exists-at-least? session ::players 2))
      (is (not (quant/exists-at-least? session ::players 5))))))

(deftest test-exists-between
  (testing "exists-between? checks range"
    (let [session (-> (reduce o/add-rule (o/->session)
                        (o/ruleset
                          {::team
                           [:what
                            [id ::team-member true]
                            [id ::name name]]}))
                      (o/insert ::p1 {::team-member true ::name "A"})
                      (o/insert ::p2 {::team-member true ::name "B"})
                      (o/insert ::p3 {::team-member true ::name "C"})
                      o/fire-rules)]
      (is (quant/exists-between? session ::team 2 5))
      (is (not (quant/exists-between? session ::team 5 10))))))

(deftest test-majority
  (testing "majority? checks if more than half satisfy predicate"
    (let [session (-> (reduce o/add-rule (o/->session)
                        (o/ruleset
                          {::voters
                           [:what
                            [id ::voter true]
                            [id ::voted voted]]}))
                      (o/insert ::v1 {::voter true ::voted true})
                      (o/insert ::v2 {::voter true ::voted true})
                      (o/insert ::v3 {::voter true ::voted true})
                      (o/insert ::v4 {::voter true ::voted false})
                      o/fire-rules)]
      (is (quant/majority? session ::voters :voted)))))

(deftest test-percentage-where
  (testing "percentage-where calculates percentage"
    (let [session (-> (reduce o/add-rule (o/->session)
                        (o/ruleset
                          {::players
                           [:what
                            [id ::type :player]
                            [id ::online online]]}))
                      (o/insert ::p1 {::type :player ::online true})
                      (o/insert ::p2 {::type :player ::online true})
                      (o/insert ::p3 {::type :player ::online true})
                      (o/insert ::p4 {::type :player ::online false})
                      o/fire-rules)]
      (is (= 75.0 (quant/percentage-where session ::players :online))))))

(deftest test-in-rules
  (testing "Quantifiers work in rule :when blocks"
    (let [*game-started (atom false)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::all-players
               [:what
                [id ::type :player]
                [id ::ready ready]]

               ::start-game
               [:what
                [game ::type :game]
                :when
                (quant/forall? session ::all-players :ready)
                (quant/exists-at-least? session ::all-players 2)
                :then
                (reset! *game-started true)]}))
          (o/insert ::game1 {::type :game})
          (o/insert ::p1 {::type :player ::ready true})
          (o/insert ::p2 {::type :player ::ready true})
          (o/insert ::p3 {::type :player ::ready true})
          o/fire-rules)
      (is @*game-started))))

(deftest test-exists-more-than
  (testing "exists-more-than? compares counts"
    (let [session (-> (reduce o/add-rule (o/->session)
                        (o/ruleset
                          {::players
                           [:what
                            [id ::type :player]
                            [id ::team team]]}))
                      (o/insert ::p1 {::type :player ::team :red})
                      (o/insert ::p2 {::type :player ::team :red})
                      (o/insert ::p3 {::type :player ::team :red})
                      (o/insert ::p4 {::type :player ::team :blue})
                      o/fire-rules)]
      (is (quant/exists-more-than? session ::players
            #(= (:team %) :red)
            #(= (:team %) :blue))))))
