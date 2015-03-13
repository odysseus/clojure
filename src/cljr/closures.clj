(ns cljr.closures)

(def tax 0.08)

(defn price-with-tax []
  (fn [y] (* y (inc tax))))
