package me.paultristanwagner.satchecking.parse;

import me.paultristanwagner.satchecking.theory.bitvector.BitVector;
import me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorConstraint;
import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorConstant;
import me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorTerm;

import static me.paultristanwagner.satchecking.parse.TokenType.*;
import static me.paultristanwagner.satchecking.theory.bitvector.BitVector.DEFAULT_BIT_VECTOR_LENGTH;
import static me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorBitConstraint.bitSet;
import static me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorEqualConstraint.equal;
import static me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorGreaterThanConstraint.greaterThan;
import static me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorGreaterThanOrEqualConstraint.greaterThanOrEqual;
import static me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorLessThanConstraint.lessThan;
import static me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorLessThanOrEqualConstraint.lessThanOrEqual;
import static me.paultristanwagner.satchecking.theory.bitvector.constraint.BitVectorUnequalConstraint.unequal;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorAddition.addition;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorAnd.bitwiseAnd;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorConstant.constant;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorDivision.division;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorLeftShift.leftShift;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorNegation.negation;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorOr.bitwiseOr;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorProduct.product;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorRemainder.remainder;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorRightShift.rightShift;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorSubtraction.subtraction;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorVariable.bitvector;
import static me.paultristanwagner.satchecking.theory.bitvector.term.BitVectorXor.bitwiseXor;

public class BitVectorConstraintParser implements Parser<BitVectorConstraint> {

  /*
   *  Grammar for BitVector constraints:
   *    <S> ::= <TERM> EQUALS <TERM>
   *          | <TERM> NOT_EQUALS <TERM>
   *          | <TERM> LESS_THAN <TERM>
   *          | <TERM> GREATER_THAN <TERM>
   *          | <TERM> LOWER_EQUALS <TERM>
   *          | <TERM> GREATER_EQUALS <TERM>
   *          | <TERM> '[' <INTEGER> ']'
   *
   *    <TERM> ::= BITWISE_NOT <TERM>
   *             | <A> BITWISE_OR <A>
   *             | <A>
   *
   *    <A> ::= <B> BITWISE_XOR <B>
   *          | <B>
   *
   *    <B> ::= <C> BITWISE_AND <C>
   *          | <C>
   *
   *    <C> ::= <D> BITWISE_LEFT_SHIFT <D>
   *          | <D> BITWISE_RIGHT_SHIFT <D>
   *          | <D>
   *
   *    <D> ::= <E> ADD <E>
   *          | <E> MINUS <E>
   *          | <E>
   *
   *    <E> ::= <F> TIMES <F>
   *          | <F> DIVISION <F>
   *          | <F> REMAINDER <F>
   *          | <F>
   *
   *    <F> ::= BITWISE_NEGATION <G>
   *          | <G>
   *
   *    <G> ::= ( <TERM> )
   *          | IDENTIFIER
   *          | BINARY_CONSTANT
   *          | INTEGER
   *          | HEX_CONSTANT
   *
   */

  @Override
  public BitVectorConstraint parse(String string) {
    Lexer lexer = new BitVectorConstraintLexer(string);

    lexer.requireNextToken();

    BitVectorConstraint constraint = parseConstraint(lexer);

    lexer.requireNoToken();

    return constraint;
  }

  @Override
  public ParseResult<BitVectorConstraint> parseWithRemaining(String string) {
    Lexer lexer = new BitVectorConstraintLexer(string);

    lexer.requireNextToken();

    BitVectorConstraint constraint = parseConstraint(lexer);

    return new ParseResult<>(constraint, lexer.getCursor(), lexer.getCursor() == string.length());
  }

  private BitVectorConstraint parseConstraint(Lexer lexer) {
    BitVectorTerm term1 = parseTerm(lexer);

    lexer.requireEither(LBRACKET, EQUALS, NOT_EQUALS, LOWER_EQUALS, GREATER_EQUALS, LESS_THAN, GREATER_THAN);

    if (lexer.canConsume(LBRACKET)) {
      lexer.consume(LBRACKET);
      lexer.require(INTEGER);
      String bitIndexString = lexer.getLookahead().getValue();
      lexer.consume(INTEGER);

      int bitIndex = Integer.parseInt(bitIndexString);

      lexer.consume(RBRACKET);

      return bitSet(term1, bitIndex);
    }

    TokenType tokenType = lexer.getLookahead().getType();
    lexer.consume(tokenType);
    BitVectorTerm term2 = parseTerm(lexer);

    if (tokenType == EQUALS) {
      return equal(term1, term2);
    } else if (tokenType == NOT_EQUALS) {
      return unequal(term1, term2);
    } else if (tokenType == LOWER_EQUALS) {
      return lessThanOrEqual(term1, term2);
    } else if (tokenType == GREATER_EQUALS) {
      return greaterThanOrEqual(term1, term2);
    } else if (tokenType == GREATER_THAN) {
      return greaterThan(term1, term2);
    } else { // LESS_THAN
      return lessThan(term1, term2);
    }
  }

  private BitVectorTerm parseTerm(Lexer lexer) {
    BitVectorTerm term = parseA(lexer);

    while (lexer.canConsume(BITWISE_OR)) {
      lexer.consume(BITWISE_OR);
      term = bitwiseOr(term, parseA(lexer));
    }

    return term;
  }

  private BitVectorTerm parseA(Lexer lexer) {
    BitVectorTerm term = parseB(lexer);

    while (lexer.canConsume(BITWISE_XOR)) {
      lexer.consume(BITWISE_XOR);
      term = bitwiseXor(term, parseB(lexer));
    }

    return term;
  }

  private BitVectorTerm parseB(Lexer lexer) {
    BitVectorTerm term = parseC(lexer);

    while (lexer.canConsume(BITWISE_AND)) {
      lexer.consume(BITWISE_AND);
      term = bitwiseAnd(term, parseC(lexer));
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
        term = rightShift(term, parseD(lexer));
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

    while (lexer.canConsumeEither(TIMES, DIVIDE, REMAINDER)) {
      if (lexer.canConsume(TIMES)) {
        lexer.consume(TIMES);
        term = product(term, parseF(lexer));
      } else if (lexer.canConsume(DIVIDE)) {
        lexer.consume(DIVIDE);
        term = division(term, parseF(lexer));
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
    lexer.requireEither(LPAREN, IDENTIFIER, BINARY_CONSTANT, INTEGER, HEX_CONSTANT);

    Token token = lexer.getLookahead();
    TokenType tokenType = token.getType();

    if (tokenType == LPAREN) {
      lexer.consume(LPAREN);
      BitVectorTerm term = parseTerm(lexer);
      lexer.consume(RPAREN);
      return BitVectorParenthesisTerm.parenthesis(term);
    } else if (tokenType == IDENTIFIER) {
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
