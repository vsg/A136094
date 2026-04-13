from collections import defaultdict
from typing import NamedTuple

MAX_N = 6
SOLUTIONS = defaultdict(str)
PAIR_SOLUTIONS = defaultdict(str)

ALL_DIGITS = "123456789"

MOVES = 0

class Item(NamedTuple):
    head: str
    tail: set

    def size(self):
        return 1 + len(self.tail)

    def contains(self, other):
        return (other.head == self.head or other.head in self.tail) and other.tail.issubset(self.tail)

    def swap(self, a, b):
        head, tail = self.head, self.tail
        head = b if (head == a) else a if (head == b) else head
        have_a, have_b = a in tail, b in tail
        if have_a and not have_b: 
            tail = (tail - {a}) | {b}
        elif have_b and not have_a: 
            tail = (tail - {b}) | {a}
        return Item(head, tail)
    
    def __repr__(self):
        return self.head + "".join(sorted(self.tail))

def all_permutations(digits):
    return [Item(d, set(digits) - {d}) for d in digits]

def state_key(state):
    return ",".join(sorted(str(x) for x in state))

def symmetric(state, key, d1, d2):
    return state_key(x.swap(d1, d2) for x in state) == key

def dedup_moves(state, moves):
    result = []
    key = state_key(state)
    for d1 in sorted(moves):
        if any(symmetric(state, key, d1, d2) for d2 in result):
            continue
        result.append(d1)
    return result

def dedup_items(state):
    result = []
    for x in state:
        if any(r.contains(x) for r in result):
            continue
        result = [r for r in result if not x.contains(r)] + [x]
    return result

def make_move(state, digit):
    result = []
    for x in state:
        if x.head == digit:
            result.extend(all_permutations(x.tail))
        else:
            result.append(x)
    return dedup_items(result)

def _k2(item1, item2):
    h1, t1 = item1.head, item1.tail
    h2, t2 = item2.head, item2.tail
    return (
        len(t1),                                      # defines normalized h1, t1 digits
        0 if (h2 == h1) else 1 if (h2 in t1) else 2,  # defines normalized h2 digit
        1 if (h1 in t2) else 0,                       # defines normalized t2 & h1 digit
        len(t1 & t2),                                 # defines normalized t2 & t1 digits
        len(t2)                                       # defines normalized remaining t2 digits
    )

def k2(item1, item2):
    return min(_k2(item1, item2), _k2(item2, item1))

def prune1(prefix, state, max_len):
    if not state:
        return False
    longest_tail = max([len(x.tail) for x in state], default=0)
    return len(prefix) + len(SOLUTIONS[longest_tail]) + 1 > max_len

def prune2(prefix, state, max_len):
    if not state:
        return False
    for i, item1 in enumerate(state):
        size1 = item1.size()
        for j, item2 in enumerate(state, start=i+1):
            size2 = item2.size()
            key = k2(item1, item2)
            if len(prefix) + len(PAIR_SOLUTIONS[key]) > max_len:
                return True
    return False

def solve_len(prefix, state, max_len, seen):
    if len(prefix) <= max_len and not state:
        return prefix
    if len(prefix) >= max_len:
        return None

    global MOVES

    MOVES = MOVES + 1
    if MOVES % 10000 == 0:
        print(f"moves = {MOVES}, prefix = {prefix}, state = {state}")

    moves = set(x.head for x in state)

    # Skip symmetric moves
    moves = dedup_moves(state, moves)
    
    for digit in sorted(moves):
        next_prefix = prefix + digit
        next_state = make_move(state, digit)
        next_level = len(next_prefix)

        # Skip duplicate states
        key = state_key(next_state)
        if key in seen[next_level]:
            continue
        seen[next_level].add(key)

        # Prune
        if prune1(next_prefix, next_state, max_len):
            continue

        if prune2(next_prefix, next_state, max_len):
            continue

        result = solve_len(next_prefix, next_state, max_len, seen)
        if result:
            return result

    return None

def solve(root_state):
    global MOVES
    for max_len in range(1, 100):
        print(f"root = {root_state}, max_len = {max_len}")
        MOVES = 0
        result = solve_len("", root_state, max_len, defaultdict(set))
        if result:
            return result
    return None

def all_items():
    items = []
    for mask in range(1 << MAX_N):
        for h in range(MAX_N):
            if ((mask >> h) & 1) == 0: 
                continue
            item = Item(str(h+1), 
                        set(str(t+1) for t in range(MAX_N) 
                                     if t != h and ((mask >> t) & 1) != 0))
            items.append(item)
    return items

# Does not return all possible pairs. Returns each structurally different pair at least once.
def pair_items():
    pairs = []
    
    items = all_items()
    items.sort(key=Item.size)
    
    for size1 in range(1, MAX_N):
        item1 = Item("1", set(ALL_DIGITS[1:size1]))

        for item2 in items:
            size2 = item2.size()
            if size2 > size1:
                continue
            if item1.contains(item2) or item2.contains(item1): 
                continue
            pairs.append((item1, item2))
    
    return pairs

def precalc():
    for item1, item2 in pair_items():
        key = k2(item1, item2)
        if key in PAIR_SOLUTIONS:
            continue
        
        solution = solve((item1, item2))
        print(f"item1 = {item1}, item2 = {item2}, solution = {solution}")
        if not solution: raise Exception()
        PAIR_SOLUTIONS[key] = solution

def main():
    precalc()

    for n in range(1, MAX_N+1):
        digits = ALL_DIGITS[:n]
        permutations = all_permutations(digits)
        solution = solve(permutations)
        print(f"digits = {digits}, solution = {solution}")
        SOLUTIONS[n] = solution

if __name__ == "__main__":
    main()
