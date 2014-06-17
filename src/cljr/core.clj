(ns cljr.core
  (:gen-class))

;; Data structure with post headers and lengths
(def post-headers [{:title "first one ever" :length 430}
                   {:title "second baby step" :length 650}
                   {:title "three is company" :length 720}
                   {:title "fourth for the road" :length 190}
                   {:title "five again" :length 280}])

;; Fn to find if the header is too long based on the given value
(defn long-post-headers [threshold-length headers]
  ;; Nested Fn definition, also functions as a closure over threshold-length
  (let [is-long? (fn [header]
                   (> (header :length) threshold-length))]
    (filter is-long? headers)))

(defn long-post-titles [threshold-length headers]
  (map :title (long-post-headers threshold-length headers)))

;; Data is immutable
(def expenses [{:amount 12.99 :merchant "amazon"}])
;; Expenses remains the same after adding this
(def updated-expenses (conj expenses {:amount 199.95 :merchant "frys"}))

;; Lazy evaluation example, an infinite sequence of all Fibonacci terms
(defn next-terms [a b]
  (let [c (+ a b)]
    ;; This is a macro for creating infinite sequences
    (lazy-seq
      (cons c
            (next-terms b c)))))
(defn fibonacci [a b]
  (concat [a b]
          (next-terms a b)))

;; Transactional Mutations
(def total-expenditure (ref 0))

;; The following will throw a "No transaction running"
;; IllegalStateException exception
 ;(defn add-amount [amount]
  ;(ref-set total-expenditure (+ amount @total-expenditure)))

;; The following will work fine because it will do the update inside a
;; transaction

(defn add-amount [amount]
  ;; A macro for creating safe transactions
  (dosync
    ;; @ is a reader macro that fetches the value of total-expenditure
    (ref-set total-expenditure (+ amount @total-expenditure))))

(def users {"kyle" {:password "secretk" :number-pets 2}
            "siva" {:password "secrets" :number-pets 4}
            "rob" {:password "secretr" :number-pets 6}
            "george" {:password "secretg" :number-pets 8}})

(defn check-login [username password]
  (let [actual-password ((users username) :password)]
    (= actual-password password)))

;; Docs
;; (doc fn) will give you a short doc about the function
;; (find-doc "string") will search the docs for that string and return
;; all the results

(defn even [n]
  (cond
    (= (mod n 2) 0) (println "Even!")
    :default (println "Odd!")))

;; Commas can be used or not, the following two are equivalent
(def x [1 2 3 4 5])
(def y [1, 2, 3, 4, 5])

;; defn is actually a macro used to define functions, the macroless syntax
;; is below, followed by the same thing in defn

(def add (fn [x y]
           (+ x y)))

(defn addn [x y]
  (+ x y))

(comment
  comment is a macro and anything inside it gets commented out, cool)

;; Breaking up long code into readable bits
(defn average-pets []
  (/ (apply + (map :number-pets (vals users))) (count users)))
;; The Fn definition gives you the idea here, but it isn't very readable
;; You can use let to fix this problem by defining local names
(defn avg-pets []
  (let [user-data (vals users)
        number-pets (map :number-pets user-data)
        total-pets (apply + number-pets)
        total-users (count users)]
    (/ total-pets total-users)))

;; If you ever call a function specifically for a side effect, such as println
;; but this occurs in a context where you need to assign it a value regardless,
;; such as a let statement, you use a _ for the name by convention. This is
;; also useful when values aren't need when taking apart a data structure

(comment
;; Procedural Programming
;; While functional and procedural are not the most compatible idioms, there
;; is often a need to simply perform tasks in a row for things like writing
;; to a log file or using a database, Clojure supports these through the do
;; statement
(if (test-something)
  (do
    (log-message "blah was true")
    (store-something-in-db)
    (return-the-value))))

;; Doing it this way allows you to combine multiple expressions into one
;; in the true branch of the if statement, which would normally only
;; execute a single s-expression

;; Error Handling
;; An example of error handling with average pets
(defn averr-pets [users]
  (try
    (let [user-data (vals users)
          number-pets (map :number-pets user-data)
          total (apply + number-pets)]
      (/ total (count users)))
      (catch Exception e
        (println "Error!")
        0)))

;; Random Euler and Fibonacci methods
(defn eu1 []
  (defn eu1iter [n total]
    (if (>= n 1000)
      total
      (if (or (= (mod n 5) 0) (= (mod n 3) 0))
        (eu1iter (+ n 1) (+ total n))
        (eu1iter (+ n 1) total))))
  (eu1iter 3 0))

(defn fibo [n]
  (defn fiboiter [a b found stop]
    (if (= found stop)
      b
      (fiboiter b (+ a b) (+ found 1) stop)))
  (fiboiter 0 1 1 n))

;; Reader Macros
;; Reader macros modify the source code that follows them, the simplest
;; example being comments which ignore anything written after them

; Basic Conditionals

; If evaluates the first statement if true, the second if false
(if (> 5 3)
  true
  false)

; Cond takes a list of condition-evaluation pairs
(defn isZero? [x]
  (cond
    (< x 0) -1
    (> x 0) 1
    (= x 0) 0))

; When is basically (if true (do ... Without the alternative clause
; It can execute multiple lines of code when a condition is true
(defn whenTest [n]
  (when (> n 0)
    (println "One")
    (println "Two")
    (println "Three")))

;; When not does the same basic thing, only for false expressions
(defn whenNotTest [n]
  (when-not (> n 0)
    (println "Three")
    (println "Two")
    (println "One")))

;; Logical Testing
(comment (and (> 2 1) (< 5 4)))
;; and will also accept more arguments, it only evaluates to true if
;; all of them are true

;; or evaluates to true if any of the conditions are true
(comment (or (= (mod 15 5) 0) (= (mod 15 3) 0)))

;; not inverts a logical condition
(comment (not true))

;; Finally, all the normal comparison operators are there, but they
;; can take any number of arguments. When used on a list they
;; essentially test whether that list is in increasing or decreasing
;; order depending on the operator you use
(comment (< 1 2 3))

(defn -main [& args]
  (println "Hello, World!")
  (println (long-post-titles 300 post-headers))
  (println expenses)
  (println updated-expenses)
  ;; Without using a Fn like take the fibonacci sequence would go infinitely
  (println (take 10 (fibonacci 0 1)))
  ;; Mutable data in a transactional form
  (println total-expenditure)
  (add-amount 100)
  (println total-expenditure)
  ;; Using java methods in Clojure
  (println (.toUpperCase "clojure"))
  (println (check-login "george" "secretg"))
  (println (check-login "rob" "notright"))
  (even 5)
  (even 2)
  (println x y)
  (println (eu1))
  (println (fibo 10))
  (println (average-pets) (avg-pets))
  (println (isZero? -3))
  (whenTest 3)
  (whenNotTest -3)
  ; And accepts an arbitrary number of arguments
  (println (and true true true true true false))
  (println (or false false false false true false))
  (println (< 1 2 3 4 5))
  (println (> 5 4 3 2 1))
  (println (<= 1 2 3 3))
  (println (= 1 1 1 1))
  )
