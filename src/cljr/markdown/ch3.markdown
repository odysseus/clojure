# Chapter 3 - Clojure In Action
## Functions
Functions are a cornerstone of Lisp and get special treatment compared to other languages. Some of these features:

- Functions can be created dynamically at runtime
- Functions can be passed as arguments to other functions
- Functions can be returned from functions
- Functions can be stored as elements inside data structures
- Clojure functions are objects, and this gives them other interesting properties

### Definition
Functions are defined using the `defn` macro which uses the following form

    (defn function-name doc-string? attr-map? [params]
     conditions-map?
      (expressions))

The statements followed by ? are optional. Let's look at a few examples

    (defn square [x] (* x x))

Once the macro goes through this it expands to

    (def square (fn [x] (* x x)))

Which is the "official" syntax for defining a function, you create a name (called a `var` in Clojure), and bind a value to that, in this case an anonymous function that calculates the square of a number. That said, because function definitions are so common, the idiomatic way to define them is using `defn`. Now let's add in the docstring.

    (defn square
    "Returns the square of the argument"
      [x]
      (* x x))

This is the string that will appear if you call `(doc square)` and is a good idea for any function in a library or that might be used by another programmer. Finally, to add the conditions map.

    (defn square
    "Returns the square of the argument"
    [x]
    { :pre [(> x 0)]
      :post [(> % 0)] }
    (* x x))

The conditions map checks for valid values before calling the function, you can include multiple tests in each vector. `:pre` occurs before execution, and `:post` occurs after execution where `%` subs in for the returned value.

### Multiple Arity
Clojure functions can have multiple arity, and they are all defined in the same body. To use multiple arity you list the parameters and the function body used with that number of parameters.

    (defn add
      "Adds together an arbitrary number of arguments"
      ([] 0)
      ([x] x)
      ([x y] (+ x y)))

This function adds together anywhere from 0 to 2 numbers, returning a logical evaluation for every one of its arity values.

### Variadic Functions
Functions can also take an aribitrary number of arguments, adding that to the `add` function would look like this:

    (defn add
      "Adds together an arbitrary number of arguments"
      ([] 0)
      ([x] x)
      ([x y] (+ x y))
      ([x y & z] (+ x y (reduce + z))))

The `& z` command breaks down to "If you get any arguments beyond `x` and `y`, put them in a list called `z` and I will deal with them. The reason we use `x`, `y`, and `z` rather than simply `[x & y]` is because you need each arity to have a different number of arguments and we already have one defined for two arguments.

### Recursion
Because of limitations in the JVM, Clojure lacks some optimizations to recursive calls possessed by other languages, namely tail call optimization. Despite that, there are still optimized recursive structures in Clojure, but first lets look at different forms of recursion.

    (defn factorial [n]
      (if (= n 1)
        1
        (* n (factorial (- n 1)))))

This is a basic, stack-devouring recursive call. It uses a tremendous amount of memory because none of the values can evaluate until it reaches the end condition of n == 1. This can be modified by saving the running value and calling this via a tail call.

    (defn factorial [n]
      (defn factorial-iter [i total]
        (if (= i 0)
          total
          (factorial-iter (- i 1) (* total i))))
      (factorial-iter n 1))

This defines a second function inside the main function that keeps track of the iterations and the total calculated thusfar. This still blows up the stack in many languages because the return value of the first function, and indeed every recursive call made, still depends on the evaluation reaching an end condition.

However, when you think about the calls being made you'll realize that the other recursive calls are not required, there's no need to save values that won't matter until the last iteration. For this reason, many languages have "tail call optimization" which recognizes a recursive function such as this and allows it to consume a constant space in memory. Because Clojure is built on the JVM, there are issues with its implementation that prevent tail call optimization from being implemented. Sad day. But all is not lost because tail calls in Clojure can achieve the same memory optimizations using a different syntax.

    (defn factorial [n]
      (loop [i n tot 1]
        (if (= i 0)
          tot
          (recur (dec i) (* tot i)))))

