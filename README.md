# Satisfiability checking
Command line tool for SAT solving, SMT solving in various theories, and other algorithms related to satisfiability checking.

The techniques used here are introduced in the RWTH University
lecture '[Satisfiability checking](https://ths.rwth-aachen.de/teaching/winter-term-2021-2022/lecture-satisfiability-checking/)'
by Prof. Dr. Erika Ábrahám.

# How to run the project
Under [Releases](https://github.com/paultristanwagner/satisfiability-checking/releases) you can find the latest version of the project.
Download the jar file and run it with the following command: <br>
```
java -jar satchecking-v1.0.1.jar
```
Now you should see the command prompt indicated by a `>` symbol.

# SMT solver
An SMT solver is implemented for non-linear real arithmetic (QF_NRA), linear real arithmetic (QF_LRA), linear integer arithmetic (QF_LIA), equality logic (QF_EQ), equality logic with uninterpreted functions (QF_EQUF) and bit vector arithmetic (QF_BV).

## Examples
### QF_NRA: Non-linear real arithmetic with optimization
```c++
> smt QF_NRA (y<=x^3) & (y<=-x^2+1) & (max(x+y))
SAT:
x=(x^3+x^2-1, 3/4, 7/8) ≈ 0.7548776662466928; y=(-1x^3+2x^2-3x+1, -4, 4) ≈ 0.4301597090019467;
Time: 297ms
```
```c++
> smt QF_NRA (x^2+y^2<1) & (min(y))
UNSAT
Time: 12ms
```
```c++
> smt QF_NRA (x^2 + y^2 = 1) & (x^2 + y^3 = 1/2)
SAT:
x=(x^6-2x^4+2x^2-3/4, 3/4, 15/16) ≈ 0.8249554777467228; y=(x^9+1/2x^6+3/4x^3+1/8, -7/4, 7/4) ≈ -0.5651977173836394;
Time: 456ms
```
```c++
> smt QF_NRA (x*y > 0) & (y*z > 0) & (x*z > 0) & (x + y + z = 0)
UNSAT
Time: 7ms
```
### QF_LRA: Linear real arithmetic with optimization
```c++
> smt QF_LRA (x<=-3 | x>=3) & (y=5) & (x+y>=12) & (min(x))
SAT:
x=7; y=5;
Time: 0ms
```
```c++
> smt QF_LRA (x<=0 | x>=5) & (x+y=5/2) & (y=1)
UNSAT
Time: 1ms
```

### QF_LIA: Linear integer arithmetic with optimization
```c++
> smt QF_LIA (y+0.8x<=4) & (y-0.25x>=0) & (max(x))
SAT:
x=3; y=1;
Time: 1ms
```
```c++
> smt QF_LIA (y-x<=0) & (y+x<=1) & (y>=0.1)
UNSAT
Time: 1ms
```

### QF_EQ: Equality logic
```c++
> smt QF_EQ (a=b) & (c=d) & (a!=d)
SAT:
a=0.0; b=0.0; c=1.0; d=1.0;
Time: 0ms
```
```c++
> smt QF_EQ (a=b) & (b=c) & (c=d) & (a!=d)
UNSAT
Time: 0ms
```

### QF_EQUF: Equality logic with uninterpreted functions
```c++
> smt QF_EQUF (x1 = x2) & (x2 = x3) & (x4 = x5) & (f(x1) != f(x5))
SAT:
f(x1)=0.0; f(x5)=3.0; x1=2.0; x2=2.0; x3=2.0; x4=1.0; x5=1.0;
Time: 0ms
```
```c++
> smt QF_EQUF (f(f(y)) != x) & (x = f(y)) & (y = u) & (x = y)
UNSAT
Time: 0ms
```

### QF_BV: Bit vector arithmetic
```c++
> smt QF_BV (x >= 0) & (y > 0) & (y[6]) & (x >> y = 0) & (x + y < x * y)
SAT:
x=0b01010101 (85); y=0b01000000 (64);
Time: 3ms
```
```c+++
> smt QF_BV (a * b = c) & (b * a = c) & (x < y) & (y < x)
UNSAT
Time: 11ms
```

# Propositional logic
A SAT solver is implemented that can
employ [DPLL+CDCL](https://en.wikipedia.org/wiki/Conflict-driven_clause_learning), [DPLL](https://en.wikipedia.org/wiki/DPLL_algorithm)
as well as simple enumeration to solve propositional logic problems.

Input can be given in conjunctive normal form in the following way.

```c++
> sat (a) & (~a | b) & (~b | ~c)
SAT:
a=1, b=1, c=0;
1 model/s found in 0 ms
```
```c++
> sat (a) & (~a | b) & (~b)
UNSAT
```

# Tseitin's transformation
The Tseitin transformation is implemented for propositional logic.
It can be used to transform a formula into an equi-satisfiable formula in conjunctive normal form.
The logical operators ``'~', '&', '|', '->', '<->'`` as well as parentheses are supported.

```c++
> tseitin ~(a & (a -> b) -> b)
Tseitin's transformation:
(~h3 | ~a | b) & (a | h3) & (~b | h3) & (~h2 | a) & (~h2 | h3) & (~a | ~h3 | h2) & (~h1 | ~h2 | b) & (h2 | h1) & (~b | h1) & (~h0 | ~h1) & (h1 | h0) & (h0)
```

# Theory solvers
## Linear real arithmetic (QF_LRA)
The program can check sets of weak linear constraints for satisfiability employing
the [Simplex algorithm](https://en.wikipedia.org/wiki/Simplex_algorithm).  
If the set of constraints is satisfiable, the program will print a satisfying assignment.
Otherwise, an explanation for unsatisfiability is given in the form of an infeasible subset.
Decimals, as well as fractions are supported which will be handled by exact rational arithmetic.
This setting can be changed to use floating point arithmetic in the ``config.properties`` file.

### Examples
```c++
> simplex a+3b+5c=30 a>=5 a<=10 b>=2 c>=1
SAT!
Solution: a=10; b=5; c=1; Time: 0ms
```
```c++
> simplex x+y=3 y=1 x<=1
UNSAT!
Explanation: x<=1; y=1; x+y=3; Time: 0ms
```

An optional objective function can be given to maximize or minimize the value of a linear expression.

```c++
> simplex min(-2x-3y-4z) 3x+2y+z<=10 2x+5y+3z<=15 x>=0 y>=0 z>=0
SAT! (optimal)
Solution: x=0; y=0; z=5; Optimum: -20
Time: 0ms
```
```c++
> simplex max(x) x>=-1 x>=-1/2
UNSAT! (feasible, but unbounded)
Solution: x=-1/2;
Time: 0ms
```

## Optional: Build the project from source
Clone the git repository:  
`git clone https://github.com/paultristanwagner/satisfiability-checking.git`  
Navigate into the created directory:  
`cd satisfiability-checking`  
Let Maven build the project:  
`mvn package`  
Run the project:  
`java -jar target/satchecking-1.0-SNAPSHOT.jar`  
Now you should see the command prompt indicated by a `>` symbol.