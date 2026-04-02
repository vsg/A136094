from itertools import permutations

def make_example(n, solution):
    print(f"| Permutation | Embedded in `{solution}` |")
    print(f"|---|---|")

    digits = map(str, range(1, n+1))

    for permutation in sorted(permutations(digits)):
        embedding = []
        index = 0
        for digit in solution:
            if index < len(permutation) and digit == permutation[index]:
                digit = f"**{digit}**"
                index += 1
            embedding.append(digit)
        print(f"| {" ".join(permutation)} | {" ".join(embedding)} |")

if __name__ == "__main__":
    make_example(3, "1213121")
