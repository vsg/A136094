from collections import defaultdict
from typing import NamedTuple

MAX_N = 4

ALL_DIGITS = "123456789"

class Item(NamedTuple):
    head: str
    tail: set

    def __repr__(self):
        return self.head + "".join(sorted(self.tail))

def all_permutations(digits):
    return [Item(d, set(digits) - {d}) for d in digits]

def make_move(state, digit):
    result = []
    for x in state:
        if x.head == digit:
            result.extend(all_permutations(x.tail))
        else:
            result.append(x)
    return result

def solve_len(prefix, state, max_len):
    if len(prefix) <= max_len and not state:
        return prefix
    if len(prefix) >= max_len:
        return None

    moves = set(x.head for x in state)

    for digit in sorted(moves):
        next_prefix = prefix + digit
        next_state = make_move(state, digit)

        result = solve_len(next_prefix, next_state, max_len)
        if result:
            return result

    return None

def solve(root_state):
    for max_len in range(1, 100):
        print(f"root = {root_state}, max_len = {max_len}")
        result = solve_len("", root_state, max_len)
        if result:
            return result
    return None

def main():
    for n in range(1, MAX_N+1):
        digits = ALL_DIGITS[:n]
        permutations = all_permutations(digits)
        solution = solve(permutations)
        print(f"digits = {digits}, solution = {solution}")

if __name__ == "__main__":
    main()
