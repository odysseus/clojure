# State
State is the set of values associated with a running program at a given point in time. In a mutable language, this state is changing constantly as variables are being incremented, decremented, calculated or reassigned. For the most part this is fine in a single-threaded, single-process environment. There will never be an issue with incorrect state because only one thing is using it.

In multithreaded, parallel and/or concurrent environments however this breaks down when the state is shared among processes/threads. One thread could read a state that was invalid by the time it returned a value. In changing that value, it would make that invalid for another thread. Multithreaded, shared-state applications are just a Pandora's box of bugs waiting to happen. Just a few of these, using `a` and `b` to describe different threads.

### Lost Updates
`a` and `b` both inspect the value of `x` which is `10`, and update it using an incrementor function. Regardless of who returns first the final value is `11` because both read `10` and then incremented. As such one of the updates is lost, because the final value should have been `12`

### Dirty Read
`a` reads data that `b` is in the process of updating. As soon as `a` goes off to do its thing the data is "dirty" or inconsistent with what the actual application state should be because `b` will return with the updated data. Similarly, an *unrepeatable* read occurs when `a` reads data that is then updated by `b`, meaning that `a` will never get the same value from a read.

### Phantom Read
Occurs when a thread goes to read data that has been deleted, or modified and copied to another register. The values the thread is reading are dead values as far as the program is concerned.

## Locks
The traditional solution to these problems is to use locks. Essentially each mutable process has a lock allowing the thread that holds it to change the underlying data. While this lock is held by a thread the other threads are incapable of performing operations that might result in errors. When the lock becomes free again another process can claim it and execute its code.

This works for simple problems, but what about a situation in which multiple updates need to occur in a coordinated manner?  This requires a thread to obtain multiple locks, which could significantly slow the execution of a program because many structures might be locked while waiting for the last one to free up. It's still possible to write complicated programs with this model, but it's also clearly a workaround for the dealing with the problems of parallelization and concurrency for a language that does not support it by default.

### Problems With Locks
There are a few traditional problems with locks, though these are not the only potential problems introduced by the pattern. The first problem is that locking removes some of the benefit of multithreading. By locking data you're essentially forcing all operations on it to be in a single-threaded manner to prevent data inconsistencies.

Furthermore, when looking at the problems detailed above you'll see that not only writers need to lock, readers need to as well to prevent the data they are reading from changing during their operation. However, reading doesn't change the data and in theory could be done in parallel, but locks suffer from the "hammer problem": To a man with a hammer, everything is a nail. Simple locks are a one size fits all solution that in many cases aren't a good fit at all.

Finally, locks are manual and must be performed by the programmer. Forgetting to lock the data when writing a method can cause a bug that may go unnoticed for a long time, and be hard to track down.

These are the most general problems with locking, but they are not the only issues. Further problems with locks are:

- **Deadlock**: Occurs when two or more threads hold locks that the other one needs, and all threads are waiting for those locks to become available. Because execution won't proceed until all of the needed locks are in their possession, the program itself won't proceed.
- **Starvation**: Occurs when a thread is not allocated enough resources to do its job, and thus never completes it when it runs out of resources.
- **Livelock**: Is a more lively version of deadlock. In deadlock the two threads halt execution until the resources they need are made available. In livelock the threads continue executing but make no progress towards their goal. It can lead to a starvation situation if the thread ends up consuming more resources than it is allocated.
- **Race Condition**: Occurs when the interleaving execution of threads leads to an undesired and/or unexpected result. Difficutl to find and difficult to debug because they are rare.

### Identity and State
In most OO programming languages the ideas of identity and state are conflated, but in a technical sense this doesn't have to be. The identity of something can be seen as a list of the variables that describe it, the state would be the values these variables possess at any given point in time. In Ruby these ideas are fundamentally the same, the variable is a value and the value is the variable, but supposed you took a different approach that separated these two?

Identity remains the same, the list of variables that define an object are always the same type regardless of the instance or their individual values. Even as the instance variables of a particular object changes, the template remains the same.  The template in this case is analagous to the "identity" of the object. With that in mind, we can extend this notion to view the state of the object as simply a snapshot encapsulating the values of its variables at any point in time. Similarly, if we were to retain these snapshots across changes in state, we would have an object with a fixed identity, but a mutating state, yet without both of these ideas being conflated to the point where identity and state are one and the same.

