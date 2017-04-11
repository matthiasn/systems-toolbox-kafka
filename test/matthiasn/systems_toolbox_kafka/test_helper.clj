(ns matthiasn.systems-toolbox-kafka.test-helper)

(defmacro eventually
  "Generic assertion macro, that waits for a predicate test to become true.
  'form' is a predicate test, that clojure.test/is can understand
  'timeout': optional; in ms; how long to wait in total (defaults to 5000)
  'interval' optional; in ms; how long to pause between tries (defaults to 10)

      Example:
      Since this will fail half of the time ...
        (is (= 1 (rand-int 2)))

      ... use this:
        (eventually (= 1 (rand-int 2)))

  Borrowed from: https://github.com/otto-de/tesla-microservice"
  [form & {:keys [timeout interval]
           :or   {timeout  10000
                  interval 100}}]
  `(let [start-time# (System/currentTimeMillis)]
     (loop []
       (let [last-stats# (atom nil)
             pass?# (with-redefs [clojure.test/do-report (fn [s#] (reset! last-stats# s#))]
                      (clojure.test/is ~form))
             took-too-long?# (> (- (System/currentTimeMillis) start-time#) ~timeout)]
         (if (or pass?# took-too-long?#)
           (clojure.test/do-report @last-stats#)
           (do
             (Thread/sleep ~interval)
             (recur)))))))
