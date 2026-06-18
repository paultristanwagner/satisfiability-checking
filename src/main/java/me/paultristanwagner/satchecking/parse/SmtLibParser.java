package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.smt.TheoryClause;
import me.paultristanwagner.satchecking.theory.Constraint;
import me.paultristanwagner.satchecking.theory.EqualityConstraint;
import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint;
import me.paultristanwagner.satchecking.theory.EqualityFunctionConstraint.Function;
import me.paultristanwagner.satchecking.theory.LinearConstraint;
import me.paultristanwagner.satchecking.theory.LinearTerm;
import me.paultristanwagner.satchecking.theory.arithmetic.Number;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for a subset of SMT-LIB 2.6 scripts.
 *
 * <p>This is an additive front-end: it parses an SMT-LIB script into the project's existing
 * internal data structures (a list of {@link TheoryClause} for the asserted boolean skeleton plus
 * the declared logic and the optional {@code :status}). It does not run the solver itself; see
 * {@code command/impl/SmtLibCommand}.
 *
 * <p>Supported logics: QF_LRA, QF_LIA, QF_EQ, QF_UF, QF_EQUF. Supported boolean structure is a CNF
 * fragment only (conjunctions of clauses, where a clause is a disjunction of positive atoms).
 *
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 */
public class SmtLibParser {

  /** Result of parsing an entire script. */
  public static class SmtLibScript {

    private final String logic;
    private final String status; // "sat" | "unsat" | "unknown" | null
    private final List<TheoryClause<Constraint>> clauses;
    private final List<String> warnings;

    public SmtLibScript(
        String logic,
        String status,
        List<TheoryClause<Constraint>> clauses,
        List<String> warnings) {
      this.logic = logic;
      this.status = status;
      this.clauses = clauses;
      this.warnings = warnings;
    }

    public String getLogic() {
      return logic;
    }

    public String getStatus() {
      return status;
    }

    public List<TheoryClause<Constraint>> getClauses() {
      return clauses;
    }

    public List<String> getWarnings() {
      return warnings;
    }
  }

  /** A single token together with the index in the source it started at. */
  private record Tok(TokenType type, String value, int index) {}

  /** An s-expression: either an atom (a token) or a list of s-expressions. */
  private static final class SExpr {
    final Tok atom; // null for lists
    final List<SExpr> list; // null for atoms
    final int index;

    SExpr(Tok atom) {
      this.atom = atom;
      this.list = null;
      this.index = atom.index();
    }

    SExpr(List<SExpr> list, int index) {
      this.atom = null;
      this.list = list;
      this.index = index;
    }

    boolean isAtom() {
      return atom != null;
    }

    boolean isList() {
      return list != null;
    }

    String head() {
      // The symbol/keyword at the head of a list, or the atom value itself.
      if (isAtom()) {
        return atom.value();
      }
      if (!list.isEmpty() && list.get(0).isAtom()) {
        return list.get(0).atom.value();
      }
      return null;
    }
  }

  private final String input;
  private final List<Tok> tokens;
  private int pos;

  // Logic kind. Determined by set-logic.
  private enum Kind {
    ARITHMETIC, // QF_LRA, QF_LIA
    EQUALITY, // QF_EQ
    UF // QF_UF, QF_EQUF
  }

  private String logicName;
  private Kind kind = Kind.ARITHMETIC; // default
  private String status;

  // Declared 0-arity symbols (constants/variables) and uninterpreted functions (arity > 0).
  private final List<String> declaredFunctions = new ArrayList<>();
  private final List<TheoryClause<Constraint>> clauses = new ArrayList<>();
  private final List<String> warnings = new ArrayList<>();

  public SmtLibParser(String input) {
    this.input = input;
    this.tokens = tokenize(input);
    this.pos = 0;
  }

  private List<Tok> tokenize(String input) {
    SmtLibLexer lexer = new SmtLibLexer(input);
    List<Tok> result = new ArrayList<>();
    while (lexer.hasNextToken()) {
      Token token = lexer.getLookahead();
      int index = lexer.getCursor();
      if (token.getType() != SmtLibLexer.COMMENT) {
        result.add(new Tok(token.getType(), token.getValue(), index));
      }
      lexer.skip(token.getValue().length());
    }
    return result;
  }

  // ---- s-expression reading -------------------------------------------------

