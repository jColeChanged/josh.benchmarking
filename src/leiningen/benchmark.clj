(ns leiningen.benchmark
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project])
  (:use [bultitude.core :only [namespaces-on-classpath]]))


(def ffilter (comp first (partial filter)))

(defn get-benchmark-dependency
  "Returns the plugins entry in the plugins key for this library."
  [project]
  (let [dep (ffilter #(= `org.clojars.joshua/josh.benchmarking (first %)) (:plugins project))]
    (if (nil? dep) [] [dep])))

(defn get-benchmark-sourcepaths [project]
  (get-in project [:benchmark-paths] ["benchmarks/"]))

(defn benchmark-namespaces
  "Return all the namespaces on the classpath in sorted order."
  [project]
  (sort
    (namespaces-on-classpath
      :classpath
      (map io/file (get-benchmark-sourcepaths project)))))


(defn benchmark
  "Run the performance tests in the :benchmarks-path directory."
  [project & args]
  (let [benchmark-profile (merge {:source-paths (get-benchmark-sourcepaths project)
                                  :dependencies (get-benchmark-dependency project)}
                                 (get-in project [:profiles :josh.benchmarking]))
        project (project/merge-profiles project [benchmark-profile])
        options {}
        environments [{:namespaces (benchmark-namespaces project)}]]
    (doseq [{:keys [profiles namespaces fixtures]} environments]
      (println "Running benchmarks...")
      (println "======================")
      (let [project (project/merge-profiles project profiles)
            action `(do
                      (when (seq '~namespaces)
                        (apply require :reload '~namespaces))
                      (josh-bench/-main ~options '~namespaces))]
        (eval/eval-in-project project
                              action
                              `(require
                                ['~'josh.benchmarking.core :as '~'josh-bench]
                                ~@(map
                                   #(list 'quote (symbol (namespace %)))
                                   fixtures)))))))