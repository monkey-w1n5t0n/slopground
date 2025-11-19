(ns odoyle-rules-extensions.examples.accumulators-example
  "Examples demonstrating accumulator functions for O'Doyle Rules.

  Accumulators solve the problem of aggregating data across multiple rule matches.
  This is a common need in many domains: gaming, analytics, monitoring, etc."
  (:require [odoyle.rules :as o]
            [odoyle-rules-extensions.accumulators :as acc]))

;; Example 1: Game Statistics Dashboard
;; Calculate multiple statistics about players in real-time

(def game-stats-rules
  (o/ruleset
    {:player-statistics
     [:what
      [id ::player-type :active]
      [id ::health health]
      [id ::score score]
      [id ::level level]
      :then-finally
      (let [stats (acc/accumulate-all session ::player-statistics
                    {:total-players (acc/count)
                     :total-health  (acc/sum :health)
                     :avg-health    (acc/avg :health)
                     :total-score   (acc/sum :score)
                     :avg-score     (acc/avg :score)
                     :max-level     (acc/max :level)
                     :min-level     (acc/min :level)})]
        (println "\n=== Player Statistics ===")
        (println "Total Players:" (:total-players stats))
        (println "Total Health:" (:total-health stats))
        (println "Average Health:" (double (:avg-health stats)))
        (println "Total Score:" (:total-score stats))
        (println "Average Score:" (double (:avg-score stats)))
        (println "Level Range:" (:min-level stats) "-" (:max-level stats))
        (o/insert! ::dashboard ::stats stats))]}))

(defn example-1-game-stats []
  (println "\n========== Example 1: Game Statistics ==========")
  (-> (reduce o/add-rule (o/->session) game-stats-rules)
      (o/insert ::player1 {::player-type :active
                           ::health 100
                           ::score 1500
                           ::level 5})
      (o/insert ::player2 {::player-type :active
                           ::health 75
                           ::score 2300
                           ::level 8})
      (o/insert ::player3 {::player-type :active
                           ::health 90
                           ::score 1800
                           ::level 6})
      (o/insert ::player4 {::player-type :active
                           ::health 50
                           ::score 3200
                           ::level 12})
      o/fire-rules))

;; Example 2: Team Grouping and Analysis
;; Group players by team and analyze each team

(def team-analysis-rules
  (o/ruleset
    {:team-composition
     [:what
      [id ::player-type :active]
      [id ::team team]
      [id ::name name]
      [id ::health health]
      [id ::role role]
      :then-finally
      (let [by-team (acc/accumulate session ::team-composition
                      (acc/group-by :team :name))
            team-health (acc/accumulate session ::team-composition
                          (acc/group-by :team :health))
            team-roles (acc/accumulate session ::team-composition
                         (acc/group-by :team :role))]
        (println "\n=== Team Composition ===")
        (doseq [[team players] by-team]
          (let [healths (get team-health team)
                roles (get team-roles team)
                total-health (reduce + healths)
                avg-health (/ total-health (count healths))]
            (println "\nTeam" team ":")
            (println "  Players:" (clojure.string/join ", " players))
            (println "  Total Health:" total-health)
            (println "  Average Health:" (double avg-health))
            (println "  Roles:" (frequencies roles))))
        (o/insert! ::game ::team-stats by-team))]}))

(defn example-2-team-analysis []
  (println "\n========== Example 2: Team Analysis ==========")
  (-> (reduce o/add-rule (o/->session) team-analysis-rules)
      (o/insert ::p1 {::player-type :active ::team :red
                      ::name "Alice" ::health 100 ::role :tank})
      (o/insert ::p2 {::player-type :active ::team :red
                      ::name "Bob" ::health 80 ::role :healer})
      (o/insert ::p3 {::player-type :active ::team :red
                      ::name "Charlie" ::health 90 ::role :damage})
      (o/insert ::p4 {::player-type :active ::team :blue
                      ::name "David" ::health 85 ::role :tank})
      (o/insert ::p5 {::player-type :active ::team :blue
                      ::name "Eve" ::health 75 ::role :damage})
      (o/insert ::p6 {::player-type :active ::team :blue
                      ::name "Frank" ::health 70 ::role :healer})
      o/fire-rules))

;; Example 3: Leaderboards with Top-N
;; Track top players and worst performing players

(def leaderboard-rules
  (o/ruleset
    {:high-score-board
     [:what
      [id ::player-type :active]
      [id ::name name]
      [id ::score score]
      :then-finally
      (let [top-5 (acc/accumulate session ::high-score-board
                    (acc/top-n 5 :score))
            bottom-3 (acc/accumulate session ::high-score-board
                       (acc/bottom-n 3 :score))]
        (println "\n=== Leaderboard ===")
        (println "\nTop 5 Players:")
        (doseq [[idx player] (map-indexed vector top-5)]
          (println (str "  " (inc idx) ". " (:name player) " - " (:score player))))
        (println "\nNeed Improvement:")
        (doseq [player bottom-3]
          (println (str "  " (:name player) " - " (:score player))))
        (o/insert! ::game ::leaderboard top-5))]}))

