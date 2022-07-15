(ns josh.benchmarking
  (:require [clojure.java.shell :refer [sh]]
            [criterium.core :refer [benchmark report-result]]
            [tech.v3.dataset :as ds]
            [clojure.string :as string]
            [taoensso.timbre :as timbre
             :refer [debug  info]]))

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

(def benchmark-configuration
  {:database-config {:filename "benchmarks.edn"}
   :benchmarks [{:name "Benchmark name" :benchmark #(Thread/sleep 1000)}]
   :event-interceptors [version-stamp-interceptor]})

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


;; (:dataset (load-dataset benchmark-configuration))

(defn -main
  []
  (info :benchmarking-started)
  (write-dataset
   (benching (load-dataset benchmark-configuration))))

(-main)