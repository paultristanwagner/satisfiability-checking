# Satisfiability checking
This project is supposed to be a practice environment to implement algorithms related to satisfiability checking.
The techniques used here are introduced in the RWTH University lecture '[Satisfiability checking](https://ths.rwth-aachen.de/teaching/winter-term-2021-2022/lecture-satisfiability-checking/)' by Prof. Dr. Erika Ábrahám.

# How to build and run the project
Clone the git repository:  
`git clone https://github.com/paultristanwagner/satisfiability-checking.git`  
Navigate into the created directory:  
`cd satisfiability-checking`  
Let Maven build the project:  
`mvn package`  
Run the project:  
`java -jar target/satchecking-1.0-SNAPSHOT.jar`  
Now you should see the command prompt indicated by a `>` symbol.

# Propositional logic

## Enumeration

A simple SAT solver using enumeration is implemented.

## DPLL

The [DPLL Algorithm](https://en.wikipedia.org/wiki/DPLL_algorithm) is implemented.

## DPLL+CDCL

A [DPLL+CDCL](https://en.wikipedia.org/wiki/Conflict-driven_clause_learning) solver that applies conflict resolution is
used by the command line interface.

### Example

Input can be given in conjunctive normal form in the following way.

<img src="images/cnf-input-sat.png" alt="How to input in CNF" width="350" /> <br>
<img src="images/cnf-input-unsat.png" alt="How to input in CNF" width="350" /> <br>

# Theory solvers

## Linear programming

The program can check sets of weak linear constraints for satisfiability employing
the [Simplex algorithm](https://en.wikipedia.org/wiki/Simplex_algorithm).  
If the set of constraints is satisfiable, the program will print a satisfying assignment.
Otherwise, an explanation for unsatisfiability is given in the form of an infeasible subset.
Currently, the program supports decimal coefficients which will be handled via floating point arithmetic.

### Examples

<img src="images/simplex-sat.png" alt="Satisfiable Simplex example" width="350" /> <br>
<img src="images/simplex-unsat.png" alt="Unsatisfiable Simplex example" width="350" /> <br>