While the syntax itself takes a little getting used to, it's actually a fairly expressive way of defining a recursive function. While this was covered already in Ch 2, it's being covered again in Ch 3 and since I'm slavishly following the book I'm explaining it again as well.

The first part defines the function, we know that. The second part, the `loop` line binds variables to be used in the tail-recursive version. They bind the same way hash variables bind so i=n and tot=1 in this example. Next you set the end condition for the loop, i=0 in this case. Finally you use the `recur` command and alter the variables for the next recursive iteration. In our factorial example this means multiplying the sum and decrementing `i`.

It's of moderate interest to note that `recur` can be called without using `loop`. Why is this of moderate interest, you ask? Because you can only bind variables that exist in the original function definition and tail end recursion generally requires more variables to proxy the mutable state of a function. Anyway, here's the example in the book:

    (defn countdown [n]
      (if-not (zero? n)
        (do
          (if (= 0 (rem n 100))
                (println "countdown: " n))
          (recur (dec n)))))

### Mutually Recursive Functions
Is the name of my next band as well as a pair of functions that recursively call each other.

    (declare hat)

    (defn cat [n]
      (if-not (zero? n)
        (do
          (if (= 0 (rem n 100))
               (println "cat:" n))
          (hat (dec n)))))

    (defn hat [n]
      (if-not (zero? n)
        (do
          (if (= 0 (rem n 100))
               (println "hat:" n))
          (cat (dec n)))))

