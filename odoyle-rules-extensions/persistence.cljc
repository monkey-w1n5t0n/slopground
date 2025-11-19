(ns odoyle-rules-extensions.persistence
  "Extension for O'Doyle Rules to support session persistence.

  Rationale:
  Save and restore session state for:
  - Save games
  - Checkpointing
  - State migration
  - Distributed systems

  Usage:
  (require '[odoyle.rules :as o]
           '[odoyle-rules-extensions.persistence :as persist])

  ;; Save
  (def saved (persist/save session))

  ;; Restore
  (def restored (persist/load saved rules))"
  (:require [odoyle.rules :as o]))

(defn save
  "Saves session state to a Clojure data structure.

  Returns map with:
    :facts - vector of [id attr value] facts
    :version - persistence format version

  Example:
    (persist/save session)
    ;; => {:facts [[::player ::health 100] ...]
    ;;     :version 1}"
  [session]
  {:facts (o/query-all session)
   :version 1})

(defn load
  "Loads session state from saved data.

  Arguments:
    saved-data - map from persist/save
    rules      - vector of rules to add to new session

  Returns:
    New session with rules and facts restored

  Example:
    (persist/load saved-data my-rules)"
  [saved-data rules]
  (let [session (reduce o/add-rule (o/->session) rules)]
    (reduce (fn [s fact]
              (o/insert s fact))
            session
            (:facts saved-data))))

(defn serialize
  "Serializes session to EDN string.

  Example:
    (persist/serialize session)"
  [session]
  (pr-str (save session)))

(defn deserialize
  "Deserializes session from EDN string.

  Example:
    (persist/deserialize edn-string rules)"
  [edn-string rules]
  (load (read-string edn-string) rules))

(defn checkpoint
  "Creates a named checkpoint of session state.

  Returns session with checkpoint saved in metadata.

  Example:
    (persist/checkpoint session :before-battle)"
  [session checkpoint-name]
  (vary-meta session assoc-in [::checkpoints checkpoint-name]
    (save session)))

(defn rollback
  "Restores session to a named checkpoint.

  Arguments:
    session - session with checkpoints
    checkpoint-name - name of checkpoint to restore
    rules - vector of rules

  Example:
    (persist/rollback session :before-battle rules)"
  [session checkpoint-name rules]
  (if-let [saved-data (get-in (meta session) [::checkpoints checkpoint-name])]
    (load saved-data rules)
    (throw (ex-info (str "Checkpoint " checkpoint-name " not found") {}))))

(defn list-checkpoints
  "Lists all checkpoint names.

  Example:
    (persist/list-checkpoints session)"
  [session]
  (keys (get (meta session) ::checkpoints)))
