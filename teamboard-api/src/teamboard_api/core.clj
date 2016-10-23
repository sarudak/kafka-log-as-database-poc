(ns teamboard-api.core
  (:require [teamboard-api.web :refer [app start-data-reader]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main [& args]
  (println "Starting server")
  (start-data-reader)
  (run-jetty app {:port 8180}))

