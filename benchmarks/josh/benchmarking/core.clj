(ns benchmarks.josh.benchmarking.core
  (:require [josh.benchmarking.core :refer [defbenchmark]]))

(def ^:is-benchmark thread-sleep #(Thread/sleep 1000))
