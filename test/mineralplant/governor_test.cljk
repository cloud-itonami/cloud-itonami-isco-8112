(ns mineralplant.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [mineralplant.store :as store]
            [mineralplant.advisor :as advisor]
            [mineralplant.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-operator! st {:operator-id "operator-1" :name "Kobo Tanaka"})
    (store/register-plant! st {:plant-id "P-1" :name "Kobo Mineral Plant" :max-supply-cost 2000})
    st))

(defn- op [op-kw & {:as extra}]
  (merge {:op op-kw :effect :propose :plant-id "P-1"
          :confidence 0.9 :stake :low}
         extra))

(def ^:private req {:operator-id "operator-1"})

(deftest ok-log-work-record
  (let [st (fresh-store)
        v (governor/check req {} (op :log-work-record) st)]
    (is (:ok? v))))

(deftest ok-schedule-crew-operation
  (let [st (fresh-store)
        v (governor/check req {} (op :schedule-crew-operation) st)]
    (is (:ok? v))))

(deftest ok-supply-order-at-threshold-boundary
  (testing "the supply-cost threshold escalate boundary is exclusive (over, not at)"
    (let [st (fresh-store)
          v (governor/check req {} (op :coordinate-supply-order :cost 2000) st)]
      (is (:ok? v)))))

(deftest hard-on-unregistered-operator
  (let [st (fresh-store)
        v (governor/check {:operator-id "nobody"} {} (op :log-work-record) st)]
    (is (:hard? v))
    (is (some #(= :no-operator (:rule %)) (:violations v)))))

(deftest hard-on-unregistered-plant
  (let [st (fresh-store)
        v (governor/check req {} (op :log-work-record :plant-id "P-ghost") st)]
    (is (:hard? v))
    (is (some #(= :no-plant (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (op :log-work-record) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-op-outside-closed-allowlist
  (let [st (fresh-store)
        v (governor/check req {} (op :dispatch-equipment) st)]
    (is (:hard? v))
    (is (some #(= :unknown-op (:rule %)) (:violations v)))))

(deftest hard-on-scope-excluded-op-finalize-plant-operation-decision
  (testing "finalizing a plant-operation-execution decision is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :finalize-plant-operation-decision) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-authorize-plant-operation
  (testing "authorizing a plant operation is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :authorize-plant-operation) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-proceed-with-plant-operation
  (testing "proceeding with a plant operation is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :proceed-with-plant-operation) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-declare-plant-safety-cleared
  (testing "declaring the plant safety cleared is a permanent block, never a routine op -- this actor never makes a plant-safety-clearance decision"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :declare-plant-safety-cleared) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-override-plant-safety-officer-judgment
  (testing "overriding a plant safety officer's judgment is a plant safety officer's exclusive judgment, never this actor's"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :override-plant-safety-officer-judgment) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-even-with-allowlisted-op
  (testing "defense-in-depth: a rationale that itself attempts to finalize the plant-operation decision is blocked even if the op looks routine"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :schedule-crew-operation)
                                           :rationale "recommend we finalize the plant-operation decision now")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-override-plant-safety-officer
  (testing "defense-in-depth: a rationale attempting to override the plant safety officer's judgment is blocked even if the op looks routine"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :log-work-record)
                                           :rationale "override the plant safety officer's judgment and proceed")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-declare-plant-safety-cleared
  (testing "defense-in-depth: a rationale that itself attempts to declare the plant safety cleared is blocked even if the op looks routine"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :log-work-record)
                                           :rationale "we declare the plant safety cleared for the next shift")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest always-escalates-safety-concern-even-at-high-confidence
  (testing "a machinery-hazard/dust-exposure/equipment-condition concern always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :flag-safety-concern :hazard-type :dust-exposure-risk)
                                           :confidence 0.99)
                             st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-supply-order-above-threshold
  (let [st (fresh-store)
        v (governor/check req {} (assoc (op :coordinate-supply-order :cost 5000) :confidence 0.99) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (op :log-work-record) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest default-mock-advisor-proposals-never-self-trip-on-scope-exclusion
  (testing "the governor's scope-exclusion term list must never match the mock advisor's own default rationale text for any allowlisted op — CLAUDE.md's known self-tripping bug pattern (rationale legitimately contains bare nouns like 'crushing'/'screening'/'plant safety officer', but never the full finalization-action phrases)"
    (let [st (fresh-store)
          adv (advisor/mock-advisor)
          ops [:log-work-record :schedule-crew-operation
               :flag-safety-concern :coordinate-supply-order]]
      (doseq [o ops]
        (let [request {:operator-id "operator-1" :op o :plant-id "P-1"
                        :stake :low :task "routine crushing/screening task"
                        :hazard-type :dust-exposure-risk :cost 500}
              proposal (advisor/-advise adv st request)
              v (governor/check request {} proposal st)]
          (is (not (:hard? v))
              (str o " proposal unexpectedly hard-blocked: " (:violations v))))))))
