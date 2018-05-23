(ns matthiasn.systems-toolbox-kafka.integration-tests2
  (:require [clojure.test :refer :all]
            [matthiasn.systems-toolbox-kafka.test-helper :refer [eventually]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component.msg-handling :as mh]
            [matthiasn.systems-toolbox-kafka.kafka-consumer2 :as kc]
            [matthiasn.systems-toolbox-kafka.kafka-producer2 :as kp]
            [clojure.spec.alpha :as s]))

(def kafka-cfg {:cfg         {:bootstrap-servers "localhost:9092"
                              :topic             "test-topic2"}
                :relay-types #{:test/msg}})

(s/def :test/msg (s/map-of keyword? number?))

(deftest basic-test
  (let [sb (sb/component :test/switchboard)
        rcv-state (atom {:received []})
        test-handler (fn [{:keys [current-state msg]}]
                       (let [new-state (update-in current-state [:received] conj msg)]
                         {:new-state new-state}))
        test-cmp-map {:cmp-id      :test/inbox
                      :state-fn    (fn [_put-fn] {:state rcv-state})
                      :handler-map {:test/msg test-handler}}
        test-msgs (mapv (fn [n] [:test/msg {:a n :b (rand-int 100)}])
                        (range 1000))]
    (sb/send-mult-cmd sb
                      [[:cmd/init-comp
                        #{(kc/cmp-map :test/consumer kafka-cfg)
                          (kp/cmp-map :test/producer kafka-cfg)
                          test-cmp-map}]

                       [:cmd/route {:from :test/consumer
                                    :to   :test/inbox}]])

    (Thread/sleep 5000)

    (doseq [m test-msgs]
      (mh/send-msg sb
                   [:cmd/send {:to  :test/producer
                               :msg m}]))

    (testing "it should send and receive message via Kafka"
      (eventually (= 1000 (count (:received @rcv-state))))
      (eventually (= test-msgs (:received @rcv-state))))

    (testing "meta-data on message is preserved"
      (let [msg-meta (meta (last (:received @rcv-state)))]
        (eventually (= #{:cmp-seq
                         :corr-id
                         :system-info
                         :tag
                         :tag-ts
                         :test/consumer
                         :test/inbox
                         :test/producer}
                       (set (keys msg-meta))))
        (eventually (= [:test/producer :test/consumer :test/inbox]
                       (:cmp-seq msg-meta)))))))
