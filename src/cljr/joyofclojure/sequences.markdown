# Sequences
The Joy of Clojure on sequences.

### Persistent
Sequences in Clojure are persistent, meaning that they preserve their previous states when being changed, thus retaining immutability from the program's perspective. Here's an example:

    (def ds [:willie :barnabas :adam])
    >> [:willie :barnabas :adam]
    (def ds1 (replace {:barnabas :quentin} ds))
    >> [:willie :quentin :adam]
    ds
    >> [:willie :barnabas :adam]

Here we see that replacing an item in the list didn't actually replace the item in place. Instead it created a new list with the new values and retained the list that `ds` was assigned to at creation, so it stays immutable. At first this seems expensive because a naive implementation would create new lists at every change. In truth, this is not what happens. Clojure's implementations are based heavily on Hash Array Mapped Trie (HAMT) structures, but discussing those is way beyond the scope of these notes.
