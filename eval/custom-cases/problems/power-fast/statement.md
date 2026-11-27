# Fast Modular Exponentiation

Given $Q$ queries, each consisting of three non-negative integers $a$, $e$, and $m$ ($m \ge 1$), compute

$$a^e \bmod m$$

using $0^0 = 1$ (so $0^0 \bmod 1 = 0$ and $0^0 \bmod m = 1$ for $m > 1$).

## Input

- The first line contains an integer $Q$ ($1 \le Q \le 1000$).
- Each of the next $Q$ lines contains three space-separated integers $a$, $e$, and $m$ ($0 \le a \le 10^9$, $0 \le e \le 10^4$, $1 \le m \le 10^9 + 7$).

## Output

Print $Q$ lines. Line $i$ must contain the single integer $a_i^{e_i} \bmod m_i$.

## Example

Input:
```
4
2 10 1000
3 5 100
7 3 13
12 0 5
```

Output:
```
24
43
5
1
```
