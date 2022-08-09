// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

/*
 * Copyright (c) 2006, Sun Microsystems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. * Neither the name of the Sun Microsystems, Inc. nor
 * the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.javacc.generator;

import org.fastcc.source.SourceWriter;
import org.javacc.JavaCC;
import org.javacc.JavaCCContext;
import org.javacc.JavaCCLanguage;
import org.javacc.JavaCCRequest;
import org.javacc.generator.ParserData.Phase3Data;
import org.javacc.parser.Action;
import org.javacc.parser.BNFProduction;
import org.javacc.parser.Choice;
import org.javacc.parser.Expansion;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.Lookahead;
import org.javacc.parser.NonTerminal;
import org.javacc.parser.NormalProduction;
import org.javacc.parser.OneOrMore;
import org.javacc.parser.Options;
import org.javacc.parser.ParseException;
import org.javacc.parser.RegularExpression;
import org.javacc.parser.Sequence;
import org.javacc.parser.Token;
import org.javacc.parser.TryBlock;
import org.javacc.parser.ZeroOrMore;
import org.javacc.parser.ZeroOrOne;
import org.javacc.semantic.Semanticize;

import java.io.IOException;
import java.util.List;

public abstract class ParserGenerator extends CodeGenerator {

  // Constants used in the following method "buildLookaheadChecker".
  protected enum LookaheadState {
    NOOPENSTM,
    OPENIF,
    OPENSWITCH
  }

  private int labelIndex;
  private int rIndex;

  /**
   * Constructs an instance of {@link ParserGenerator}.
   *
   * @param source
   * @param language
   */
  protected ParserGenerator(SourceWriter source, JavaCCLanguage language) {
    super(source, language);
    this.labelIndex = 0;
    this.rIndex = 0;
  }

  protected final int nextLabelIndex() {
    return ++this.labelIndex;
  }

  private final int nextRIndex() {
    return ++this.rIndex;
  }

  protected final void start(JavaCCRequest request, JavaCCContext context) throws ParseException, IOException {
    if (JavaCCErrors.hasError()) {
      throw new ParseException();
    }

    List<String> toolNames = context.getToolNames();
    toolNames.add(JavaCC.TOOLNAME);

    ParserData data = new ParserData(request, context);
    for (NormalProduction p : data.getProductions()) {
      if (p instanceof BNFProduction) {
        buildPhase1(data, p.getExpansion());
      }
    }

    for (Lookahead la : data.getLoakaheads()) {
      data.addExpansion(la);
    }

    int phase3index = 0;
    while (phase3index < data.phase3list.size()) {
      for (; phase3index < data.phase3list.size(); phase3index++) {
        setupPhase3Builds(data, data.phase3list.get(phase3index));
      }
    }

    for (Expansion e : data.getExpansions()) {
      buildPhase3Routine(data, e, data.getCount(e));
    }

    generate(data, toolNames);
  }

  protected abstract void generate(ParserData data, List<String> toolNames) throws IOException;

  private final void buildPhase1(ParserData data, Expansion e) {
    if (e instanceof Choice) {
      Choice e_nrw = (Choice) e;
      Lookahead[] conds = new Lookahead[e_nrw.getChoices().size()];
      // In previous line, the "throw" never throws an exception since the
      // evaluation of jj_consume_token(-1) causes ParseException to be
      // thrown first.
      for (int i = 0; i < e_nrw.getChoices().size(); i++) {
        Sequence nestedSeq = (Sequence) (e_nrw.getChoices().get(i));
        buildPhase1(data, nestedSeq);
        conds[i] = (Lookahead) (nestedSeq.units.get(0));
      }
      data.setLookupAhead(e, conds);

      buildLookahead(data, conds);
    } else if (e instanceof Sequence) {
      Sequence e_nrw = (Sequence) e;
      // We skip the first element in the following iteration since it is the
      // Lookahead object.
      for (int i = 1; i < e_nrw.units.size(); i++) {
        // For C++, since we are not using exceptions, we will protect all the
        // expansion choices with if (!error)
        buildPhase1(data, (Expansion) (e_nrw.units.get(i)));
      }
    } else if (e instanceof OneOrMore) {
      OneOrMore e_nrw = (OneOrMore) e;
      Expansion nested_e = e_nrw.expansion;
      Lookahead la;
      if (nested_e instanceof Sequence) {
        la = (Lookahead) (((Sequence) nested_e).units.get(0));
      } else {
        la = new Lookahead();
        la.setAmount(Options.getLookahead());
        la.setLaExpansion(nested_e);
      }

      Lookahead[] conds = { la };
      data.setLookupAhead(e, conds);

      buildPhase1(data, nested_e);
      buildLookahead(data, conds);
    } else if (e instanceof ZeroOrMore) {
      ZeroOrMore e_nrw = (ZeroOrMore) e;
      Expansion nested_e = e_nrw.expansion;
      Lookahead la;
      if (nested_e instanceof Sequence) {
        la = (Lookahead) (((Sequence) nested_e).units.get(0));
      } else {
        la = new Lookahead();
        la.setAmount(Options.getLookahead());
        la.setLaExpansion(nested_e);
      }

      Lookahead[] conds = { la };
      data.setLookupAhead(e, conds);

      buildLookahead(data, conds);
      buildPhase1(data, nested_e);
    } else if (e instanceof ZeroOrOne) {
      ZeroOrOne e_nrw = (ZeroOrOne) e;
      Expansion nested_e = e_nrw.expansion;

      Lookahead la;
      if (nested_e instanceof Sequence) {
        la = (Lookahead) (((Sequence) nested_e).units.get(0));
      } else {
        la = new Lookahead();
        la.setAmount(Options.getLookahead());
        la.setLaExpansion(nested_e);
      }

      Lookahead[] conds = { la };
      data.setLookupAhead(e, conds);

      buildPhase1(data, nested_e);
      buildLookahead(data, conds);
    } else if (e instanceof TryBlock) {
      TryBlock e_nrw = (TryBlock) e;
      buildPhase1(data, e_nrw.exp);
    }
  }

  /**
   * This method takes two parameters - an array of Lookahead's "conds", and an array of String's
   * "actions". "actions" contains exactly one element more than "conds". "actions" are Java source
   * code, and "conds" translate to conditions - so lets say "f(conds[i])" is true if the lookahead
   * required by "conds[i]" is indeed the case. This method returns a string corresponding to the
   * Java code for:
   *
   * if (f(conds[0]) actions[0] else if (f(conds[1]) actions[1] . . . else actions[action.length-1]
   *
   * A particular action entry ("actions[i]") can be null, in which case, a noop is generated for
   * that action.
   */
  private void buildLookahead(ParserData data, Lookahead[] conds) {
    LookaheadState state = LookaheadState.NOOPENSTM;
    boolean jj2LA = false;

    int[] tokenMask = null;
    int tokenMaskSize = ((data.getTokenCount() - 1) / 32) + 1;
    boolean[] casedValues = new boolean[data.getTokenCount()];

    for (Lookahead la : conds) {
      jj2LA = false;

      if ((la.getAmount() == 0) || Semanticize.emptyExpansionExists(la.getLaExpansion())) {
        // This handles the following cases:
        // . If syntactic lookahead is not wanted (and hence explicitly specified
        // as 0).
        // . If it is possible for the lookahead expansion to recognize the empty
        // string - in which case the lookahead trivially passes.
        // . If the lookahead expansion has a JAVACODE production that it directly
        // expands to - in which case the lookahead trivially passes.
        if (la.getActionTokens().isEmpty()) {
          // In addition, if there is no semantic lookahead, then the
          // lookahead trivially succeeds. So break the main loop and
          // treat this case as the default last action.
          break;
        } else {
          // This case is when there is only semantic lookahead
          // (without any preceding syntactic lookahead). In this
          // case, an "if" statement is generated.
          switch (state) {
            case OPENSWITCH:
              data.addMask(tokenMask, la);
            case OPENIF:
            case NOOPENSTM:
          }
          state = LookaheadState.OPENIF;
        }

      } else if ((la.getAmount() == 1) && (la.getActionTokens().size() == 0)) {
        // Special optimal processing when the lookahead is exactly 1, and there
        // is no semantic lookahead.
        boolean[] firstSet = new boolean[data.getTokenCount()];
        for (int i = 0; i < data.getTokenCount(); i++) {
          firstSet[i] = false;
        }

        // jj2LA is set to false at the beginning of the containing "if" statement.
        // It is checked immediately after the end of the same statement to determine
        // if lookaheads are to be performed using calls to the jj2 methods.
        jj2LA = genFirstSet(data, la.getLaExpansion(), firstSet, jj2LA);
        // genFirstSet may find that semantic attributes are appropriate for the next
        // token. In which case, it sets jj2LA to true.
        if (!jj2LA) {
          // This case is if there is no applicable semantic lookahead and the lookahead
          // is one (excluding the earlier cases such as JAVACODE, etc.).
          switch (state) {
            case OPENIF:
            case NOOPENSTM:
              for (int i = 0; i < data.getTokenCount(); i++) {
                casedValues[i] = false;
              }
              tokenMask = new int[tokenMaskSize];
              for (int i = 0; i < tokenMaskSize; i++) {
                tokenMask[i] = 0;
              }
              // Don't need to do anything if state is OPENSWITCH.
            default:
          }
          for (int i = 0; i < data.getTokenCount(); i++) {
            if (firstSet[i] && !casedValues[i]) {
              casedValues[i] = true;
              int j1 = i / 32;
              int j2 = i % 32;
              tokenMask[j1] |= 1 << j2;
            }
          }
          state = LookaheadState.OPENSWITCH;
        }
      } else {
        // This is the case when lookahead is determined through calls to
        // jj2 methods. The other case is when lookahead is 1, but semantic
        // attributes need to be evaluated. Hence this crazy control structure.
        jj2LA = true;
      }

      if (jj2LA) {
        // In this case lookahead is determined by the jj2 methods.
        switch (state) {
          case OPENSWITCH:
            data.addMask(tokenMask, la);
          case OPENIF:
          case NOOPENSTM:
        }

        // At this point, la.la_expansion.internal_name must be "".
        la.getLaExpansion().internal_name = "_" + data.addLookupAhead(la);
        state = LookaheadState.OPENIF;
      }
    }

    switch (state) {
      case OPENSWITCH:
        data.addMask(tokenMask, conds[conds.length - 1]);
      case OPENIF:
      case NOOPENSTM:
    }
  }

  /**
   * Sets up the array "firstSet" above based on the Expansion argument passed to it. Since this is
   * a recursive function, it assumes that "firstSet" has been reset before the first call.
   */
  protected final boolean genFirstSet(ParserData data, Expansion exp, boolean[] firstSet, boolean jj2la) {
    if (exp instanceof RegularExpression) {
      firstSet[((RegularExpression) exp).ordinal] = true;
    } else if (exp instanceof NonTerminal) {
      jj2la = genFirstSet(data, ((BNFProduction) (((NonTerminal) exp).getProd())).getExpansion(), firstSet, jj2la);
    } else if (exp instanceof Choice) {
      Choice ch = (Choice) exp;
      for (Object element : ch.getChoices()) {
        jj2la = genFirstSet(data, (Expansion) (element), firstSet, jj2la);
      }
    } else if (exp instanceof Sequence) {
      Sequence seq = (Sequence) exp;
      Object obj = seq.units.get(0);
      if ((obj instanceof Lookahead) && (((Lookahead) obj).getActionTokens().size() != 0)) {
        jj2la = true;
      }
      for (Object element : seq.units) {
        // Javacode productions can not have FIRST sets. Instead we generate the FIRST set
        // for the preceding LOOKAHEAD (the semantic checks should have made sure that
        // the LOOKAHEAD is suitable).
        jj2la = genFirstSet(data, (Expansion) (element), firstSet, jj2la);
        if (!Semanticize.emptyExpansionExists((Expansion) (element))) {
          break;
        }
      }
    } else if (exp instanceof OneOrMore) {
      OneOrMore om = (OneOrMore) exp;
      jj2la = genFirstSet(data, om.expansion, firstSet, jj2la);
    } else if (exp instanceof ZeroOrMore) {
      ZeroOrMore zm = (ZeroOrMore) exp;
      jj2la = genFirstSet(data, zm.expansion, firstSet, jj2la);
    } else if (exp instanceof ZeroOrOne) {
      ZeroOrOne zo = (ZeroOrOne) exp;
      jj2la = genFirstSet(data, zo.expansion, firstSet, jj2la);
    } else if (exp instanceof TryBlock) {
      TryBlock tb = (TryBlock) exp;
      jj2la = genFirstSet(data, tb.exp, firstSet, jj2la);
    }
    return jj2la;
  }

  private void setupPhase3Builds(ParserData data, Phase3Data p3d) {
    Expansion e = p3d.exp;
    if (e instanceof RegularExpression) {
      // nothing to here
    } else if (e instanceof NonTerminal) {
      // All expansions of non-terminals have the "name" fields set. So
      // there's no need to check it below for "e_nrw" and "ntexp". In
      // fact, we rely here on the fact that the "name" fields of both these
      // variables are the same.
      NonTerminal e_nrw = (NonTerminal) e;
      NormalProduction ntprod = data.getProduction(e_nrw.getName());
      generate3R(data, ntprod.getExpansion(), p3d);
    } else if (e instanceof Choice) {
      Choice e_nrw = (Choice) e;
      for (Object element : e_nrw.getChoices()) {
        generate3R(data, (Expansion) (element), p3d);
      }
    } else if (e instanceof Sequence) {
      Sequence e_nrw = (Sequence) e;
      // We skip the first element in the following iteration since it is the
      // Lookahead object.
      int cnt = p3d.count;
      for (int i = 1; i < e_nrw.units.size(); i++) {
        Expansion eseq = (Expansion) (e_nrw.units.get(i));
        setupPhase3Builds(data, data.new Phase3Data(eseq, cnt));
        cnt -= ParserGenerator.minimumSize(data, eseq);
        if (cnt <= 0) {
          break;
        }
      }
    } else if (e instanceof TryBlock) {
      TryBlock e_nrw = (TryBlock) e;
      setupPhase3Builds(data, data.new Phase3Data(e_nrw.exp, p3d.count));
    } else if (e instanceof OneOrMore) {
      OneOrMore e_nrw = (OneOrMore) e;
      generate3R(data, e_nrw.expansion, p3d);
    } else if (e instanceof ZeroOrMore) {
      ZeroOrMore e_nrw = (ZeroOrMore) e;
      generate3R(data, e_nrw.expansion, p3d);
    } else if (e instanceof ZeroOrOne) {
      ZeroOrOne e_nrw = (ZeroOrOne) e;
      generate3R(data, e_nrw.expansion, p3d);
    }
  }


  private void generate3R(ParserData data, Expansion e, Phase3Data inf) {
    Expansion seq = e;
    if (e.internal_name.equals("")) {
      while (true) {
        if ((seq instanceof Sequence) && (((Sequence) seq).units.size() == 2)) {
          seq = (Expansion) ((Sequence) seq).units.get(1);
        } else if (seq instanceof NonTerminal) {
          NonTerminal e_nrw = (NonTerminal) seq;
          NormalProduction ntprod = data.getProduction(e_nrw.getName());
          seq = ntprod.getExpansion();
        } else {
          break;
        }
      }

      if (seq instanceof RegularExpression) {
        RegularExpression re = (RegularExpression) seq;
        e.internal_name =
            "jj_scan_token(" + ((re.label == null) || re.label.isEmpty() ? "" + re.ordinal : re.label) + ")";
        return;
      }

      e.internal_name = "R_" + e.getProductionName() + "_" + nextRIndex();
    }

    Integer count = data.phase3table.get(e);
    if ((count == null) || (count < inf.count)) {
      data.phase3list.add(data.new Phase3Data(e, inf.count));
      data.phase3table.put(e, inf.count);
    }
  }


  private void buildPhase3Routine(ParserData data, Expansion e, int count) {
    if (e.internal_name.startsWith("jj_scan_token")) {
      return;
    }

    if (e instanceof Choice) {
      Sequence nested_seq;
      Choice e_nrw = (Choice) e;
      for (int i = 0; i < e_nrw.getChoices().size(); i++) {
        nested_seq = (Sequence) (e_nrw.getChoices().get(i));
        Lookahead la = (Lookahead) (nested_seq.units.get(0));
        if (la.getActionTokens().size() != 0) {
          // We have semantic lookahead that must be evaluated.
          data.setLookAheadNeeded(true);
        }
      }
    } else if (e instanceof Sequence) {
      Sequence e_nrw = (Sequence) e;
      // We skip the first element in the following iteration since it is the
      // Lookahead object.
      int cnt = count;
      for (int i = 1; i < e_nrw.units.size(); i++) {
        Expansion eseq = (Expansion) (e_nrw.units.get(i));
        buildPhase3Routine(data, eseq, cnt);
        cnt -= ParserGenerator.minimumSize(data, eseq);
        if (cnt <= 0) {
          break;
        }
      }
    } else if (e instanceof TryBlock) {
      TryBlock e_nrw = (TryBlock) e;
      buildPhase3Routine(data, e_nrw.exp, count);
    }
  }

  protected static int minimumSize(ParserData data, Expansion e) {
    return ParserGenerator.minimumSize(data, e, Integer.MAX_VALUE);
  }

  /*
   * Returns the minimum number of tokens that can parse to this expansion.
   */
  private static int minimumSize(ParserData data, Expansion e, int oldMin) {
    int retval = 0; // should never be used. Will be bad if it is.
    if (e.inMinimumSize) {
      // recursive search for minimum size unnecessary.
      return Integer.MAX_VALUE;
    }
    e.inMinimumSize = true;
    if (e instanceof RegularExpression) {
      retval = 1;
    } else if (e instanceof NonTerminal) {
      NonTerminal e_nrw = (NonTerminal) e;
      NormalProduction ntprod = data.getProduction(e_nrw.getName());
      Expansion ntexp = ntprod.getExpansion();
      retval = ParserGenerator.minimumSize(data, ntexp);
    } else if (e instanceof Choice) {
      int min = oldMin;
      Expansion nested_e;
      Choice e_nrw = (Choice) e;
      for (int i = 0; (min > 1) && (i < e_nrw.getChoices().size()); i++) {
        nested_e = (Expansion) (e_nrw.getChoices().get(i));
        int min1 = ParserGenerator.minimumSize(data, nested_e, min);
        if (min > min1) {
          min = min1;
        }
      }
      retval = min;
    } else if (e instanceof Sequence) {
      int min = 0;
      Sequence e_nrw = (Sequence) e;
      // We skip the first element in the following iteration since it is the
      // Lookahead object.
      for (int i = 1; i < e_nrw.units.size(); i++) {
        Expansion eseq = (Expansion) (e_nrw.units.get(i));
        int mineseq = ParserGenerator.minimumSize(data, eseq);
        if ((min == Integer.MAX_VALUE) || (mineseq == Integer.MAX_VALUE)) {
          min = Integer.MAX_VALUE; // Adding infinity to something results in infinity.
        } else {
          min += mineseq;
          if (min > oldMin) {
            break;
          }
        }
      }
      retval = min;
    } else if (e instanceof TryBlock) {
      TryBlock e_nrw = (TryBlock) e;
      retval = ParserGenerator.minimumSize(data, e_nrw.exp);
    } else if (e instanceof OneOrMore) {
      OneOrMore e_nrw = (OneOrMore) e;
      retval = ParserGenerator.minimumSize(data, e_nrw.expansion);
    } else if (e instanceof ZeroOrMore) {
      retval = 0;
    } else if (e instanceof ZeroOrOne) {
      retval = 0;
    } else if (e instanceof Lookahead) {
      retval = 0;
    } else if (e instanceof Action) {
      retval = 0;
    }
    e.inMinimumSize = false;
    return retval;
  }

  protected final String getTrailingComments(Token t) {
    return (t.next == null) ? "" : getLeadingComments(t.next);
  }

  protected final String getLeadingComments(Token t) {
    String retval = "";
    if (t.specialToken == null) {
      return retval;
    }
    Token tt = t.specialToken;
    while (tt.specialToken != null) {
      tt = tt.specialToken;
    }
    while (tt != null) {
      retval += getStringForTokenOnly(tt);
      tt = tt.next;
    }
    if ((this.ccol != 1) && (this.cline != t.beginLine)) {
      retval += "\n";
      this.cline++;
      this.ccol = 1;
    }
    return retval;
  }
}