Together these functions are mutually recursive, and will eventually blow the stack. Unfortunately, `recur` is only useful for functions that recurse on themselves, so we need another solution to deal with this, the amazingly named "trampoline".

    (declare hatt)

    (defn catt [n]
      (if-not (zero? n)
        (do
           (if (= 0 (rem n 100))
               (println "catt:" n))
           #(hatt (dec n)))))

    (defn hatt [n]
      (if-not (zero? n)
        (do
           (if (= 0 (rem n 100))
               (println "hatt:" n))
           #(catt (dec n)))))

The difference here is the `#` macro, which defines an anonymous function that, when called, calls `hat`, which has its own anonymous function that calls `cat`. Our code no longer contains a genuine recursive call, the function returns an anonymous function, rather than waiting on the result of the whole chain to evaluate. But because the recursive calls are gone, it requires a special syntax to work.

    (trampoline cat 10000)

This evaluates without blowing the stack and does it much faster than the mutually recursive version. The way trampoline works is by clearing values off the stack as long as the next return value is a function, as soon as an actual value returns, it returns that value directly, ignoring what would still be a mutually recursive call chain. My suspicion is that, as with tail call optimization, Clojure and the JVM simply require a more explicit syntax in order to accomplish the normal recursive optimizations that some other languages do by default.

### Calling Functions
The most common function calling form has been seen here already dozens of times

    (+ 1 2 3 4 5)


You can also take an existing list and use it as the arguments to a function with `apply`

    (apply + list)

As mentioned in the previous chapter, the difference between `apply` and `reduce` is that apply prepends the function to the list and evaluates it, so it works with all variadic functions, whereas `reduce` essentially places the function in between every item on the list (from an infix perspective)

### Higher-Order Functions
Functions are first class objects, they can be treated like data, passed as arguments and returned from other functions. Because of these traits we consider functions in Clojure to be "higher order functions". Higher order functions are important for _function composition_. A few examples of useful higher-order functions include `map`, `filter` and `reduce` seen previously, as well as the following:

#### Every/some
`every?` takes a predicate and a sequence and checks to see if every value in the sequence passes the test, if so it returns `true`, if not it returns `false`.

    (def x (into [] (range 2 12 2))
    (every? even? x)
    >> true

`some` is the same basic idea, but it only requires one of the values to be true in order to return true, otherwise it returns `nil`

#### Constantly
Constantly takes a value and returns a function. Every call to that function will return the original value passed no matter what. It's incredibly useful when you want a function that makes no sense.

    (def x (constantly 10))
    (x 1 2)
    >> 10
    (x 1 2 3 4 5 6 7)
    >> 10
    (x (take 1000000 (range 1 1000000000000)))
    >> 10
    (x "Hello, world!")
    >> 10

True to the function's definition, x is constantly 10

#### Complement
Takes a function and returns a function of the same arity that returns the logical opposite of the first function.

    (def not-empty? (complement empty?))

While I currently feel that this is a very odd feature to implement as a core feature of a language, I can see genuine uses for this one.

#### Partial
`partial` is a function for creating function partials.

    (defn gt? [threshold test] (> number threshold))

Defines a predicate function that returns true if the value given is above the threshold, to define it for a fixed threshold we can use partial.

    (def gt5? (partial gt? 5))
    (gt5? 10)
    >> true
    (gt5? 3)
    >> false
    (gt5? 5)
    >> false

#### Memoize
`memoize` implements memoization, which remembers the results of previous runs and stores them for faster calculation

    defn fibo [n]
      (if (or (= n 0) (= n 1))
        n
        (+ (fibo (- n 1)) (fibo (- n 2)))))

    (def fib (memoize fibo))

    (defn -main [& args]
      (time (println (fib 35)))
      (time (println (fib 35))))

    >> 9227465
    >> "Elapsed time: 492.084 msecs"
    >> 9227465
    >> "Elapsed time: 0.155 msecs"

As you can see the second call to the function runs *much* faster because the result of that function had been recorded, so rather than run the function again, it simply looks up the results.

However, this doesn't actually work the way memoization is supposed to. Memoization is supposed to remember the values of every function call, including the recursive ones. In other words, the call to `(fib 35)` should also memoize the values of fib-1 to fib-34 as well as the value for 35. This would allow us to easily calculate the values of larger fib numbers by leaning on the memoized values, and streamlines the number of calls made calculating the same fibo numbers over and over again.

The reason for this is in the recursive calls.  We call `fib` but `fib` calls `fibo` and `fibo` recurses on itself.  We need the recursive calls to call the memoized function instead. We could do this by simply changing

    (def fib (memoize fibo))

to

    (def fibo (memoize fibo))

But re-binding that symbol is confusing to read and easily missed. A better way is to simply have `fib` bind to a memoized anonymous function in the first place.

## Function Composition
Let's look at an example of composing higher-order-functions with simpler ones.

    (def users [
      {:username "kyle"
       :balance 175.00
       :member-since "2009-04-16"}

      {:username "zak"
       :balance 12.95
       :member-since "2009-02-01"}
      {:username "rob"
       :balance 98.50
       :member-since "2009-03-30"}
    ])

    (defn username [user]
      (user :username))

    (defn balance [user]
      (user :balance))

    (defn sorter-using [ordering-fn]
      (fn [users]
        (sort-by ordering-fn users)))

    (def poorest-first (sorter-using balance))

    (def alphabetically (sorter-using username))

We start with a data structure representing users as a vector of hashes for each user. Suppose we want to sort users by different traits. We start by defining two functions, `username` and `balance` that fetch the username and the balance, respectively, given a user hash. Next we define a function, `sorter-using`, that takes an ordering function, then creates an anonymous function that uses this function and `users` to sort users with the predicate supplied. Finally we combine these in a set of two functions that use the `sorter-using` function with the `username` and `balance` functions, allowing us to sort the vector by username and balance.

### Anonymous Functions
There are times when functions only need to be run once, like passing a function as an argument or as a callback. In Clojure this is simple, in fact, anonymous functions are the only way to define functions, naming them is simply binding an anonymous function to a name, which is what the `defn` macro expands to. Anyway, an example of an anonymous function

    (fn [x y] (+ x y))

Again, without a name there's no way of calling this, but then again this section isn't called "Nonymous Functions". An example using the users from above

    (map (fn [user] (user :member-since)) users)

This will return a list of all `:member-since` dates from the vector. Because this is a common pattern there are macros that make this even easier.

    (map #(% :member-since) users)

This is exactly equivalent to the previous version. The `#` defines an anonymous function, and the `%` acts as a placeholder for the first variable, if more than one variable is needed you can use `%1`, `%2`, etc. Here's an example with the ubiquitous addition function.

    #(+ %1 %2)

That's it. It combines the function body and the function definition into a single statement that expands to:

    (fn [x y] (+ x y))

### Keywords and Symbols
In Clojure, keywords work as functions which interact with hashes. So you can write code like the following.

    (def person {:username "Zac"
                 :balance 102.3
                 :member-since "2009-02-01"})
    (person :username)
    >> "Zac"
    (:member-since person)
    >> "2009-02-01"

Because they are functions you can also pass this directly to map, so the anonymous function we used above to get the `:member-since` value for every item in the vector could be replaced by:

    (map :member-since users)

These can also accept a third argument that will be returned if nothing matches.

    (:favorite-food user :not-found)

If you want to get the functionality of the default value in a map call we can simply fall back on the anonymous function form.

    (map #(:favorite-food % :not-found) users)

Symbols in Clojure are values that represent something else, and evaluate to that when used in code. Symbols are always associated with the value they are bound to. For example, when we declare a function what we are actually doing is binding an anonymous function to a symbol. When we use that symbol in code it evaluates to the function it references (which in turn evaluates to whatever return value it has). For example:

    (def x 10)
    ;; x is a symbol
    (println x)
    ;; x evaluates to 10
    >> 10

In this example we see that x returns the value it represents. But what if we wanted to use the symbol itself, without having it evaluate? We can use the quoting macro to accomplish this.

    (def x 10)
    (println x)
    >> 10
    (println 'x)
    >> x

Quoting symbols also allows us to use them as keys in maps, among other things.

    (def expense { 'name "Snow Leopard"
                   'cost 29.95 })

Like keywords, these also work as functions:

    (expense 'name)
    >> "Snow Leopard"
    ('name expense)
    >> "Snow Leopard"

## Scope
Clojure has two types of scoping: lexical scoping and dynamic scoping. Lexical scoping is the standard scoping seen in most languages. A variable is visible in the code block(s) that it is declared. Lisp languages offer another scoping called "dynamic scope" with a different set of rules from lexical scoping.

### Vars and Binding
Vars in Clojure are similar to globals in other languages because, like all data, vars are immutable. They are declared using `def`.

    (def MAX-CONNECTIONS 10)

Vars can also be declared without a binding

    (def RABBITMQ-CONNECTION)

To use these you need to bind them using `binding`, this can also change the value of a bound var, within that scope, it seems un-functional but I'm sure there's a reason for that.

    (binding [MAX-CONNECTIONS 20
              RABBITMQ-CONNECTION (new-connection)]
      ( ;; Code here ))

Because function definitions are simply a macro that expands to a `(def x (fn...` form, this means that functions themselves are also vars and can be re-bound. This is useful for things like stubbing out methods for unit tests.

### Special Variables
Special variables are vars whose value needs to be bound before using them, to differentiate them from normal vars the naming convention is to surround them with `*`.  An example of a special variable:

    (def *db-host* "localhost")

    (defn expense-report [start-date end-date]
      (println *db-host*))

Suppose we've tested that code locally and it works, now we can apply that to the production db.

    (binding [*db-host* "production"]
      (expense-report "2010" "2014"))

This will print out "production", showing that the value of `*db-host*` has changed.

### Dynamic Scope
Dynamic scope in Clojure means that the value of a var is determined by the execution path taken by the program. If a variable is re-bound, that binding will stick for all code executed after that, including other function calls. An example:


    (def ^:dynamic *eval-me* 10)

    (defn print-the-var [label]
      (println label *eval-me*))

    (print-the-var "A:")

    (binding [*eval-me* 20]
      (print-the-var "B:")
      (binding [*eval-me* 30]
        (print-the-var "C:"))
      (print-the-var "D:"))

    (print-the-var "E:")

This will output:

    A: 10
    B: 20
    C: 30
    D: 20
    E: 10

The `^:dynamic` declaration is needed to make the variable dynamic. If you fail to include this you'll get a compile-time error telling you to either declare it dynamic or change the name so that it doesn't look like a dynamic variable.

Let's analyze this a little closer. First the value is bound to 10, and the `A` value shows 10. Next it is re-bound to 20 as `B` shows.  Another bind call is nested in this same statement, changing the value to 30 as seen in `C`. When the C block evaluates the bound value reverts back to the `B` value, and the print statement for `D` shows this. Finally we drop out of this block entirely and the value reverts to its original value of 10, as seen in `E`

#### Aspect Oriented Logging
Here's a practical use of bindings to automatically log function calls without changing the function definition.

    (defn ^:dynamic twice [x]
      (println "original function")
      (* 2 x))

    (defn call-twice [y]
      (twice y))

    (defn with-log [function-to-call log-statement]
      (fn [& args]
        (println log-statement)
        (apply function-to-call args)))

    (call-twice 10)

    (binding [twice (with-log twice "Calling the twice function")]
       (call-twice 20))

    (call-twice 30)

This outputs the first and last call normally, but includes a log statement for the second call.  It does require the `^:dynamic` modifier on the definition of `twice`, but it's possible there are compiler flags that would allow things to be dynamic in a debugging setting.

### Thread Local State
A var's root binding is visible to all threads unless and until another binding overrides it. When this happens the binding is local to that thread and not visible to any other threads.

### Laziness
Dynamic variables can cause some weird interactions when it comes to lazily evaluated sequences. Consider the following.

    (def ^:dynamic *factor* 10)
    (defn multiply [x]
      (* x *factor*))

    (defn -main [& args]
      (println (map multiply (range 1 6)))
      (println (binding [*factor* 20]
        (map multiply (range 1 6))))
      )

The first one naturally returns `[10 20 30 40 50]`. Then we call the second one and that returns: `[10 20 30 40 50]`. Double you tee eff?

What's happening here is that a lazy sequence isn't realized until it's needed. When that does occur, realization occurs outside of the bound scope for `20`, so it reverts to the root binding of `10`. In order to get the behavior we want, we need to force realization of the lazy sequence from within the binding form.

    (binding [*factor* 20]
      (doall (map multiply (range 1 6))))

`doall` forces the realization of lazy sequences and evaluates everything in the proper namespace to get what we want.

You can also define functions in a `let` block, because functions are first-class.

    (defn upcase-arr [arr]
      (let [upcase (fn [item] (.toUpperCase item))]
        (map upcase arr)))

### Closures and Free Variables
A variable is "free" if there is no binding occurrence of the variable the lexical scope of that form. In other words, a variable is free if it was bound outside the scope it is being evaluated in. Let's look at an example.

    (defn create-scaler [scale]
      (fn [x]
        (* x scale)))

In the anonymous function there is no binding for scale, it was bound in the `create-scalar` function, so within the anonymous function `scale` is a free variable. Dynamic variables cannot be free, only lexically scoped variables. Anything that encloses over a free variable is a "closure." In other words, a closure captures the value of a local variable that might otherwise change. Examples:

    (def hundred-scaler (create-scaler 100))
    (def thousand-scaler (create-scaler 1000))
    (println (hundred-scaler 0.42))
    >> 42
    (println (thousand-scaler 0.42))
    >> 420

Both of these call `create-scaler` and both of them give different values for `scale`, yet in both cases they enclose over the value of scale they have been passed and retain the different values.
