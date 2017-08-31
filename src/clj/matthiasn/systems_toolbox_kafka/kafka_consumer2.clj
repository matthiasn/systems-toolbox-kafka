(ns matthiasn.systems-toolbox-kafka.kafka-consumer2
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :as a]
            [cognitect.transit :as transit]
            [matthiasn.systems-toolbox-kafka.utils :as u])
  (:import (org.apache.kafka.clients.consumer KafkaConsumer ConsumerRecords ConsumerRecord)
           (org.apache.kafka.common.serialization ByteArrayDeserializer)
           (org.apache.kafka.common.metrics KafkaMetric)
           (java.util Map)
           (java.io ByteArrayInputStream)))

(defn send-message
  "Forwards parsed message. Logs error when there's a problem."
  [msg-value put-fn]
  (try
    (let [in (ByteArrayInputStream. msg-value)
          reader (transit/reader in :json)
          parsed (transit/read reader)
          {:keys [msg-type msg-payload msg-meta]} parsed]
      (if msg-type
        (let [msg (with-meta [msg-type msg-payload] (or msg-meta {}))]
          (log/debug "Received message on Kafka topic" msg)
          (put-fn msg))
        (log/error "Don't know what to do with message:" parsed)))
    (catch Throwable e
      (log/error e "Exception when handling message:" msg-value))))

(defn kafka-consumer-state-fn
  "Returns function that creates the Kafka consumer component state, using the
   provided configuration."
  [cfg]
  (fn [put-fn]
    (let [kafka-cfg (u/config->kafka-config cfg)
          _ (log/info "Starting Kafka consumer" kafka-cfg)
          consumer (KafkaConsumer. ^Map kafka-cfg (ByteArrayDeserializer.) (ByteArrayDeserializer.))
          topic (:topic cfg)
          topics (if (vector? topic) topic [topic])
          shutdown (atom false)]
      (log/info "Started Kafka consumer" topics consumer)
      (.subscribe consumer topics)
      (a/thread
        (try
          (while (not @shutdown)
            (let [^ConsumerRecords records (.poll consumer 100)]
              (doseq [^ConsumerRecord record records]
                (send-message (.value record) put-fn))))
          (catch Throwable e
            (log/error e "Going to stop consuming because of this exception."))
          (finally
            (log/info "closing Kafka consumer")
            (.close consumer))))
      {:state (atom {})})))

(defn cmp-map
  "Creates Kafka consumer component."
  {:added "0.4.9"}
  [cmp-id opts]
  {:cmp-id      cmp-id
   :state-fn    (kafka-consumer-state-fn (:cfg opts))
   :handler-map {}})
