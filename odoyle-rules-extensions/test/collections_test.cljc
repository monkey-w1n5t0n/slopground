(ns odoyle-rules-extensions.collections-test
  (:require [clojure.test :refer [deftest is testing]]
            [odoyle.rules :as o]
            [odoyle-rules-extensions.collections :as coll]))

(deftest test-contains
  (testing "contains? checks for value in collection"
    (is (coll/contains? [:a :b :c] :b))
    (is (coll/contains? #{:x :y :z} :y))
    (is (not (coll/contains? [:a :b] :c)))))

(deftest test-contains-all
  (testing "contains-all? checks all values present"
    (is (coll/contains-all? [:a :b :c :d] [:a :c]))
    (is (coll/contains-all? #{:x :y :z} [:x :y]))
    (is (not (coll/contains-all? [:a :b] [:a :c])))))

(deftest test-contains-any
  (testing "contains-any? checks at least one value present"
    (is (coll/contains-any? [:a :b :c] [:x :a]))
    (is (coll/contains-any? #{:red :blue} [:green :red]))
    (is (not (coll/contains-any? [:a :b] [:x :y])))))

(deftest test-contains-none
  (testing "contains-none? checks no values present"
    (is (coll/contains-none? [:a :b] [:x :y]))
    (is (not (coll/contains-none? [:a :b] [:a :x])))))

(deftest test-count-operations
  (testing "count comparison operations"
    (is (coll/count-eq? [:a :b :c] 3))
    (is (coll/count-gt? [:a :b :c :d] 3))
    (is (coll/count-gte? [:a :b :c] 3))
    (is (coll/count-lt? [:a :b] 3))
    (is (coll/count-lte? [:a :b :c] 3))))

(deftest test-empty-operations
  (testing "empty? and not-empty?"
    (is (coll/empty? []))
    (is (coll/not-empty? [:a]))
    (is (not (coll/empty? #{:x})))
    (is (not (coll/not-empty? #{})))))

(deftest test-set-operations
  (testing "subset?, superset?, disjoint?, intersects?"
    (is (coll/subset? #{:a :b} #{:a :b :c}))
    (is (coll/superset? #{:a :b :c} #{:a :b}))
    (is (coll/disjoint? #{:a :b} #{:c :d}))
    (is (coll/intersects? #{:a :b :c} #{:c :d}))
    (is (not (coll/disjoint? #{:a :b} #{:b :c})))
    (is (not (coll/intersects? #{:a :b} #{:c :d})))))

(deftest test-map-operations
  (testing "has-key?, has-keys?, has-any-key?, has-value?"
    (is (coll/has-key? {:a 1 :b 2} :a))
    (is (coll/has-keys? {:a 1 :b 2 :c 3} [:a :c]))
    (is (coll/has-any-key? {:a 1 :b 2} [:c :a]))
    (is (coll/has-value? {:a 1 :b 2} 2))
    (is (not (coll/has-key? {:a 1} :b)))
    (is (not (coll/has-value? {:a 1} 2)))))

(deftest test-get-in
  (testing "get-in? checks nested paths"
    (is (coll/get-in? {:a {:b {:c 10}}} [:a :b :c]))
    (is (coll/get-in? {:a {:b {:c 10}}} [:a :b :c] 10))
    (is (coll/get-in? {:a {:b {:c 10}}} [:a :b :c] #(> % 5)))
    (is (not (coll/get-in? {:a {:b {}}} [:a :b :c])))
    (is (not (coll/get-in? {:a {:b {:c 10}}} [:a :b :c] 20)))))

(deftest test-nth
  (testing "nth? checks indexed access"
    (is (coll/nth? [:a :b :c] 1))
    (is (coll/nth? [:a :b :c] 1 :b))
    (is (coll/nth? [:a :b :c] 2 #(= % :c)))
    (is (not (coll/nth? [:a :b] 5)))
    (is (not (coll/nth? [:a :b :c] 1 :wrong)))))

(deftest test-first-last
  (testing "first? and last? checks"
    (is (coll/first? [:a :b :c]))
    (is (coll/first? [:a :b :c] :a))
    (is (coll/first? [:a :b :c] #(= % :a)))
    (is (coll/last? [:a :b :c]))
    (is (coll/last? [:a :b :c] :c))
    (is (not (coll/first? [])))
    (is (not (coll/last? [])))))

(deftest test-predicate-operations
  (testing "any?, all?, none?"
    (is (coll/any? even? [1 3 5 6 7]))
    (is (coll/all? even? [2 4 6 8]))
    (is (coll/none? even? [1 3 5 7]))
    (is (not (coll/any? even? [1 3 5])))
    (is (not (coll/all? even? [2 3 4])))
    (is (not (coll/none? even? [1 2 3])))))

(deftest test-count-where
  (testing "count-where counts matching elements"
    (is (= 3 (coll/count-where even? [1 2 3 4 5 6])))
    (is (= 0 (coll/count-where even? [1 3 5])))
    (is (= 4 (coll/count-where #(> % 5) [1 6 7 3 8 9])))))

(deftest test-distinct-values
  (testing "distinct-values? checks uniqueness"
    (is (coll/distinct-values? [:a :b :c]))
    (is (coll/distinct-values? #{:x :y :z}))
    (is (not (coll/distinct-values? [:a :b :a])))
    (is (not (coll/distinct-values? [1 2 3 2])))))

(deftest test-in-rules-inventory
  (testing "Collection checks work in rule :when blocks - inventory check"
    (let [*matched (atom [])]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::ready-for-quest
               [:what
                [player ::inventory inv]
                [player ::name name]
                :when
                (coll/contains-all? inv [:sword :shield :potion])
                :then
                (swap! *matched conj name)]}))
          (o/insert ::player1 {::name "Alice"
                               ::inventory [:sword :shield :potion :map]})
          (o/insert ::player2 {::name "Bob"
                               ::inventory [:sword :map]})
          (o/insert ::player3 {::name "Charlie"
                               ::inventory [:sword :shield :potion :armor]})
          o/fire-rules)
      (is (= 2 (count @*matched)))
      (is (contains? (set @*matched) "Alice"))
      (is (contains? (set @*matched) "Charlie")))))

(deftest test-in-rules-skills
  (testing "Collection checks work in rule :when blocks - skill check"
    (let [*mages (atom [])]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::is-mage
               [:what
                [char ::skills skills]
                [char ::name name]
                :when
                (coll/has-any? skills #{:fire-magic :ice-magic :lightning-magic})
                :then
                (swap! *mages conj name)]}))
          (o/insert ::char1 {::name "Wizard"
                             ::skills #{:fire-magic :teleport}})
          (o/insert ::char2 {::name "Warrior"
                             ::skills #{:sword-mastery :shield-bash}})
          (o/insert ::char3 {::name "Sorcerer"
                             ::skills #{:ice-magic :enchant}})
          o/fire-rules)
      (is (= 2 (count @*mages)))
      (is (contains? (set @*mages) "Wizard"))
      (is (contains? (set @*mages) "Sorcerer")))))

(deftest test-in-rules-nested-data
  (testing "Collection checks work with nested data"
    (let [*valid (atom [])]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::valid-config
               [:what
                [id ::config cfg]
                [id ::name name]
                :when
                (coll/get-in? cfg [:database :port] #(> % 1000))
                (coll/has-keys? cfg [:database :cache])
                :then
                (swap! *valid conj name)]}))
          (o/insert ::app1 {::name "App1"
                            ::config {:database {:host "localhost" :port 5432}
                                     :cache {:ttl 300}}})
          (o/insert ::app2 {::name "App2"
                            ::config {:database {:host "localhost" :port 80}
                                     :cache {:ttl 300}}})
          (o/insert ::app3 {::name "App3"
                            ::config {:database {:host "localhost" :port 3306}
                                     :logger {:level :info}}})
          o/fire-rules)
      (is (= ["App1"] @*valid)))))

(deftest test-in-rules-count-checks
  (testing "Count checks work in rules"
    (let [*small-teams (atom [])]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::understaffed-team
               [:what
                [team ::members members]
                [team ::name name]
                :when
                (coll/count-lt? members 3)
                :then
                (swap! *small-teams conj name)]}))
          (o/insert ::team1 {::name "Alpha" ::members [:alice :bob]})
          (o/insert ::team2 {::name "Beta" ::members [:charlie :david :eve]})
          (o/insert ::team3 {::name "Gamma" ::members [:frank]})
          o/fire-rules)
      (is (= 2 (count @*small-teams)))
      (is (contains? (set @*small-teams) "Alpha"))
      (is (contains? (set @*small-teams) "Gamma")))))

(deftest test-has-capability
  (testing "has-capability? helper works correctly"
    (is (coll/has-capability? #{:can-fly :can-swim :can-run} [:can-fly]))
    (is (coll/has-capability? #{:admin :write :read} [:write :read]))
    (is (not (coll/has-capability? #{:read} [:write])))))

(deftest test-missing-requirements
  (testing "missing-requirements returns what's missing"
    (is (= '(:shield :potion)
           (coll/missing-requirements? [:sword] [:sword :shield :potion])))
    (is (clojure.core/empty?
         (coll/missing-requirements? [:a :b :c] [:a :b])))))

(deftest test-composite-patterns
  (testing "Complex patterns with multiple collection checks"
    (let [*qualified (atom [])]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::qualified-player
               [:what
                [player ::name name]
                [player ::inventory inv]
                [player ::skills skills]
                [player ::achievements achievements]
                :when
                ;; Must have basic equipment
                (coll/contains-all? inv [:sword :armor])
                ;; Must have at least one magic skill
                (coll/has-any? skills #{:fire :ice :lightning})
                ;; Must have at least 5 achievements
                (coll/count-gte? achievements 5)
                ;; Must not have any forbidden items
                (coll/contains-none? inv [:cursed-ring :poison-vial])
                :then
                (swap! *qualified conj name)]}))
          (o/insert ::p1 {::name "Alice"
                          ::inventory [:sword :armor :potion]
                          ::skills #{:fire :healing}
                          ::achievements [:quest1 :quest2 :quest3 :quest4 :quest5]})
          (o/insert ::p2 {::name "Bob"
                          ::inventory [:sword :armor :cursed-ring]  ; Has forbidden item
                          ::skills #{:ice}
                          ::achievements [:a :b :c :d :e :f]})
          (o/insert ::p3 {::name "Charlie"
                          ::inventory [:sword :armor]
                          ::skills #{:melee}  ; No magic skills
                          ::achievements [:x :y :z :w :q :r]})
          (o/insert ::p4 {::name "Diana"
                          ::inventory [:sword :armor :map]
                          ::skills #{:lightning :stealth}
                          ::achievements [:q1 :q2 :q3 :q4 :q5 :q6]})
          o/fire-rules)
      (is (= 2 (count @*qualified)))
      (is (contains? (set @*qualified) "Alice"))
      (is (contains? (set @*qualified) "Diana")))))
