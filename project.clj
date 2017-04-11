(defproject matthiasn/systems-toolbox-kafka "0.6.1"
  :description "Kafka producer and consumer components for systems-toolbox"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj"]

  :dependencies [[org.clojure/tools.logging "0.3.1"]
                 [org.clojure/core.async "0.3.442"]
                 [spootnik/kinsky "0.1.16" :exclusions [org.clojure/core.async]]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0-alpha15"]
                                  [matthiasn/systems-toolbox "0.6.6"]]}}

  :plugins [[lein-codox "0.10.3" :exclusions [org.clojure/clojure]]
            [lein-ancient "0.6.10"]])
