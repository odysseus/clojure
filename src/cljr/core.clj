(ns cljr.core
  (:require [cljr.euler :as eu]
            [cljr.sicp :as sicp])
  (:gen-class))

(defn -main [& args]
  (println (sicp/sqrt 25))
  (println (sicp/cube-root 27))
  )
