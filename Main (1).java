import java.util.*;
import java.util.regex.*;

/**
 * NeuroProof Tutor (Java Edition)
 * --------------------------------
 * - Rule-based engine for checking algebraic proof steps.
 * - Uses pattern-based rewrite rules with regex.
 * - Verifies each step, scores the proof, and suggests next steps.
 */
public class NeuroProofTutor {

    // ===== Rule model =====
    static class Rule {
        String name;
        String description;
        Pattern pattern;
        String replacement;

        Rule(String name, String description, String regexPattern, String replacement) {
            this.name = name;
            this.description = description;
            this.pattern = Pattern.compile(regexPattern);
            this.replacement = replacement;
        }

        // Apply rule to a single expression (no equality)
        List<String> applyToExpr(String expr) {
            List<String> results = new ArrayList<>();
            Matcher m = pattern.matcher(expr);
            while (m.find()) {
                StringBuffer sb = new StringBuffer();
                m.appendReplacement(sb, replacement);
                m.appendTail(sb);
                results.add(sb.toString());
                break; // one application per call
            }
            return results;
        }

        @Override
        public String toString() {
            return name + " : " + description;
        }
    }

    // ===== Proof step model =====
    static class ProofStep {
        int index;
        String expression;
        boolean valid;
        String ruleUsed;

        ProofStep(int index, String expression) {
            this.index = index;
            this.expression = expression;
        }
    }

    // ===== Engine =====
    static class ProofEngine {
        List<Rule> rules = new ArrayList<>();

        ProofEngine() {
            loadDefaultRules();
        }

        void loadDefaultRules() {
            // Difference of squares: a^2 - b^2 = (a-b)(a+b)
            rules.add(new Rule(
                    "DiffSquares",
                    "a^2 - b^2 = (a-b)(a+b)",
                    "(\\w+)\\^2\\s*-\\s*(\\w+)\\^2",
                    "($1-$2)($1+$2)"
            ));

            // Expand (a-b)(a+b) = a^2 - b^2
            rules.add(new Rule(
                    "ExpandDiffSquares",
                    "(a-b)(a+b) = a^2 - b^2",
                    "\\((\\w+)-(\\w+)\\)\\((\\w+)\\+(\\w+)\\)",
                    "$1^2-$2^2"
            ));

            // Trig identity: sin^2(x) + cos^2(x) = 1
            rules.add(new Rule(
                    "TrigPythagorean",
                    "sin^2(x) + cos^2(x) = 1",
                    "sin\\^2\\((\\w+)\\)\\s*\\+\\s*cos\\^2\\((\\w+)\\)",
                    "1"
            ));

            // Commutativity of addition: a + b = b + a
            rules.add(new Rule(
                    "AddCommute",
                    "a + b = b + a",
                    "(\\w+)\\s*\\+\\s*(\\w+)",
                    "$2+$1"
            ));

            // Simple factorization: a^2 + 2ab + b^2 = (a+b)^2
            rules.add(new Rule(
                    "SquareBinomial",
                    "a^2 + 2ab + b^2 = (a+b)^2",
                    "(\\w+)\\^2\\s*\\+\\s*2(\\w+)(\\w+)\\s*\\+\\s*(\\w+)\\^2",
                    "($1+$3)^2"
            ));
        }

        // Generate all expressions reachable from prev by one rule application
        List<String> generateNextExpressions(String expr) {
            List<String> next = new ArrayList<>();
            for (Rule r : rules) {
                next.addAll(r.applyToExpr(expr));
            }
            return next;
        }

        // Check if step is valid: can we reach 'current' from 'prev' by one rule?
        ProofStep checkStep(int index, String prev, String current) {
            ProofStep step = new ProofStep(index, current);
            for (Rule r : rules) {
                List<String> candidates = r.applyToExpr(prev);
                for (String c : candidates) {
                    if (normalize(c).equals(normalize(current))) {
                        step.valid = true;
                        step.ruleUsed = r.name;
                        return step;
                    }
                }
            }
            step.valid = false;
            step.ruleUsed = null;
            return step;
        }

        // Suggest possible next steps from current expression
        List<String> suggestNext(String current) {
            Set<String> suggestions = new LinkedHashSet<>();
            for (Rule r : rules) {
                List<String> cands = r.applyToExpr(current);
                for (String c : cands) {
                    if (!normalize(c).equals(normalize(current))) {
                        suggestions.add(c + "   [via " + r.name + "]");
                    }
                }
            }
            return new ArrayList<>(suggestions);
        }

        String normalize(String s) {
            return s.replaceAll("\\s+", "");
        }
    }

    // ===== CLI =====
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        ProofEngine engine = new ProofEngine();

        System.out.println("=== NeuroProof Tutor (Java Edition) ===");
        System.out.println("Enter a target statement (for reference only), e.g.: a^2 - b^2 = (a-b)(a+b)");
        System.out.print("Target: ");
        String target = in.nextLine().trim();

        System.out.println("\nNow enter your proof steps one per line.");
        System.out.println("First line should be the starting expression.");
        System.out.println("Type 'END' to finish.\n");

        List<String> stepsInput = new ArrayList<>();
        while (true) {
            System.out.print("Step " + (stepsInput.size() + 1) + ": ");
            String line = in.nextLine().trim();
            if (line.equalsIgnoreCase("END")) break;
            if (!line.isEmpty()) stepsInput.add(line);
        }

        if (stepsInput.isEmpty()) {
            System.out.println("No steps provided. Exiting.");
            return;
        }

        System.out.println("\n--- Analyzing proof ---\n");

        List<ProofStep> checked = new ArrayList<>();
        checked.add(new ProofStep(1, stepsInput.get(0))); // first step = given
        checked.get(0).valid = true;
        checked.get(0).ruleUsed = "GIVEN";

        int correctSteps = 1;
        for (int i = 1; i < stepsInput.size(); i++) {
            ProofStep ps = engine.checkStep(i + 1, stepsInput.get(i - 1), stepsInput.get(i));
            checked.add(ps);
            if (ps.valid) correctSteps++;
        }

        // Scoring
        int totalSteps = stepsInput.size();
        double baseScore = (double) correctSteps / totalSteps * 100.0;
        boolean reachedTarget = engine.normalize(stepsInput.get(stepsInput.size() - 1))
                .equals(engine.normalize(target));
        if (reachedTarget) baseScore += 10;
        if (totalSteps > 8) baseScore -= 5; // penalty for very long proofs
        if (baseScore < 0) baseScore = 0;
        if (baseScore > 100) baseScore = 100;

        // Report
        for (ProofStep ps : checked) {
            System.out.println("Step " + ps.index + ": " + ps.expression);
            System.out.println("   Valid: " + ps.valid +
                    (ps.ruleUsed != null ? ("   [Rule: " + ps.ruleUsed + "]") : ""));
        }

        System.out.println("\nReached target? " + reachedTarget);
        System.out.printf("Proof score: %.1f / 100\n", baseScore);

        // Suggestions from last step
        String last = stepsInput.get(stepsInput.size() - 1);
        System.out.println("\nPossible next steps from your last expression:");
        List<String> suggestions = engine.suggestNext(last);
        if (suggestions.isEmpty()) {
            System.out.println("  No obvious next steps found with current rule set.");
        } else {
            for (String s : suggestions) {
                System.out.println("  -> " + s);
            }
        }

        System.out.println("\nDone.");
    }
}