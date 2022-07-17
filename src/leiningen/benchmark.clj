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
  (ffilter #(= `josh.benchmarking/josh.benchmarking (first %)) (:plugins project)))

(defn get-benchmark-sourcepaths [project]
  (get-in project [:benchmark-paths] ["benchmarks/"]))

(defn benchmark-namespaces
  [project]
  (sort
    (namespaces-on-classpath
      :classpath
      (map io/file (get-benchmark-sourcepaths project)))))



(defn parse-args
  "Takes the command args as input and returns a vector with two elements. The
   first is a set of the specified environments (the name key in each
   environment) and the second is a map of the options given."
  [args]
  (let [[environments options] (split-with #(not (.startsWith % "-")) args)]
    [(into #{} environments)
     (into {} (map (fn [opt-str] (vector (keyword (apply str
                                                         (filter #(not= \- %)
                                                                 opt-str)))
                                         true)) options))]))

(defn benchmark
  "Run the performance tests in the :benchmarks-path directory."
  [project & args]
  (let [benchmark-profile (merge {:source-paths (get-benchmark-sourcepaths project) 
                                  :dependencies [(get-benchmark-dependency project)]}
                                 (get-in project [:profiles :josh.benchmarking]))
        project (project/merge-profiles project [benchmark-profile])
        [specified-environments options] (parse-args args)
        environments [{:namespaces (benchmark-namespaces project)}]
        _ (println (:source-paths project))
        ]
    (doseq [{:keys [name profiles namespaces fixtures] :as environment}
            environments]
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