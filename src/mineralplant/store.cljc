(ns mineralplant.store
  "SSoT for the ISCO-08 8112 mineral and stone processing plant
  scheduling/logistics coordination actor (itonami actor pattern,
  ADR-2607121000 / CLAUDE.md Actors section; README's 'Robotics
  premise' — a plant scheduling/logistics coordination robot performs
  crew scheduling, production-run/inventory/progress record logging
  and spare-parts/raw-material supply-order coordination for a
  crushing/grinding/screening plant crew under this advisor/governor
  pair, which never dispatches hardware itself, never operates plant
  equipment itself, and never finalizes a plant-operation-execution
  decision or a plant-safety-clearance decision, nor overrides a
  plant safety officer's judgment — those remain the plant safety
  officer's exclusive judgment). Modeled on
  cloud-itonami-isco-7211's foundrycoord.store (closest domain shape
  — heavy-machinery + hazardous-material industrial-site scheduling/
  logistics coordination).

  Domain:

    operator — a registered mineral/stone processing plant equipment
               operator (:operator-id, :name)
    plant    — a registered mineral/stone processing plant site
               {:plant-id :name :max-supply-cost number}.
               `:max-supply-cost` is an informational registered
               ceiling used only to decide whether a
               `:coordinate-supply-order` proposal escalates to human
               sign-off (the governor never blocks a within-threshold
               order outright; it only decides commit vs. escalate).
    record   — a committed operating record (a logged production-run/
               inventory/progress entry, a scheduled crew/shift
               operation, a flagged safety concern, or a coordinated
               raw-material/spare-parts supply order) — written ONLY
               via commit-record!.
    ledger   — append-only audit trail, commit or hold.")

(defprotocol Store
  (operator [s operator-id])
  (plant [s plant-id])
  (records-of [s operator-id])
  (ledger [s])
  (register-operator! [s o])
  (register-plant! [s p])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (operator [_ operator-id] (get-in @a [:operators operator-id]))
  (plant [_ plant-id] (get-in @a [:plants plant-id]))
  (records-of [_ operator-id] (filter #(= operator-id (:operator-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-operator! [s o]
    (swap! a assoc-in [:operators (:operator-id o)] o) s)
  (register-plant! [s p]
    (swap! a assoc-in [:plants (:plant-id p)] p) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:operators {} :plants {} :records [] :ledger []}
                                    seed)))))
