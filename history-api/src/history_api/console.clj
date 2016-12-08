(ns history-api.console
  (:require
    [teamboard-api.web :refer [app start-data-reader]]
    [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main [& args]
  (Thread/sleep 5000)
  (println "Starting server")
  (start-data-reader)
  (run-jetty app {:port 8180}))