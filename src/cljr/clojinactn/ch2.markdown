# Clojure In Action - Chapter 2

## Basics

    $ lein repl

Starts the basic repl

    (println "Hello, world!")

A hash and a basic function to check if the password passed is the correct one.

    (def users {"kyle" {:password "secretk" :number-pets 2}
            "siva" {:password "secrets" :number-pets 4}
            "rob" {:password "secretr" :number-pets 6}
            "george" {:password "secretg" :number-pets 8}})

    (defn check-login [username password]
      (let [actual-password ((users username) :password)]
        (= actual-password password)))

Documentation for anything can be easily found using the `(doc foo)` command at the REPL

    user=> (doc println)
    -------------------------
    clojure.core/println
    ([& more])
      Same as print followed by (newline)
    nil

`find-doc` is similar but it accepts a search string and looks through the documentation for that.

    user=> (find-doc "exponent")
    -------------------------
    clojure.pprint/float-parts-base
    ([f])
      Produce string parts for the mantissa (normalized 1-9) and exponent
    -------------------------
    clojure.pprint/insert-decimal
    ([m e])
      Insert the decimal point at the right spot in the number to match an exponent
    -------------------------
    clojure.pprint/insert-scaled-decimal
    ([m k])
      Insert the decimal point at the right spot in the number to match an exponent
    nil

## Functions
### Defining
An anonymous function is defined as such

    (fn [x y] (+ x y))

Which obviously doesn't give us a way to call it if the call does not happen immediately. Naming a function through `def` is of the form:

    (def adder (fn [x y]
                 (+ x y)))

However, because this is incredibly common there's a built in macro, `defn` that allows a more compact definition, and then expands the function to the proper form before compiling.

    (defn adder [x y]
      (+ x y))

### Variable Arity and Multi-Variadicity
Functions can take a variable number of arguments, and be defined differently for varying arities as well.

    (defn adder
      ([] 0)
      ([x] x)
      ([x y] (+ x y))
      ([x y & z] (+ x y (reduce + z))))

The above function will take any number of arguments, from zero to 100, and return logical values for all of them. The final form `[x y & z]` is used for an unknown number of arguments. The `z` arguments are all placed in a list.

## Other Syntax
### Let
Allows you to rename a piece of evaluated data locally within a function for the purposes of readability and ease of writing.

    (defn average-pets []
      (/ (apply + (map :number-pets (vals users))) (count users)))

This function can be hard to read given the number of operations taking place in here. In an imperative language we would probably use named variables to simplify the code structure, but since we're working in a variable-less functional language, we use `let` to redefine the names of certain terms to make this more readable.

    (defn average-pets []
      (let [user-data (vals users)
            number-pets (map :number-pets user-data)
            total (apply + number-pets)]
        (/ total (count users))))

This redefines the function to make it much more readable. We could even go a step further and bind `(count users)` to something like `total-users` but even without this is very readable.

### Do
Do allows you to process several statements in a row, which is useful when you need a side effect of a function.

    (defn do-many-things []
      (do-first-thing)
      (do-another-thing)
      (return-final-value))

Naturally, side effects go against the whole idea of functional programming. Everything in a functional world evaluates to one thing and side effects do not exist and are certainly never used. Beyond the theory, however, there are a lot of reasons to need side effects, like interacting with a database or closing a file. Side effects are pretty necessary in the real world. Do allows you to process things like this without compromising the functional nature of the language.

### Error Handling
Occurs in a pretty standard try/catch/finally block

    (defn average-pets [users]
      (try
       (let [user-data (vals users)
            number-pets (map :number-pets user-data)
            total (apply + number-pets)]
        (/ total (count users)))
       (catch Exception e
         (println "Error!")
         0)))

### Reader Macros
Do a variety of things to modify the source code that comes after them. The simplest of these is `;` which simply ignores everything after it. There are a number more, the best thing is to simply find a table of them and what they do.

