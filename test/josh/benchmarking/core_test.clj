(ns josh.benchmarking.core-test
  (:use clojure.test josh.benchmarking.core))

(def just-addition (fn [] (+ 1 1)))
(def ^:is-benchmark addition-benchmark just-addition)
(def ^:is-benchmark subtraction-benchmark (fn [] (- 1 1)))

(def benchmark-configuration
  {:database-config {:filename "benchmarks.edn"}
   :benchmarks [addition-benchmark subtraction-benchmark]
   :event-interceptors [version-stamp-interceptor]})
 ;;                       flatten-benchmark
 ;;                       compare-benchmarks]})

(deftest test-benchmark-detection
  (testing "Testing that function which is benchmark is benchmark."
    (is (is-benchmark? #'addition-benchmark)))
  (testing "Testing that function which is not a benchmark is not a benchmark."
    (is (not (is-benchmark? #'just-addition)))))
