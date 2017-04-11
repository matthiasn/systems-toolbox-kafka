(ns matthiasn.systems-toolbox-kafka.kafka-consumer
  (:require [clojure.tools.logging :as log]
            [kinsky.client :as client]
            [kinsky.async :as ka]
            [clojure.core.async :as a]))

(defn kafka-consumer-state-fn
  "Returns function that creates the Kafka consumer component state state while using provided
  configuration.
  This component creates multiple listeners, one for each topic provided in the topic set
  from config. Note that messages taken off the topics need to be sent by the systems-toolbox,
  encoded by Nippy."
  [cfg]
  (fn [put-fn]
    (log/info "Starting Kafka consumer" cfg)
    (let [[out ctl] (ka/consumer (:client-cfg cfg)
                                 (client/keyword-deserializer)
                                 (client/edn-deserializer))
          topic (:topic cfg)]
      (a/go-loop []
        (when-let [msg (a/<! out)]
          (try
            (when-let [value (:value msg)]
              (let [{:keys [msg-type msg-payload msg-meta]} value]
                (log/debug "Received message on Kafka topic" msg)
                (put-fn (with-meta [msg-type msg-payload] (or msg-meta {})))))
            (catch Exception ex (log/error "Error while taking message off Kafka topic:" ex)))
          (recur)))
      (a/put! ctl {:op :subscribe :topic topic})
      {:state (atom {})})))

(defn cmp-map
  "Creates Kafka consumer component.
  Inside the cfg parameter, a set of topics to listen to is specified, like this:

      {:topics #{some-topic-name-string another-topic-name-string}}

  This component is currently one-way as in not having a message handler. But it could
  make sense to for example listen to messages that ask for listening to additional
  topics, or for returning component stats."
  {:added "0.4.9"}
  [cmp-id cfg]
  {:cmp-id      cmp-id
   :state-fn    (kafka-consumer-state-fn cfg)
   :handler-map {}})
