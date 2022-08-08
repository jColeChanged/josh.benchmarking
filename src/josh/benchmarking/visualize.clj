(ns josh.benchmarking.visualize
  (:require [tech.viz.vega :as vega]
            [tech.v3.dataset :as ds]
            [tech.v3.datatype.datetime :as dtype-dt]))



;; Loads the dataset and converts epoch longs to datetimes.

(first (:timestamp (ds/->dataset "benchmarks.edn" 
                                 {:parser-fn {:timestamp 
                                              [:packed-local-date-time (fn [data]
                                                     (java.time.LocalDateTime/parse data))]}})))                                                                         }})))


(-> (ds/->dataset "benchmarks.edn"
                  {:parser-fn {:timestamp
                               [:local-date-time (fn [data]
                                                          (java.time.LocalDateTime/parse data))]}})
    (ds/update-column :timestamp dtype-dt/datetime->milliseconds)
    (ds/mapseq-reader)
    (vega/time-series :timestamp :mean {:label-key :name :title "Benchmarks" :background "white"})
    (vega/vega->svg-file "bencmark.svg"))