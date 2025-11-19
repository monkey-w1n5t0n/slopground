(ns odoyle-rules-extensions.profiling
  "Extension for O'Doyle Rules to support performance profiling and debugging.

  Rationale:
  ---------
  Understanding rule performance is crucial for optimization:
  - Which rules fire most often?
  - Which rules are slowest?
  - How many matches per rule?
  - Rule execution timeline
  - Fact insertion patterns

  Usage:

  (require '[odoyle.rules :as o]
           '[odoyle-rules-extensions.profiling :as prof])

  (def session
    (-> (o/->session)
        prof/enable-profiling))

  ;; Use session normally...
  (-> session
      (o/insert ::player ::health 100)
      prof/fire-rules)  ; Use prof/fire-rules instead of o/fire-rules

  ;; Get statistics
  (prof/stats session)
  (prof/print-report session)"
  (:require [odoyle.rules :as o]))

;; Profiling state

(def ^:private prof-key ::profiling-data)

(defrecord ProfilingData
  [enabled                ; boolean
   rule-stats            ; map of rule-name -> stats
   fact-stats           ; map of [id attr] -> stats
   fire-count          ; total number of fire-rules calls
   total-fire-time-ms  ; total time spent in fire-rules
   ])

(defn enable-profiling
  "Enables profiling for a session.

  Example:
    (prof/enable-profiling session)"
  [session]
  (vary-meta session assoc prof-key
    (->ProfilingData true {} {} 0 0)))

(defn- get-profiling-data [session]
  (get (meta session) prof-key))

(defn- set-profiling-data [session data]
  (vary-meta session assoc prof-key data))

(defn- update-profiling-data [session f & args]
  (set-profiling-data session (apply f (get-profiling-data session) args)))

(defn enabled? [session]
  (boolean (:enabled (get-profiling-data session))))

;; Rule statistics tracking

(defn- update-rule-stats [session rule-name duration-ms]
  (update-profiling-data session
    (fn [data]
      (update data :rule-stats
        (fn [stats]
          (update stats rule-name
            (fn [rule-stat]
              (-> (or rule-stat {:fire-count 0 :total-time-ms 0 :matches 0})
                  (update :fire-count inc)
                  (update :total-time-ms + duration-ms)))))))))

(defn- update-match-count [session rule-name match-count]
  (update-profiling-data session
    (fn [data]
      (update-in data [:rule-stats rule-name :matches]
        (fn [current] (max (or current 0) match-count))))))

;; Fact statistics tracking

(defn- update-fact-stats [session id attr operation]
  (update-profiling-data session
    (fn [data]
      (update data :fact-stats
        (fn [stats]
          (update stats [id attr]
            (fn [fact-stat]
              (-> (or fact-stat {:inserts 0 :retracts 0})
                  (update operation inc)))))))))

;; Wrapped O'Doyle functions

(defn insert
  "Profiling-aware insert. Use instead of o/insert.

  Example:
    (prof/insert session ::player ::health 100)"
  ([session fact]
   (apply insert session fact))
  ([session id attr->value]
   (reduce-kv (fn [s attr value]
                (insert s id attr value))
              session attr->value))
  ([session id attr value]
   (let [session (if (enabled? session)
                   (update-fact-stats session id attr :inserts)
                   session)]
     (o/insert session id attr value))))

(defn retract
  "Profiling-aware retract. Use instead of o/retract.

  Example:
    (prof/retract session ::player ::health)"
  [session id attr]
  (let [session (if (enabled? session)
                  (update-fact-stats session id attr :retracts)
                  session)]
    (o/retract session id attr)))

(defn fire-rules
  "Profiling-aware fire-rules. Use instead of o/fire-rules.

  Example:
    (prof/fire-rules session)"
  ([session]
   (fire-rules session {}))
  ([session opts]
   (if-not (enabled? session)
     (o/fire-rules session opts)
     (let [start-time #?(:clj (System/nanoTime)
                        :cljs (.now js/Date))
           ;; Collect match counts before firing
           session (reduce
                     (fn [s [rule-name _]]
                       (let [match-count (count (o/query-all s rule-name))]
                         (update-match-count s rule-name match-count)))
                     session
                     (:rule-name->node-id session))
           ;; Fire rules
           session (o/fire-rules session opts)
           end-time #?(:clj (System/nanoTime)
                      :cljs (.now js/Date))
           duration-ms #?(:clj (/ (- end-time start-time) 1000000.0)
                         :cljs (- end-time start-time))
           ;; Update profiling data
           session (update-profiling-data session
                     (fn [data]
                       (-> data
                           (update :fire-count inc)
                           (update :total-fire-time-ms + duration-ms))))]
       session))))

