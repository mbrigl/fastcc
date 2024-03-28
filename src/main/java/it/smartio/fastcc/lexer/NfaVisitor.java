/*
 * Copyright (c) 2001-2021 Territorium Online Srl / TOL GmbH. All Rights Reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code as defined in and that are
 * subject to the Territorium Online License Version 1.0. You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at http://www.tol.info/license/
 * and read it before using this file.
 *
 * The Original Code and all software distributed under the License are distributed on an 'AS IS'
 * basis, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, AND TERRITORIUM ONLINE HEREBY
 * DISCLAIMS ALL SUCH WARRANTIES, INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT. Please see the License for
 * the specific language governing rights and limitations under the License.
 */

package it.smartio.fastcc.lexer;

import java.util.ArrayList;
import java.util.List;

import it.smartio.fastcc.generator.LexerStateData;
import it.smartio.fastcc.parser.CharacterRange;
import it.smartio.fastcc.parser.JavaCCErrors;
import it.smartio.fastcc.parser.RCharacterList;
import it.smartio.fastcc.parser.RChoice;
import it.smartio.fastcc.parser.REndOfFile;
import it.smartio.fastcc.parser.RJustName;
import it.smartio.fastcc.parser.ROneOrMore;
import it.smartio.fastcc.parser.RRepetitionRange;
import it.smartio.fastcc.parser.RSequence;
import it.smartio.fastcc.parser.RStringLiteral;
import it.smartio.fastcc.parser.RZeroOrMore;
import it.smartio.fastcc.parser.RZeroOrOne;
import it.smartio.fastcc.parser.RegularExpression;
import it.smartio.fastcc.parser.RegularExpressionVisitor;
import it.smartio.fastcc.parser.SingleCharacter;

/**
 * The {@link NfaVisitor} class.
 */
public final class NfaVisitor implements RegularExpressionVisitor<Nfa, LexerStateData> {

  private final boolean ignoreCase;

  /**
   * Constructs an instance of {@link NfaVisitor}.
   *
   * @param ignoreCase
   */
  public NfaVisitor(boolean ignoreCase) {
    this.ignoreCase = ignoreCase;
  }

  /**
   * Return <code>true</code> if the cases are ignored.
   */
  private boolean isIgnoreCase() {
    return this.ignoreCase;
  }

  @Override
  public Nfa visit(RCharacterList expr, LexerStateData data) {
    if (!expr.isTransformed()) {
      if (data.ignoreCase() || isIgnoreCase()) {
        expr.ToCaseNeutral();
        expr.SortDescriptors();
      }

      if (expr.isNegated_list()) {
        expr.RemoveNegation(); // This also sorts the list
      } else {
        expr.SortDescriptors();
      }
    }

    if ((expr.getDescriptors().size() == 0) && !expr.isNegated_list()) {
      JavaCCErrors.semantic_error(this, "Empty character set is not allowed as it will not match any character.");
      return new Nfa(data);
    }

    expr.setTransformed();
    Nfa retVal = new Nfa(data);
    NfaState startState = retVal.start;
    NfaState finalState = retVal.end;
    int i;

    for (i = 0; i < expr.getDescriptors().size(); i++) {
      if (expr.getDescriptors().get(i) instanceof SingleCharacter) {
        startState.AddChar(((SingleCharacter) expr.getDescriptors().get(i)).ch);
      } else // if (descriptors.get(i) instanceof CharacterRange)
      {
        CharacterRange cr = (CharacterRange) expr.getDescriptors().get(i);

        if (cr.getLeft() == cr.getRight()) {
          startState.AddChar(cr.getLeft());
        } else {
          startState.AddRange(cr.getLeft(), cr.getRight());
        }
      }
    }

    startState.next = finalState;

    return retVal;
  }

  @Override
  public Nfa visit(RChoice expr, LexerStateData data) {
    expr.CompressCharLists();

    if (expr.getChoices().size() == 1) {
      return expr.getChoices().get(0).accept(this, data);
    }

    Nfa retVal = new Nfa(data);
    NfaState startState = retVal.start;
    NfaState finalState = retVal.end;

    for (Object element : expr.getChoices()) {
      Nfa temp;
      RegularExpression curRE = (RegularExpression) element;

      temp = curRE.accept(this, data);

      startState.AddMove(temp.start);
      temp.end.AddMove(finalState);
    }

    return retVal;
  }

