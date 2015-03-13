(ns cljr.dates
  (:require [clojure.string :as string :refer :all :exclude [reverse replace]])
  (:import (java.text SimpleDateFormat)
           (java.util Calendar GregorianCalendar)))

(defn date [date-string]
  (let [f (SimpleDateFormat. "yyyy-MM-dd")
        d (.parse f date-string)]
    (doto (GregorianCalendar.)
      (.setTime d))))

(defn day-from [d]
  (.get d Calendar/DAY_OF_MONTH))

(defn month-from [d]
  (inc (.get d Calendar/MONTH)))

(defn year-from [d]
  (.get d Calendar/YEAR))

(defn as-string [date]
  (let [y (year-from date)
        m (month-from date)
        d (day-from date)]
    (join "-" [y m d])))
