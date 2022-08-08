(defproject josh.benchmarking "0.1.0-SNAPSHOT"
  :description "A benchmark automation utility."
  :url "https://github.com/jcolechanged/josh.benchmarking"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [techascent/tech.ml.dataset "6.091"]
                 [com.taoensso/nippy "3.1.3"]
                 [com.taoensso/timbre "5.2.1"]
                 [criterium "0.4.6"]
                 [techascent/tech.viz "6.00-beta-16-3"]]
  :josh.benchmarking {:benchmark-paths ["benchmarks/"] 
                      :environments [{:namespaces [josh.benchmarking.core]}]}
  :eval-in :leiningen)
