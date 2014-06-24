(ns cljr.euler)

(defn eu1 []
  (reduce + (filter (fn [x] (or (= (mod x 3) 0) (= (mod x 5) 0))) (range 1 1000))))

(def fib-seq
  "Generates an infinite sequence of fibonacci numbers"
  ((fn rfib [a b]
     (lazy-seq (cons a (rfib b (+ a b)))))
   0 1))

(defn eu2 []
  (reduce + (filter (fn [x] (and (even? x) (< x 4000000))) (take 35 fib-seq))))

(defn prime? [n]
  (.isProbablePrime (BigInteger/valueOf n) 7))

