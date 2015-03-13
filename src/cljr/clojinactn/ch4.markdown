# Polymorphism
Clojure polymorphism is not done in the same way as Ruby and other OO languges. But to discuss the way Clojure does things, we're going to review the common OO approach and then present how Clojure is different from these things.

### Subtype Polymorphism
Is the most common polypmorphic pattern in use today. It uses classes, inheritance, and method overloading to ensure that functions have consistent names and can be used in consistent ways while ensuring that the code that gets run is specific to that class. Clojure does not natively support subtyping.

### Duck Typing
*“When I see a bird that walks like a duck and swims like a duck and quacks like a duck, I call that bird a duck”*

Duck typing simply refers to inferring the type of an object based on how it looks and how it behaves.  Therefore `10` looks like an int and can be added to other numbers, so it's probably an int, whereas `10.0` looks more like a float. This is the type of polymorphism supported by most dynamic languages.

Duck typing, while convenient, doesn't allow for functions to be defined with different arities or accepting different types on its own. To do that another form of polymorphism, such as subtyping or overloading, needs to be implemented.

## Method Dispatch
Method dispatch describes the process by which a function name is bound to a function definition, either at runtime or compile time.  In terms of polymorphism, what this means is that the compiler/interpreter needs to determine which of the identically named methods is needed for the given arguments. This is usually done by comparing arities and types.

### Single Dispatch
Let's imagine a couple of basic classes in Java that inherit from each other:

    public interface Person {
        public void pet(Dog d);
        public void pet(Cat c);
    }

    public class Man implements Person {
      public void pet(Dog d) {
        System.out.println("Man here, petting dog:" + d.makeSound());
      }

      public void pet(Cat c) {
        System.out.println("Man here, petting cat:" + c.makeSound());
      }
    }

    public class Woman implements Person{
      public void pet(Dog d) {
        System.out.println("Woman here, petting dog:" + d.makeSound());
      }

      public void pet(Cat c) {
           System.out.println("Woman here, petting cat:" + c.makeSound());
      }
    }

Let's follow up the Person classes with the Animals:

    public interface Animal {
        public String makeSound();
    }

    public class Dog implements Animal{
         public String makeSound() {
             return "Bow, wow";
         }
    }

    public class Cat implements Animal {
         public String makeSound() {
             return "Meow";
         }
    }

The ideas here are pretty basic, people can pet animals and the animals can make sounds, so let's run that in a new class called `Park`

    public class Park {
      public static void main(String[] args) {
         Person p = new Man();
         Animal a = new Dog();
         p.pet(a);
      }
    }

The code here is fairly basic: `p` is declared as a `Person` then uses `Man()` as its constructor, which is valid because Man is a subclass of Person. Then `a` is declared as an `Animal` and constructed using `Dog()`, which is also valid. Then we call `p.pet(a)` and get an error. Que?! The error you'll get is that `pet` can't be resolved, because it doesn't find a version of the `pet` method that takes an `Animal` as it's receiver.

This is the problem with single dispatch, and it occurs when you have multiple hierarchies interacting in this way. Java only looks at the runtime type of the receiver in a method call. So it sees that `p` is declared as a `Person` but determines at runtime that the type is `Man`, so far so good. Unfortunately it doesn't do the same for `a`. It sees that it was declared as `Animal` and then stops. So when looking for a `pet` method it needs one where the argument passed is an `Animal`. If we had a way to determine the runtime type of `a` then the whole problem would be fixed.

### The Visitor Pattern
In the scenario above, only the receiver of a method call is resolved to its runtime type, but we need the argument to be resolved as well. This basically describes double dispatch, in double dispatch both the receiver and the first argument are resolved before determining the method to call.

Even though Java doesn't have double dispatch we can simulate it using a pattern called the "Visitor Pattern", which we're going to look at here for some reason before the `#SpoilerAlert` revelation that Clojure doesn't need the visitor pattern.