(defn example-3-leaderboards []
  (println "\n========== Example 3: Leaderboards ==========")
  (-> (reduce o/add-rule (o/->session) leaderboard-rules)
      (o/insert ::p1 {::player-type :active ::name "Alice" ::score 9500})
      (o/insert ::p2 {::player-type :active ::name "Bob" ::score 8200})
      (o/insert ::p3 {::player-type :active ::name "Charlie" ::score 7800})
      (o/insert ::p4 {::player-type :active ::name "David" ::score 9200})
      (o/insert ::p5 {::player-type :active ::name "Eve" ::score 6500})
      (o/insert ::p6 {::player-type :active ::name "Frank" ::score 8900})
      (o/insert ::p7 {::player-type :active ::name "Grace" ::score 7200})
      (o/insert ::p8 {::player-type :active ::name "Henry" ::score 6100})
      o/fire-rules))

;; Example 4: Monitoring System - Statistical Analysis
;; Calculate variance and standard deviation for monitoring

(def monitoring-rules
  (o/ruleset
    {:response-time-analysis
     [:what
      [id ::measurement-type :response-time]
      [id ::value value]
      [id ::timestamp timestamp]
      :then-finally
      (let [stats (acc/accumulate-all session ::response-time-analysis
                    {:count    (acc/count)
                     :avg      (acc/avg :value)
                     :min      (acc/min :value)
                     :max      (acc/max :value)
                     :variance (acc/variance :value)
                     :std-dev  (acc/std-dev :value)
                     :all      (acc/collect :value)})]
        (println "\n=== Response Time Analysis ===")
        (println "Sample Count:" (:count stats))
        (println "Average:" (format "%.2f ms" (double (:avg stats))))
        (println "Min:" (:min stats) "ms")
        (println "Max:" (:max stats) "ms")
        (println "Variance:" (format "%.2f" (double (:variance stats))))
        (println "Std Dev:" (format "%.2f" (double (:std-dev stats))))
        (println "All values:" (:all stats))
        (when (> (:std-dev stats) 50)
          (println "\n⚠ WARNING: High variance detected!"))
        (o/insert! ::monitor ::response-stats stats))]}))

(defn example-4-monitoring []
  (println "\n========== Example 4: Monitoring System ==========")
  (-> (reduce o/add-rule (o/->session) monitoring-rules)
      (o/insert ::m1 {::measurement-type :response-time
                      ::value 45 ::timestamp 1000})
      (o/insert ::m2 {::measurement-type :response-time
                      ::value 52 ::timestamp 1100})
      (o/insert ::m3 {::measurement-type :response-time
                      ::value 48 ::timestamp 1200})
      (o/insert ::m4 {::measurement-type :response-time
                      ::value 150 ::timestamp 1300})  ; Outlier
      (o/insert ::m5 {::measurement-type :response-time
                      ::value 51 ::timestamp 1400})
      (o/insert ::m6 {::measurement-type :response-time
                      ::value 47 ::timestamp 1500})
      o/fire-rules))

;; Example 5: Custom Accumulator - Median Calculation
;; Implement a custom accumulator for median

(defn median-accumulator []
  (acc/custom
    (fn [] [])  ; init: empty vector
    (fn [state match] (conj state (:value match)))  ; step: add value
    (fn [state]  ; finish: calculate median
      (let [sorted (sort state)
            n (count sorted)]
        (cond
          (zero? n) nil
          (odd? n) (nth sorted (quot n 2))
          :else (/ (+ (nth sorted (quot n 2))
                      (nth sorted (dec (quot n 2))))
                   2))))))

(def custom-acc-rules
  (o/ruleset
    {:salary-analysis
     [:what
      [id ::employee-type :active]
      [id ::name name]
      [id ::salary salary]
      :then-finally
      (let [median (acc/accumulate session ::salary-analysis
                     (median-accumulator))
            avg (acc/accumulate session ::salary-analysis
                  (acc/avg :salary))
            salaries (acc/accumulate session ::salary-analysis
                       (acc/collect :salary))]
        (println "\n=== Salary Analysis ===")
        (println "All salaries:" (sort salaries))
        (println "Median salary:" median)
        (println "Average salary:" (double avg))
        (println "Difference (avg - median):" (- (double avg) median))
        (o/insert! ::hr ::salary-stats {:median median :avg avg}))]}))