### Immutability
As mentioned many times already, structures in Clojure are immutable, but the technical side of that is a little more interesting than "You can't change this." Constantly overriting values in memory with new values, or creating new values every time a change is made has the potential to be an incredibly resource intensive way of preserving immutability, so it's not surprising that most functional languages don't do things this way.  In the case of Clojure, objects track themselves almost like a changelog, and despite appearances to the contrary, different versions of the same object are stored at the same location in memory, they are just stored in a versioned state, so that every reference to the object points to a valid and consistent instance of it.

### Immutability and Concurrency
The two main problems with concurrency in general are lost updates/incorrect writes, and inconsistent data/incorrect reads. Immutability by default solves the first problem right away. Immutable structures will always be in a consistent state and cannot be modified into an inconsistent one while being read.

Incorrect writes is harder to solve, but it helps to think of it as more of a data problem and less of a language design problem.  When you need guarantees about the consistency of your data the best solution is to create a runtime environment that manages both memory and data for the programmer. Much like a database, Clojure relies on a variety of processes to deal with data, and forbids the programmer from accessing memory registers directly. Thanks to these processes it can manage versioned stores of data structures, enforce validity, check for changes, and enforce a variety of things that ensure integrity of the data. No imperative language offers these kinds of guarantees over memory management and data integrity.

## Managed References
An integral part of the memory management mentioned in the previous section is Clojure's "Managed references" which are named types that can be used in different concurrent programming situations. A brief description of those here:

- `ref`: Shared changes, synchronous coordinated changes
- `agent`: Shared changes, asynchronous independent changes
- `atom`: Shared changes, synchronous independent changes
- `var`: Isolated changes

Taken together these types provide lock-free concurrency control.

### Ref
`ref` creates a managed reference that can be used for synchronous and coordinated changes to mutable data. Imagine a site with users, using a hash to store all the values:

    (def all-users (ref {}))

You can check the value of `all-users` by dereferencing it.

    (deref all-users)
    {}

Clojure also provides a macro for dereferencing by prepending it with the `@` symbol:

    @all-users
    {}

### Mutation
#### ref-set
Mutating the ref is done in several ways, the most straightforward is with `ref-set`, which takes a ref and a new value and replaces the old value with the new one:

    (ref-set all-users {:doug {:name "Doug"}})

But if you run this code you'll get an error saying something about running it inside a transaction, which would make the modification thread-safe. You do this using `dosync`.

    (dosync
       (ref-set all-users {:doug {:name "Doug"}}))

This replaces the old value wholesale, which in most cases is not what you want. One of the circumstances where wholesale replacement comes in handy is with resetting data structures to an empty value. If you wanted to clear out all users you could simply do this:

    (dosync (ref-set all-users {} ))

This would set `all-users` to an empty hash.

#### alter
A common process when mutating values is to read the value of a ref, apply a function that modifies the value, and then store the new value back in the original location. `alter` streamlines this as a function that does precisely that. The basic form is:

    (alter ref fn & args)

Let's examine that by writing a function that adds a new user:

    (defn new-user [id login monthly-budget]
      {:id id
       :login login
       :monthly-budget monthly-budget
       :total-expenses 0})

    (defn add-new-user [login budget-amount]
      (dosync
        (let [current-number (count @all-users)
              user (new-user (inc current-number) login budget-amount)]
          (alter all-users assoc login user))))

We have two functions here, one of them creates a new user as a hash, which is a common Clojure pattern for representing objects. The next function takes arguments needed to create a user, and finally starts an `async` block that creates the new user and passes it to `assoc`. `assoc` takes a hash, key and value and returns a hash with all the previous values plus the new one. In the case of `alter` this now means that the `ref` has been changed to the new hash as well.

Note that you can dereference a ref without needing a `dosync` block, the reason it's done this way is to ensure consistent data. `dosync` essentially locks the ref until the entire process it encloses is completed (It actually doesn't lock, it uses transactions, but for the purposes of explaining it here "locking" will suffice). If we placed the dosync around the `alter` call only it would lock only during the updating of the struct. Before that occurs it would be possible for another thread to create a new user by accessing the soon-to-be stale version of that data. When that other thread returned, it would have created a user with a duplicate user id because the id is based on the total count of the users. By enclosing the entire process in a `dosync` block we ensure that this doesn't happen because the read and the write will be synchronized.

