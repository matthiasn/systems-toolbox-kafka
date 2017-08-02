(ns matthiasn.systems-toolbox-kafka.integration-tests
  (:require [clojure.test :refer :all]
            [matthiasn.systems-toolbox-kafka.test-helper :refer [eventually]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component.msg-handling :as mh]
            [matthiasn.systems-toolbox-kafka.kafka-consumer :as kc]
            [matthiasn.systems-toolbox-kafka.kafka-producer :as kp]
            [clojure.spec.alpha :as s]
            [matthiasn.systems-toolbox.component.helpers :as h]))

(def kafka-cfg {:cfg         {:bootstrap-servers "localhost:9092"
                              :group-id          (h/make-uuid)
                              :topic             "test-topic"}
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
                        (range 100))]
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
      (eventually (= 100 (count (:received @rcv-state))))
      (eventually (= test-msgs (:received @rcv-state))))

    (testing "meta-data on message is preserved"
      (let [msg-meta (meta (last (:received @rcv-state)))]
        (eventually (= #{:cmp-seq :corr-id :tag :test/inbox :test/consumer :test/producer}
                       (set (keys msg-meta))))
        (eventually (= [:test/producer :test/inbox]
                       (:cmp-seq msg-meta)))))))
