(ns odoyle-rules-extensions.accumulators-test
  (:require [clojure.test :refer [deftest is testing]]
            [odoyle.rules :as o]
            [odoyle-rules-extensions.accumulators :as acc]))

(deftest test-count-accumulator
  (testing "Count accumulator counts all matches"
    (let [*count (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::count-players
               [:what
                [id ::type :player]
                [id ::health health]
                :then-finally
                (reset! *count
                  (acc/accumulate session ::count-players
                    (acc/count)))]}))
          (o/insert ::player1 {::type :player ::health 100})
          (o/insert ::player2 {::type :player ::health 80})
          (o/insert ::player3 {::type :player ::health 60})
          (o/insert ::enemy1 {::type :enemy ::health 50})
          o/fire-rules)
      (is (= 3 @*count)))))

(deftest test-sum-accumulator
  (testing "Sum accumulator sums numeric values"
    (let [*total (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::total-health
               [:what
                [id ::type :player]
                [id ::health health]
                :then-finally
                (reset! *total
                  (acc/accumulate session ::total-health
                    (acc/sum :health)))]}))
          (o/insert ::player1 {::type :player ::health 100})
          (o/insert ::player2 {::type :player ::health 80})
          (o/insert ::player3 {::type :player ::health 60})
          o/fire-rules)
      (is (= 240 @*total)))))

(deftest test-avg-accumulator
  (testing "Avg accumulator calculates average"
    (let [*avg (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::avg-score
               [:what
                [id ::type :player]
                [id ::score score]
                :then-finally
                (reset! *avg
                  (acc/accumulate session ::avg-score
                    (acc/avg :score)))]}))
          (o/insert ::player1 {::type :player ::score 100})
          (o/insert ::player2 {::type :player ::score 80})
          (o/insert ::player3 {::type :player ::score 70})
          o/fire-rules)
      (is (= 250/3 @*avg))))

  (testing "Avg accumulator returns nil for no matches"
    (let [*avg (atom ::not-set)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::avg-score
               [:what
                [id ::type :player]
                [id ::score score]
                :then-finally
                (reset! *avg
                  (acc/accumulate session ::avg-score
                    (acc/avg :score)))]}))
          o/fire-rules)
      (is (nil? @*avg)))))

(deftest test-min-max-accumulators
  (testing "Min accumulator finds minimum value"
    (let [*min (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::min-health
               [:what
                [id ::type :player]
                [id ::health health]
                :then-finally
                (reset! *min
                  (acc/accumulate session ::min-health
                    (acc/min :health)))]}))
          (o/insert ::player1 {::type :player ::health 100})
          (o/insert ::player2 {::type :player ::health 30})
          (o/insert ::player3 {::type :player ::health 60})
          o/fire-rules)
      (is (= 30 @*min))))

  (testing "Max accumulator finds maximum value"
    (let [*max (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::max-level
               [:what
                [id ::type :player]
                [id ::level level]
                :then-finally
                (reset! *max
                  (acc/accumulate session ::max-level
                    (acc/max :level)))]}))
          (o/insert ::player1 {::type :player ::level 5})
          (o/insert ::player2 {::type :player ::level 42})
          (o/insert ::player3 {::type :player ::level 10})
          o/fire-rules)
      (is (= 42 @*max)))))

(deftest test-collect-accumulator
  (testing "Collect accumulator gathers values into vector"
    (let [*names (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::player-names
               [:what
                [id ::type :player]
                [id ::name name]
                :then-finally
                (reset! *names
                  (acc/accumulate session ::player-names
                    (acc/collect :name)))]}))
          (o/insert ::player1 {::type :player ::name "Alice"})
          (o/insert ::player2 {::type :player ::name "Bob"})
          (o/insert ::player3 {::type :player ::name "Charlie"})
          o/fire-rules)
      (is (= #{"Alice" "Bob" "Charlie"} (set @*names)))
      (is (= 3 (count @*names))))))

