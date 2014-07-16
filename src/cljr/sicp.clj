(ns cljr.sicp)

(defn square [x] (* x x))
(defn abs [x] (Math/abs x))

(defn sqrt [x]
  (defn average [x y] (/ (+ x y) 2))
  (defn improve [guess] (average guess (/ x guess)))
  (defn good-enough? [guess] (< (abs (- (square guess) x)) 0.0001))
  (defn sqrt-iter [guess]
    (if (good-enough? guess)
      guess
      (sqrt-iter (improve guess))))
  (sqrt-iter 1.0))

(defn cube-root [x]
  (defn cube [x] (* x x x))
  (defn good-enough? [guess]
    (< (abs (- (cube guess) x)) 0.0001))
  (defn improve [guess]
    (/ (+ (/ x (square guess)) (* 2 guess)) 3))
  (defn cube-iter [guess]
    (if (good-enough? guess)
      guess
      (cube-iter (improve guess))))
  (cube-iter 1.0))

;; Classic tail-recursive fibonacci, this does nothing for Clojure, to
;; get proper tail optimization you need to use loop/recur
(defn fibon [n]
  (defn fibo-iter [a b c n]
    (if (= c n)
      b
      (fibo-iter b (+ a b) (inc c) n))))

;; Memoized, tail-call recursive Fibonacci
;; 1N is required to force this to use BigInteger rather than longs
(def fibo
  (memoize
    (fn [n]
      (loop [a 0N b 1N c 1]
        (if (= c n)
          b
          (recur b (+ a b) (inc c)))))))

;; An exponentiating Fn with O(log n) efficiency
(defn fast-expt [b n]
  (cond (= n 0) 1
        (even? n) (square (fast-expt b (/ n 2)))
        :else (* b (fast-expt b (- n 1)))))

;; Euler's algorithm for the greatest common denominator
(defn gcd [a b]
  (if (= b 0)
    a
    (gcd b (mod a b))))

;; Beginning of the methods to test primality
(defn smallest-divisor [n]
  (defn divides? [a b]
    (= (mod b a) 0))
  (defn find-divisor [a b]
    (cond (> (square b) a) n
          (divides? b a) b
          :else (find-divisor a (inc b))))
  (find-divisor n 2))

(defn is-prime? [n]
  (= n (smallest-divisor n)))

;; Fermat's Little Theorem -- Beginning of a better way to test primality
(defn expmod [base exp m]
  (cond (= exp 0) 1
        (even? exp) (mod (square (expmod base (/ exp 2) m)) m)
        :else (mod (* base (expmod base (- exp 1) m)) m)))

