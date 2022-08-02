(ns josh.benchmarking.core
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as string]
            [criterium.core :refer [benchmark report-result]]
            [taoensso.timbre :as timbre :refer [debug info]]
            [tech.v3.dataset :as ds]
            [tech.v3.dataset.rolling :as ds-roll]))
  

(defn is-benchmark?
  "Returns true when a function is a benchmark."
  [d]
  {:post [(boolean? %)]}
  (contains? (meta d) :is-benchmark))

(def ns-is-benchmark? (comp is-benchmark? second))

(def ->benchmark
  "Converts a ns-map entry to a benchmark map."
  (comp
   (partial zipmap [:name :benchmark])
   (juxt (comp str first) second)))

(defn collect-benchmarks
  "Returns all the benchmarks in a namespace."
  [ns]
  (map ->benchmark (filter ns-is-benchmark? (ns-map ns))))

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

(defn label-upper [field] (keyword (str (name field) "-ub")))
(defn label-lower [field] (keyword (str (name field) "-lb")))
(defn center [[stat [_ _]]] stat)
(defn upper [[stat [_ upper]]] (+ stat upper))
(defn lower [[stat [lower _]]] (+ stat lower))

(defn flatten-stat
  "Flattens a stat field."
  [field]
  (fn [coll]
    (-> coll
        (assoc (label-upper field) (upper (get coll field)))
        (assoc (label-lower field) (lower (get coll field)))
        (assoc field (center (field coll))))))

(defn flatten-benchmark
  "Flattens a benchmark."
  [coll]
  (-> coll
      ((flatten-stat :mean))
      ((flatten-stat :sample-mean))
      ((flatten-stat :variance))
      ((flatten-stat :sample-variance))
      ((flatten-stat :upper-q))
      ((flatten-stat :lower-q))))

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

(defn recent-comparison
  [dataset new-benchmark]
  (last (:mean (ds-roll/rolling (get (ds/group-by dataset :name) (get new-benchmark :name))
                   {:window-type :fixed
                    :window-size 2
                    :relative-window-position :left}
                   {:mean (ds-roll/mean :mean)}))))

;; (defn local-comparison
;;   [dataset new-benchmark] 
;;   (:mean (ds-roll/rolling (get new-benchmark "name") (ds/group-by dataset :name))
;;                    {:window-type :fixed
;;                     :window-size 10
;;                     :relative-window-position :left}
;;                    {:mean (ds-roll/mean :mean)}))

(defn compare-benchmarks
  [dataset benchmark]
  (info "Recent" (recent-comparison dataset benchmark) "vs Just now" (get benchmark :mean))
  ;;(info (local-comparison dataset benchmark))
  benchmark)

(defn create-benchmark-comparison-interceptor
  [config]
  (partial compare-benchmarks (:dataset config)))

(defn add-benchmark-comparison-interceptor
  [config]
  (update config :event-interceptors conj (create-benchmark-comparison-interceptor config)))

(defn -main
  [settings namespaces] 
  (info :benchmarking-started 
        :namspaces namespaces
        :settings settings)
  (let [benchmark-configuration
        {:database-config {:filename "benchmarks.edn"}
         :benchmarks (mapcat collect-benchmarks namespaces)
         :event-interceptors [version-stamp-interceptor
                              flatten-benchmark]}]
    (write-dataset  
     (benching 
      (add-benchmark-comparison-interceptor
       (load-dataset benchmark-configuration))))))