Consider two objects, one called the `element` and the other the `visitor`. The `element` has an `accept()` method that takes the visitor as an argument. `accept()` then calls a `visit()` method of the visitor and passes itself as an argument to `visit()` So:

- When `accept()` is called, its implementation is chosen based on:
  - The dynamic type of the element
  - The static type of the visitor
- When the associated `visit()` method is called, its implementation is chosen based on:
  - The dynamic type of the visitor
  - The static type of the element as known from within the implementation of the `accept()` method, which is the dynamic type of the element.

In other words, the visitor patterns allows the program to discern the dynamic type of both the element and the visitor, and effecively achieves double dispatch.

While this likely covers most use cases, it's complicated and still misses out on any dispatch levels above two. Luckily for us Clojure supports multiple dispatch through multimethods.

### Multimethods
Multimethods allow all kinds of cool dispatching. You can define multimethods using things other than simply the types of arguments being sent. Multimethods are defined using `defmulti` and candidates for the multimethod are added using `defmethod`. Let's take an example without multimethods, using a function that finds the fee that needs to be paid to different affiliates.

    (defn fee-amount [percentage user]
      (float (* 0.01 percentage (:salary user))))

    (defn affiliate-fee-cond [user]
      (cond
        (= :google.com (:referrer user)) (fee-amount 0.01 user)
        (= :mint.com (:referrer user)) (fee-amount 0.03 user)
        :default (fee-amount 0.02 user)))

The problem with this is that the `cond` statement in the affiliate fee function is going to get messy pretty quickly. Now lets start implementing the same thing with multimethods

    (defmulti name dispatch-fn & options)

This is the basic multimethod definition. `dispatch-fn` is a Clojure function that simply accepts the arguments that are passed when the multimethod is called and returns a "dispatching value". Let's look at this with a concrete example:

    (defmulti affiliate-fee :referrer)

    (defmethod affiliate-fee :mint.com [user]
      (fee-amount 0.03 user))

    (defmethod affiliate-fee :google.com [user]
      (fee-amount 0.01 user))

    (defmethod affiliate-fee :default [user]
      (fee-amount 0.02 user))

    (def user-1 {:login "rob" :referrer :mint.com :salary 100000})
    (def user-2 {:login "kyle" :referrer :google.com :salary 90000})
    (def user-3 {:login "celeste" :referrer :yahoo.com :salary 70000})

    (affiliate-fee user-1)
    30.0
    (affiliate-fee user-2)
    9.0
    (affiliate-fee user-3)
    14.0

When the call is first made to `affiliate-fee` the multimethod calls the `dispatch-fn` first. The arguments to the dispatch function are the same as the arguments to the function, a hash map, in this case. Because keys work as functions, the function `:referrer` is called on the hash which returns the value associated with that and the function is processed. You can add in more criteria and define more methods so that every possible outcome is handled properly.

### Ad-Hoc Hierarchies
You can use the `derive` method to create hierarchies and classifications. Imagine a service that has different membership levels:

    (derive ::bronze ::basic)
    (derive ::silver ::basic)
    (derive ::gold ::premier)
    (derive ::platinum ::premier)

    (isa? ::platinum ::premier)
    true

    (isa? ::bronze ::premier)
    false

By doing this we can redefine our methods to use these classifications, you can have separate handling for `basic` and `premier` as opposed to needing methods that handle all four levels of service separately.

### Java Hierarchies
Clojure supports Java class hierarchies out of the box, so there's no need to construct these using `derive`. Subclasses will be able to identify themselves as members of their parent classes.

### Multiple Inheritance
Clojure doesn't forbid multiple inheritance so you can use things like this:

    (derive ::programmer ::employee)
    (derive ::programmer ::geek)

Of course, with any kind of multiple inheritance situation you need a way to determine precedence when both inherited classes implement the same method. You can do this in Clojure with the following syntax.

    (prefer-method multimethod-name ::geek ::employee)


