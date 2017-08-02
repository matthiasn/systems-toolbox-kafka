(ns matthiasn.systems-toolbox-kafka.utils
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::bootstrap-servers string?)
(s/def ::group-id string?)
(s/def ::topic string?)

(s/def ::cfg
  (s/keys :req-un [::bootstrap-servers
                   ::group-id
                   ::topic]))

(defn config->kafka-config
  "Kafka configs are now maps of strings to strings. Morph
   an arbitrary clojure map into this representation."
  [cfg]
  {:pre [(s/valid? ::cfg cfg)]}
  (let [cfg->kafka (fn [[k v]] [(str/replace (name k) "-" ".") (str v)])]
    (into {} (map cfg->kafka cfg))))
