# Macros
### Basics
Clojure is homoiconic, which means that the source code is itself expressed by a primitive type of the language, in this case a list. Because of this you can process code the same way you would process data. This is what makes the macro system possible.  Before running the program the code is expanded and converted into the proper data structures, then these structures are evaluated to run the program. With normal, macro-less code the expansion results in the same exact code as before. When using a macro, the macro is evaluated and the expanded version of the code is inserted.

Let's look at a simple example that wraps any call to set a `ref` into a `dosync` block. Here's the normal form

    (dosync
      (ref-set a-ref 1))

But what we want to type is this:

    (sync-set a-ref 1)

Here's a macro that would accomplish that

    (defmacro sync-set [r v]
      (list 'dosync (list 'ref-set r v)))

In the real world you would just make a function for this, or use an `atom` rather than a `ref`, but those points are moot.  The point here is that you can expand your code through macros almost trivially.

### Adding Unless
Another macro example is adding `unless` to the language, which is the opposite of `if` in terms of control flow. Suppose we want a function that prints out a statement if the number passed in is not even.

    (defn exhibits-oddity? [x]
      (unless (even? x)
        (println "Very odd, indeed!")))

Let's attempt this as a function first.

    (defn unless [test then]
      (if (not test)
        then))

Now let's test the implementation:

    (exhibits-oddity? 13)
    >> "Very odd, indeed!"

So far so good

    (exhibits-oddity? 12)
    >> "Very odd, indeed!"

Well that is not correct at all. The problem is that the function and all of its arguments are evaluated when the function is called. Because this is a function, and not a conditional, it means the statement is printed regardless of what we pass in as an argument. There is no way to dodge this execution of the function, and thus no way to implement what we want using a functional approach.

You _could_ hack your way around this by requiring the then clause to be passed in as a function. If the user passed in `#(println "Rather odd!")`, it wouldn't be evaluated immediately because it's a function, and could be called later to achieve the desired behavior. But this is bad practice, it makes no sense to the programmer as to why they need to pass in a simple statement as an anonymous function, especially when the other conditional forms do not use this syntax. Now let's look at the macro version.

    (defmacro unless [test then]
      (list 'if (list 'not test)
            then))

To see the results of a macro expansion you can use the `macroexpand` function.

    (macroexpand '(defn square [x] (* x x)))
    >> (def square (clojure.core/fn ([x] (* x x))))

### Templating
The one issue with the macro above is the repeated calls to `list`, which obfuscates the code being written. Clojure has another macro, the backquote reader macro, AKA the syntax quote character, that uses backticks in place of calls to `list`. The `unless` macro written using these characters.

    (defmacro unless [test then]
      `(if (not ~test)
          ~then))

The great thing about using the quoting syntax is that the final, expanded code is immediately obvious because the template is almost identical, plus a few extra characters.

The one extra character in here is the `~`, which is known as the 'unquote character'. Predictably, this unquotes anything that is in a quoted form and evaluates it. Suppose we removed the `~` from in front of the `then` statement. The expanded form would then have just the symbol `then` in the place where we would want the actual `then` clause that we passed in.

### Variadic
Suppose we wanted to make it so that unless would execute an arbitrary number of expressions rather than simply the first one. We can expand the arity of the call the same way we would a function, and change the macro to use a `do` block.

    (defmacro unless [test & exprs]
      `(if (not ~test)
         (do ~exprs)))

This seems like it should work, but it does not. The problem is the way the macro expands.

    user=>(macroexpand-1 '(unless (even? x)
                            (println "Odd!")
                            (println "Very odd!")))

    (if (clojure.core/not (even? x))
      (do ((println "Odd!") (println "Very odd!"))))

The `println` statements are enclosed in parens, which evaluates them to `nil` (the return value of `println`). Because they are still in a list, that evaluates to `(nil nil)`. Finally, because Lisp expects a function as the first item of every list it attempts to evaluate `nil` as a function with one argument, `nil` and fails throwing a `NullPointerException`

So we need to remove those parentheses surrounding the two `println` statements. If only there were a macro that could both unquote the items, and somehow splice them into the current list rather than keeping them separate.

