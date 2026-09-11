# cloud-itonami-isco-8112

Open Occupation Blueprint for **ISCO-08 8112**: Mineral and Stone Processing Plant Operators.

This repository designs a forkable OSS business for a mineral and stone processing plant scheduling and logistics coordination practice: a plant scheduling and supply-coordination robot manages crew/task records under a governor-gated actor, so a crushing/grinding/screening plant crew keeps its own operating records instead of renting a closed workforce-management SaaS.

**Maturity: `:implemented`.** `src/mineralplant/` implements the
`MineralPlantCoordActor` as a `langgraph.graph/state-graph`
(`mineralplant.actor`) wired to a `Mineral and Stone Processing Plant
Coordination Advisor` (`mineralplant.advisor`) and an independent
`MineralPlantCoordGovernor` (`mineralplant.governor`), following the
itonami actor pattern (ADR-2607121000): `:intake -> :advise -> :govern
-> :decide -+-> :commit (:ok?) +-> :request-approval (:escalate?,
human-in-the-loop interrupt) +-> :hold (:hard?)`. 23 tests / 49
assertions green (`kbb -M:test`). HARD invariants (always hold,
never overridable): operator provenance, plant provenance,
no-actuation (`:effect` must be `:propose`), a closed op-allowlist
(`:log-work-record`, `:schedule-crew-operation`,
`:flag-safety-concern`, `:coordinate-supply-order` — nothing else may
ever be proposed), and a permanent, unconditional block on any
proposal that would directly finalize a plant-operation-execution
decision or a plant-safety-clearance decision, or override a plant
safety officer's judgment. Always-escalate paths (human sign-off
regardless of confidence, mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-supply-order` above
the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a plant scheduling/logistics coordination robot performs crew scheduling, production-run/inventory/progress-record logging and raw-material/spare-parts supply-order coordination for a mineral and stone processing (crushing/grinding/screening) plant crew, under an actor that proposes actions and an independent **Mineral and Stone Processing Plant Coordination Governor** that gates them. The governor never
dispatches hardware itself, never operates crushing/grinding/screening plant equipment on the plant floor, and never finalizes a plant-operation-execution decision or a plant-safety-clearance decision, nor overrides a plant safety officer's judgment; `:high`/`:safety-critical` actions (such as a flagged machinery-hazard/dust-exposure/equipment-condition concern, or an above-threshold supply order) require human sign-off. **This actor coordinates plant scheduling/logistics only — it never operates crushing/grinding/screening plant equipment or makes safety-clearance decisions itself.**

## Core Contract

```text
crew roster + plant registration + safety-reporting policy
        |
        v
Mineral and Stone Processing Plant Coordination Advisor -> MineralPlantCoordGovernor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, finalize
a plant-operation-execution decision, declare a plant-safety-clearance decision,
override a plant safety officer's judgment, suppress an operating record, or
disclose sensitive data without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8112`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
