# Chapter 9 & 10
## Closures
### Closures to enclose variables
Closures are structures that "close over" a value that is outside their scope and retain it for further computation. This revolves around the concept of a "free variable".

*Given a lexically scoped block of code, a free variable is one that is neither an argument nor a local variable.*

    (defn adder [x]
      (fn [y]
        (+ x y)))

This takes a number, `x`, and returns an anonymous function. In the enclosed, anonymous function `y` is given as an argument, so it is not a free variable, but `x` is neither an argument nor is it a local so `x` *is* a free variable.

In this trivial example it might be hard to see why this is important so let's look at another example.

    (ns cljr.closures)

    (def tax 0.08)

    (defn price-with-tax []
      (fn [y] (* y (inc tax))))

Now, from another namespace, we call this:

    (defn -main [& args]
      (def tax-price (closures/price-with-tax))
      (println (tax-price 100)))

    >> 108.0

But wait! This calculation has clearly used the value for `tax`, which should not be visible in this namespace. Just to be sure

    (println tax)
    >> Error: Unable to resolve symbol tax...

Note that in the first namespace tax is declared at the top level, so it is neither an argument nor a local to the `price-with-tax` method, but because it is visible to the `price-with-tax` function it can be used. That's not surprising. What is surprising is that when we change namespaces and get a copy of the anonymous function returned by `price-with-tax`, then call that with a value we get the price of what we passed in plus 8%, the value defined in the other namespace.

Closures work by "closing over" the values of vars at their creation. If a symbol is rebound, then the enclosed value remains the same.

### Closures to delay execution
Another purpose of closures is to delay execution of something until it is needed. Clojure as a language evaluates functions eagerly, everything gets looked at and evaluated before the function is run. This is why so many things that require conditional behavior in clojure are macros, only they can choose to execute part of the code that has been passed to them.

To use this conditional pattern all you need to do is wrap things in an anonymous function. Code wrapped in an anonymous function won't be evaluated until the function is evaluated. Looking at a simple example. (`(Date.)` initializes a new `Date` instance to the current time, and `.getTime` gets the time in ms since the epoch.)

    (import java.util.Date)

    (defn -main []
      (def b (fn [] (Date.)))
      (def a (Date.))
      (Thread/sleep 1000)
      (println (- (.getTime a) (.getTime (b)))))

    >> -1001

If the functions were evaluated completely in the order they are presented, then we would expect b to be created first, and thus a - b would yield a positive number. Instead we see a value of -1001, indicating that the evaluation of b comes after our 1s delay.

Let's try one more:

    (defn time-diff [a b]
      (let [ta (.getTime a)
            tb (.getTime b)]
        (if (> tb ta)
          (float (/ (- tb ta) 1000))
          (float (/ (- ta tb) 1000)))))

    (defn enclosed []
      (let [d1 (Date.)]
        (fn []
          (time-diff d1 (Date.)))))

Looking at this it seems that when the function is first called the value in the `let` block should evaluate and be enclosed inside the anonymous function. Next when we evaluate the anonymous function, a new `Date` object should be created, the time difference between the two taken, and the result of that returned.

    (defn -main [& args]
      (def d (enclosed))
      (Thread/sleep 1000)
      (println (d)))

    >> 1.0

Perfect, the time difference between the two was exactly 1s which is what we expected.

### Closures that feel like data
Here's an interesting example of a pattern I had not seen before:

    (defn new-user [login password email]
      (fn [a]
        (condp = a
          :login login
          :password password
          :email email)))

    (def anya (new-user "anya" "secret" "anya@currylogic.com"))

    (anya :login)
    >> "anya"
    (anya :password)
    >> "secret"

This takes arguments for a object constructor, and then traps the values inside a closure and returns that anonymous function, which matches arguments sent to it to the original arguments. In other words it functions an awful lot like a hash, but has the added benefit of being able to remove fields to keep them private. We could remove the `:password` line and that would make it impossible to access this user's password.

This is a basic form of message passing. The function that has been returned will accept specific messages and deal with them conditionally. We could make this more object-ey by adding functions to the conditional behavior in addition to the basic fetch methods:

    (defn new-user [login password email]
      (fn [a & args]
        (condp = a
          :login login
    :email email
    :authenticate (= password (first args)))))

With `:authenticate` we now have a message that runs a function using the enclosed data.