## Conditionals
### If
If takes a test and two actions to perform, one of true and another if false, of the form:

    (if test true-execution false-execution)

### If Not
If-not does the same thing, but executes the first test only if the predicate is false.

    (if-not test false-execution true-execution)

### Cond
Cond is the case statement for Clojure, it's a series of predicates followed by execution statements to evaluate when one is found true. Of note is that Clojure uses one less set of parentheses than other lisps on this, normally parens enclose each pair of statements as well.

    (defn range-info [x]
      (cond
        (< x 0) (println "Negative!")
        (= x 0) (println "Zero!")
        :default (println "Positive!")))

### When
When is a macro that essentially expands to an `if-do`, so it will execute multiple oeprations when the test is true.

    (when (some-condition?)
      (do-this-first)
      (then-that)
      (and-return-this))

### When Not
The opposite of when

## Logical Functions
### And
And returns true only if all the items evaluate to true. It can be used with any number of arguments, calling it with zero returns true.

    => (and true true true true true true true)
    true

### Or
Or evaluates to true if anything is true.

    => (or false false false false false true)
    true

### Not
Simply inverts the truth value of whatever is passed to it.

    => (not (and true true false))
    true

### Comparison Operators
All do what you would expect them to do and a little more, they can be run on an arbitrary number of arguments.

    (< 1 2 3 4 5)
    true

`<` checks to see if every element is larger than the one before it, and distinct, because it evaluates false for equal elements. This essentially checks to see if they are in ascending order, `>` does the same for descending order, and adding the equals sign will allow duplicate elements.

    (>= 5 4 4 3 2 2 1 1)
    true

## Iteration
### While
While continues to evaluate while the test is true. Keep in mind that this requires the code inside the loop to modify the thing being tested, otherwise it will fall into an infinite loop.

    (while (request-on-queue?)
      (handle-request (pop-request-queue)))

### Loop/recur
A fairly important note to anyone coming from another Lisp or another functional language is that Clojure *does not have tail call optimizations*. This has to do with the implementation of Clojure on the JVM and features of the JVM itself that disallow it.

Having said that, you can achieve the same effect (with less code to boot) by using the recursion syntax. A simple Fibonacci example

    (defn fibot [n]
      (loop [a 0 b 1 n n]
        (if (= 1 n)
          b
          (recur b (+ a b) (dec n)))))

The first piece of this is the `loop` call, followed by a vector of bindings. The form for this is `[name value]` so in this case `a = 0`, `b = 1` and the loop local `n` equals the `n` passed in as the argument.

Next you define an end case, likely using `if`. In here the end case is when `n == 1`. Finally, in the `recur` statement you modify all the values that need modifying at the end of each iteration, in the order that they were declared. So `(recur b (+ a b) (dec n)))))` is setting `a = b`, `b = a + b`, and decrements `n`.

`loop/recur` is the only way to do optimized, tail-end recursion in Clojure.

### Doseq
Doseq is essentially a list comprehension. Immediately after `doseq` there is a bdingings section wherein you name the element that will be passed as it iterates over the sequence, in the example below it iterates over the list of all users and gives the name `user` to each. Finally you do whatever is needed with that item, in the case below, running `(run-reports user)`

    (defn run-report [user]
      (println "Running report for" user))

    (defn dispatch-reporting-jobs [all-users]
      (doseq [user all-users]
        (run-reports user)))

While convenient, by far the most common iterative patterns in Clojure, as in other functional languages, is `map filter reduce`

### Map
Map takes a single function of any number of arguments, and a number of sequences to match the number of arguments in the function, it then applies the function iteratively to each item in the sequences using the first sequence for the first argument, the second for the second, etc. It stops when a single sequence runs out of values, in other words it iterates the length of the shortest sequence. Let's start with a simple example

    (defn square [x] (* x x))
    (def r (range 21))
    (map square r)
    >> (0 1 4 9 16 25 36 49 64 81 100 121 144 169 196 225 256 289 324 361 400)