  private SExpr readSExpr() {
    if (pos >= tokens.size()) {
      throw err("Unexpected end of input", input.length());
    }
    Tok t = tokens.get(pos);
    if (t.type() == SmtLibLexer.LPAREN) {
      int start = t.index();
      pos++;
      List<SExpr> list = new ArrayList<>();
      while (pos < tokens.size() && tokens.get(pos).type() != SmtLibLexer.RPAREN) {
        list.add(readSExpr());
      }
      if (pos >= tokens.size()) {
        throw err("Missing closing ')'", input.length());
      }
      pos++; // consume RPAREN
      return new SExpr(list, start);
    } else if (t.type() == SmtLibLexer.RPAREN) {
      throw err("Unexpected ')'", t.index());
    } else {
      pos++;
      return new SExpr(t);
    }
  }

  // ---- top level ------------------------------------------------------------

  /** Parses the whole script and returns the accumulated structure. */
  public SmtLibScript parse() {
    while (pos < tokens.size()) {
      SExpr command = readSExpr();
      handleCommand(command);
    }
    return new SmtLibScript(logicName, status, clauses, warnings);
  }

  private void handleCommand(SExpr command) {
    if (!command.isList() || command.list.isEmpty() || !command.list.get(0).isAtom()) {
      throw err("Expected a command s-expression", command.index);
    }
    String name = command.list.get(0).atom.value();
    List<SExpr> args = command.list.subList(1, command.list.size());

    switch (name) {
      case "set-logic" -> handleSetLogic(args, command.index);
      case "set-info" -> handleSetInfo(args);
      case "declare-const" -> handleDeclareConst(args, command.index);
      case "declare-fun" -> handleDeclareFun(args, command.index);
      case "declare-sort", "define-sort" -> {
        /* tracked/ignored */
      }
      case "assert" -> handleAssert(args, command.index);
      case "check-sat", "check-sat-assuming" -> {
        /* the command driver triggers solving; nothing to accumulate */
      }
      case "exit", "set-option", "get-model", "get-info", "push", "pop", "reset", "echo" -> {
        // No-op for our subset. push/pop are not supported as incremental solving but
        // single-frame scripts are common; we warn for push/pop specifically below.
        if (name.equals("push") || name.equals("pop")) {
          warnings.add("ignoring unsupported incremental command '" + name + "'");
        }
      }
      default -> warnings.add("ignoring unknown command '" + name + "'");
    }
  }

  private void handleSetLogic(List<SExpr> args, int index) {
    if (args.isEmpty() || !args.get(0).isAtom()) {
      throw err("set-logic expects a logic name", index);
    }
    logicName = args.get(0).atom.value();
    switch (logicName) {
      case "QF_LRA", "QF_LIA" -> kind = Kind.ARITHMETIC;
      case "QF_EQ" -> kind = Kind.EQUALITY;
      case "QF_UF", "QF_EQUF" -> kind = Kind.UF;
      default ->
          throw err(
              "unsupported logic '"
                  + logicName
                  + "' (supported: QF_LRA, QF_LIA, QF_EQ, QF_UF, QF_EQUF)",
              index);
    }
  }

  private void handleSetInfo(List<SExpr> args) {
    if (args.size() >= 2 && args.get(0).isAtom() && ":status".equals(args.get(0).atom.value())) {
      SExpr v = args.get(1);
      if (v.isAtom()) {
        String s = v.atom.value();
        if (s.equals("sat") || s.equals("unsat") || s.equals("unknown")) {
          status = s;
        }
      }
    }
  }

  private void handleDeclareConst(List<SExpr> args, int index) {
    // (declare-const name Sort)
    if (args.size() < 2 || !args.get(0).isAtom()) {
      throw err("declare-const expects a name and a sort", index);
    }
    declaredFunctions.add(args.get(0).atom.value());
  }

  private void handleDeclareFun(List<SExpr> args, int index) {
    // (declare-fun name (argSorts...) retSort)
    if (args.size() < 3 || !args.get(0).isAtom()) {
      throw err("declare-fun expects a name, argument sorts and a return sort", index);
    }
    declaredFunctions.add(args.get(0).atom.value());
    // 0-arity -> variable/constant; >0 arity -> uninterpreted function. We don't need to store the
    // arity: term parsing distinguishes by whether the symbol appears applied.
  }

