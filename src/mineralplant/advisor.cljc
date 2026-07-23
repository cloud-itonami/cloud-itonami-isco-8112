(ns mineralplant.advisor
  "Mineral and Stone Processing Plant Coordination Advisor —
  proposing a plant scheduling/logistics coordination operation (log
  a work record, schedule a crew operation, flag a safety concern,
  coordinate a raw-material/spare-parts supply order) from a crew
  roster, plant registration and safety-reporting policy. Swappable
  mock/llm; the advisor ONLY proposes — `mineralplant.governor`
  independently gates every proposal and always escalates safety
  concerns and above-threshold supply orders. The advisor never
  proposes to directly finalize a plant-operation-execution decision
  or a plant-safety-clearance decision, and never proposes to
  override a plant safety officer's judgment — those stay
  permanently out of this actor's scope. Modeled on
  cloud-itonami-isco-7211's foundrycoord.advisor (closest domain
  shape).

  A proposal: {:op :log-work-record|:schedule-crew-operation|
               :flag-safety-concern|:coordinate-supply-order
               :effect :propose :operator-id str :plant-id str
               :cost number :hazard-type kw :task str :stake kw
               :confidence n :rationale str}"
  (:require #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as edn])))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- rationale-for [op operator-id plant-id hazard-type]
  (case op
    :log-work-record
    (str "logged work record for operator " operator-id " at plant " plant-id)

    :schedule-crew-operation
    (str "scheduled crew operation for crushing/screening shift at plant " plant-id)

    :flag-safety-concern
    (str "flagged " (name (or hazard-type :hazard)) " concern for operator "
         operator-id " at plant " plant-id " — routed for plant safety officer review")

    :coordinate-supply-order
    (str "coordinated supply order for operator " operator-id " at plant " plant-id)

    (str "proposed " (name op) " for operator " operator-id " at plant " plant-id)))

(defn- infer [_store {:keys [op stake operator-id plant-id cost hazard-type task]
                       :as request}]
  {:op op
   :effect :propose
   :operator-id operator-id
   :plant-id plant-id
   :cost cost
   :hazard-type hazard-type
   :task task
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (rationale-for op operator-id plant-id hazard-type)})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a mineral and stone processing plant scheduling/logistics
   coordination advisor. Given a request, propose an :op (one of
   :log-work-record, :schedule-crew-operation, :flag-safety-concern,
   :coordinate-supply-order), the :operator-id, :plant-id, and any
   :cost/:hazard-type/:task fields, an honest :confidence and a
   :stake. Never propose an op outside this closed list, and never
   propose to directly finalize a plant-operation-execution decision
   or a plant-safety-clearance decision, or to override a plant
   safety officer's judgment — those are always out of this actor's
   scope; it coordinates plant scheduling/logistics only and never
   operates crushing/grinding/screening equipment or makes
   safety-clearance decisions itself. Safety concerns always require
   human sign-off regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