In the example above we define a function that squares values, and the range from 0 to 20, then map that function to that range, returning a sequence of squares. Let's use an example with multiple values.

    (def a (1 2 3 4 5))
    (def b (2 4 6 8 10))
    (def c (range 10 10010 10))
    (map * a b c)
    >> (20 160 540 1280 2500)

Essentially the nth item in the new list is `a[n] * b[n] * c[n]`, and map stops after five values, at which point `a` and `b` have no remaining items.

### Filter
Filter takes a predicate and a sequence and collects values that pass the predicate. for example we could get all even numbers from 1 to 100.

    (filter even? (range 1 101))

Or find a list of divisors for a number within a given range:

    (defn divisor? [a b] (= (mod a b) 0))
    (defn ndivisor? [n] (fn [b] (divisor? n b)))
    (def divisor88? (ndivisor? 88))
    (def factors88 (filter divisor88? (range 1 88)))
    >> (1 2 4 8 11 22 44 88)

This creates two functions, `divisor?` which checks to see if the second argument evenly divides the first argument, and `ndivisor?` which partially evaluates divisor to lock in the value of the first number. Then we return a partially evaluated function that checks to see if the argument divides evenly into 88. Finally we use that to filter the range of numbers from 1 to 88, returning a list of all factors for the number.

### Reduce
A function of many names, reduce also goes by `fold` and `inject` in other languages. In all cases it does the same thing. Reduce takes a function and a sequence and uses that function to reduce everything to a single value. Continuing with our example from before, lets add a function which creates a list of all factors for a number and then use that to find the sum of its divisors.

    (defn factors [n] (filter (ndivisor? n b) (range 1 n)))
    (defn sumdiv [n] (reduce + (factors n)))

Here we take the list of factors and 'inject' or 'fold in' the `+` function, reducing the values of the list to a single value by adding them all together. Another use of this would be averaging the values in a list.

    (defn average [n] (float (/ (reduce + n) (count n))))

### For
`for` does make an appearance in Clojure, but it is used for list comprehensions, much like `doseq`. Here is an example of using `for` to create labels on a chessboard.

    (defn chessboard-labels []
      (for [alpha "abcdefgh"
           num (range 1 9)]
        (str alpha num)))

