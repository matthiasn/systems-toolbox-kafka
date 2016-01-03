(defproject matthiasn/systems-toolbox-kafka "0.1.2"
  :description "Kafka producer and consumer components for systems-toolbox"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-kafka "0.3.4"]
                 [com.taoensso/nippy "2.11.0-beta1"]]

  :plugins [[lein-codox "0.9.1" :exclusions [org.clojure/clojure]]])