(deftest test-collect-set-accumulator
  (testing "Collect-set accumulator gathers unique values into set"
    (let [*teams (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::unique-teams
               [:what
                [id ::type :player]
                [id ::team team]
                :then-finally
                (reset! *teams
                  (acc/accumulate session ::unique-teams
                    (acc/collect-set :team)))]}))
          (o/insert ::player1 {::type :player ::team :red})
          (o/insert ::player2 {::type :player ::team :blue})
          (o/insert ::player3 {::type :player ::team :red})
          (o/insert ::player4 {::type :player ::team :green})
          o/fire-rules)
      (is (= #{:red :blue :green} @*teams)))))

(deftest test-group-by-accumulator
  (testing "Group-by accumulator groups matches"
    (let [*groups (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::players-by-team
               [:what
                [id ::type :player]
                [id ::team team]
                [id ::name name]
                :then-finally
                (reset! *groups
                  (acc/accumulate session ::players-by-team
                    (acc/group-by :team :name)))]}))
          (o/insert ::player1 {::type :player ::team :red ::name "Alice"})
          (o/insert ::player2 {::type :player ::team :blue ::name "Bob"})
          (o/insert ::player3 {::type :player ::team :red ::name "Charlie"})
          (o/insert ::player4 {::type :player ::team :blue ::name "David"})
          o/fire-rules)
      (is (= 2 (count (:red @*groups))))
      (is (= 2 (count (:blue @*groups))))
      (is (contains? (set (:red @*groups)) "Alice"))
      (is (contains? (set (:red @*groups)) "Charlie")))))

(deftest test-first-last-accumulators
  (testing "First accumulator returns first match value"
    (let [*first (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::first-player
               [:what
                [id ::type :player]
                [id ::name name]
                :then-finally
                (reset! *first
                  (acc/accumulate session ::first-player
                    (acc/first :name)))]}))
          (o/insert ::player1 {::type :player ::name "Alice"})
          (o/insert ::player2 {::type :player ::name "Bob"})
          o/fire-rules)
      (is (some? @*first))
      (is (contains? #{"Alice" "Bob"} @*first))))

  (testing "Last accumulator returns last match value"
    (let [*last (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::last-event
               [:what
                [id ::type :event]
                [id ::timestamp ts]
                :then-finally
                (reset! *last
                  (acc/accumulate session ::last-event
                    (acc/last :timestamp)))]}))
          (o/insert ::event1 {::type :event ::timestamp 1000})
          (o/insert ::event2 {::type :event ::timestamp 2000})
          (o/insert ::event3 {::type :event ::timestamp 3000})
          o/fire-rules)
      (is (some? @*last)))))

(deftest test-custom-accumulator
  (testing "Custom accumulator with median calculation"
    (let [*median (atom nil)
          median-acc (acc/custom
                       (fn [] [])
                       (fn [state match] (conj state (:value match)))
                       (fn [state]
                         (let [sorted (sort state)
                               n (count sorted)]
                           (if (zero? n)
                             nil
                             (nth sorted (quot n 2))))))]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::median-calc
               [:what
                [id ::type :measurement]
                [id ::value value]
                :then-finally
                (reset! *median
                  (acc/accumulate session ::median-calc median-acc))]}))
          (o/insert ::m1 {::type :measurement ::value 10})
          (o/insert ::m2 {::type :measurement ::value 20})
          (o/insert ::m3 {::type :measurement ::value 30})
          (o/insert ::m4 {::type :measurement ::value 40})
          (o/insert ::m5 {::type :measurement ::value 50})
          o/fire-rules)
      (is (= 30 @*median)))))

(deftest test-accumulate-all
  (testing "Accumulate-all computes multiple accumulators at once"
    (let [*stats (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::player-stats
               [:what
                [id ::type :player]
                [id ::health health]
                [id ::score score]
                :then-finally
                (reset! *stats
                  (acc/accumulate-all session ::player-stats
                    {:count        (acc/count)
                     :total-health (acc/sum :health)
                     :avg-score    (acc/avg :score)
                     :max-health   (acc/max :health)
                     :min-score    (acc/min :score)}))]}))
          (o/insert ::player1 {::type :player ::health 100 ::score 85})
          (o/insert ::player2 {::type :player ::health 80 ::score 90})
          (o/insert ::player3 {::type :player ::health 60 ::score 75})
          o/fire-rules)
      (is (= 3 (:count @*stats)))
      (is (= 240 (:total-health @*stats)))
      (is (= 250/3 (:avg-score @*stats)))
      (is (= 100 (:max-health @*stats)))
      (is (= 75 (:min-score @*stats))))))

(deftest test-top-n-accumulator
  (testing "Top-n accumulator returns top N matches"
    (let [*top3 (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::high-scores
               [:what
                [id ::type :player]
                [id ::score score]
                [id ::name name]
                :then-finally
                (reset! *top3
                  (acc/accumulate session ::high-scores
                    (acc/top-n 3 :score)))]}))
          (o/insert ::p1 {::type :player ::score 100 ::name "Alice"})
          (o/insert ::p2 {::type :player ::score 95 ::name "Bob"})
          (o/insert ::p3 {::type :player ::score 90 ::name "Charlie"})
          (o/insert ::p4 {::type :player ::score 85 ::name "David"})
          (o/insert ::p5 {::type :player ::score 80 ::name "Eve"})
          o/fire-rules)
      (is (= 3 (count @*top3)))
      (is (= [100 95 90] (map :score @*top3))))))

