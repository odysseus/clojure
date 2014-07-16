(ns cljr.core
  (:require [cljr.sicp :as sicp]
            [clojure.string :as s])
  (:gen-class))

(defn qsort [[pivot :as coll]]
  (when pivot
    (lazy-cat (qsort (filter #(< % pivot) coll))
              (filter #{pivot} coll)
              (qsort (filter #(> % pivot) coll)))))

(defn merge* [left right]
  (cond (nil? left) right
        (nil? right) left
        true (let [[l & *left] left
                   [r & *right] right]
               (if (<= l r) (cons l (merge* *left right))
                            (cons r (merge* left *right))))))

(defn merge-sort [L]
  (let [[l & *L] L]
    (if (nil? *L)
      L
      (let [[left right] (split-at (/ (count L) 2) L)]
        (merge* (merge-sort left) (merge-sort right))))))

(defn rand-arr [len maxrange]
  (take len (repeatedly #(rand-int maxrange))))

(defmacro unless [test & exprs]
  `(if (not ~test)
     (do ~@exprs)))

(defn exhibits-oddity? [x]
  (unless (even? x)
          (println "Very odd!")
          (println "Very odd, indeed!")))

(defmacro deklar [& names]
  `(do
     ~@(map #(list 'def %) names)))

(defmacro tim [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (println
       (str "Elapsed Time: "
            (/ (double (- (. System (nanoTime)) start#))
               1000000.0)
            " ms"))
     ret#))

(defmacro infix [left op right]
  `(~op ~left ~right))

(defmacro infixexpr [expr]
  (let [[left op right] expr]
    (list op left right)))

(defmacro randomly [& exprs]
  (let [len (count exprs)
        index (rand-int len)
        conditions (map #(list '= index %) (range len))]
    `(cond ~@(interleave conditions exprs))))

(defmacro randomly-2 [& exprs]
  (rand-nth exprs))

(defmacro random [& exprs]
  (let [len# (count exprs)
        conds# (interleave (range len#) exprs)]
    `(condp = (rand-int ~len#) ~@conds#)))

(defn randomexpr [& exprs]
  (rand-nth exprs))

(defn randprint []
  (random
    (println "mister")
    (println "blue")
    (println "sky")))

(defn -main [& args]
  (randprint)
  (randprint)
  (randprint)
  (randprint)
  (randprint)
  (randprint))
