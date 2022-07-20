(ns leiningen.test.benchmarking
  (:use clojure.test leiningen.benchmark))

(def sample-project-default-sourcepath
  '(defproject player-of-games "0.1.0-SNAPSHOT"
     :description "Really cool game player."
     :dependencies [[org.clojure/clojure "1.10.0"]]
     :plugins [[josh.benchmarking "0.2.0-SNAPSHOT"]]))

(def sample-project-non-default-sourcepath
  '(defproject player-of-games "0.1.0-SNAPSHOT"
     :description "Really cool game player."
     :dependencies [[org.clojure/clojure "1.10.0"]]
     :benchmark-paths [["strangegame/"]]
     :plugins [[josh.benchmarking "0.2.0-SNAPSHOT"]]))

(deftest test-sourcepaths-configuration
  (testing "default sourcepath"
    (is (= ["benchmarks/"]
           (get-benchmark-sourcepaths sample-project-default-sourcepath))))
  (testing "custom sourcepath"
    (is (= ["strangegame/"])
        (get-benchmark-sourcepaths sample-project-non-default-sourcepath))))