  @Override
  public Nfa visit(REndOfFile expr, LexerStateData data) {
    return null;
  }

  @Override
  public Nfa visit(RJustName expr, LexerStateData data) {
    return expr.getRegexpr().accept(this, data);
  }

  @Override
  public Nfa visit(ROneOrMore expr, LexerStateData data) {
    Nfa retVal = new Nfa(data);
    NfaState startState = retVal.start;
    NfaState finalState = retVal.end;

    Nfa temp = expr.getRegexpr().accept(this, data);

    startState.AddMove(temp.start);
    temp.end.AddMove(temp.start);
    temp.end.AddMove(finalState);

    return retVal;
  }

  @Override
  public Nfa visit(RRepetitionRange expr, LexerStateData data) {
    List<RegularExpression> units = new ArrayList<>();
    RSequence seq;
    int i;

    for (i = 0; i < expr.getMin(); i++) {
      units.add(expr.getRegexpr());
    }

    if (expr.hasMax() && (expr.getMax() == -1)) // Unlimited
    {
      RZeroOrMore zoo = new RZeroOrMore();
      zoo.setRegexpr(expr.getRegexpr());
      units.add(zoo);
    }

    while (i++ < expr.getMax()) {
      RZeroOrOne zoo = new RZeroOrOne();
      zoo.setRegexpr(expr.getRegexpr());
      units.add(zoo);
    }
    seq = new RSequence(units);
    return seq.accept(this, data);
  }

  @Override
  public Nfa visit(RSequence expr, LexerStateData data) {
    if (expr.getUnits().size() == 1) {
      return expr.getUnits().get(0).accept(this, data);
    }

    Nfa retVal = new Nfa(data);
    NfaState startState = retVal.start;
    NfaState finalState = retVal.end;
    Nfa temp1;
    Nfa temp2 = null;

    RegularExpression curRE;

    curRE = expr.getUnits().get(0);
    temp1 = curRE.accept(this, data);
    startState.AddMove(temp1.start);

    for (int i = 1; i < expr.getUnits().size(); i++) {
      curRE = expr.getUnits().get(i);

      temp2 = curRE.accept(this, data);
      temp1.end.AddMove(temp2.start);
      temp1 = temp2;
    }

    temp2.end.AddMove(finalState);

    return retVal;
  }

  @Override
  public Nfa visit(RStringLiteral expr, LexerStateData data) {
    if (expr.getImage().length() == 1) {
      RCharacterList temp = new RCharacterList(expr.getImage().charAt(0));
      return temp.accept(this, data);
    }

    NfaState startState = new NfaState(data);
    NfaState theStartState = startState;
    NfaState finalState = null;

    if (expr.getImage().length() == 0) {
      return new Nfa(theStartState, theStartState);
    }

    int i;

    for (i = 0; i < expr.getImage().length(); i++) {
      finalState = new NfaState(data);
      startState.charMoves = new char[1];
      startState.AddChar(expr.getImage().charAt(i));

      if (data.ignoreCase() || isIgnoreCase()) {
        startState.AddChar(Character.toLowerCase(expr.getImage().charAt(i)));
        startState.AddChar(Character.toUpperCase(expr.getImage().charAt(i)));
      }

      startState.next = finalState;
      startState = finalState;
    }

    return new Nfa(theStartState, finalState);
  }

  @Override
  public Nfa visit(RZeroOrMore expr, LexerStateData data) {
    Nfa retVal = new Nfa(data);
    Nfa temp = expr.getRegexpr().accept(this, data);


    NfaState startState = retVal.start;
    NfaState finalState = retVal.end;
    startState.AddMove(temp.start);
    startState.AddMove(finalState);
    temp.end.AddMove(finalState);
    temp.end.AddMove(temp.start);

    return retVal;
  }

  @Override
  public Nfa visit(RZeroOrOne expr, LexerStateData data) {
    Nfa retVal = new Nfa(data);
    Nfa temp = expr.getRegexpr().accept(this, data);

    NfaState startState = retVal.start;
    NfaState finalState = retVal.end;
    startState.AddMove(temp.start);
    startState.AddMove(finalState);
    temp.end.AddMove(finalState);

    return retVal;
  }
}
