(ns odoyle-rules-extensions.modules
  "Extension for O'Doyle Rules to support rule groups and modules.

  Rationale:
  ---------
  As rule systems grow, organization becomes critical:
  - Feature toggles (enable/disable rule groups)
  - Different modes (dev vs production rules)
  - Plugin-style architecture
  - Conditional rule activation
  - Testing subsets of rules

  Design Decisions:
  ----------------
  1. Modules are named groups of rules
  2. Modules can be enabled/disabled dynamically
  3. Disabled modules' rules don't fire
  4. Module state stored in session metadata
  5. Compatible with all O'Doyle features

  Usage example:

  (require '[odoyle.rules :as o]
           '[odoyle-rules-extensions.modules :as mod])

  (def combat-rules (o/ruleset {...}))
  (def trading-rules (o/ruleset {...}))

  (-> (o/->session)
      mod/enable-modules
      (mod/register-module :combat combat-rules)
      (mod/register-module :trading trading-rules)
      (mod/enable :combat)
      (mod/disable :trading)
      ...)"
  (:require [odoyle.rules :as o]))

;; Module management

(def ^:private modules-key ::registered-modules)
(def ^:private enabled-key ::enabled-modules)

(defn enable-modules
  "Enables module support for a session.

  Must be called before registering modules.

  Example:
    (-> (o/->session)
        mod/enable-modules)"
  [session]
  (-> session
      (vary-meta assoc modules-key {})
      (vary-meta assoc enabled-key #{})))

(defn- get-modules [session]
  (get (meta session) modules-key {}))

(defn- get-enabled-modules [session]
  (get (meta session) enabled-key #{}))

(defn- set-modules [session modules]
  (vary-meta session assoc modules-key modules))

(defn- set-enabled-modules [session enabled]
  (vary-meta session assoc enabled-key enabled))

(defn register-module
  "Registers a module (group of rules) with the session.

  Arguments:
    session     - module-enabled session
    module-name - keyword identifying the module
    rules       - vector of rules

  Returns:
    Updated session (rules not yet added)

  Example:
    (mod/register-module session :combat combat-rules)"
  [session module-name rules]
  (let [modules (get-modules session)]
    (set-modules session (assoc modules module-name rules))))

(defn unregister-module
  "Unregisters a module and removes its rules if they were added.

  Arguments:
    session     - module-enabled session
    module-name - keyword identifying the module

  Returns:
    Updated session

  Example:
    (mod/unregister-module session :combat)"
  [session module-name]
  (let [modules (get-modules session)
        rules (get modules module-name)]
    (-> session
        (set-modules (dissoc modules module-name))
        (cond->
          (contains? (get-enabled-modules session) module-name)
          (-> (set-enabled-modules (disj (get-enabled-modules session) module-name))
              (#(reduce o/remove-rule % (map :name rules))))))))

(defn enable
  "Enables a module, adding its rules to the session.

  Arguments:
    session     - module-enabled session
    module-name - keyword identifying the module

  Returns:
    Updated session with module rules added

  Example:
    (mod/enable session :combat)"
  [session module-name]
  (let [modules (get-modules session)
        enabled (get-enabled-modules session)]
    (if (contains? enabled module-name)
      session  ; Already enabled
      (if-let [rules (get modules module-name)]
        (-> session
            (set-enabled-modules (conj enabled module-name))
            (#(reduce o/add-rule % rules)))
        (throw (ex-info (str "Module " module-name " not registered") {}))))))

(defn disable
  "Disables a module, removing its rules from the session.

  Arguments:
    session     - module-enabled session
    module-name - keyword identifying the module

  Returns:
    Updated session with module rules removed

  Example:
    (mod/disable session :combat)"
  [session module-name]
  (let [modules (get-modules session)
        enabled (get-enabled-modules session)]
    (if-not (contains? enabled module-name)
      session  ; Already disabled
      (if-let [rules (get modules module-name)]
        (-> session
            (set-enabled-modules (disj enabled module-name))
            (#(reduce o/remove-rule % (map :name rules))))
        session))))

(defn toggle
  "Toggles a module on or off.

  Arguments:
    session     - module-enabled session
    module-name - keyword identifying the module

  Returns:
    Updated session

  Example:
    (mod/toggle session :debug-mode)"
  [session module-name]
  (if (contains? (get-enabled-modules session) module-name)
    (disable session module-name)
    (enable session module-name)))

(defn enabled?
  "Returns true if module is currently enabled.

  Example:
    (mod/enabled? session :combat)"
  [session module-name]
  (contains? (get-enabled-modules session) module-name))

(defn registered?
  "Returns true if module is registered.

  Example:
    (mod/registered? session :combat)"
  [session module-name]
  (contains? (get-modules session) module-name))

(defn list-modules
  "Returns a map of all registered modules.

  Returns:
    Map of module-name => {:enabled boolean :rule-count integer}

  Example:
    (mod/list-modules session)
    ;; => {:combat {:enabled true :rule-count 5}
    ;;     :trading {:enabled false :rule-count 3}}"
  [session]
  (let [modules (get-modules session)
        enabled (get-enabled-modules session)]
    (reduce-kv
      (fn [m module-name rules]
        (assoc m module-name
          {:enabled (contains? enabled module-name)
           :rule-count (count rules)}))
      {}
      modules)))

(defn enable-all
  "Enables all registered modules.

  Example:
    (mod/enable-all session)"
  [session]
  (reduce enable session (keys (get-modules session))))

(defn disable-all
  "Disables all enabled modules.

  Example:
    (mod/disable-all session)"
  [session]
  (reduce disable session (get-enabled-modules session)))

;; Convenience functions

(defn with-modules
  "Creates a session with multiple modules registered and optionally enabled.

  Arguments:
    modules - vector of {:name keyword :rules vector :enabled? boolean}

  Returns:
    Session with modules registered

  Example:
    (mod/with-modules
      [{:name :combat :rules combat-rules :enabled? true}
       {:name :trading :rules trading-rules :enabled? false}])"
  [modules]
  (reduce
    (fn [session {:keys [name rules enabled?]}]
      (cond-> (register-module session name rules)
        enabled? (enable name)))
    (enable-modules (o/->session))
    modules))

(defn create-module
  "Helper to create a module definition.

  Arguments:
    name      - keyword module name
    rules     - vector of rules
    enabled?  - (optional) whether to enable by default

  Returns:
    Module definition map

  Example:
    (def combat-mod
      (mod/create-module :combat combat-rules true))"
  ([name rules]
   {:name name :rules rules :enabled? false})
  ([name rules enabled?]
   {:name name :rules rules :enabled? enabled?}))

;; Conditional module activation

(defn enable-if
  "Conditionally enables a module based on a predicate.

  Arguments:
    session     - module-enabled session
    module-name - keyword identifying the module
    pred-fn     - function session => boolean

  Returns:
    Updated session

  Example:
    (mod/enable-if session :debug-mode
      (fn [session] (= :development (:env session))))"
  [session module-name pred-fn]
  (if (pred-fn session)
    (enable session module-name)
    session))

(defn disable-if
  "Conditionally disables a module based on a predicate.

  Arguments:
    session     - module-enabled session
    module-name - keyword identifying the module
    pred-fn     - function session => boolean

  Returns:
    Updated session

  Example:
    (mod/disable-if session :experimental
      (fn [session] (= :production (:env session))))"
  [session module-name pred-fn]
  (if (pred-fn session)
    (disable session module-name)
    session))

;; Module sets

(defn enable-set
  "Enables a set of modules, disabling all others.

  Useful for switching between mutually exclusive module sets.

  Arguments:
    session      - module-enabled session
    module-names - collection of module keywords to enable

  Returns:
    Updated session

  Example:
    (mod/enable-set session [:combat :movement :inventory])"
  [session module-names]
  (let [all-modules (keys (get-modules session))
        to-enable (set module-names)
        to-disable (remove to-enable all-modules)]
    (-> session
        (#(reduce disable % to-disable))
        (#(reduce enable % to-enable)))))

;; Debugging and introspection

(defn stats
  "Returns statistics about modules.

  Returns:
    Map with :registered, :enabled, :disabled counts

  Example:
    (mod/stats session)
    ;; => {:registered 5 :enabled 3 :disabled 2}"
  [session]
  (let [modules (get-modules session)
        enabled (get-enabled-modules session)]
    {:registered (count modules)
     :enabled (count enabled)
     :disabled (- (count modules) (count enabled))}))

(defn print-status
  "Prints the status of all modules.

  Example:
    (mod/print-status session)
    ;; Prints:
    ;; Module Status:
    ;;   [✓] combat (5 rules)
    ;;   [ ] trading (3 rules)
    ;;   [✓] movement (2 rules)"
  [session]
  (println "\nModule Status:")
  (doseq [[module-name info] (list-modules session)]
    (println (format "  [%s] %s (%d rules)"
                    (if (:enabled info) "✓" " ")
                    (name module-name)
                    (:rule-count info)))))
