(ns josh.benchmarking.core-test
  (:require [josh.benchmarking.core :refer [version-stamp-interceptor
                                            is-benchmark?
                                            ->benchmark flatten-benchmark
                                            ns-is-benchmark?
                                            git-commit-id
                                            run-benchmark
                                            timestamp
                                            flatten-stat
                                            load-dataset
                                            version-information
                                            benching
                                            write-dataset
                                            create-benchmark-comparison-interceptor
                                            add-benchmark-comparison-interceptor]]
                                            
            [clojure.test :refer [deftest testing is]])) 

(def just-addition (fn [] (+ 1 1)))
(def ^:is-benchmark addition-benchmark just-addition)
(def ^:is-benchmark subtraction-benchmark (fn [] (- 1 1)))

(def benchmark-configuration
  {:database-config {:filename "benchmarks.edn"}
   :benchmarks [{:name "addition-benchmark" 
                 :benchmark addition-benchmark}
                {:name "subtraction-benchmark"
                 :benchmark subtraction-benchmark}]
   :event-interceptors [version-stamp-interceptor flatten-benchmark]})


(def benchmark-configuration-with-dataset
  {:database-config {:filename "test/test-benchmarks.edn"}
   :benchmarks [{:name "addition-benchmark" 
                 :benchmark addition-benchmark}
                {:name "subtraction-benchmark"
                 :benchmark subtraction-benchmark}]
   :event-interceptors [version-stamp-interceptor flatten-benchmark]})


(def empty-benchmark-configuration-dataset
  {:database-config {:filename "test/empty-benchmarks.edn"}
   :benchmarks [{:name "addition-benchmark" 
                 :benchmark addition-benchmark}
                {:name "subtraction-benchmark"
                 :benchmark subtraction-benchmark}]
   :event-interceptors [version-stamp-interceptor flatten-benchmark]})


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
    (is (timestamp)))
  (testing "Testing that we get version information."
    (let [version-information (version-information)]
      (is (:git-id version-information))
      (is (:timestamp version-information))))
  (testing "Testing that benchmarks can be version stamped"
    (let [benchmark (-> (list "addition-benchmark" addition-benchmark)
                        ->benchmark
                        version-stamp-interceptor)]
      (is (:git-id benchmark))
      (is (:timestamp benchmark)))))


(deftest test-single-benchmark-runnable
  (let [benchmark (->benchmark (list "addition-benchmark" addition-benchmark))
        benchmark-results (run-benchmark benchmark)]
    (testing "Tests that a benchmark run returns results"
      (is (map? benchmark-results)))
    (testing "Tests that flattening a single stat works." 
      (is (map? ((flatten-stat :mean) benchmark-results))))
    (testing "Test that flattening stats works."
      (is (map? (flatten-benchmark benchmark-results))))))


(deftest test-benchmark-config-runnable 
  (testing "Tests that a benchmark config run returns results"
    (is (map? (benching benchmark-configuration)))))

(deftest test-benchmark-loading
  (testing "Tests that benchmarks can be loaded"
    (is (load-dataset 
         {:database-config {:filename "tests/test-benchmarks.edn"}})))
  (testing "Tests that empty benchmarks can be loaded"
   (is (load-dataset
        {:database-config {:filename "tests/empty-benchmarks.edn"}}))))
  

(defn benchmark-accreter
  []
  (write-dataset (benching (load-dataset benchmark-configuration))))

(deftest create-benchmark-dataset
  (testing "That a benchmark dataset can be generated and saved repeatedly."
    (dotimes [_ 3]
      (benchmark-accreter))
    (is (map? (load-dataset benchmark-configuration)))))

(deftest compare-benchmarks
  (testing "That a benchmark comparison interceptor can be created."
    (is (create-benchmark-comparison-interceptor
         (load-dataset
          {:database-config {:filename "tests/test-benchmarks.edn"}}))))
  (testing "That a benchmark comparison interceptor can be added to a benchmark config."
    (is (> (count (:event-interceptors
                   (add-benchmark-comparison-interceptor
                    (load-dataset benchmark-configuration-with-dataset))))
           (count (:event-interceptors benchmark-configuration-with-dataset)))))
  (testing "That benchmark comparison produces printed results."
    (is (benching (add-benchmark-comparison-interceptor
                   (load-dataset benchmark-configuration-with-dataset)))))
  (testing "That benchmark doesn't fail to run when empty."
    (is (benching (add-benchmark-comparison-interceptor
                   (load-dataset empty-benchmark-configuration-dataset))))))