(defproject teamboard-api "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.apache.kafka/kafka-clients "0.10.0.0"]
                 [cheshire "5.6.3"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring-json-response "0.2.0"]
                 [ring "1.4.0"]]
  :main ^:skip-aot teamboard-api.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