#### Commute
When using `ref-set` or `alter`, if multiple threads are trying operations on the same data, only one of them will succeed, causing the others to fail. When that happens the transaction is rolled-back and attempted again using the new data, and this process repeats until it succeeds. This is the result you want in many cases, but it has the drawback that transactions may be attempted multiple times before finally succeeding.

But there are a number of other situations where the most current value of a structure really doesn't matter, or the order of function application doesn't matter, it just needs to be done. In this case you can use the `commute` function

    (commute ref fn & args)

Is the general form.  The name comes from commutative operations in math.  For example, addition and multiplication are commutative. `a + b = b + a`, it doesn't matter what order the operands come in, the result is the same.

### Software Transactional Memory
As mentioned above, Clojure doesn't use a locking system to maintain memory consistency, it uses transactions. Much like a database, transactions are atomic, undoable operations. Atomic meaning that the entire operation must succeed or none of it succeeds, an operation will never be partially applied. Undoable is a natural consequence of atomic, if part of a transaction fails for any reason, the changes that have been executed must be undone. The reasons for this approach are manifold, but the main reason is to avoid the various problems with locking: deadlocks, starvation, livelocks, and race conditions. Here's how transactions work, and how they differ from locks.

In a locking system, when code needs to mutate data it needs to get the lock for every mutable structure it needs before proceeding. If all of them are not available, it grabs what it can and waits for the others to become available.

In a transactional system, mutable code is placed inside a transaction block (specified by `dosync`) and then it takes an optimistic approach to execution. In other words, it executes, under the assumption that it will succeed. You go code! As the transaction operates, all changes are made locally in the transaction and only the thread making the changes can see the changed values. If multiple threads are operating on the same structure in parallel, the first one to finish commits the values that have been changed. When any other thread attempts to commit to this based on stale data, the transaction is rolled back and aborted. Typically the transaction is then attempted again and again until it either succeeds or reaches some kind of internal limit.

### ACI(D)
Clojure gets a 3/4 on the ACID test:

- **Atomic**: All changes made are atomic, from an outside standpoint all mutations made by a block of code are committed at the same time, and if any of these fail then all of them are rolled back.
- **Consistent**: All transactions bring the mutable structures from one valid state to another.
- **Isolated**: Transactions executed in parallel result in the same outcome as if they were executed in serial.

Clojure is not durable because it's a programming language, and not a database. Values live in memory, not in hard storage, as such it would be impossible to make the system durable, nor would you want to, as the file reads would slow the system down.

### MVCC
Clojure supports multiversion concurrency control, another feature pioneered by and used in databases. In an MVCC system, each thread is given a snapshot of the mutable state when it starts the transaction and mutable changes are made to this snapshot. This allows both readers and writers to perform their jobs without any use of blocking.

## Agents
Agents allow for asynchronus and independent changes to mutable data. The `agent` function allows you to create an agent, which holds values that can be changed using the special functions `send` and `send-off`. Both of these take an agent and a function that will compute the new value. The application of this function happens later and on a separate thread. For this reason agents are also a useful way to run a task on another thread. When that function returns, the value becomes to new value of the agent.

Creating agents is similar to refs, in fact all the syntax is similar to refs, so let's look at examples of basic declaration and reading:

    (def total-cpu-time (agent 0))
    (deref total-cpu-time)
    >> 0
    @total-cpu-time
    >> 0

### Mutating Agents
Agents are used for asynchronus changes. The functions sent to them run on a separate thread at a later time. There are two commands for mutating the value of an agent.

#### Send
The basic form:

    (send agent function & args)

An example:

    (send total-cpu-time + 700)

Despite the infix appearance here, it's important to remember that `+` is a function, all functions sent to an agent will eval like so:

    (fn agentval args)

The result of this evaluation becomes the new value of the agent. So in this case the evaluated form looks like this:

    (+ 0 700)

Returning `700` as the value, so when we dereference the agent now:

    @total-cpu-time
    >> 700

The value that was returned has become the new value of the agent. The only caveat here is that dereferencing the agent before the function has run will still return the old value, the call to send is nonblocking so output can still occur from the agent even though it has a function pending.

Actions sent to `send` use a pool of threads maintained by Clojure. If you exceed the number of threads in this pool the remaining actions get queued and run when threads free up. The number of threads never grows, so as a rule you should use `send` for CPU intensive operations that don't block, this uses the CPU efficiently and prevents a thread from being hogged by a blocking process.

