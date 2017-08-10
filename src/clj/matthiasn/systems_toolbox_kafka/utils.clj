(ns matthiasn.systems-toolbox-kafka.utils
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [matthiasn.systems-toolbox.component.helpers :as h]))

(s/def ::bootstrap-servers string?)
(s/def ::group-id string?)
(s/def ::topic (s/or :topic string?
                     :topics (s/coll-of string?)))

(s/def ::cfg
  (s/keys :req-un [::bootstrap-servers
                   ::topic]
          :opt-un [::group-id]))

(defn config->kafka-config
  "Kafka configs are now maps of strings to strings. Morphs an arbitrary clojure
   map into this representation. Assigns random group id when none set."
  [cfg]
  {:pre [(s/valid? ::cfg cfg)]}
  (let [cfg (update-in cfg [:group-id] #(or % (h/make-uuid)))
        cfg->kafka (fn [[k v]] [(str/replace (name k) "-" ".") (str v)])]
    (into {} (map cfg->kafka cfg))))
