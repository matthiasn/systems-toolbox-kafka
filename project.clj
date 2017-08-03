(defproject matthiasn/systems-toolbox-kafka "0.6.3"
  :description "Kafka producer and consumer components for systems-toolbox"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj"]

  :dependencies [[org.apache.kafka/kafka_2.11 "0.11.0.0"]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                                  [org.clojure/tools.logging "0.4.0"]
                                  [ch.qos.logback/logback-classic "1.2.3"]
                                  [matthiasn/systems-toolbox "0.6.10"]]}}

  :plugins [[lein-codox "0.10.3" :exclusions [org.clojure/clojure]]
            [test2junit "1.3.3"]
            [lein-ancient "0.6.10"]])