#### Send-off
`send-off` uses the same form as `send` does:

    (send-off agent function & args)

The only real difference between `send` and `send-off` is that the pool of threads used by `send-off` can grow based on the queue of actions it needs to perform, and as such it can handle potentially blocking actions.

### Working With Agents
Clojure provides a number of functions designed specifically for working with agents.

#### Await / await-for
A common pattern with agents is to send it a bunch of actions and then wait for it to finish, `await` and `await-for` are purpose built to do this:

    (await & agents)

Is the general form. Suppose you sent a bunch of actions to a group of agents, but you are at a point in the program where you need to ensure that they have finished processing before you can continue. The line:

    (await double-oh-seven double-oh-eight double-oh-six)

Would cause the current thread to block until all the actions of these three agents were completed, then the thread would resume. Await blocks indefinitely, so it isn't a good choice if there is a chance an action being performed by an agent might fail. In that case you would use `await-for`

    (await-for timeout-in-ms & the-agents)

Await for ends when the agents complete their processing or the timeout in milliseconds runs out, whichever comes first. If you are expecting possible errors you can then check the agents for errors after the timeout finishes, and deal with them accordingly.

#### Errors
If you send an agent a function that would throw an error, the operation fails and the value of the agent remains unchanged. Calls to dereference it still return the same values, but further calls to `send` will return an error.

    (def bad-agent (agent 10))
    (send bad-agent / 0)
    >> #<Agent@2e8dac45 FAILED: 10>
    @bad-agent
    >> 10
    (send bad-agent + 1)
    >> ArithmeticException Divide by zero ...
    @bad-agent
    >> 10

