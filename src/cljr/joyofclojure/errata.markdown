# Errata
Notes of interest from the chapters that overlap what was covered in Clojure in Action, but I either missed or it wasn't discussed.

### Symbols and Equality
Let's start with something unexpected:

    (identical 'goat 'goat)
    >> false

why does this evaluate as false when they are clearly the same symbol? To deepen the mystery, why does this return true?

    (= 'goat 'goat)
    >> true

The answer is metadata. Clojure only evaluates two symbols as identical if they point to the same object in memory.

    (let [x 'goat y x] (identical? x y))
    >> true

Let's define two vars pointing to the same symbol with different metadata:

    (let [x (with-meta 'goat {:ornery true})
          y (with-meta 'goat {:ornery false})]
      [(= x y)
       (identical? x y)
       (meta x)
       (meta y)])

    >> [true false {:ornery true} {:ornery false}]

In this example we see that `x` and `y` are equal, are not identical, and contain different metadata despite pointing to the same symbol.

### Lisp-1
There are two types of Lisps. Lisp-1 and Lisp-2. Lisp-1 uses the same namespace evaluation for functions as it does for vars and other values. That allows us to write code like this:

    (defn run-fn [fn x y]
      (fn x y))

Note that the only thing differentiating the three vars being passed to this are their ordering in the function body. In a Lisp-2 language we would need to call another function explicitly that calls the function instead:

    (defun run-fn (fn x y)
      (funcall fn x y))

Lisp-1 is more concise and in many ways more expressive. This is its primary benefit. That said, it leads to a more polluted namespace because functions and vars are sharing it. This often leads to name shadowing, which in itself is not a bad thing when done properly, but this can lead to errors so it's something you need to be conscious of.
