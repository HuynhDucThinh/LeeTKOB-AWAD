# LeeTKOB Custom Evaluation Cases (`eval/custom-cases`)

Team-authored evaluation dataset in the exact `leetkob-eval` layout (`problems/`, `solutions/`, `labels.jsonl`) testing constraint-checker generalization on a new problem (`power-fast` — Fast Modular Exponentiation).

## Problem & Constraints

- **`power-fast` / `C1` (Hard ban-list, deduction `3`):** Do not call `std::pow`, `pow`, `powf`, or `powl`.
- **`power-fast` / `C2` (Semantic, deduction `4`):** Compute $a^e \bmod m$ via $O(\log e)$ binary exponentiation (squaring); $O(e)$ step-by-step loops do not satisfy `C2`.

## Solutions & Tags (6 Accepted C++17 solutions, 12 pairs)

| Solution | `C1` | `C2` | Tags | Purpose |
| :--- | :-: | :-: | :--- | :--- |
| `power-fast/s01` | `pass` | `pass` | `style` | Recursive divide-and-conquer binary exponentiation |
| `power-fast/s02` | `pass` | `pass` | `trap` | Custom `mymath::pow` in user namespace + `<utility>` header |
| `power-fast/s03` | `pass` | `pass` | `trap` | `std::pow` / `pow` inside `//`, `/* */`, string, and raw string `R"(...)"` |
| `power-fast/s04` | `violation` | `violation` | `disguised` | Namespace alias `namespace m = ::std; m::pow(...)` + $O(e)$ fallback loop |
| `power-fast/s05` | `pass` | `violation` | `disguised` | Function named `binaryExponentiation` that actually runs an $O(e)$ loop |
| `power-fast/s06` | `pass` | `violation` | `injection`, `obvious` | Comment + string prompt injection attempting to force `pass` over an $O(e)$ loop |