You can check for errors in a particular agent with the function `agent-errors`, which returns a list of all errors that have occurred while processing the agent requests. To make the agent usable again, you need to call `clear-agent-errors`

    (agent-errors bad-agent)
    >> (#<ArithmeticException java.lang.ArithmeticException: Divide by zero>)
    (clear-agent-errors bad-agent)
    (send bad-agent + 1)
    @bad-agent
    >> 11

#### Validations
Finally, you can pass in metadata and validations to an agent at creation using the form:

    (agent initial-value & options)

The only two options are `:meta` and `:validator`. `:meta` takes a map which then becomes the metadata of the agent. `:validator` takes a function which takes a single argument, every time an update to the agent is made, the value is sent to the validation function, if the function returns false or raises an error, the update doesn't occur.

    (def ag (agent 100 :validator #(< % 1000)))
    (ag send + 800)
    @ag
    >> 900
    (ag send + 600)
    @ag
    >> 900

As seen in the example, the validator function requires the value to be less than 1000, and any update that would bring the value equal to or greater than 1000 fails and adds an error to the agent errors list. At this point it becomes the same as above. Any additional attempts to update the agent fail, and all calls to dereference it return the last valid value. To get this working again you need to call `clear-agent-errors` first.

### Agents and Side Effects
As mentioned above, transactions may be executed multiple times before actually being committed, so it's ideally the code being run has no side effects, like print statements, that would repeatedly print themselves out over multiple attempts. But printing and logging are both fairly useful things, so there should be a way to do that in a transaction safe way.

    (dosync
       (send agent-one log-message args-one)
       (send-off agent-two send-message-on-queue args-two)
       (alter a-ref ref-function)
         (some-pure-function args-three))

Agents to the rescue! Clojure holds all agent actions until a transaction completes, so you can create agents that print log statements or do any other kind of side effect, and they will only execute after the `async` block has succeeded.

## Atoms
Atoms are for synchronus and independent changes to mutable data. Atoms are essentially synchronus agents, changes to atoms happen immediately once they are called. Unlike a `ref`, which is also synchronus, changes between atoms can't be coordinated. The syntax for creating and reading them is predictable:

    (def total-rows (atom 0))
    (deref total-rows)
    >> 0
    @total-rows
    >> 0

### Mutating Atoms
Changes to one atom are independent to changes of other atoms, so they don't need to use transactions when updating them.

`reset!` resets the value of the atom to the new value provided:

    (reset! atom new-value)

`swap!` applies a function to the atom with the arguments given

    (def x (atom 0))
    (swap! x + 100)
    @x
    >> 100

There's another form of `reset!` called `compare-and-set!` that takes an old value and a new value, then applies the new value only if the old-value is equal to the value it reads.

    (def x (atom 10))
    (compare-and-set! x 10 20)
    >> true
    @x
    >> 20
    (compare-and-set! x 10 30)
    >> false
    @x
    >> 20

In the examples above the second call to `compare-and-set!` fails because the value of `x` at that point is equal to 20. The most common usage of this function is to use this as a lightweight transaction. At the beginning of a function you dereference the atom, then you perform the function, and at the end you use `compare-and-set!` to change the value. If the value had been changed in the interim by another thread, the mutation would fail, and you could deal with that accordingly. `swap!` uses this internally when setting a value, if the mutation fails then it retries the function application until it succeeds.

## Vars
Thusfar our use of `ref`, `agent`, and `atom` has covered mutable data whose state needs to be shared, but sometimes you need values that are capable of mutating, without the need to share their state. That's where `var`s come in.

Think of a var as a pointer to location for mutable storage, in a thread-local environment. Declaration is a bit different than with the other mutable types as they are declared like a normal variable but with stars around them.

    (def *hbase-master* "localhost")

We would say that `*hbase-master*` is a var with a root binding of `"localhost"`, the asterisks denote that the var needs to be rebound before use. To ensure this you can specify no root binding, which would lead to an exception being thrown whenever you attempt to use it without first reassigning it.

Whenever a var is mutated, that mutation is local to the thread it is mutated in. Whenever another thread accesses the var, it sees either the root binding, or gets an error because there is no binding. Other threads will never see the re-bindings performed in the local thread. Let's look at a real-ish world example of this:

    (defn db-query [db]
      (binding [*mysql-host* db]
        (count *mysql-host*)))

    (def mysql-hosts ["test-mysql" "dev-mysql" "staging-mysql"])

    (pmap db-query mysql-hosts)

In the above example, each version of the function sees a different version of the `*mysql-host*`

That's apparently it for vars.

## The Watchers on the Wall
Because mutation is not really the functional way to do things, being aware of mutation is important for handling side effects. You can do this via "watcher functions". A watcher function takes four arguments and can be set to watch any mutable value for state changes. The four arguments are an identification key, the ref it's being registered against, the old value of the ref and the new value. Here's an example:

    (def x (atom 10))

    (defn on-change [key ref oldval newval]
      (println ref "changed from" oldval "to" newval))

    (add-watch x :watcher on-change)

    (reset! x 20)
    >> #<Atom@5cc218df: 20> has changed from 10 to 20

This watcher function simply prints out every time the value changes, but using this general format it's easy to see how you could do most anything you'd want to with value changes using this pattern. Removing a watcher is easy as well, using `remove-watch`

    (remove-watch x :watcher)

## Futures and Promises
### Futures
A future is a way to run code on a different thread, useful for long running functions or blocking calls that you don't want slowing the execution of the main thread. Using a contrived example:

    (defn long-calc [x y]
      (Thread/sleep 5000)
      (* x y))

Now suppose we had to run several of these:

    (defn long-run []
      (let [x (long-calculation 11 13)
            y (long-calculation 13 17)
            z (long-calculation 17 19)]
        (* x y z)))

Because these are run sequentially by default this would take 15 seconds to run. Since there really isn't a reason for these to be run sequentially we can run them concurrently using `future`

    (defn fast-run []
      (let [x (future (long-calculation 11 13))
            y (future (long-calculation 13 17))
            z (future (long-calculation 17 19))]
        (* @x @y @z)))

This takes almost exactly five seconds to run, the same amount of time as if one of these calls was run individually because it runs them all on separate threads.

Some other functions useful for dealing with futures:
- `future?` returns true if the object is a future
- `future-done?` returns true if the computation of the future is finished
- `future-cancel` Attempts to cancel the future, it does nothing if the future has already started executing.
- `future-cancelled?` returns true if the future has been cancelled

### Promises
A promise is an object that represents the commitment that a value will be delivered to it. You create a promise using the no argument `promise` function.

    (def p (promise))

And read the value using `deref` or the reader macro `@`

    @p

The reader blocks the reading thread until a value has been delivered to it. Delivering a value is done using the almost poetic sounding `deliver` function with the form `(deliver promise value)`

    (deliver p 12)
    @p
    >> 12

Typically the `deliver` call is run on a separate thread from the `deref` call so this is a convenient way of communicating between threads.
