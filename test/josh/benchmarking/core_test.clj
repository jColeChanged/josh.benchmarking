(ns josh.benchmarking.core-test
  (:require [josh.benchmarking.core :refer [->benchmark]])
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
    (is (not (is-benchmark? #'just-addition))))
  (testing "Testing that ns-map forms are detected as benchmarks"
    (is (ns-is-benchmark? (list "addition-benchmark" #'addition-benchmark)))))

(deftest test-benchmark-creation
  (testing "Testing that benchmark is created."
    (let [benchmark (->benchmark (list "addition-benchmark" addition-benchmark))]
      (is (= (:name benchmark) "addition-benchmark"))
      (is (= (:benchmark benchmark) addition-benchmark)))))

(deftest test-version-stamping
  (testing "Testing that we get git commit ids"
    (is (not= (git-commit-id) "")))
  (testing "Testing that we get timestamps"
    (is (number? (timestamp))))
  (testing "Testing that we get version information."
    (let [version-information (version-information)]
      (is (:git-id version-information))
      (is (:timestamp version-information)))))
