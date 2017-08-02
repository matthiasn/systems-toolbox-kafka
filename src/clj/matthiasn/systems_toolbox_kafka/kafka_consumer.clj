(ns matthiasn.systems-toolbox-kafka.kafka-consumer
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :as a]
            [matthiasn.systems-toolbox-kafka.utils :as u])
  (:import (org.apache.kafka.clients.consumer KafkaConsumer ConsumerRecords ConsumerRecord)
           (org.apache.kafka.common.serialization StringDeserializer)
           (org.apache.kafka.common.metrics KafkaMetric)
           (java.util Map)))

(defn kafka-consumer-state-fn
  "Returns function that creates the Kafka consumer component state, using the
   provided configuration."
  [cfg]
  (fn [put-fn]
    (log/info "Starting Kafka consumer" cfg)
    (let [kafka-cfg (u/config->kafka-config cfg)
          consumer (KafkaConsumer. ^Map kafka-cfg (StringDeserializer.) (StringDeserializer.))
          topic (:topic cfg)
          shutdown (atom false)]
      (.subscribe consumer [topic])
      (a/thread
        (try
          (while (not @shutdown)
            (let [^ConsumerRecords records (.poll consumer 100)]
              (doseq [^ConsumerRecord record records]
                (let [value (read-string (.value record))
                      {:keys [msg-type msg-payload msg-meta]} value
                      msg (with-meta [msg-type msg-payload] (or msg-meta {}))]
                  (log/debug "Received message on Kafka topic" msg)
                  (put-fn msg)))
              ;(update-lag-gauge current-lag records consumer)
              ;(update-reserve-gauge current-reserve records consumer)
              ))
          (catch Exception e
            (log/error e "Going to stop consuming because of this exception."))
          (finally (.close consumer))))
      {:state (atom {})})))

(defn cmp-map
  "Creates Kafka consumer component."
  {:added "0.4.9"}
  [cmp-id opts]
  {:cmp-id      cmp-id
   :state-fn    (kafka-consumer-state-fn (:cfg opts))
   :handler-map {}})