`for` can also accept `let`, `when`, and `while` clauses. Here's an example of a `when` clause used to find prime numbers less than the given number:

    (defn prime? [x]
      (let [divisors (range 2 (inc (int (Math/sqrt x))))
              remainders (map #(rem x %) divisors)]
        (not (some zero? remainders))))

    (defn primes-less-than [n]
      (for [x (range 2 (inc n))
               :when (prime? x)]
        x))

Now let's do something a bit more complex and add a `let` clause in this function that finds pairs of numbers whose sum is prime.

    (defn pairs-for-primes [n]
      (let [z (range 2 (inc n))]
        (for [x z y z :when (prime? (+ x y))]
          (list x y))))

Finally, for the sake of completeness, an example with a `while` clause:

    (for [x (range 20) :while (not= x 10)] x)
    >> (0 1 2 3 4 5 6 7 8 9)

The difference between when and while is that when will evaluate the body of the loop whenever the condition is true, whereas while evaluates the body of the loop until the condition is false for the first time.

## Useful Macros
### Thread First
The threading macros allow you to write nested operations in a more imperative looking way, which often makes the order of execution, and even the purpose of execution, much easier to understand. Take, for example, an equation to calculate the total amount of a loan based on interest and the number of time periods in the loan.

    (defn final-amount [principle rate time-periods]
      (* (Math/pow (+ 1 (/ rate 100)) time-periods) principle))

This is readable but it takes a bit of parsing on the human side to determine what order everything is being executed. Compare this to the thread first version of the same equation:

    (defn final-amount-> [principle rate time-periods]
      (-> rate
          (/ 100)
          (+ 1)
          (Math/pow time-periods)
          (* principle)))

Thread first takes the first argument passed in and makes it the first argument of the next function, then takes that and makes it the first argument of the function following, and so on. So this expands to:

    (* (Math/pow (+ (/ rate 100) 1) time-periods) principle)

### Thread Last
Thread last does the same basic thing as thread first, but it inserts the value as the second argument to any function in the chain. Take a factorial example:

    (defn factorial [n] (apply * (range 1 (+ 1 n))))

Compared to the thread last version:

    (defn factorial->> [n]
      (->> n
           (+ 1)
           (range 1)
           (apply *)))

This expands to the exact same thing as the above equation.

The point of the threading macros is to allow programmers to define code as a sequence of operations, which are then easily combined into the more functional, nested syntax.

### Apply vs. Reduce
While they generate the same result in many cases, the difference between apply and reduce can be visualized like this:

    (def x '(1 2 3 4 5))
    (apply + x)
    >> (+ 1 2 3 4 5)
    (reduce + x)
    >> (+ (+ (+ (+ 1 2) 3) 4) 5)

In other words, apply simply applies the function to the list by prepending it and evaluating. This obviously will have problems if the function does not support variable arity, but there are also cases in which this behavior is precisely what you are looking for.

## Data Structures
### Truthiness and nil
`nil` is false, `nil` is also the absence of anything. The truthiness of different data is identical to what it is in Ruby. Only `nil` and `false` evaluate to false, `0` and `""` are both true because they represent a type and a value, even if it is an empty one.

### Chars
Are java chars. Because the single quote is used extensively for the quoting macro, you can't use that here, instead there's a reader macro that will read single chars preceeded by a backslash as chars.

    \h \e \l \l \o

### Strings
Strings are java strings and can use all the java string libraries.

   (.toUpperCase "clojure")
    >> "CLOJURE"

### Numbers
All numbers are java boxed numbers and Clojure handles the conversion from `Integer` to `Long` and `BigInteger` if the number exceeds the value for that type.

There is one additional number type in Clojure that is found in other lisps: the ratio. Ratios are essentially the fractional representation of a number, and can be typed in literally or they occur during division. If you don't want a ratio when dividing, cast it to float.

    (def a 3/2)
    >> 3/2
    (def b (/ 49 21))
    >> 7/3
    (float b)
    >> 2.3333333

### Keywords (Symbols)
Keywords in Clojure are essentially the same as symbols in Ruby. They are created once in memory and serve as a symbolic identifier, frequently used as keys inside hash maps.

Keywords are also functions, when called on a hash map they look up the value associated with that keyword.

Symbols, on the other hand, are different from symbols in Ruby. Symbols in Clojure are identifiers that evaluate to the thing they name. So a bound value evaluates to the value, a function name evaluates the function.

Symbols are also functions! Symbols can take a hash map and look themselves up in them.

## Sequences
Putting the "Li" in "Lisp", sequences in clojure have been abstracted to include more than just the traditional singly linked list. Anything that has a sequential structure can imlement ISeq and use the included methods to iterate over the list, in a fashion that is quite similar to Java's Iterable. The three methods of ISeq are `first`, `rest` and `cons`

    (def s (range 1 11))
    (first s)
    >> 1
    (rest s)
    >> (2 3 4 5 6 7 8 9 10)
    (cons 1 s)
    >> (1 1 2 3 4 5 6 7 8 9 10)
    (conj s 1)
    >> (1 1 2 3 4 5 6 7 8 9 10)

As shown by the code above, `first` and `rest` are the traditional `head` and `tail` methods of functional programming.

`cons` adds an element to the front of a list or an array and takes the form `(cons item coll)`, `conj`'s behavior is dependent on the collection being added to. With vectors the item is added to the end, with lists it is pushed onto the front. Being a singly linked list, adding something to the end is an expensive operation because it requires iterating the entire list to find the last element.

### List
The basic data structure of Clojure and most lisps, they are denoted by parentheses filled with values delimited by whitespace

    (1 2 3 4 5)

But wait, this code won't actually work. Because the list is a fundamental Clojure construct, it expects the first item of any list to be a function, followed by its arguments. To create a literal list we need to use the quoting macro.

    (quote (1 2 3 4 5))

Or, to do this much more simply and idiomatically:

    '(1 2 3 4 5)

But while lists are a major part of Clojure, the introduction of other collection types has cut down on the pervasiveness of lists.

### Vectors
Vectors are denoted with square backets and are indexed, the nth element of a collection can be gotten by calling the function `nth` on the vector and with the index.

    (def r (into [] (range 100)))
    (nth r 10)
    >> 10

`into` which is also used here, is a function that converts a collection into another collection. Here we're turning a range (which is a list) into a vector, you can also turn hash maps into multidimensional vectors, or vice versa.

`nth` throws an exception when the index is out of bounds, if you need to avoid this behavior, use `get`, which simply returns nil if the index is out of range.

    (def r (into [] (range 10)))
    (get r 12)
    >> nil

### Hash Maps
Are hash maps, also known as dicitonaries, hashes, maps, or (by no one except the people who write programming books) associative arrays. A sequence of key-value pairs, the keys can be nearly any kind of object. They are defined using curly braces followed by `key-value` declarations.

    (def mapp {:a 1 :b 2 :c 3 :d 4})

Optionally, commas can be used to separate the values into a more readable format. Commas are treated by Clojure as whitespace, and as such are never significant in terms of parsing. Thus, even imporperly comma'ed code like

    (def mapp {:a, 1 :b, 2 :c 3})

Will evaluate to a normal map with the odd arguments as keys and the even arguments as values.

Fetching values from a map is done by calling the map name like a function followed by the key.

    (mapp :a)
    >> 1

Similary, but somewhat more surprisingly, the keys themselves are also functions, so you can call the key and pass it a map and it will return the same thing.

    (:a mapp)
    >> 1

The advantages of allowing both of these to function as functions is code flexibility and function composition, it allows for cleaner code.

Common functions for working with maps are `assoc` and `dissoc` for updating/removing keys.

    (def updated-map (assoc the-map :f 15))
    (dissoc mapp :a)

The first will add a new value and the second will remove a value, but it's important to note that neither of them modify the underlying map, it simply returns a new map with the new strucutre.

### Dealing with nested maps
Nested maps are a common structure but can be difficult to deal with using the functions shown here already. Imagine a map that looks like this:

    (def users {:kyle {
                  :date-joined "2009-01-01"
                  :summary {
                    :average {
                      :monthly 1000
                      :yearly 12000}}}})

Defining a function to update the monthly average would look something like this:

    (defn set-average-in [users-map user type amount]
      (let [user-map (users-map user)
            summary-map (:summary user-map)
            averages-map (:average summary-map)]
        (assoc users-map user
               (assoc user-map :summary
                      (assoc summary-map :average
                             (assoc averages-map type amount))))))

Annoying, but at least it's done. Similarly fetching the value would look something like this:

    (defn average-for [user type]
      (type (:average (:summary (user @users)))))

Because these are common and tedious operations, there are convenience functions defined to make this better. The first is `assoc-in`, which takes a map and then a path of keys to traverse through followed by the value to update.

    (assoc-in users [:kyle :summary :average :monthly] 2000)

If any of the dicitonaries/keys along the way don't exist, they are created. Similarly there is a getter function which uses the same addresslike syntax.

    (get-in users [:kyle :summary :average :monthly])

Finally theres a function called `update-in` that allows you to change a pre-existing value mathematically.

    (update-in users [:kyle :summary :average :yearly] + 1000)

## Fin
