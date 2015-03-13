(ns cljr.core
  (:use clojure.test)
  (import java.util.Date)
  (:require [cljr.sicp :as sicp]
            [clojure.string :as s]
            [cljr.dates :as dates]
            [cljr.dates_spec :as dates-spec]
            [cljr.closures :as closures])
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

(defmacro assert-true [test-expr]
  (if-not (= 3 (count test-expr))
    (throw (RuntimeException.
         "Argument must be of the form
               (operator test-expr expected-expr)")))
  (if-not (some #{(first test-expr)} '(< > <= >= = not=))
    (throw (RuntimeException.
       "operator must be one of < > <= >= = not=")))
  (let [[operator lhs rhs] test-expr]
    `(let [lhsv# ~lhs rhsv# ~rhs ret# ~test-expr]
       (if-not ret#
         (throw (RuntimeException.
            (str '~lhs " is not " '~operator " " rhsv#)))
         true))))

(defmacro assert-false [test-expr]
  (if-not (= 3 (count test-expr))
    (throw (RuntimeException.
         "Argument must be of the form
               (operator test-expr expected-expr)")))
  (if-not (some #{(first test-expr)} '(< > <= >= = not=))
    (throw (RuntimeException.
       "operator must be one of < > <= >= = not=")))
  (let [[operator lhs rhs] test-expr]
    `(let [lhsv# ~lhs rhsv# ~rhs ret# ~test-expr]
       (if ret#
         (throw (RuntimeException.
            (str '~lhs " is " '~operator " " rhsv#)))
         true))))

(def assert-equal #(assert-true (= %1 %2)))

(def assert-not-equal #(assert-true (not= %1 %2)))

(defn time-diff [a b]
  (let [ta (.getTime a)
        tb (.getTime b)]
    (if (> tb ta)
      (float (/ (- tb ta) 1000))
      (float (/ (- ta tb) 1000)))))

(defn now []
  (Date.))

(defn enclosed []
  (let [d1 (Date.)]
    (fn []
      (time-diff d1 (Date.)))))

(defn -main [& args]
  (def d (enclosed))
  (Thread/sleep 1000)
  (println (d)))
