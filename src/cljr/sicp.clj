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