;; Statistics and reporting

(defn stats
  "Returns profiling statistics.

  Returns map with:
    :rule-stats - Map of rule-name -> {:fire-count N :total-time-ms N :matches N}
    :fact-stats - Map of [id attr] -> {:inserts N :retracts N}
    :fire-count - Total fire-rules calls
    :total-fire-time-ms - Total time in fire-rules

  Example:
    (prof/stats session)"
  [session]
  (if-let [data (get-profiling-data session)]
    (select-keys data [:rule-stats :fact-stats :fire-count :total-fire-time-ms])
    {}))

(defn rule-stats
  "Returns statistics for a specific rule.

  Example:
    (prof/rule-stats session ::my-rule)"
  [session rule-name]
  (get-in (get-profiling-data session) [:rule-stats rule-name]))

(defn top-rules
  "Returns top N rules by some metric.

  Arguments:
    session - profiled session
    n       - number of top rules to return
    metric  - :fire-count, :total-time-ms, or :matches

  Example:
    (prof/top-rules session 5 :fire-count)"
  [session n metric]
  (let [stats (:rule-stats (get-profiling-data session))]
    (->> stats
         (sort-by (fn [[_ s]] (get s metric 0)) >)
         (take n)
         (into {}))))

(defn print-report
  "Prints a formatted profiling report.

  Example:
    (prof/print-report session)"
  [session]
  (let [data (get-profiling-data session)]
    (println "\n========== Profiling Report ==========")
    (println "\nOverview:")
    (println "  Fire-rules calls:" (:fire-count data))
    (println "  Total time:" (format "%.2fms" (:total-fire-time-ms data)))
    (when (pos? (:fire-count data))
      (println "  Avg time/call:" (format "%.2fms"
                                         (/ (:total-fire-time-ms data)
                                            (:fire-count data)))))

    (println "\nRule Statistics:")
    (doseq [[rule-name stats] (sort-by (fn [[_ s]] (:fire-count s 0)) >
                                      (:rule-stats data))]
      (println (format "  %-30s fires: %4d  time: %8.2fms  matches: %4d"
                      (name rule-name)
                      (:fire-count stats 0)
                      (:total-time-ms stats 0)
                      (:matches stats 0))))

    (println "\nTop Fact Operations:")
    (doseq [[[id attr] stats] (take 10 (sort-by (fn [[_ s]]
                                                  (+ (:inserts s 0) (:retracts s 0)))
                                                >
                                                (:fact-stats data)))]
      (println (format "  [%s %s] inserts: %d retracts: %d"
                      id attr (:inserts stats 0) (:retracts stats 0))))
    (println "\n======================================")))

(defn reset-stats
  "Resets all profiling statistics.

  Example:
    (prof/reset-stats session)"
  [session]
  (if (enabled? session)
    (set-profiling-data session
      (->ProfilingData true {} {} 0 0))
    session))

;; Debugging helpers

(defn trace-rule-firings
  "Wraps rules to trace when they fire.

  Arguments:
    rules - vector of rules
    trace-fn - (optional) function (rule-name, match) => void

  Returns:
    Vector of wrapped rules

  Example:
    (def traced-rules
      (prof/trace-rule-firings my-rules
        (fn [rule-name match]
          (println \"Rule\" rule-name \"fired with\" match))))"
  ([rules]
   (trace-rule-firings rules
     (fn [rule-name match]
       (println "Rule" rule-name "fired"))))
  ([rules trace-fn]
   (mapv
     (fn [rule]
       (if-let [then-fn (:then-fn rule)]
         (assoc rule :then-fn
           (fn [session match]
             (trace-fn (:name rule) match)
             (then-fn session match)))
         rule))
     rules)))

(defn trace-fact-insertions
  "Prints whenever facts are inserted (via prof/insert).

  Enable by setting session metadata.

  Example:
    (-> session
        (vary-meta assoc ::trace-facts true)
        (prof/insert ::player ::health 100))
    ;; Prints: \"Inserting fact: [::player ::health 100]\""
  [session]
  (vary-meta session assoc ::trace-facts true))
