(ns mineralplant.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [mineralplant.actor :as actor]
            [mineralplant.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-operator! st {:operator-id "operator-1" :name "Kobo Tanaka"})
    (store/register-plant! st {:plant-id "P-1" :name "Kobo Mineral Plant" :max-supply-cost 2000})
    st))

(deftest commits-a-registered-work-log
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:operator-id "operator-1" :op :log-work-record :stake :low
                  :plant-id "P-1" :task "crushing-run progress log"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "operator-1"))))))

(deftest holds-an-unregistered-plant-proposal
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:operator-id "operator-1" :op :log-work-record :stake :low
                  :plant-id "P-ghost" :task "crushing-run progress log"}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "operator-1")))))

(deftest interrupts-then-approves-safety-concern-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:operator-id "operator-1" :op :flag-safety-concern :stake :low
                  :plant-id "P-1" :hazard-type :dust-exposure-risk}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "operator-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "operator-1")))))))

(deftest holds-a-scope-excluded-op-even-at-high-confidence
  (testing "an actor run can never commit a proposal that would finalize a plant-operation-execution decision, regardless of disposition path"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:operator-id "operator-1" :op :finalize-plant-operation-decision :stake :low
                    :plant-id "P-1" :task "operation decision"}
          result (actor/run-request! graph request {} "thread-4")]
      (is (= :done (:status result)))
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "operator-1"))))))
