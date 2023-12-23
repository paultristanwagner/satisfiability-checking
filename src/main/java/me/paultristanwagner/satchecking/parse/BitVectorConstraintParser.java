package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.sat.Assignment;
import me.paultristanwagner.satchecking.sat.CNF;
import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.sat.solver.SATSolver;
import me.paultristanwagner.satchecking.theory.bitvector.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import static me.paultristanwagner.satchecking.parse.TokenType.*;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVector.DEFAULT_BIT_VECTOR_LENGTH;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorAddition.addition;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorConstant.constant;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorEqualConstraint.equal;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorLeftShift.leftShift;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorLessThanConstraint.lessThan;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorNegation.negation;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorProduct.product;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorRemainder.remainder;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorSubtraction.subtraction;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorUnequalConstraint.unequal;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVectorVariable.bitvector;

public class BitVectorConstraintParser implements Parser<BitVectorConstraint> {

  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);

    BitVectorConstraintParser parser = new BitVectorConstraintParser();

    List<BitVectorConstraint> constraints = new ArrayList<>();

    while (true) {
      try {
        System.out.print("> ");
        String input = scanner.nextLine();

        if (input.equalsIgnoreCase("solve")) {
          break;
        }

        BitVectorConstraint constraint = parser.parse(input);

        constraints.add(constraint);

      } catch (SyntaxError syntaxError) {
        syntaxError.printWithContext();
      }
    }

    for (BitVectorConstraint constraint : constraints) {
      System.out.println(constraint);
    }
    System.out.println();

    BitVectorFlattener flattener = new BitVectorFlattener();
    CNF cnf = flattener.flatten(constraints);

    // combine variables from all constraints
    Set<BitVectorVariable> variables =
    constraints.stream()
        .map(BitVectorConstraint::getVariables)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());

    SATSolver solver = new DPLLCDCLSolver();
    solver.load(cnf);

    Assignment assignment;
    if ((assignment = solver.nextModel()) != null) {
      System.out.println("Solution found!");
      for (BitVectorVariable variable : variables) {
        BitVector value = flattener.reconstruct(variable, assignment);
        System.out.println(variable + " = " + value + " (" + value.asInt() + ")");
      }
      System.out.println();
    } else {
      System.out.println("No solution found!");
    }
  }

  /*
   *  Grammar for BitVector constraints:
   *    <S> ::= <TERM> EQUALS <TERM>
   *          | <TERM> NOT_EQUALS <TERM>
   *          | <TERM> LESS_THAN <TERM>
   *
   *    <TERM> ::= BITWISE_NOT <TERM>
   *             | <A> BITWISE_OR <A>
   *
   *    <A> ::= <B> BITWISE_XOR <B>
   *
   *    <B> ::= <C> BITWISE_AND <C>
   *
   *    <C> ::= <D> BITWISE_LEFT_SHIFT <D>
   *          | <D> BITWISE_RIGHT_SHIFT <D>
   *
   *    <D> ::= <E> ADD <E>
   *          | <E> MINUS <E>
   *
   *    <E> ::= <F> TIMES <F>
   *          | <F> REMAINDER <F>
   *
   *    <F> ::= BITWISE_NEGATION <G>
   *
   *    <G> ::= IDENTIFIER
   *          | BINARY_CONSTANT
   *          | INTEGER
   *          | HEX_CONSTANT
   *
   *   todo: add support for parentheses
   */

  @Override
  public ParseResult<BitVectorConstraint> parseWithRemaining(String string) {
    Lexer lexer = new BitVectorConstraintLexer(string);

    lexer.requireNextToken();

    BitVectorConstraint constraint = parseConstraint(lexer);

    return new ParseResult<>(constraint, lexer.getCursor(), lexer.getCursor() == string.length());
  }

  private BitVectorConstraint parseConstraint(Lexer lexer) {
    BitVectorTerm term1 = parseTerm(lexer);

    TokenType tokenType = lexer.getLookahead().getType();
    lexer.consumeEither(EQUALS, NOT_EQUALS, LESS_THAN);
    BitVectorTerm term2 = parseTerm(lexer);

    if (tokenType == EQUALS) {
      return equal(term1, term2);
    } else if (tokenType == NOT_EQUALS) {
      return unequal(term1, term2);
    } else { // LESS_THAN
      return lessThan(term1, term2);
    }
  }

  private BitVectorTerm parseTerm(Lexer lexer) {
    BitVectorTerm term = parseA(lexer);

    while (lexer.canConsume(BITWISE_OR)) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    return term;
  }

  private BitVectorTerm parseA(Lexer lexer) {
    BitVectorTerm term = parseB(lexer);

    while (lexer.canConsume(BITWISE_XOR)) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    return term;
  }

  private BitVectorTerm parseB(Lexer lexer) {
    BitVectorTerm term = parseC(lexer);

    while (lexer.canConsume(BITWISE_AND)) {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    return term;
  }

  private BitVectorTerm parseC(Lexer lexer) {
    BitVectorTerm term = parseD(lexer);

    while (lexer.canConsumeEither(BITWISE_LEFT_SHIFT, BITWISE_RIGHT_SHIFT)) {
      if (lexer.canConsume(BITWISE_LEFT_SHIFT)) {
        lexer.consume(BITWISE_LEFT_SHIFT);
        term = leftShift(term, parseD(lexer));
      } else {
        lexer.consume(BITWISE_RIGHT_SHIFT);
        throw new UnsupportedOperationException("Not implemented yet");
      }
    }

    return term;
  }

  private BitVectorTerm parseD(Lexer lexer) {
    BitVectorTerm term = parseE(lexer);

    while (lexer.canConsumeEither(PLUS, MINUS)) {
      if (lexer.canConsume(PLUS)) {
        lexer.consume(PLUS);
        term = addition(term, parseE(lexer));
      } else {
        lexer.consume(MINUS);
        term = subtraction(term, parseE(lexer));
      }
    }

    return term;
  }

  private BitVectorTerm parseE(Lexer lexer) {
    BitVectorTerm term = parseF(lexer);

    while (lexer.canConsumeEither(TIMES, REMAINDER)) {
      if (lexer.canConsume(TIMES)) {
        lexer.consume(TIMES);
        term = product(term, parseF(lexer));
      } else {
        lexer.consume(REMAINDER);
        term = remainder(term, parseF(lexer));
      }
    }

    return term;
  }

  private BitVectorTerm parseF(Lexer lexer) {
    if (lexer.canConsume(BITWISE_NOT)) {
      lexer.consume(BITWISE_NOT);

      return negation(parseG(lexer));
    }

    return parseG(lexer);
  }

  private BitVectorTerm parseG(Lexer lexer) {
    lexer.requireEither(IDENTIFIER, BINARY_CONSTANT, INTEGER, HEX_CONSTANT);

    Token token = lexer.getLookahead();
    TokenType tokenType = token.getType();

    if (tokenType == IDENTIFIER) {
      lexer.consume(IDENTIFIER);
      return bitvector(
          token.getValue(), DEFAULT_BIT_VECTOR_LENGTH); // TODO: get bit width from somewhere
    } else if (tokenType == INTEGER) {
      lexer.consume(INTEGER);
      int value = Integer.parseInt(token.getValue());
      return constant(
          new BitVector(value, DEFAULT_BIT_VECTOR_LENGTH)); // TODO: get bit width from somewhere
    } else if (tokenType == BINARY_CONSTANT) {
      lexer.consume(BINARY_CONSTANT);
      return parseBinaryConstant(token.getValue());
    }

    throw new UnsupportedOperationException("Not implemented yet");
  }

  private BitVectorConstant parseBinaryConstant(String string) {
    string = string.substring(2); // remove 0b

    boolean[] bits = new boolean[DEFAULT_BIT_VECTOR_LENGTH];
    int max = Math.min(bits.length, string.length());
    for (int i = 0; i < max; i++) {
      bits[max - i - 1] = string.charAt(i) == '1';
    }

    return constant(new BitVector(bits));
  }
}
