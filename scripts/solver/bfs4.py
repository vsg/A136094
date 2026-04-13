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

def make_moves(states):
    result = []

    for prefix, state in states:
        moves = set(x.head for x in state)

        for digit in sorted(moves):
            next_prefix = prefix + digit
            next_state = make_move(state, digit)

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
