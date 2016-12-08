(ns teamboard-api.web
  (:require [cheshire.core :refer :all]
            [teamboard-api.project-seed :as project-seed]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.json-response :refer [json-response]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:import  [org.apache.kafka.clients.consumer KafkaConsumer ConsumerRecords ConsumerRecord]
            [java.util Properties]))

(def task-to-project (atom {}))

(def project-data
  (atom
    (->> project-seed/data
         (map (fn [item] [(:projectId item) (dissoc item :anchor)]))
         (into {}))))

(def base-config {"bootstrap.servers"       "ec2-54-191-31-21.us-west-2.compute.amazonaws.com:9092"
                  "group.id"                "testing8"
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
  (cond
    (not percent-complete) "Not Started"
    (>= percent-complete 100) "Complete"
    (>= percent-complete 80) "Testing"
    (>= percent-complete 30) "In Progress"
    (>= percent-complete 1) "Analysis"
    :else "Not Started"))

(defn create-task! [{:keys [taskId projectId name percentComplete]}]
  (do (swap! task-to-project assoc taskId projectId)
      (swap! project-data #(assoc-in % [projectId :tasks taskId]
                                     {:taskId taskId :name name
                                      :status (percent->status percentComplete)}))))

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
    (.subscribe consumer ["tasks-demo"])
    (loop []
      (let [messages (seq (.poll consumer 1000))]
        (doall (map update-from! messages))
        (recur)))))

(defn start-data-reader []
  (.start (Thread. data-reader-loop)))

(defroutes app-routes
           (GET "/project/:id" [id] (json-response (@project-data id)))
           (GET "/task/:id" [id] (json-response (@project-data (@task-to-project id)))))

(def app
  (-> app-routes
      (wrap-defaults api-defaults)))
