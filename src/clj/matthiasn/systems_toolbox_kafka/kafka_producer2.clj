(ns matthiasn.systems-toolbox-kafka.kafka-producer2
  (:require [clojure.tools.logging :as log]
            [matthiasn.systems-toolbox-kafka.utils :as u]
            [clojure.string :as str]
            [cognitect.transit :as transit]
            [clojure.spec.alpha :as s])
  (:import [org.apache.kafka.clients.producer KafkaProducer ProducerRecord Callback]
           [org.apache.kafka.common.serialization ByteArrayDeserializer ByteArraySerializer]
           [org.apache.kafka.common.metrics KafkaMetric]
           [java.util Map]
           [java.io ByteArrayInputStream ByteArrayOutputStream]))


(defn metadata->map
  [metadata]
  {:offset     (.offset metadata)
   :timestamp  (.timestamp metadata)
   :checksum   (.checksum metadata)
   :key-size   (.serializedKeySize metadata)
   :value-size (.serializedValueSize metadata)
   :topic      (.topic metadata)
   :partition  (.partition metadata)})

(defn publish-msg
  "Publishes messages on Kafka topic when the msg-type-to-topic mapping contains
  the msg-type. Messages on the topic contain metadata for the systems-toolbox
  message."
  [{:keys [current-state msg-type msg-meta msg-payload]}]
  (let [prod (:prod current-state)
        topic (-> current-state :cfg :topic)
        serializable {:msg-type    msg-type
                    :msg-meta    msg-meta
                    :msg-payload msg-payload}
        out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)
        _ (transit/write writer serializable)
        pr (ProducerRecord. topic (.toByteArray out))
        callback (reify Callback
                   (onCompletion [this metadata exception]
                     (log/debug "publish callback:" (metadata->map metadata) exception)
                     (when exception
                       (log/error "publish failed:" exception))))]
    (.close out)
    (log/debug "Publishing message on topic" topic serializable)
    (.send prod pr callback)
    {}))

(defn kafka-producer-state-fn
  "Returns initial component state function. Calling this function will return the
  initial component state containing the Kafka producer and the config.."
  [cfg]
  (fn [put-fn]
    (let [kafka-cfg (u/config->kafka-config cfg)
          prod (KafkaProducer. kafka-cfg (ByteArraySerializer.) (ByteArraySerializer.))]
      (log/info "Starting Kafka producer with config" kafka-cfg)
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
