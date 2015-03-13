(ns cljr.dates_spec
  (:use cljr.dates)
  (:use clojure.test))

(deftest simple-date-parsing
  (let [d (date "2012-03-16")]
    (is (= (day-from d) 16))
    (is (= (month-from d) 3))
    (is (= (year-from d) 2012))))

(deftest test-as-string
  (let [d (date "2012-03-16")]
    (is (= (as-string d) "2012-3-16"))))
