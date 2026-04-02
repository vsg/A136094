from collections import defaultdict

GROUPED = False

def all_permutations_state(digits):
    """Return representation of all permutations of the given digits."""
    return [(d, set(digits) - {d}) for d in digits]

def contains(item1, item2):
    """Check whether the set of permutations represented by item1 contains all permutations represented by item2."""
    (head1, tail1), (head2, tail2) = item1, item2
    return (head2 == head1 or head2 in tail1) and tail1.issuperset(tail2)

def dedup(state):
    """Remove duplicate permutations."""
    result = []
    for item in state:
        if all([not contains(seen, item) for seen in result]):
            result = [seen for seen in result if not contains(item, seen)] + [item]
    return result

def make_move(state, move):
    next = []
    for (head, tail) in state:
        if head == move:
            next.extend(sorted(all_permutations_state(tail)))
        else:
            next.append((head, tail))
    return list(dedup(next))

def parse_problem(problem):
    return [(item[0], set(item[1:])) for item in problem.split()]

def state_str(state):
    if GROUPED:
        return ' '.join(f"{''.join(sorted(heads))}/{''.join(sorted(digits))}" for heads, digits in group_state(state))
    else:
        return ' '.join(f"{head}({''.join(sorted(tail))})" for head, tail in state)

def group_state(state):
    grouped = defaultdict(set)
    for head, tail in state:
        digits = tail | {head}
        key = tuple(sorted(digits))
        grouped[key].add(head)
    return [(set(heads), set(digits)) for digits, heads in grouped.items()]

def print_state(prefix, prefix_max_length, state):
    print(f"{''.join(prefix):>{prefix_max_length}} : {state_str(state)}")

def verify_solution(problem, solution):
    """Verify that the provided solution solves the problem. Does not verify that the provided solution is minimal."""
    state = problem
    
    prefix = []
    print_state(prefix, len(solution), state)

    for move in solution:
        prefix.append(move)
        state = make_move(state, move)
        print_state(prefix, len(solution), state)
    
    return len(state) == 0

def main():
    for problem, solution in [
        (all_permutations_state("1"), "1"),
        (all_permutations_state("12"), "121"),
        (all_permutations_state("123"), "1213121"),
        (all_permutations_state("1234"), "123412314213"),
        (all_permutations_state("12345"), "1234512341523142351"),
        (all_permutations_state("123456"), "1234516234152361425312643512"),
        (all_permutations_state("1234567"), "123451672341526371425361274351263471253"),
        (all_permutations_state("12345678"), "1234156782315426738152643718265341278635124376812453"),
        (all_permutations_state("123456789"), "123456781923451678234915627348152963471825364912783546123976845123"),
        
        # Omit punctuation for brevity: 1(2345) + 2(1345) + 3(1245) + 4(1235) + 6(1237) + 7(1236) => 12345 21345 31245 41235 61237 71236
        (parse_problem("12345 21345 31245 41235 61237 71236"), "61247315246132571456231"), 
    ]:
        print(verify_solution(problem, solution))
        print()

if __name__ == "__main__":
    main()
