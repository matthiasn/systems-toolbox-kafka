(ns matthiasn.systems-toolbox-kafka.integration-tests
  (:require [clojure.test :refer :all]
            [matthiasn.systems-toolbox-kafka.test-helper :refer [eventually]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component.msg-handling :as mh]
            [matthiasn.systems-toolbox-kafka.kafka-consumer :as kc]
            [matthiasn.systems-toolbox-kafka.kafka-producer :as kp]
            [clojure.spec :as s]
            [matthiasn.systems-toolbox.component.helpers :as h]))

(def kafka-cfg {:topic       "test-topic"
                :client-cfg  {:bootstrap.servers "localhost:9092"
                              :group.id          (h/make-uuid)}
                :relay-types #{:test/msg}})

(s/def :test/msg (s/map-of keyword? number?))

(deftest basic-test
  (let [sb (sb/component :test/switchboard)
        rcv-state (atom [])
        test-handler (fn [{:keys [current-state msg msg-meta]}]
                       {:new-state (conj current-state [msg msg-meta])})
        test-cmp-map {:cmp-id      :test/inbox
                      :state-fn    (fn [_put-fn] {:state rcv-state})
                      :handler-map {:test/msg test-handler}}
        test-msg [:test/msg {:a (rand-int 100) :b (rand-int 100)}]]
    (sb/send-mult-cmd sb
                      [[:cmd/init-comp
                        #{(kc/cmp-map :test/consumer kafka-cfg)
                          (kp/cmp-map :test/producer kafka-cfg)
                          test-cmp-map}]

                       [:cmd/route {:from :test/consumer
                                    :to   :test/inbox}]])

    (Thread/sleep 1000)

    (dotimes [_ 1000]
      (mh/send-msg sb
                   [:cmd/send {:to  :test/producer
                               :msg [:test/msg {:a (rand-int 100)
                                                :b (rand-int 100)}]}]))

    (mh/send-msg sb
                 [:cmd/send {:to  :test/producer
                             :msg test-msg}])

    (testing "it should send and receive message via Kafka"
      (eventually (= 1001 (count @rcv-state)))
      (eventually (= test-msg (first (last @rcv-state)))))
    (testing "meta-data on message is preserved"
      (let [msg-meta (second (last @rcv-state))]
        (eventually (= #{:cmp-seq :corr-id :tag :test/inbox :test/consumer :test/producer}
                       (set (keys msg-meta))))
        (eventually (= [:test/producer :test/inbox]
                       (:cmp-seq msg-meta)))))))