(defn example-5-custom-accumulator []
  (println "\n========== Example 5: Custom Accumulator (Median) ==========")
  (-> (reduce o/add-rule (o/->session) custom-acc-rules)
      (o/insert ::e1 {::employee-type :active ::name "Alice" ::salary 50000})
      (o/insert ::e2 {::employee-type :active ::name "Bob" ::salary 55000})
      (o/insert ::e3 {::employee-type :active ::name "Charlie" ::salary 60000})
      (o/insert ::e4 {::employee-type :active ::name "David" ::salary 120000})  ; Outlier
      (o/insert ::e5 {::employee-type :active ::name "Eve" ::salary 58000})
      o/fire-rules))

;; Example 6: Distinct Count - Diversity Metrics
;; Count unique values for diversity analysis

(def diversity-rules
  (o/ruleset
    {:skill-diversity
     [:what
      [id ::employee-type :active]
      [id ::name name]
      [id ::department dept]
      [id ::skill-set skills]
      :then-finally
      (let [unique-depts (acc/accumulate session ::skill-diversity
                           (acc/distinct-count :department))
            all-depts (acc/accumulate session ::skill-diversity
                        (acc/collect-set :department))
            dept-distribution (acc/accumulate session ::skill-diversity
                                (acc/group-by :department :name))]
        (println "\n=== Organizational Diversity ===")
        (println "Number of Departments:" unique-depts)
        (println "Departments:" all-depts)
        (println "\nDepartment Distribution:")
        (doseq [[dept employees] dept-distribution]
          (println (str "  " dept ": " (count employees) " employees - "
                       (clojure.string/join ", " employees))))
        (o/insert! ::org ::diversity-metrics
          {:dept-count unique-depts
           :distribution dept-distribution}))]}))

(defn example-6-diversity-metrics []
  (println "\n========== Example 6: Diversity Metrics ==========")
  (-> (reduce o/add-rule (o/->session) diversity-rules)
      (o/insert ::e1 {::employee-type :active ::name "Alice"
                      ::department :engineering ::skill-set [:clojure :sql]})
      (o/insert ::e2 {::employee-type :active ::name "Bob"
                      ::department :engineering ::skill-set [:java :python]})
      (o/insert ::e3 {::employee-type :active ::name "Charlie"
                      ::department :sales ::skill-set [:negotiation]})
      (o/insert ::e4 {::employee-type :active ::name "David"
                      ::department :marketing ::skill-set [:seo :content]})
      (o/insert ::e5 {::employee-type :active ::name "Eve"
                      ::department :engineering ::skill-set [:rust :go]})
      (o/insert ::e6 {::employee-type :active ::name "Frank"
                      ::department :sales ::skill-set [:crm]})
      o/fire-rules))

;; Example 7: Dynamic Updates - Real-time Accumulation
;; Show how accumulators update as facts change

(def dynamic-acc-rules
  (o/ruleset
    {:live-score-tracking
     [:what
      [id ::player-type :active]
      [id ::name name]
      [id ::score score]
      :then-finally
      (let [stats (acc/accumulate-all session ::live-score-tracking
                    {:total (acc/sum :score)
                     :avg   (acc/avg :score)
                     :high  (acc/max :score)
                     :count (acc/count)})]
        (println "\n--- Live Score Update ---")
        (println "Players:" (:count stats))
        (println "Total Points:" (:total stats))
        (println "Average:" (double (:avg stats)))
        (println "High Score:" (:high stats)))]}))

(defn example-7-dynamic-updates []
  (println "\n========== Example 7: Dynamic Updates ==========")
  (-> (reduce o/add-rule (o/->session) dynamic-acc-rules)
      (o/insert ::p1 {::player-type :active ::name "Alice" ::score 100})
      (o/insert ::p2 {::player-type :active ::name "Bob" ::score 150})
      o/fire-rules
      ((fn [session]
         (println "\n>>> Adding new player...")
         session))
      (o/insert ::p3 {::player-type :active ::name "Charlie" ::score 200})
      o/fire-rules
      ((fn [session]
         (println "\n>>> Updating Bob's score...")
         session))
      (o/insert ::p2 ::score 250)
      o/fire-rules))

;; Run all examples

(defn run-all-examples []
  (println "=" 60)
  (println "ACCUMULATOR FUNCTIONS EXAMPLES")
  (println "=" 60)

  (example-1-game-stats)
  (example-2-team-analysis)
  (example-3-leaderboards)
  (example-4-monitoring)
  (example-5-custom-accumulator)
  (example-6-diversity-metrics)
  (example-7-dynamic-updates)

  (println "\n" (apply str (repeat 60 "=")))
  (println "All examples completed!")
  (println (apply str (repeat 60 "="))))

;; Uncomment to run when loaded
;; (run-all-examples)
