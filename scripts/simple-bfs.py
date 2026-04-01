from typing import NamedTuple

class Item(NamedTuple):
    head: str
    tail: set

    def contains(self, other):
        return (other.head == self.head or other.head in self.tail) and other.tail.issubset(self.tail)

    def replace(self, a, b):
        head, tail = self.head, self.tail
        if a == head: 
            head = b
        elif a in tail: 
            tail = (tail - {a}) | {b}
        return Item(head, tail)
    
    def __repr__(self):
        return self.head + "".join(sorted(self.tail))

def all_permutations(digits):
    return [Item(d, set(digits) - {d}) for d in digits]

def dedup(state):
    result = []
    for x in state:
        if all([not r.contains(x) for r in result]):
            result = [r for r in result if not x.contains(r)] + [x]
    return result

def make_move(state, digit):
    result = []
    for x in state:
        if x.head == digit:
            result.extend(all_permutations(x.tail))
        else:
            result.append(x)
    return dedup(result)

def make_moves(states):
    result = []
    seen = set()

    for prefix, state in states:
        moves = set(x.head for x in state)

        for digit in sorted(moves):
            next_prefix = prefix + digit
            next_state = make_move(state, digit)

            key = tuple(sorted([str(x) for x in next_state]))
            if key in seen:
                continue
            seen.add(key)

            result.append((next_prefix, next_state))

    return result

def solve(digits):
    prefix0, state0 = '', all_permutations(digits)

    states = [(prefix0, state0)]
    for level in range(1, 100):
        states = make_moves(states)
        if not states:
            break

        print(level, len(states))

        for prefix, state in states:
            if not state:
                return prefix

print(solve('1234'))
