# Engineering
Tools for creating larger programs

## Namespaces
Namespaces function much the same as they do in most other languages. A namespace provides a mapping between symbols and the things they represent within that namespace, and outside of that namespace all of those things can be accessed using a fully qualified name.

### Namespace Macros
the `ns` macro is used in essentially every clojure source code file, and using it does a surprising number of things. First it creates a new namespace of the name given, next it switches to that namespace. Finally, it loads in all the basic functions from the `java.lang` package and the `clojure.core` namespace.

    (ns bacon)

See, super simple

The `in-ns` macro creates the namespace and imports `java.lang` but not `clojure.core`. This feels like one of those, "You'll know when you need it" sort of things. Not even going to bother with examples because it works the same way as it does above.

Finally, `create-ns` creates a namespace (predictably) and returns it as an object. This does *not* switch to that namespace. It creates java bindings but not `clojure.core`. You can create values in that namespace by using `intern`.

You can remove namespaces by using `remove-ns`

Generally speaking, the `ns` macro is likely what you'll use 95% of the time, `in-ns` as specific use cases, and `create-ns` is for very specific, very advanced techniques that are best avoided until you really know what you're doing.

## A Cascade of Classes
Clojure has essentially six different ways to define a class-like data structure: `gen-class`, `reify`, `proxy`, `defstruct`, `deftype`, and finally `defrecord`. This is a radical departure from the general simplicity seen elsewhere in the language, so what exactly is going on, and what are all these used for? This StackOverflow answer says it better than I could:

---

*Note: `defstruct` is not included in this answer and has, at any rate, been deprecated in favor of the new constructors like `defrecord`*

So first, let's consider what these do. deftype and genclass are similar in that they both define a named class for ahead-of-time compilation. Genclass came first, followed by deftype in clojure 1.2. Deftype is preferred, and has better performance characteristics, but is more restrictive. A deftype class can conform to an interface, but cannot inherit from another class.

Reify and proxy are both used to dynamically create an instance of an anonymous class at runtime. Proxy came first, reify came along with deftype and defrecord in clojure 1.2. Reify is preferred, just as deftype is, where the semantics are not too restrictive.

That leaves the question of why both deftype and defrecord, since they appeared at the same time, and have a similar role. For most purposes, we will want to use defrecord: it has all the various clojure goodness that we know and love, sequability and so forth. Deftype is intended for use as a low level building block for the implementation of other datastructures. It doesn't include the regular clojure interfaces, but it does have the option of mutable fields (though this isn't the default).

---

In other words, for your bread and butter object-like needs you'll normally go with `defrecord`. If you have a special use-case that seems to not fir this, then you can look at some of the other options.

## Records
Records are defined 
