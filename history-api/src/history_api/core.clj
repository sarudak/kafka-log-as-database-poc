(ns history-api.core
      (:require [cheshire.core :refer :all]
                [history-api.user-seed :as user-seed]
                [compojure.core :refer :all]
                [compojure.route :as route]
                [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
                [ring.adapter.jetty :refer [run-jetty]])
      (:import  [org.apache.kafka.clients.consumer KafkaConsumer ConsumerRecords ConsumerRecord]
                [java.util Properties]))

(def task-history-data {})

(def user-data
     (atom
       (->> user-seed/data
            (map (fn [item] [(:userId item) (:name item)]))
            (into {}))))

(def base-config {"bootstrap.servers"       "kafka:9092"
                  "group.id"                "testing6"
                  "enable.auto.commit"      "true"
                  "auto.commit.interval.ms" "1000"
                  "auto.offset.reset"       "earliest"
                  "session.timeout.ms"      "30000"
                  "key.deserializer"        "org.apache.kafka.common.serialization.StringDeserializer"
                  "value.deserializer"      "org.apache.kafka.common.serialization.StringDeserializer"})



(defn gen-properties [props]
      (let [kafka-properties (Properties.)]
        (doall (for [[k v] props] (.put kafka-properties k v)))
        kafka-properties))

(defn percent->status [percent-complete]
      (get {0 "Not Started" 100 "Complete"} percent-complete "In Progress"))

(defn create-task! [{:keys [taskId projectId name percentComplete]}]
  (swap! task-history-data merge-with
         #(merge-with  % [projectId :tasks taskId]
                          {:taskId taskId :name name
                            :status (percent->status percentComplete)})))

(defn update-task! [{:keys [taskId percentComplete]}]
      (let [project-id (@task-to-project taskId)]
        (swap! project-data #(assoc-in % [project-id :tasks taskId :status] (percent->status percentComplete)))))

(defn update-from! [message]
      (let [message-data (parse-string (.value message) true)
            message-type (:type message-data)]
        (if (= "TaskCreated" message-type)
          (create-task! message-data)
          (update-task! message-data))))

(defn data-reader-loop []
      (let [consumer (KafkaConsumer. (gen-properties base-config))]
        (.subscribe consumer ["tasks"])
        (loop []
          (let [messages (seq (.poll consumer 1000))]
            (doall (map update-from! messages))
            (recur)))))

(defn start-data-reader []
      (.start (Thread. data-reader-loop)))

(defroutes app-routes
           (GET "/project/:id" [id] (generate-string (@project-data id))))

(def app
     (-> app-routes
         (wrap-defaults api-defaults)))