  // ---- assert / boolean structure ------------------------------------------

  private void handleAssert(List<SExpr> args, int index) {
    if (args.size() != 1) {
      throw err("assert expects exactly one formula", index);
    }
    SExpr formula = args.get(0);
    addFormula(formula);
  }

  /** Adds the clauses produced by one asserted formula (CNF fragment). */
  private void addFormula(SExpr formula) {
    rejectNonCnf(formula);

    String head = formula.isList() ? formula.head() : null;
    if ("and".equals(head)) {
      // conjunction of sub-formulas, each must itself be a clause (atom or or-of-atoms)
      for (int i = 1; i < formula.list.size(); i++) {
        addClause(formula.list.get(i));
      }
    } else {
      addClause(formula);
    }
  }

  /**
   * Adds a clause from a formula: either an atom (unit clause) or an (or atom...) disjunction.
   *
   * <p>An atom may expand to several constraints (e.g. {@code (distinct a b c)} expands pairwise).
   * As a top-level conjunct, those constraints are conjoined (each becomes its own unit clause).
   * Inside an {@code or}, an atom must yield a single constraint, otherwise the pairwise expansion
   * would be mis-interpreted as a disjunction; we reject that case.
   */
  private void addClause(SExpr formula) {
    rejectNonCnf(formula);
    String head = formula.isList() ? formula.head() : null;
    if ("or".equals(head)) {
      if (formula.list.size() < 2) {
        throw err("'or' requires at least one disjunct", formula.index);
      }
      List<Constraint> constraints = new ArrayList<>();
      for (int i = 1; i < formula.list.size(); i++) {
        SExpr disjunct = formula.list.get(i);
        List<Constraint> atomConstraints = parseAtom(disjunct);
        if (atomConstraints.size() != 1) {
          throw err(
              "unsupported boolean structure (needs negation handling, issue #9): "
                  + "pairwise 'distinct' with more than two arguments inside an 'or'",
              disjunct.index);
        }
        constraints.addAll(atomConstraints);
      }
      clauses.add(new TheoryClause<>(constraints));
    } else {
      // unit atom; pairwise expansion -> conjunction -> one unit clause each
      for (Constraint constraint : parseAtom(formula)) {
        clauses.add(new TheoryClause<>(List.of(constraint)));
      }
    }
  }

  private void rejectNonCnf(SExpr formula) {
    if (!formula.isList()) {
      return;
    }
    String head = formula.head();
    if (head == null) {
      return;
    }
    switch (head) {
      case "not", "=>", "implies", "ite", "xor", "iff" ->
          throw err(
              "unsupported boolean structure (needs negation handling, issue #9): '" + head + "'",
              formula.index);
      default -> {
        /* ok */
      }
    }
  }

  // ---- atom parsing (returns 1 constraint, except distinct which expands pairwise) ----

  private List<Constraint> parseAtom(SExpr atom) {
    rejectNonCnf(atom);
    if (!atom.isList()) {
      throw err(
          "expected an atom (a predicate application), found symbol '" + atom.atom.value() + "'",
          atom.index);
    }
    String head = atom.head();
    if (head == null) {
      throw err("expected an atom", atom.index);
    }

    // Nested and/or inside a clause is not part of the CNF fragment.
    if (head.equals("and") || head.equals("or")) {
      throw err(
          "unsupported boolean structure (needs negation handling, issue #9): nested '"
              + head
              + "' inside a clause",
          atom.index);
    }

    return switch (kind) {
      case ARITHMETIC -> List.of(parseArithmeticAtom(atom, head));
      case EQUALITY -> parseEqualityAtom(atom, head);
      case UF -> parseUfAtom(atom, head);
    };
  }

  // ---- arithmetic (QF_LRA / QF_LIA) ----------------------------------------

  private Constraint parseArithmeticAtom(SExpr atom, String head) {
    switch (head) {
      case "<", ">" ->
          throw err(
              "unsupported: strict inequality (no strict bound in LinearConstraint): '"
                  + head
                  + "'",
              atom.index);
      case "=", "<=", ">=" -> {
        if (atom.list.size() != 3) {
          throw err(
              "'" + head + "' expects exactly two arguments in this subset", atom.index);
        }
        LinearTerm lhs = parseLinearTerm(atom.list.get(1));
        LinearTerm rhs = parseLinearTerm(atom.list.get(2));
        return switch (head) {
          case "=" -> LinearConstraint.equal(lhs, rhs);
          case "<=" -> LinearConstraint.lessThanOrEqual(lhs, rhs);
          default -> LinearConstraint.greaterThanOrEqual(lhs, rhs);
        };
      }
      default ->
          throw err("unsupported arithmetic predicate '" + head + "'", atom.index);
    }
  }