Oh hey look! The unquote splice reader macro! `~@`:

    (defmacro unless [test & exprs]
      `(if (not ~test)
         (do ~@exprs)))

The unquote splice unquotes an item or items and splices them into the current context rather than wrapping them in quotes.

### Reader Macro and Unique Names
One issue with macros in many Lisps is that symbols in a macro can sometimes redefine or form an unintentional closure over a variable that you didn't intend. This occurs when using common names like `now`. Let's look at an example of a macro that defines a self-logging function:

    (defmacro def-logged-fn [fn-name args & body]
      `(defn ~fn-name ~args
         (let [now (System/currentTimeMillis)]
           (println "[" now "] Call to" (str (var ~fn-name)))
           ~@body)))

Looks fine, but if you expand this you'll see that the expanded version has changed `now` to `user/now`

    user> (macroexpand-1 '(def-logged-fn printname [name]
            (println "hi" name)))

    (clojure.core/defn printname [name]
      (clojure.core/let [user/now (java.lang.System/currentTimeMillis)]
        (clojure.core/println "[" user/now ":] Call to"
           (clojure.core/str (var printname)))
        (println "hi" name)))

If this were allowed to occur (it's not), it would capture the `user/now` value and use that in all future calls. So a separate method that defines `now` to be a date instead gets a huge number in ms, and presumably explodes.

To prevent this Clojure does two things. First, it will raise an error when you attempt to use a qualified name like `now` in a macro definition. Second, it provides the reader macro `#` to give you a way out of this conundrum with minimal difficulty.

    (defmacro def-logged-fn [fn-name args & body]
      `(defn ~fn-name ~args
         (let [now# (System/currentTimeMillis)]
           (println "[" now# "] Call to" (str (var ~fn-name)))
           ~@body)))

What the reader macro actually does is generate a unique symbol based on that name. So `now#` will expand to something like `now__14187__auto__`. Everyone is happy. The program gets unique names that won't clash, the programmer gets to use plain english in the definitions while still being protected from improper namespacing.

### Macros: Why and When
For most programmers new to the macro system, understanding when to use a macro and when not to can be difficult. It's useful to contrast what functionality macros define that functions can't do:

- Macros can delay the execution of code
- Macros can prevent the execution of code
- Macros can change the flow of execution
- Macros can define new syntactic forms
- Macros can be used to create a DSL
- Macros can work with special forms like `def`, which only accepts a symbol
- Macros can provide convenience more general than a function. A function can automate a specific set of steps, a macro can automate a general set of steps using things like implicit `do` blocks and other convenience features to make writing code very general.

## Macros in Clojure
Looking at some examples of macros from the Clojure source code itself.

#### Comment

    (defmacro comment [& body])

That's it, the whole macro simply ignores everything that is passed to it. That's the amazing thing about macros. Suppose Clojure didn't have comments and you wanted to implement that. In any other language it would be incredibly hard to implement something that did what you want, because the ability to completely ignore code such that it takes no space in memory is typically beyond what the programmer can do. Not so with Clojure.

#### Declare
`declare` creates a var for every symbol passed to it, typically so that it can be reassigned at some later point, but still exist at the time to prevent any kind of errors stemming from nonexistence.

    (defmacro declare [& names]
      `(do
        ~@(map #(list 'def %) names)))

#### Defonce
Takes a var and an initializer, but only initializes the var if it has not already been initialized. Checking for initialization is done by checking for a root binding.

    (defmacro defonce [name expr]
      `(let [v# (def ~name)]
        (when-not (.hasRoot v#)
          (def ~name ~expr))))

Very straightforward.

#### And
`and` is typically a special form in most languages. In Clojure it's a macro. A testament to the expressiveness and capabilities of macros.

    (defmacro and
      ([] true)
      ([x] x)
      ([x & next]
        `(let [and# ~x]
          (if and# (and ~@next) and#))))

A recursive macro. This one uses recursion to create nested if/then clauses until the arguments to the function have been exhausted.

#### Time
Time is a handy way to time functions by simply nesting them inside `(time %)`. Let's look at the definition

    (defmacro time [expr]
      `(let [start# (. System (nanoTime))
             ret# ~expr]
         (prn
           (str "Elapsed Time: "
                (/ (double (- (. System (nanoTime)) start#))
                   1000000.0)
                " ms"))
         ret#))

The start time and expression are both declared in the let block, this leads both expressions to be evaluated. First the system time is read, then the expression is evaluated and the return value kept. Next a string is constructed during which the system time is read again and the difference between the two is taken.

That string is printed with the call to `prn`. `prn` is roughly equivalent to `println` except that it prints the programmatic representation of the item, in this case the string has the quotes included at the beginning and end with `prn` but not with `println`. Finally, the value of the original expression is returned.

## Mo Macros
### Infix
Let's start by adding a macro that turns evaluates an infix expression. This can be done very simply:

    (defmacro infix [left op right]
      `(~op ~left ~right))

Or if you expect the infix expression to be passed as a list:

    (defmacro infixexpr [expr]
      (let [[left op right] expr]
        (list op left right)))

Boom.

### Randomly
What if you wanted a control flow statement that randomly chose a path rather than being deterministic? Let's define a `randomly` macro that chooses a random path to execute.

    (defmacro randomly [& exprs]
      (let [len (count exprs)
            index (rand-int len)
            conditions (map #(list '= index %) (range len))]
        `(cond ~@(interleave conditions exprs))))

The approach here is pretty cool. First it finds the length of the list, then generates a random number within that length. The `conditions` part of the let clause creates a list of values that will evaluate to things like `(= 1 4) (= 2 4) (= 3 4)` and so on. When interleaved with the expressions list it creates something that looks like this:

    (= 0 0) (println "church")
    (= 0 1) (println "turing")
    (= 0 2) (println "haskell"))

So when the `cond` statement is evaluated in the code, the only expression that will evaluate to `true` is the one where the index matches the random number, and that code will be executed.

However, there's an easier way to do this.

    (defmacro randomly [& exprs]
      (nth exprs (rand-int (count exprs))))

Or, even easier:

    (defmacro randomly [& exprs]
      (rand-nth exprs))

That works the same. However, they all actually have a bug. Suppose you evaluate this during a function definition in the hopes of getting some randomization inside a method. It won't work as intended because the generation of the random number occurs at the macro level and from that point on the randomness disappears. So calling this from within a function will result in the same value being returned every time.

For the most part that point is moot, you could use a function to return a random element rather than a macro, the one edge case deals with side effects like IO. If your conds were `println` statements, then they would all be evaluated and printed when passed into a function regardless of which one was meant to execute. There is a macro way to randomly choose and execute an element, even one with side effects, that a function could not do:

    (defmacro random [& exprs]
      (let [len# (count exprs)
            conds# (interleave (range len#) exprs)]
        `(condp = (rand-int ~len#) ~@conds#)))

### Defwebmethod
Now let's use a macro that could be the basis for a domain specific language. Imagine we have a set of methods that take a hash returned by web requests, pull some information from that, and process it. An example might look like:

    (defn login-user [request]
      (let [username (:username request)
            password (:password request)]
        (if (check-credentials username password)
          (str "Welcome back, " username ", " password " is correct!")
          (str "Login failed!"))))

The `let` block in this has a very high likelihood of being repeated over and over again in methods like this. Rather than repeating ourselves, let's define a macro that automatically does the hash stuff for us.

    (defmacro defwebmethod [name args & exprs]
      `(defn ~name [{:keys ~args}]
         ~@exprs))

Now we can rewrite the earlier code as this:

    (defwebmethod login-user [username password]
      (if (check-credentials username password)
        (str "Welcome, " username ", " password " is still correct!")
        (str "Login failed!")))

It saves two lines and a little hassle for this one method, but across a large project it's easy to see how this could be very useful, and if you needed to add additional steps to process at the beginning of each request it would be trivial to add them to the macro, but rather more difficult to add them at another stage.

### Assert-true
Let's add a simple macro that works like a lightweight testing framework, it should ensure that the statement passed to it is true.

    (defmacro assert-true [test-expr]
      (let [[operator lhs rhs] test-expr]
        `(let [lhsv# ~lhs rhsv# ~rhs ret# ~test-expr]
           (if-not ret#
             (throw (RuntimeException.
                      (str '~lhs " is not " '~operator " " rhsv#)))
             true))))

You can even add basic checks for proper formatting:

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


