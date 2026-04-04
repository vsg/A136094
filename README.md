# A136094 Solver

Computes values of OEIS sequence [A136094](https://oeis.org/A136094), where a(n) is the smallest number consisting of digits {1, ...,n} that contains all the permutations of {1, ...,n} as subsequences. 

## The Problem

Given n symbols, a complete sequence over {1, …, n} is a sequence that contains every one of the n! permutations as a subsequence (not necessarily contiguous).
Among all shortest complete sequences, a(n) is the lexicographically smallest, interpreted as a decimal number.

The related sequence [A062714](https://oeis.org/A062714) gives the length of the shortest complete sequence for each n.

### Example: n = 3

The shortest complete sequence over {1, 2, 3} has length 7. The smallest such sequence is `1213121`, which contains all 6 permutations of {1, 2, 3} as subsequences:

| Permutation | Embedded in `1213121` |
|---|---|
| 1 2 3 | <ins>1</ins> <ins>2</ins> 1 <ins>3</ins> 1 2 1 |
| 1 3 2 | <ins>1</ins> 2 1 <ins>3</ins> 1 <ins>2</ins> 1 |
| 2 1 3 | 1 <ins>2</ins> <ins>1</ins> <ins>3</ins> 1 2 1 |
| 2 3 1 | 1 <ins>2</ins> 1 <ins>3</ins> <ins>1</ins> 2 1 |
| 3 1 2 | 1 2 1 <ins>3</ins> <ins>1</ins> <ins>2</ins> 1 |
| 3 2 1 | 1 2 1 <ins>3</ins> 1 <ins>2</ins> <ins>1</ins> |

### Known Values

| n | A062714 (min length) | A136094 (smallest number) |
|---|---|---|
| 1 | 1 | 1 |
| 2 | 3 | 121 |
| 3 | 7 | 1213121 |
| 4 | 12 | 123412314213 |
| 5 | 19 | 1234512341523142351 |
| 6 | 28 | 1234516234152361425312643512 |
| 7 | 39 | **123451672341526371425361274351263471253** |
| 8 | 52 | **1234156782315426738152643718265341278635124376812453** |
| 9 | 66 | **123456781923451678234915627348152963471825364912783546123976845123** |

Values for n ≤ 6 were previously known. The values for n = 7, 8, and 9 were first computed by this project, using variations of the algorithm described here.

## How the Algorithm Works

The program builds candidate sequences digit by digit, exploring a tree where each path from root to leaf represents a potential complete sequence. 
At each node, it tracks the set of permutation suffixes, called obligations, that subsequent digits still need to cover. 
When a digit is placed, any obligation starting with that digit has its first element consumed. A sequence is complete when no obligations remain.

The program first tries to find a solution of length 1, searching the entire tree. If none exists, it tries length 2, then length 3, and so on, until a valid supersequence is found.
Since the target length is fixed at each attempt, any branch that cannot satisfy its obligations in the remaining number of digits is pruned.
The program pre-computes many smaller possible subsets of obligations and the minimum number of digits needed to satisfy them. 
At each node, if any known subset is found within the current obligations and cannot be satisfied in the remaining positions of the sequence, the branch is pruned.

Additionally, many branches are equivalent under relabeling of digits. 
If two nodes have obligation sets that can be made identical by renaming digits, they would produce equivalent subtrees, so only the one with the lexicographically smaller path needs to be explored.

## Algorithm Details

The following sections describe the notation, data structures, and techniques used by the program.

### Obligations

#### Basic Notation

Let's introduce the following notation: `(1234)` denotes the set of all permutations of {1, 2, 3, 4} - that is, all 24 sequences:

    1234, 1243, 1324, 1342, 1423, 1432,
    2134, 2143, 2314, 2341, 2413, 2431,
    3124, 3142, 3214, 3241, 3412, 3421,
    4123, 4132, 4213, 4231, 4312, 4321

A prefix before the parentheses fixes the leading element. For example, `1(234)` denotes the 6 permutations that start with 1:

    1234, 1243, 1324, 1342, 1423, 1432

Note that a set of permutations can be decomposed by the first element:

    (1234) = 1(234) ∪ 2(134) ∪ 3(124) ∪ 4(123)

The set `(1234)` contains smaller obligations as subsets. For example:

- `(124)` = all 6 permutations of {1, 2, 4}, each present in `(1234)`
- `(23)` = the sequences 23 and 32, both present in (1234)
- `3(24)` = the sequences 324 and 342, both present in (1234)
- `1(2)`, `3(4)`, `4(1)` = individual two-digit sequences, each present in (1234)
- `1`, `2`, `3`, `4` = single digits, trivially present in (1234)

For brevity, the union symbol ∪ is omitted in the rest of this document. Items written next to each other represent their union.

#### Example: n = 4 with Basic Notation

The following example walks through a complete path in the tree for n = 4, showing the remaining obligations after each digit is placed:
```
                 : 1(234) 2(134) 3(124) 4(123)
               1 : 2(134) 3(124) 4(123)
              12 : 1(34) 3(124) 4(123)
             123 : 1(24) 1(34) 2(14) 4(123)
            1234 : 1(23) 1(24) 1(34) 2(13) 2(14) 3(12)
           12341 : 2(13) 2(14) 3(12) 3(4) 4(2) 4(3)
          123412 : 1(3) 1(4) 3(12) 3(4) 4(1) 4(2) 4(3)
         1234123 : 1(2) 1(3) 1(4) 2(1) 4(1) 4(2) 4(3)
        12341231 : 2(1) 4(1) 4(2) 4(3)
       123412314 : 2(1) 3()
      1234123142 : 1() 3()
     12341231421 : 3()
    123412314213 : 
```

The starting obligation is the full set of permutations `(1234) = 1(234) 2(134) 3(124) 4(123)`.

At each step, placing a digit affects only the obligation items that start with that digit. The first element of each such item is consumed, while other items remain unchanged.
After consuming the first element, the resulting obligation may now be a subset of another existing obligation. 
If it is a subset, then it can be dropped, since satisfying the larger one would automatically satisfy the smaller.

In the first step, placing digit 1: only `1(234)` starts with 1, so it is consumed and becomes `(234) = 2(34) 3(24) 4(23)`. 
But `2(34)` is already contained in `2(134)`, `3(24)` is contained in `3(124)`, and `4(23)` in `4(123)`. 
After dropping these, the remaining obligations simplify to: `2(134) 3(124) 4(123)`.

    placing 1:
        1(234) 2(134) 3(124) 4(123)
        => (234)  2(134) 3(124) 4(123)                  # 1(234) consumed, leading 1 removed
        =  2(34) 3(24) 4(23)  2(134) 3(124) 4(123)      # (234) split into 2(34) 3(24) 4(23)
        =  2(134) 3(124) 4(123)                         # 2(34) 3(24) 4(23) dropped as subsets

In the second step, after placing digit 2: `2(134)` becomes `(134)` = `1(34) 3(14) 4(13)`. Again, `3(14)` is contained in `3(124)` and `4(13)` in `4(123)`, so these can be dropped. 
The remaining obligations simplify to: `1(34) 3(124) 4(123)`.

    placing 2:
        2(134) 3(124) 4(123)
        => (134)  3(124) 4(123)                         # 2(134) consumed, leading 2 removed
        =  1(34) 3(14) 4(13)  3(124) 4(123)             # (134) split into 1(34) 3(14) 4(13)
        =  1(34) 3(124) 4(123)                          # 3(14) 4(13) dropped as subsets

One obligation is contained in another if either: 

- all of the first obligation's digits appear among the other's tail digits, or 
- both obligations start with the same head and the first obligation's tail is a subset of the other's tail.

In the program, each obligation item with a single-digit head can be represented as a head digit (4 bits) combined with a 9-bit bitmap for the tail digits, fitting in a 16-bit number. 
A set of obligation items can be stored as an array of such numbers.

#### Grouped Notation

As n grows, the number of individual obligation items can reach hundreds. 
Searching for known subproblems within the obligations (described in "Subproblem Pruning" below) requires nested loops of depth 2-5, which becomes prohibitively expensive with this many items. 
A representation that groups individual items sharing the same set of digits into a single item reduces the number of elements to iterate over, making these operations much less expensive.

Let's introduce a grouped notation, in which obligations sharing the same set of unique digits are written together. 
The notation `heads/digits` means: for each digit in `heads`, there is an obligation consisting of that digit followed by all permutations of the remaining digits from `digits`. For example:

    1234/1234 = 1(234) 2(134) 3(124) 4(123) = (1234)
    13/1234   = 1(234) 3(124)
    2/124     = 2(14)
    2/2       = 2

The first part (heads) lists which digits appear as the leading element. The second part (digits) lists all unique digits involved in the obligation.

For small n the difference is modest, but as n grows the grouped notation typically reduces the number of obligation items to iterate by a factor of 2 to 5.

#### Example: n = 4 with Grouped Notation

Obligations after each prefix for n = 4, using the grouped notation:
```
             : 1234/1234
           1 : 234/1234
          12 : 34/1234 1/134
         123 : 4/1234 12/124 1/134
        1234 : 123/123 12/124 1/134
       12341 : 23/123 2/124 34/34 4/24
      123412 : 3/123 14/14 34/34 1/13 4/24
     1234123 : 12/12 14/14 1/13 4/24 4/34
    12341231 : 2/12 4/14 4/24 4/34
   123412314 : 2/12 3/3
  1234123142 : 1/1 3/3
 12341231421 : 3/3
123412314213 : 
```

When placing a digit, each obligation item with that digit as head has its head consumed. In grouped notation:

    placing 1:
        1234/1234
        => 234/234  234/1234                             # head 1 consumed; 234/1234 unchanged
        =  2/234 3/234 4/234  234/1234                   # 234/234 split into 2/234 3/234 4/234
        =  234/1234                                      # 2/234 3/234 4/234 dropped as subsets

Placing digit 1 splits `1234/1234` into two items: `234/234`, the result of consuming head 1, and `234/1234`, the remaining heads unchanged.

To see why, consider that `1234/1234` in basic notation is `1(234) 2(134) 3(124) 4(123)`. Placing digit 1 consumes `1(234)`, producing `(234)` = `234/234`. 
The remaining obligations `2(134) 3(124) 4(123)` = `234/1234` are unaffected.

The item `234/234` can be simplified: for each of its heads (2, 3, 4), the corresponding obligation uses a subset of the digits in `234/1234`, which has the same heads and more digits. 
Since every element of `234/234` is contained in the corresponding element of `234/1234`, the entire item can be dropped, leaving only `234/1234`.

In the second step:

    placing 2:
        234/1234
        => 134/134  34/1234                              # head 2 consumed; 34/1234 unchanged
        =  1/134 3/134 4/134  34/1234                    # 134/134 split into 1/134 3/134 4/134
        =  1/134 34/1234                                 # 3/134 4/134 dropped as subsets

Placing digit 2 splits `234/1234` into two items: `134/134`, the result of consuming head 2, and `34/1234`, the remaining heads unchanged.

The item `134/134` can be simplified: heads 3 and 4 also appear as heads in `34/1234`, which has more digits. Since `3/134` is contained in `3/1234` and `4/134` in `4/1234`, these can be dropped. 
Only head 1 has no corresponding head in `34/1234`, so `134/134` reduces to `1/134`.

In grouped notation, one item is contained in another if:

- all of the first item's digits appear among the second item's digits, and either:
   - each of the first item's heads is also a head in the second item, or
   - the second item has at least one head not present in the first item's digits.

In the program, each grouped notation item can be represented as two 9-bit bitmaps: one for the heads and one for the digits, both fitting in a 32-bit number. 
A set of grouped obligation items can be stored as an array of such numbers.

### Subproblem Pruning

While exploring the tree of candidate solutions, the program prunes a branch if any subset of its obligations is known to require more digits than the remaining positions in the sequence. 
For example, if a node's obligations are `3/123 14/14 1/13 4/24`, and the subset `3/123 1/13` is known to require at least 5 digits (its minimal solution is `13121`), 
then this branch can be pruned when searching for solutions of length 4 or less.

The program pre-computes the minimum solution length for combinations of 1 to 5 obligation items. 
To make lookup fast, solution lengths are stored as multi-index Java arrays of bytes, which are nested arrays forming a tree, where each index selects the next item in the combination. 
Combinations of different lengths are stored in separate arrays.
The search for known subsets is performed by 1 to 5 nested loops, each selecting one obligation item from the current node's obligations to form a combination, 
then looking up that combination in the corresponding array.

Many combinations are equivalent under relabeling of digits. For example, `12/12 3/13` and `34/34 1/13` have the same structure. 
The digits in `34/34 1/13` can be relabeled as 3->1, 4->2, 1->3, producing `12/12 3/13`. 
Storing all such equivalent combinations separately would waste a lot of memory. 

Instead, a group of combinations that are equivalent under relabeling of digits could be stored only once. 
Among all relabelings of a combination, one element could be selected as the key, and stored to represent the whole group. 
Any element could be such key, as long as there is a deterministic way to map any element of the group to the same key element. 
For example, one way to choose such key is to pick the lexicographically smallest element of the group, 
but any deterministic mapping that consistently produces the same result for all elements of the group would work.

```
# Pre-computation
for each combination C of 1 to 5 items, from all possible values:
    k1, k2, k3, k4, k5 = normalize(C)
    length = solve(C)
    if len(C) == 1: lookup1[k1] = length
    if len(C) == 2: lookup2[k1][k2] = length
    if len(C) == 3: lookup3[k1][k2][k3] = length
    if len(C) == 4: lookup4[k1][k2][k3][k4] = length
    if len(C) == 5: lookup5[k1][k2][k3][k4][k5] = length

# Pruning
for item1 in obligations:                            # loop 1
    k1 = normalize(item1)
    if lookup1[k1] > remaining_digits: prune

    for item2 in obligations after item1:            # loop 2
        k1, k2 = normalize(item1, item2)
        if lookup2[k1][k2] > remaining_digits: prune

        for item3 in obligations after item2:        # loop 3
            k1, k2, k3 = normalize(item1, item2, item3)
            if lookup3[k1][k2][k3] > remaining_digits: prune

            ...                                      # loops 4, 5
```

Unfortunately, normalizing all combination elements at every iteration of the several nested loops is too expensive.
The program chooses a middle ground approach. Instead of fully normalizing each combination, it partially normalizes.
Only the digits appearing in the first one or two items are mapped to canonical form. 
This relabeling is applied to all items in the combination, so the first one or two items become fully normalized, while the remaining items are relabeled consistently but not themselves normalized. 
The lookup arrays store combinations in this partially normalized form. 
This significantly reduces memory storage requirements compared to storing all equivalent combinations.
The outer one or two loops must compute the partial normalization and relabel the remaining items, but the inner loops require no normalization and perform only direct array lookups.

```
# Pre-computation
for each combination C of 1 to 3 items, from all possible values:
    k1, m2, m3 = partial_normalize(C)             # normalize first item, relabel the rest
    length = solve(C)
    if len(C) == 1: lookup1[k1] = length
    if len(C) == 2: lookup2[k1][m2] = length
    if len(C) == 3: lookup3[k1][m2][m3] = length

for each combination C of 4 to 5 items, from all possible values:
    k1, k2, m3, m4, m5 = partial_normalize(C)     # normalize first two items, relabel the rest
    length = solve(C)
    if len(C) == 4: lookup4[k1][k2][m3][m4] = length
    if len(C) == 5: lookup5[k1][k2][m3][m4][m5] = length

# Pruning
for item1 in obligations:                                   # loop 1
    k1, mapping = normalize(item1)
    if lookup1[k1] > remaining_digits: prune
    relabeled = [relabel(item, mapping) for item in obligations after item1]
    for m2 in relabeled:                                    # loop 2
        if lookup2[k1][m2] > remaining_digits: prune
        for m3 in relabeled after m2:                       # loop 3
            if lookup3[k1][m2][m3] > remaining_digits: prune

for item1 in obligations:
    for item2 in obligations after item1:
        k1, k2, mapping = normalize(item1, item2)
        relabeled = [relabel(item, mapping) for item in obligations after item2]
        for m3 in relabeled:
            for m4 in relabeled after m3:                   # loop 4
                if lookup4[k1][k2][m3][m4] > remaining_digits: prune
                for m5 in relabeled after m4:               # loop 5
                    if lookup5[k1][k2][m3][m4][m5] > remaining_digits: prune
```

An earlier version of the program used a trie-like approach to normalize subsets of 1 to 5 obligation items in a generic way. 
For n = 9, there are 19,171 different possible obligation items in grouped notation.

Without accounting for symmetry, the trie size would grow as a power of that number. 
The first level of the trie would have up to 19,171 nodes, one for each possible first item. 
The second level would branch from each of those, requiring 367 million nodes just to store all pairs of items.

| Items | All combinations | Unique up to relabeling |
|---|---|---|
| 1 | 19,171 | 45 |
| 2 | 367,527,241 | 6,800 |
| 3 | 7,045,864,737,211 | 4,970,599 |
| 4 | 135,076,272,877,072,081 | 820,811,271 |


Taking relabeling symmetry into account reduces the number of nodes, since items that differ only by a relabeling can share the same subtree. 
However, each branch of the trie must also store a normalizing relabeling - a mapping that describes how digits should be renamed when following that branch. 
While walking the trie during lookup, the iterator would carry not only a reference to the current node, but also the combined relabeling accumulated from all previous steps. 
This combined relabeling describes how the digits in the original obligation items should be renamed to match the subset represented by the current node. 
Multiple parent nodes can point to the same child node but with different transition relabelings, so the trie effectively becomes a directed acyclic graph (DAG).

TODO k1, k2

### Symmetry Normalization

While exploring the tree of candidates, the program detects when two nodes have obligations that differ only by a relabeling of digits. 
Such nodes are duplicates and will produce solutions of the same length - only the one with the lexicographically smaller path needs to be explored.

One way to detect such duplicates is to define a normalization function that maps any set of obligations to a canonical form - the same output regardless of how the digits are relabeled. 
This canonical form serves as a key: while exploring the tree, if a previously seen key is encountered, the duplicate node can be discarded.

The straightforward way to normalize is to try all n! relabelings of digits. 
For each relabeling, sort the items within each obligation, then sort the obligations themselves, and select the lexicographically smallest such representation as the canonical form. 
Two obligation sets with the same canonical form are duplicates, so one can be discarded. 
However, for n = 9 this means inspecting up to 9! = 362,880 relabelings per node, which is far too expensive.

Instead of treating all n digits as potentially interchangeable, the program partitions digits into equivalence groups based on the structure of the obligations. 
Digits that play structurally different roles cannot be interchangeable, so there is no need to consider relabelings between them.

If the groups have sizes a, b, c, ... (where `a + b + c + ... = n`), only `a! * b! * c! * ...` relabelings need to be considered. For n = 9:

    Group sizes                   Number of relabelings
    [9]                           362,880
    [1, 8]                        1 * 40,320 = 40,320
    [4, 5]                        24 * 120 = 2,880
    [2, 2, 2, 3]                  2 * 2 * 2 * 6 = 48
    [1, 1, 1, 1, 2, 3]            2 * 6 = 12
    [1, 1, 1, 1, 1, 1, 1, 1, 1]   1

In practice, deeper in the tree the groups tend to be smaller, and the number of relabelings drops to a manageable amount.

#### Structural Hashing

Two digits can belong to the same equivalence group only if they play identical structural roles in the current obligations. 
The program characterizes each digit by a set of counts that depend on the digit's position within the structure of the obligations, but do not depend on the digit's value. 
Many such structural counts are possible; the program uses the following:

- how many items the digit appears in
- how many items the digit appears in as a head
- how many different digits appear across all items containing this digit
- how many different digits appear as heads across all items containing this digit as a head

Each of these counts is computed for each digit separately across obligation items of different sizes (number of heads, number of digits). 
Two digits with identical counts across all categories belong to the same equivalence group.

The full set of structural counts per digit is easy to compute but produces many values, which are inconvenient to store and compare. 
Instead, the program computes a hash of the counts for each digit, sorts digits by their hash values, and treats consecutive runs of digits with the same hash value as one equivalence group. 
This occasionally produces false positives - digits that hash the same but are not truly equivalent. 
This slightly increases the number of relabelings to consider, but does not affect correctness.

#### Structural Hashing Example

The program computes structural counts separately for obligation items of each size. Consider a node with n = 4 and the following obligations:

    23/123  24/124  13/13  14/14

Counting structural roles for each digit in items of size 2 heads / 3 digits:

    Digit 1: appears in 2 such items (23/123, 24/124), among 4 different digits (1, 2, 3, 4)
             appears as head in 0 such items, with 0 other digits

    Digit 2: appears in 2 such items (23/123, 24/124), among 4 different digits (1, 2, 3, 4)
             appears as head in 2 such items (23/123, 24/124), among 3 different digits as heads (2, 3, 4)
    
    Digit 3: appears in 1 such item (23/123), among 3 different digits (1, 2, 3)
             appears as head in 1 such item (23/123), among 2 different digits as heads (2, 3)
    
    Digit 4: appears in 1 such item (24/124), among 3 different digits (1, 2, 4)
             appears as head in 1 such item (24/124), among 2 different digits as heads (2, 4)

Counting structural roles for each digit in items of size 2 heads / 2 digits:

    Digit 1: appears in 2 such items (13/13, 14/14), among 3 different digits (1, 3, 4)
             appears as head in 2 such items (13/13, 14/14), among 3 different digits as heads (1, 3, 4)
    
    Digit 2: appears in 0 such items, with 0 other digits
             appears as head in 0 such items, with 0 other digits as heads
    
    Digit 3: appears in 1 such item (13/13), among 2 different digits (1, 3)
             appears as head in 1 such item (13/13), among 2 different digits as heads (1, 3)
    
    Digit 4: appears in 1 such item (14/14), among 2 different digits (1, 4)
             appears as head in 1 such item (14/14), among 2 different digits as heads (1, 4)

The resulting counts and their hashes for each digit, across items of each size:

    Digit 1: [[2, 4, 0, 0], [2, 3, 2, 3]], hash = -1554236668
    Digit 2: [[2, 4, 2, 3], [0, 0, 0, 0]], hash = -1494270333
    Digit 3: [[1, 3, 1, 2], [1, 2, 1, 2]], hash = 140862021
    Digit 4: [[1, 3, 1, 2], [1, 2, 1, 2]], hash = 140862021

Digits 3 and 4 have identical hashes, so they belong to the same equivalence group. 
After sorting by hash, the groups are `{1}, {2}, {3, 4}` with sizes `[1, 1, 2]`. 
The number of relabelings to consider is `1 * 1 * 2! = 2`, instead of `4! = 24`.

## Usage

Build:
```
mvnw clean package -DexcludedGroups=slow
```

Build and run all tests (including slower ones):
```
mvnw clean verify
```

Run:
```
solve 7                # solve for n=7
solve 8                # solve for n=8
```

Solve a specific subproblem:
```
solve 7-dfs --save-files --checkpoint-shapes=1/7 --solve "1/123456 24/12347"
```

See [usage.txt](usage.txt) for more examples and available options.

### Performance

Approximate running times and resource requirements:
- n = 7: ~1 minute on 2 CPU cores / 500 MB RAM
- n = 8: ~1 hour on 16 CPU cores / 32 GB RAM, or ~3 hours on 4 CPU cores / 16 GB RAM
- n = 9: ~1 month on 192 CPU cores / 256 GB RAM

## See Also

- V. Chvátal, D. A. Klarner, D. E. Knuth, *Selected Combinatorial Research Problems*, Stanford CS-TR-72-292, 1972. [Stanford TR](http://i.stanford.edu/TR/CS-TR-72-292.html)
- M. Newey, *Notes on a Problem Involving Permutations as Subsequences*, Stanford CS-TR-73-340, 1973. [Stanford TR](http://i.stanford.edu/TR/CS-TR-73-340.html) | [SDR](https://purl.stanford.edu/br155cb4219)
- L. Adleman, *Short Permutation Strings*, Discrete Mathematics 10(2), 1974. [DOI](https://doi.org/10.1016/0012-365X(74)90116-2)
- P. J. Koutas, T. C. Hu, *Shortest String Containing All Permutations*, Discrete Mathematics 11(2), 1975. [DOI](https://doi.org/10.1016/0012-365X(75)90004-7)
- D. J. Kleitman, D. J. Kwiatkowski, *A Lower Bound on the Length of a Sequence Containing All Permutations as Subsequences*, J. Combin. Theory Ser. A 21(2), 1976. [DOI](https://doi.org/10.1016/0097-3165(76)90057-1)
- G. Galbiati, F. P. Preparata, *On Permutation-Embedding Sequences*, SIAM Journal on Applied Mathematics 30(3), 1976. [DOI](https://doi.org/10.1137/0130040)
- S. P. Mohanty, *Shortest String Containing All Permutations*, Discrete Mathematics 31(1), 1980. [DOI](https://doi.org/10.1016/0012-365X(80)90177-6)
- M.-C. Cai, *A New Bound on the Length of the Shortest String Containing All r-Permutations*, Discrete Mathematics 38(2–3), 1982. [DOI](https://doi.org/10.1016/0012-365X(82)90155-8)
- C. Savage, *Short Strings Containing All k-Element Permutations*, Discrete Mathematics 42(2–3), 1982. [DOI](https://doi.org/10.1016/0012-365X(82)90224-2)
- A. A. Schäffer, *Shortest Prefix Strings Containing All Subset Permutations*, Discrete Mathematics 65(3), 1987. [DOI](https://doi.org/10.1016/0012-365X(87)90193-2)
- R. Erra, N. Lygeros, N. Stewart, *On Minimal Strings Containing the Elements of Sn by Decimation*, DMTCS 14(2), 2012. [DOI](https://doi.org/10.46298/dmtcs.2289)
- E. Zalinescu, *Shorter Strings Containing All k-Element Permutations*, Information Processing Letters 111(12), 2011. [DOI](https://doi.org/10.1016/j.ipl.2011.03.018)
- S. Radomirovic, *A Construction of Short Sequences Containing All Permutations of a Set as Subsequences*, Electronic J. Combinatorics 19(4), 2012. [DOI](https://doi.org/10.37236/2859)
- P. Uznański, *All Permutations Supersequence is coNP-complete*, 2015. [arXiv](https://doi.org/10.48550/arXiv.1506.05079)
- M. Engen, V. Vatter, *Containing All Permutations*, Amer. Math. Monthly 128(1), 2021. [arXiv](https://doi.org/10.48550/arXiv.1810.08252)
- O. Tan, *Skip Letters for Short Supersequence of All Permutations*, Discrete Mathematics 345(12), 2022. [arXiv](https://doi.org/10.48550/arXiv.2201.06306)

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) file for details.