  private LinearTerm parseLinearTerm(SExpr e) {
    if (e.isAtom()) {
      Tok t = e.atom;
      if (t.type() == SmtLibLexer.NUMERAL || t.type() == SmtLibLexer.DECIMAL) {
        LinearTerm term = new LinearTerm();
        term.setConstant(Number.parse(t.value()));
        return term;
      }
      // a variable
      LinearTerm term = new LinearTerm();
      term.setCoefficient(t.value(), Number.ONE());
      return term;
    }

    String head = e.head();
    if (head == null) {
      throw err("malformed arithmetic term", e.index);
    }
    switch (head) {
      case "+" -> {
        LinearTerm term = new LinearTerm();
        for (int i = 1; i < e.list.size(); i++) {
          term = term.add(parseLinearTerm(e.list.get(i)));
        }
        return term;
      }
      case "-" -> {
        if (e.list.size() == 2) {
          // unary negation
          return parseLinearTerm(e.list.get(1)).negate();
        }
        if (e.list.size() < 2) {
          throw err("'-' requires at least one argument", e.index);
        }
        LinearTerm term = parseLinearTerm(e.list.get(1));
        for (int i = 2; i < e.list.size(); i++) {
          term = term.subtract(parseLinearTerm(e.list.get(i)));
        }
        return term;
      }
      case "*" -> {
        return parseProduct(e);
      }
      case "/" -> {
        // rational constant (/ p q)
        return parseDivision(e);
      }
      default ->
          throw err("unsupported arithmetic operator '" + head + "'", e.index);
    }
  }

  /** Parses a linear product: at least one factor must be a numeric constant. */
  private LinearTerm parseProduct(SExpr e) {
    if (e.list.size() < 3) {
      throw err("'*' requires at least two arguments", e.index);
    }
    // Fold factors. Track the running coefficient (numeric) and at most one non-constant term.
    Number coefficient = Number.ONE();
    LinearTerm nonConstant = null;
    for (int i = 1; i < e.list.size(); i++) {
      SExpr factor = e.list.get(i);
      Number c = tryConstant(factor);
      if (c != null) {
        coefficient = coefficient.multiply(c);
      } else {
        if (nonConstant != null) {
          throw err("unsupported: nonlinear term", e.index);
        }
        nonConstant = parseLinearTerm(factor);
      }
    }
    if (nonConstant == null) {
      LinearTerm term = new LinearTerm();
      term.setConstant(coefficient);
      return term;
    }
    // multiply nonConstant by the scalar coefficient
    LinearTerm result = new LinearTerm();
    final Number coeff = coefficient;
    nonConstant.getCoefficients().forEach((v, k) -> result.addCoefficient(v, k.multiply(coeff)));
    result.setConstant(nonConstant.getConstant().multiply(coeff));
    return result;
  }

  private LinearTerm parseDivision(SExpr e) {
    if (e.list.size() != 3) {
      throw err("'/' expects exactly two arguments (a rational constant)", e.index);
    }
    Number numerator = tryConstant(e.list.get(1));
    Number denominator = tryConstant(e.list.get(2));
    if (numerator == null || denominator == null) {
      throw err("unsupported: nonlinear term (division by a non-constant)", e.index);
    }
    LinearTerm term = new LinearTerm();
    term.setConstant(numerator.divide(denominator));
    return term;
  }