(deftest test-bottom-n-accumulator
  (testing "Bottom-n accumulator returns bottom N matches"
    (let [*bottom2 (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::low-health
               [:what
                [id ::type :player]
                [id ::health health]
                [id ::name name]
                :then-finally
                (reset! *bottom2
                  (acc/accumulate session ::low-health
                    (acc/bottom-n 2 :health)))]}))
          (o/insert ::p1 {::type :player ::health 100 ::name "Alice"})
          (o/insert ::p2 {::type :player ::health 20 ::name "Bob"})
          (o/insert ::p3 {::type :player ::health 50 ::name "Charlie"})
          (o/insert ::p4 {::type :player ::health 10 ::name "David"})
          o/fire-rules)
      (is (= 2 (count @*bottom2)))
      (is (= [10 20] (map :health @*bottom2))))))

(deftest test-distinct-count-accumulator
  (testing "Distinct-count accumulator counts unique values"
    (let [*unique-teams (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::team-diversity
               [:what
                [id ::type :player]
                [id ::team team]
                :then-finally
                (reset! *unique-teams
                  (acc/accumulate session ::team-diversity
                    (acc/distinct-count :team)))]}))
          (o/insert ::p1 {::type :player ::team :red})
          (o/insert ::p2 {::type :player ::team :blue})
          (o/insert ::p3 {::type :player ::team :red})
          (o/insert ::p4 {::type :player ::team :green})
          (o/insert ::p5 {::type :player ::team :blue})
          o/fire-rules)
      (is (= 3 @*unique-teams)))))

(deftest test-variance-std-dev-accumulators
  (testing "Variance accumulator calculates variance"
    (let [*variance (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::value-variance
               [:what
                [id ::type :measurement]
                [id ::value value]
                :then-finally
                (reset! *variance
                  (acc/accumulate session ::value-variance
                    (acc/variance :value)))]}))
          (o/insert ::m1 {::type :measurement ::value 10})
          (o/insert ::m2 {::type :measurement ::value 20})
          (o/insert ::m3 {::type :measurement ::value 30})
          o/fire-rules)
      ;; Variance of [10, 20, 30] = ((10-20)^2 + (20-20)^2 + (30-20)^2) / 3 = 200/3
      (is (some? @*variance))
      (is (< (abs (- @*variance 200/3)) 0.0001))))

  (testing "Std-dev accumulator calculates standard deviation"
    (let [*std-dev (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::value-std-dev
               [:what
                [id ::type :measurement]
                [id ::value value]
                :then-finally
                (reset! *std-dev
                  (acc/accumulate session ::value-std-dev
                    (acc/std-dev :value)))]}))
          (o/insert ::m1 {::type :measurement ::value 10})
          (o/insert ::m2 {::type :measurement ::value 20})
          (o/insert ::m3 {::type :measurement ::value 30})
          o/fire-rules)
      (is (some? @*std-dev))
      ;; Should be sqrt(200/3)
      (is (< (abs (- @*std-dev
                     #?(:clj (Math/sqrt (/ 200.0 3))
                        :cljs (js/Math.sqrt (/ 200.0 3)))))
             0.0001)))))

(deftest test-accumulator-with-function-key
  (testing "Accumulators work with function keys instead of keywords"
    (let [*total (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::computed-sum
               [:what
                [id ::type :player]
                [id ::x x]
                [id ::y y]
                :then-finally
                (reset! *total
                  (acc/accumulate session ::computed-sum
                    (acc/sum (fn [m] (+ (:x m) (:y m))))))]}))
          (o/insert ::p1 {::type :player ::x 10 ::y 5})
          (o/insert ::p2 {::type :player ::x 20 ::y 10})
          (o/insert ::p3 {::type :player ::x 30 ::y 15})
          o/fire-rules)
      (is (= 90 @*total)))))

(deftest test-accumulator-updates-on-fact-changes
  (testing "Accumulators reflect updated facts"
    (let [*total (atom nil)]
      (-> (reduce o/add-rule (o/->session)
            (o/ruleset
              {::total-score
               [:what
                [id ::type :player]
                [id ::score score]
                :then-finally
                (reset! *total
                  (acc/accumulate session ::total-score
                    (acc/sum :score)))]}))
          (o/insert ::p1 {::type :player ::score 100})
          (o/insert ::p2 {::type :player ::score 50})
          o/fire-rules
          ((fn [session]
             (is (= 150 @*total))
             session))
          ;; Update p2's score
          (o/insert ::p2 ::score 75)
          o/fire-rules
          ((fn [session]
             (is (= 175 @*total))
             session))))))

;; Helper for floating point comparison
(defn abs [x]
  (if (neg? x) (- x) x))
