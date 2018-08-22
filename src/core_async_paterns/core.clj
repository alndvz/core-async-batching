(ns core-async-paterns.core
  (:require [clojure.core.async :as as])
  (:gen-class))

(defn timed-take
  ([n t ch]
   (timed-take n t ch nil))
  ([n t ch buf-or-n]
   (let [out (as/chan buf-or-n)]
     (as/go (loop [x 0]
              (when (< x n)
                (let [[v _] (as/alts! [ch (as/timeout t)])]
                  (when (not (nil? v))
                    (as/>! out v)
                    (recur (inc x))))))
            (as/close! out))
     out)))

(defn batch [in]
  (let [out (as/chan)]
    (as/go-loop []
      (let [v (as/<! (->> (timed-take 2 5000 in)
                          (as/reduce conj nil)))]
        (when (not (nil? v))
          (as/>! out v))
        (recur)))
    out))

(defn batch-worker [in cb]
  (let [slots (as/chan (as/buffer 50))]
    (as/go-loop []
      (let [v (as/<! in)]
        (as/>! slots (as/thread
                       (cb v)
                       (as/poll! slots))))
      (recur))))

(def in (as/chan))
(def out (batch-worker (batch in)
                       (fn [v]
                         (Thread/sleep 1000)
                         (println v))))

(comment

  (as/put! in "test")
  (as/poll! out)

  (as/onto-chan in (range 100) false)

  )

(defn -main
  [& args]
  (println "Check my source and execute my code at the REPL!"))