  /**
   * If the expression evaluates to a numeric constant, returns it; otherwise null. Handles plain
   * numerals/decimals, unary minus of a constant, and (/ p q) of constants.
   */
  private Number tryConstant(SExpr e) {
    if (e.isAtom()) {
      Tok t = e.atom;
      if (t.type() == SmtLibLexer.NUMERAL || t.type() == SmtLibLexer.DECIMAL) {
        return Number.parse(t.value());
      }
      return null;
    }
    String head = e.head();
    if (head == null) {
      return null;
    }
    switch (head) {
      case "-" -> {
        if (e.list.size() == 2) {
          Number inner = tryConstant(e.list.get(1));
          return inner == null ? null : inner.negate();
        }
        return null;
      }
      case "/" -> {
        if (e.list.size() == 3) {
          Number num = tryConstant(e.list.get(1));
          Number den = tryConstant(e.list.get(2));
          if (num != null && den != null) {
            return num.divide(den);
          }
        }
        return null;
      }
      case "*" -> {
        Number acc = Number.ONE();
        for (int i = 1; i < e.list.size(); i++) {
          Number c = tryConstant(e.list.get(i));
          if (c == null) {
            return null;
          }
          acc = acc.multiply(c);
        }
        return acc;
      }
      default -> {
        return null;
      }
    }
  }

  // ---- equality (QF_EQ) -----------------------------------------------------

  private List<Constraint> parseEqualityAtom(SExpr atom, String head) {
    switch (head) {
      case "=" -> {
        if (atom.list.size() != 3) {
          throw err("'=' expects exactly two arguments in this subset", atom.index);
        }
        String left = requireVariable(atom.list.get(1));
        String right = requireVariable(atom.list.get(2));
        return List.of(new EqualityConstraint(left, right, true));
      }
      case "distinct" -> {
        List<String> vars = new ArrayList<>();
        for (int i = 1; i < atom.list.size(); i++) {
          vars.add(requireVariable(atom.list.get(i)));
        }
        if (vars.size() < 2) {
          throw err("'distinct' requires at least two arguments", atom.index);
        }
        // pairwise distinct expanded into separate constraints (conjunction). When this atom is a
        // unit clause, multiple constraints in one clause would be a disjunction, which is wrong.
        // We therefore only allow multi-arg distinct as a top-level/unit atom (handled by caller
        // putting each in its own clause). For safety expand pairwise here; the caller for clauses
        // with 'or' would mis-handle >2 args, so reject >2 inside a disjunction.
        List<Constraint> result = new ArrayList<>();
        for (int i = 0; i < vars.size(); i++) {
          for (int j = i + 1; j < vars.size(); j++) {
            result.add(new EqualityConstraint(vars.get(i), vars.get(j), false));
          }
        }
        return result;
      }
      default ->
          throw err("unsupported equality predicate '" + head + "'", atom.index);
    }
  }

  private String requireVariable(SExpr e) {
    if (!e.isAtom()) {
      throw err("expected a variable/constant symbol", e.index);
    }
    return e.atom.value();
  }

  // ---- uninterpreted functions (QF_UF / QF_EQUF) ---------------------------

  private List<Constraint> parseUfAtom(SExpr atom, String head) {
    switch (head) {
      case "=" -> {
        if (atom.list.size() != 3) {
          throw err("'=' expects exactly two arguments in this subset", atom.index);
        }
        Function left = parseFunctionTerm(atom.list.get(1));
        Function right = parseFunctionTerm(atom.list.get(2));
        return List.of(new EqualityFunctionConstraint(left, right, true));
      }
      case "distinct" -> {
        List<Function> terms = new ArrayList<>();
        for (int i = 1; i < atom.list.size(); i++) {
          terms.add(parseFunctionTerm(atom.list.get(i)));
        }
        if (terms.size() < 2) {
          throw err("'distinct' requires at least two arguments", atom.index);
        }
        List<Constraint> result = new ArrayList<>();
        for (int i = 0; i < terms.size(); i++) {
          for (int j = i + 1; j < terms.size(); j++) {
            result.add(new EqualityFunctionConstraint(terms.get(i), terms.get(j), false));
          }
        }
        return result;
      }
      default ->
          throw err("unsupported equality predicate '" + head + "'", atom.index);
    }
  }

  private Function parseFunctionTerm(SExpr e) {
    if (e.isAtom()) {
      return Function.of(e.atom.value());
    }
    String head = e.head();
    if (head == null) {
      throw err("malformed function application term", e.index);
    }
    List<Function> args = new ArrayList<>();
    for (int i = 1; i < e.list.size(); i++) {
      args.add(parseFunctionTerm(e.list.get(i)));
    }
    return Function.of(head, args);
  }

  // ---- error helper ---------------------------------------------------------

  private SyntaxError err(String message, int index) {
    return new SyntaxError(message, input, index);
  }
}
