(ns producer.core
  (:require [clojure.java.io :as io]
            [clj-time.core :as t])
  (:import  [org.apache.kafka.clients.producer KafkaProducer ProducerRecord]
            [java.util Properties])
  (:gen-class))

(def base-config {"bootstrap.servers"       "ec2-54-191-31-21.us-west-2.compute.amazonaws.com:9092"
                  "group.id"                "test-producer"
                  "enable.auto.commit"      "true"
                  "auto.commit.interval.ms" "1000"
                  "session.timeout.ms"      "30000"
                  "key.serializer"          "org.apache.kafka.common.serialization.StringSerializer"
                  "value.serializer"        "org.apache.kafka.common.serialization.StringSerializer"})

(def topic "test")

(defn gen-properties [props]
  (let [kafka-properties (Properties.)]
    (doall (for [[k v] props] (.put kafka-properties k v)))
    kafka-properties))

(defn produce [data]
  (let [producer (KafkaProducer. (gen-properties base-config))]
    (doall (map #(.send producer (ProducerRecord. topic (str %))) data))))

(defn event->message [event]
  (-> event
      (dissoc :anchor)
      (assoc :audit {:userId (:audit event) :timestamp (str (t/now))})
      ))

(def task-updates (map event->message (:events (read-string (slurp (io/resource "task-updates.edn"))))))



(defn -main
  "Produces 1000 events and then 10 more every 2 seconds"
  [& args]
  (do
    (produce (take 1000 task-updates))
    (loop [remaining-events (drop 1000 task-updates)]
      (produce (take 10 remaining-events))
      (Thread/sleep 2000)
      (recur (drop 10 remaining-events)))))
