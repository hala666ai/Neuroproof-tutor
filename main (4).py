import re
from dataclasses import dataclass
from typing import List, Tuple


@dataclass
class Rule:
    name: str
    description: str
    pattern: str
    replacement: str

    def apply_to_expr(self, expr: str) -> List[str]:
        results = []
        m = re.search(self.pattern, expr)
        if m:
            new_expr = re.sub(self.pattern, self.replacement, expr, count=1)
            results.append(new_expr)
        return results


@dataclass
class ProofStep:
    index: int
    expression: str
    valid: bool = False
    rule_used: str = ""


class ProofEngine:
    def __init__(self):
        self.rules: List[Rule] = []
        self._load_default_rules()

    def _load_default_rules(self):
        self.rules.append(Rule(
            "DiffSquares",
            "a^2 - b^2 = (a-b)(a+b)",
            r"(\w+)\^2\s*-\s*(\w+)\^2",
            r"(\1-\2)(\1+\2)"
        ))

        self.rules.append(Rule(
            "ExpandDiffSquares",
            "(a-b)(a+b) = a^2 - b^2",
            r"\((\w+)-(\w+)\)\((\w+)\+(\w+)\)",
            r"\1^2-\2^2"
        ))

        self.rules.append(Rule(
            "TrigPythagorean",
            "sin^2(x) + cos^2(x) = 1",
            r"sin\^2\((\w+)\)\s*\+\s*cos\^2\((\w+)\)",
            r"1"
        ))

        self.rules.append(Rule(
            "AddCommute",
            "a + b = b + a",
            r"(\w+)\s*\+\s*(\w+)",
            r"\2+\1"
        ))

        self.rules.append(Rule(
            "SquareBinomial",
            "a^2 + 2ab + b^2 = (a+b)^2",
            r"(\w+)\^2\s*\+\s*2(\w+)(\w+)\s*\+\s*(\w+)\^2",
            r"(\1+\3)^2"
        ))

    def normalize(self, s: str) -> str:
        return re.sub(r"\s+", "", s)

    def generate_next_expressions(self, expr: str) -> List[Tuple[str, str]]:
        out = []
        for r in self.rules:
            for candidate in r.apply_to_expr(expr):
                if self.normalize(candidate) != self.normalize(expr):
                    out.append((candidate, r.name))
        return out

    def check_step(self, index: int, prev: str, current: str) -> ProofStep:
        step = ProofStep(index=index, expression=current)
        for r in self.rules:
            for candidate in r.apply_to_expr(prev):
                if self.normalize(candidate) == self.normalize(current):
                    step.valid = True
                    step.rule_used = r.name
                    return step
        return step

    def score_proof(self, steps: List[str], target: str) -> Tuple[float, List[ProofStep], bool]:
        if not steps:
            return 0.0, [], False

        checked: List[ProofStep] = []
        first = ProofStep(1, steps[0], True, "GIVEN")
        checked.append(first)
        correct_steps = 1

        for i in range(1, len(steps)):
            ps = self.check_step(i + 1, steps[i - 1], steps[i])
            checked.append(ps)
            if ps.valid:
                correct_steps += 1

        base_score = correct_steps / len(steps) * 100.0
        reached_target = self.normalize(steps[-1]) == self.normalize(target)
        if reached_target:
            base_score += 10
        if len(steps) > 8:
            base_score -= 5

        base_score = max(0.0, min(100.0, base_score))
        return base_score, checked, reached_target


def main():
    engine = ProofEngine()

    print("=== NeuroProof Tutor (Python Edition) ===")
    target = input("Enter target statement (e.g. a^2 - b^2 = (a-b)(a+b)): ").strip()

    print("\nEnter your proof steps one per line.")
    print("First line = starting expression.")
    print("Type 'END' to finish.\n")

    steps: List[str] = []
    while True:
        line = input(f"Step {len(steps)+1}: ").strip()
        if line.upper() == "END":
            break
        if line:
            steps.append(line)

    if not steps:
        print("No steps provided. Exiting.")
        return

    print("\n--- Analyzing proof ---\n")
    score, checked, reached = engine.score_proof(steps, target)

    for ps in checked:
        print(f"Step {ps.index}: {ps.expression}")
        print(f"   Valid: {ps.valid}   Rule: {ps.rule_used}")

    print(f"\nReached target? {reached}")
    print(f"Proof score: {score:.1f} / 100")

    last = steps[-1]
    print("\nPossible next steps from your last expression:")
    suggestions = engine.generate_next_expressions(last)
    if not suggestions:
        print("  No obvious next steps with current rule set.")
    else:
        for expr, rule in suggestions:
            print(f"  -> {expr}   [via {rule}]")

    print("\nDone.")


if __name__ == "__main__":
    main()