(ns matthiasn.systems-toolbox-kafka.kafka-producer
  (:require [clojure.tools.logging :as log]
            [matthiasn.systems-toolbox-kafka.utils :as u]
            [clojure.string :as str]
            [clojure.spec.alpha :as s])
  (:import (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)
           (org.apache.kafka.common.serialization StringDeserializer StringSerializer)
           (org.apache.kafka.common.metrics KafkaMetric)
           (java.util Map)))

(defn publish-msg
  "Publishes messages on Kafka topic when the msg-type-to-topic mapping contains
  the msg-type. Messages on the topic contain metadata for the systems-toolbox
  message and are serialized using Nippy."
  [{:keys [current-state msg-type msg-meta msg-payload]}]
  (let [prod (:prod current-state)
        topic (-> current-state :cfg :topic)
        serialized {:msg-type    msg-type
                    :msg-meta    msg-meta
                    :msg-payload msg-payload}
        pr (ProducerRecord. topic (pr-str serialized))]
    (log/debug "Publishing message on topic" topic serialized)
    (.send prod pr)
    {}))

(defn kafka-producer-state-fn
  "Returns initial component state function. Calling this function will return the
  initial component state containing the Kafka producer and the config.."
  [cfg]
  (fn [put-fn]
    (let [kafka-cfg (u/config->kafka-config cfg)
          prod (KafkaProducer. kafka-cfg (StringSerializer.) (StringSerializer.))]
      {:state (atom {:prod prod
                     :cfg  cfg})})))

(defn cmp-map
  "Create Kafka producer component."
  {:added "0.4.9"}
  [cmp-id opts]
  (let [msg-types (:relay-types opts)]
    {:cmp-id      cmp-id
     :state-fn    (kafka-producer-state-fn (:cfg opts))
     :handler-map (zipmap msg-types (repeat publish-msg))}))
