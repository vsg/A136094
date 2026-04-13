from collections import defaultdict
from typing import NamedTuple

MAX_N = 5

ALL_DIGITS = "123456789"

class Item(NamedTuple):
    head: str
    tail: set

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

def make_moves(states):
    result = []
    seen = set()

    for prefix, state in states:
        moves = set(x.head for x in state)

        # Skip symmetric moves
        moves = dedup_moves(state, moves)
        
        for digit in sorted(moves):
            next_prefix = prefix + digit
            next_state = make_move(state, digit)

            # Skip duplicate states
            key = state_key(next_state)
            if key in seen:
                continue
            seen.add(key)

            result.append((next_prefix, next_state))

    return result

def solve(root_state):
    states = [("", root_state)]
    for level in range(1, 100):
        states = make_moves(states)
        if not states:
            break
        print(f"root = {root_state}, level = {level}, states = {len(states)}")
        for prefix, state in states:
            if not state:
                return prefix
    return None

def main():
    for n in range(1, MAX_N+1):
        digits = ALL_DIGITS[:n]
        permutations = all_permutations(digits)
        solution = solve(permutations)
        print(f"digits = {digits}, solution = {solution}")

if __name__ == "__main__":
    main()
