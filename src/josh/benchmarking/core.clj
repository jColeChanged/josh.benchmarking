(ns josh.benchmarking.core
  (:require [clojure.java.shell :refer [sh]]
            [criterium.core :refer [benchmark report-result]]
            [tech.v3.dataset :as ds]
            [clojure.string :as string]
            [taoensso.timbre :as timbre :refer [debug  info]]))

;; Collect all benchmarks
(defn is-benchmark?
  "Returns true when a function is a benchmark."
  [d]
  (:is-benchmark (meta (second d))))

(def ->benchmark
  "Converts a ns-map entry to a benchmark map."
  (comp
   (partial zipmap [:name :benchmark])
   (juxt (comp str first) second)))

(defn collect-benchmarks
  "Returns all the benchmarks in a namespace."
  [ns]
  (map ->benchmark (filter is-benchmark? (ns-map ns))))





(defn git-commit-id
  []
  (string/trim (:out (sh "git" "rev-parse" "HEAD"))))

(defn timestamp
  []
  (System/currentTimeMillis))

(defn version-information
  []
  {:git-id (git-commit-id) :timestamp (timestamp)})

(defn version-stamp-interceptor
  [event]
  (merge event (version-information)))

(defn run-benchmark
  [bc]
  (info :benchmarking (:name bc))
  (let [result (benchmark (:benchmark bc) {})]
    (report-result result)
    (-> bc
        (merge result)
        (dissoc :benchmark)
        (dissoc :results)
        (update :outliers #(into {} %)))))

(defn benching
  [config]
  (assoc config :results
         (into []
               (comp
                (map run-benchmark)
                (map (apply comp (:event-interceptors config))))
               (:benchmarks config))))


(defn file?
  "Return whether the file exists."
  [filepath]
  (debug :calling "file?" :filepath filepath)
  (.exists (clojure.java.io/file filepath)))

(defn load-dataset
  [config]
  (assoc config :dataset
         (let [filename (-> config :database-config :filename)]
           (info :loading filename)
           (when (file? filename)
             (ds/->dataset filename)))))

(defn merge-datasets
  [config]
  (info :merging-datasets)
  (let [results-dataset (ds/->dataset (:results config))]
    (if (nil? (:datset config))
      results-dataset
      (ds/concat (:dataset config) results-dataset))))


(defn write-dataset
  [config]
  (let [merged-dataset (merge-datasets config)
        filename (-> config :database-config :filename)]
    (info :writing-dataset filename)
    (ds/write! merged-dataset filename)))


(defn -main
  [namespaces]
  (info :benchmarking-started)
  (let [benchmark-configuration
        {:database-config {:filename "benchmarks.edn"}
         :benchmarks (mapcat collect-benchmarks namespaces)
         :event-interceptors [version-stamp-interceptor]}]
    (write-dataset
     (benching (load-dataset benchmark-configuration)))))
