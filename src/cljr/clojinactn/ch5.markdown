# Java Interop
### Importing Java Classes
Clojure provides the `import` function to import Java functions

    (import & import-symbols-or-lists)

Import is a variadic function that can take any number of arguments. The arguments are lists where the first argument is the package, followed by the names of the classes you want to import.

    (import
      '(org.apache.hadoop.hbase.client HTable Scan Scanner)
      '(org.apache.hadoop.hbase.filter RegExpRowFilter StopRowFilter))

You can import Java classes in the namespace declaration using the `:import` keyword. 

    (ns com.clojureinaction.book
      (:import (java.util Set)))

### Creating Instances and Accessing Methods

    (import '(java.text SimpleDateFormat))
    (def sdf (new SimpleDateFormat "yyyy-MM-dd"))

The syntax for creating an instance here is fairly simple, `new` is a function that creates a new instance, the class name comes next, followed by the arguments typically used by the constructor method.

Clojure also supports a different syntax for `new` in which the class name is simply followed by a `.` and the arguments it would normally take. Using this form converts it into the appropriate `new` form. The above example in the different syntax.

    (def sdf (SimpleDateFormat. "yyyy-MM-dd"))

Calling static methods from classes is similarly convenient:

    (Long/parseLong "123123456")

You simply use the `Classname/methodname` format that you use for calling methods from other classes within Clojure code. The same format can be used to call static fields.

    (import '(java.util Calendar))
    Calendar/JANUARY
    Calendar/FEBRUARY

### The Dot
All underlying Java interop is done using the `.` operator.  According to the official docs, the dot can be read as "in the scope of". Looking at a few examples:

    (. Classname-symbol method-symbol args*)
    (. Classname-symbol (method-symbol args*))

This allows static methods to be called on the classes in the first argument. The first form is slightly more idiomatic, but the second form makes the call and arguments to the method itself more obvious. Now to look at the syntax for instances.

    (. instance-expr method-symbol args*)
    (. instance-expr (method-symbol args*))

And the following example which uses both of the forms:

    (import '(java.util Random))
    (def rnd (Random. ))

    (. rnd nextInt 10)
    (. rnd (nextInt 10))

The dot operator is normally used inside macros and other generated code because it follows an easily abstracted pattern, for code you write directly the forms described earlier are more idiomatic.

### Chaining Methods
Chaining method calls together in Java is a common pattern that gets a bit ugly in Clojure. Consider this snippet:

    Calenar.getInstance().getTimeZone().getDisplayName()

Converted to Clojure it would look like this:

    (. (. (Calendar/getInstance) (getTimeZone)) (getDisplayName))

Not quite as expressive as the Java version. To make code like this easier to write there's a `..` macro that simply chains together method calls.

    (.. (Calendar/getInstance) getTimeZone getDisplayName)

Or with arguments:

    (..
      (Calendar/getInstance)
      (getTimeZone)
      (getDisplayName true TimeZone/SHORT))

Much better!

### Doto
The `doto` (Do to) macro is used when performing a bunch of operations on the same Java object. So you can take code like this:

    (import '(java.util Calendar))
    (defn the-past-midnight-1 []
      (let [calendar-obj (Calendar/getInstance)]
        (.set calendar-obj Calendar/AM_PM Calendar/AM)
        (.set calendar-obj Calendar/HOUR 0)
        (.set calendar-obj Calendar/MINUTE 0)
        (.set calendar-obj Calendar/SECOND 0)
        (.set calendar-obj Calendar/MILLISECOND 0)
        (.getTime calendar-obj)))

And turn it into code like this:

    (defn the-past-midnight-2 []
      (let [calendar-obj (Calendar/getInstance)]
        (doto calendar-obj
          (.set Calendar/AM_PM Calendar/AM)
          (.set Calendar/HOUR 0)
      (.set Calendar/MINUTE 0)
      (.set Calendar/SECOND 0)
      (.set Calendar/MILLISECOND 0))
    (.getTime calendar-obj)))

That eliminates a lot of redundant calls to `calendar-obj`

### memfn
Suppose you were mapping an instance method to a vector of data:

    (map (fn [x] (.getBytes x)) ["amit" "rob" "kyle"])

One way to make this easier would simply be to use the anonymous function macro:

    (map #(.getBytes %) ["amit" "rob" "kyle"])

"But wait!" You say. ".getBytes is already a function, why can't we just map the call directly!?"

The answer is that instance methods are not first class, and cannot be called that way, they need to operate on something of their class.  However, since there is a syntax for supporting it, you would probably guess that there's a macro or something that makes it easier to write code in this specific instance, and you'd be correct. It's called `memfn` and it converts a member function to a proper function to use in calls like `map`

  (map (memfn getBytes) ["bob", "rob", "sob"])

This also works on instance methods with more than one argument by naming the arguments after the method call:

    (.subSequence "Clojure" 2 5)
    ;; Is equivalent to
    ((memfn subSequence start end) "Clojure" 2 5)

Okay, so in this example it seems to do little more than make the code look less clean, but the value returned by `memfn` is a callable function, so it can be bound to a var and used as a higher order Clojure function

    (def sub-seq (memfn subSequence a b))
    => (sub-seq "Hello" 1 2)
    "e"

### bean
Bean is useful when dealing with Java classes called "beans" that expose their internal variables via getter and setter methods. While the tools above are enough to make this work, via calling the getter and setter as `(.setValue obj x)` this gets tedious, and is not idiomatically Clojure in style. `bean` comes to the rescue by enabling you to convert an object to a hash map instead.


    (bean (Calendar/getInstance))
    ;; Creates

    {:timeInMillis 1257466522295,
     :minimalDaysInFirstWeek 1,
     :lenient true,
     :firstDayOfWeek 1,
     :class java.util.GregorianCalendar

    ;; other properties
    }

Like all Clojure structures, the returned map is immutable.

### Java Arrays
Javarrays? While they aren't used as much in idiomatic Clojure code, they are used heavily in Java code, and as such you'll end up dealing with them a fair amount. Clojure provides a handful of builtin functions for working with Arrays:

- `(alength arr)`: Finds the length of the array.
- `(aget arr 2)`: Returns the element at the index specified.
- `(aset arr 2 "newValue")`: Changes the value at index 2 to "newValue" (Assuming the array was an array of strings.)

Converting sequences into Javarrays can be done using `to-array`, `to-array-2d`, `into-array`. `make-array` allows arbitrary new Java arrays to be created. Finally, there are array specific versions of map and reduce called `amap` and `areduce`

**Mutability Alert** All Java structures are mutable because Java is a mutable language. This can be a good or a bad thing depending, the important thing is to know this. You can't rely on immutability by default on a Java structure as you would by a Clojure structure.

### Implementing Java Interfaces and Extending Classes
On a larger scale, one of the most important pieces of Java interop is implementing a Java interface or subclassing a class written in Java. Clojure provides the elements needed to make this work. Here's an example that extends a class on the Grizzly framework:

    (import '(com.sun.grizzly.tcp.http11 GrizzlyAdapter))
    (proxy [GrizzlyAdapter] []
        (service [req res]
          "Service was called!"))

The general form of this expression is:

    (proxy [class-and-interfaces] [args] fs+)


### Ahead of Time Compiling
It's possible. Chapter 5.
