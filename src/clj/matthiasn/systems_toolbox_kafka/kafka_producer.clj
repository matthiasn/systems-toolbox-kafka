(ns matthiasn.systems-toolbox-kafka.kafka-producer
  (:require [clojure.tools.logging :as log]
            [kinsky.client :as k]))

(defn kafka-producer-state-fn
  "Returns initial component state function. Calling this function will return the
  initial component state containing the Kafka producer and the config.."
  [cfg]
  (fn [put-fn]
    (let [kafka-cfg (:client-cfg cfg)
          prod (k/producer kafka-cfg (k/keyword-serializer) (k/edn-serializer))]
      {:state (atom {:prod prod
                     :cfg  cfg})})))

(defn publish-msg
  "Publishes messages on Kafka topic when the msg-type-to-topic mapping contains
  the msg-type. Messages on the topic contain metadata for the systems-toolbox
  message and are serialized using Nippy."
  [{:keys [current-state msg-type msg-meta msg-payload]}]
  (let [prod (:prod current-state)
        cfg (:cfg current-state)
        topic (:topic cfg)
        serialized {:msg-type    msg-type
                    :msg-meta    msg-meta
                    :msg-payload msg-payload}]
    (log/debug "Publishing message on topic" topic serialized)
    (k/send! prod topic nil serialized)
    {}))

(defn cmp-map
  "Create Kafka producer component."
  {:added "0.4.9"}
  [cmp-id cfg]
  (let [msg-types (:relay-types cfg)]
    {:cmp-id      cmp-id
     :state-fn    (kafka-producer-state-fn cfg)
     :handler-map (zipmap msg-types (repeat publish-msg))}))
