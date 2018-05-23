(defproject matthiasn/systems-toolbox-kafka "0.6.17"
  :description "Kafka producer and consumer components for systems-toolbox"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj"]

  :dependencies [[org.apache.kafka/kafka_2.11 "1.1.0"]
                 [com.cognitect/transit-clj "0.8.309"]
                 [io.netty/netty "4.0.0.Alpha8"]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [org.clojure/tools.logging "0.4.1"]
                                  [ch.qos.logback/logback-classic "1.2.3"]
                                  [matthiasn/systems-toolbox "0.6.35"]]
                   :exclusions [org.slf4j/slf4j-nop
                                commons-logging
                                log4j/log4j
                                org.slf4j/slf4j-log4j12]
                   :jvm-opts     ["-Dlog_appender=consoleAppender"
                                  "-Dlog_level=DEBUG"
                                  "-XX:-OmitStackTraceInFastThrow"]}}


  :plugins [[lein-codox "0.10.3" :exclusions [org.clojure/clojure]]
            [test2junit "1.4.0"]
            [lein-ancient "0.6.15"]])
