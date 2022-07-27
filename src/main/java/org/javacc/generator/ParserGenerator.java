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
import org.fastcc.utils.Encoding;
import org.javacc.JavaCC;
import org.javacc.JavaCCContext;
import org.javacc.JavaCCLanguage;
import org.javacc.JavaCCRequest;
import org.javacc.parser.Action;
import org.javacc.parser.BNFProduction;
import org.javacc.parser.Choice;
import org.javacc.parser.Expansion;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.JavaCCParserConstants;
import org.javacc.parser.JavaCodeProduction;
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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public abstract class ParserGenerator extends CodeGenerator {

  // Constants used in the following method "buildLookaheadChecker".
  private static final int NOOPENSTM  = 0;
  private static final int OPENIF     = 1;
  private static final int OPENSWITCH = 2;

  /**
   * Constructs an instance of {@link ParserGenerator}.
   *
   * @param source
   * @param language
   */
  protected ParserGenerator(SourceWriter source, JavaCCLanguage language) {
    super(source, language);
  }

  public final void start(JavaCCRequest request, JavaCCContext context) throws ParseException {
    if (JavaCCErrors.hasError()) {
      throw new ParseException();
    }

    ParserData data = new ParserData(request, context);
    List<String> toolNames = context.getToolNames();
    toolNames.add(JavaCC.TOOLNAME);
    generate(data, toolNames);
  }

  protected abstract void generate(ParserData data, List<String> toolNames) throws ParseException;

  /**
   * Returns true if there is a JAVACODE production that the argument expansion may directly expand
   * to (without consuming tokens or encountering lookahead).
   */
  private boolean javaCodeCheck(Expansion exp) {
    if (exp instanceof RegularExpression) {
      return false;
    } else if (exp instanceof NonTerminal) {
      NormalProduction prod = ((NonTerminal) exp).getProd();
      return javaCodeCheck(prod.getExpansion());
    } else if (exp instanceof Choice) {
      Choice ch = (Choice) exp;
      for (Object element : ch.getChoices()) {
        if (javaCodeCheck((Expansion) (element))) {
          return true;
        }
      }
      return false;
    } else if (exp instanceof Sequence) {
      Sequence seq = (Sequence) exp;
      for (int i = 0; i < seq.units.size(); i++) {
        Expansion[] units = seq.units.toArray(new Expansion[seq.units.size()]);
        if ((units[i] instanceof Lookahead) && ((Lookahead) units[i]).isExplicit()) {
          // An explicit lookahead (rather than one generated implicitly). Assume
          // the user knows what he / she is doing, e.g.
          // "A" ( "B" | LOOKAHEAD("X") jcode() | "C" )* "D"
          return false;
        } else if (javaCodeCheck((units[i]))) {
          return true;
        } else if (!Semanticize.emptyExpansionExists(units[i])) {
          return false;
        }
      }
      return false;
    } else if (exp instanceof OneOrMore) {
      OneOrMore om = (OneOrMore) exp;
      return javaCodeCheck(om.expansion);
    } else if (exp instanceof ZeroOrMore) {
      ZeroOrMore zm = (ZeroOrMore) exp;
      return javaCodeCheck(zm.expansion);
    } else if (exp instanceof ZeroOrOne) {
      ZeroOrOne zo = (ZeroOrOne) exp;
      return javaCodeCheck(zo.expansion);
    } else if (exp instanceof TryBlock) {
      TryBlock tb = (TryBlock) exp;
      return javaCodeCheck(tb.exp);
    } else {
      return false;
    }
  }

  /**
   * Sets up the array "firstSet" above based on the Expansion argument passed to it. Since this is
   * a recursive function, it assumes that "firstSet" has been reset before the first call.
   */
  private void genFirstSet(ParserData data, Expansion exp) {
    if (exp instanceof RegularExpression) {
      data.firstSet[((RegularExpression) exp).ordinal] = true;
    } else if (exp instanceof NonTerminal) {
      genFirstSet(data, ((BNFProduction) (((NonTerminal) exp).getProd())).getExpansion());
    } else if (exp instanceof Choice) {
      Choice ch = (Choice) exp;
      for (Object element : ch.getChoices()) {
        genFirstSet(data, (Expansion) (element));
      }
    } else if (exp instanceof Sequence) {
      Sequence seq = (Sequence) exp;
      Object obj = seq.units.get(0);
      if ((obj instanceof Lookahead) && (((Lookahead) obj).getActionTokens().size() != 0)) {
        data.setJJ2LA(true);
      }
      for (Object element : seq.units) {
        // Javacode productions can not have FIRST sets. Instead we generate the FIRST set
        // for the preceding LOOKAHEAD (the semantic checks should have made sure that
        // the LOOKAHEAD is suitable).
        genFirstSet(data, (Expansion) (element));
        if (!Semanticize.emptyExpansionExists((Expansion) (element))) {
          break;
        }
      }
    } else if (exp instanceof OneOrMore) {
      OneOrMore om = (OneOrMore) exp;
      genFirstSet(data, om.expansion);
    } else if (exp instanceof ZeroOrMore) {
      ZeroOrMore zm = (ZeroOrMore) exp;
      genFirstSet(data, zm.expansion);
    } else if (exp instanceof ZeroOrOne) {
      ZeroOrOne zo = (ZeroOrOne) exp;
      genFirstSet(data, zo.expansion);
    } else if (exp instanceof TryBlock) {
      TryBlock tb = (TryBlock) exp;
      genFirstSet(data, tb.exp);
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
  private String buildLookaheadChecker(ParserData data, Lookahead[] conds, String[] actions) {
    // The state variables.
    int state = ParserGenerator.NOOPENSTM;
    int indentAmt = 0;
    boolean[] casedValues = new boolean[data.getTokenCount()];
    String retval = "";
    Lookahead la;
    Token t = null;
    int tokenMaskSize = ((data.getTokenCount() - 1) / 32) + 1;
    int[] tokenMask = null;

    // Iterate over all the conditions.
    int index = 0;
    while (index < conds.length) {

      la = conds[index];
      data.setJJ2LA(false);

      if ((la.getAmount() == 0) || Semanticize.emptyExpansionExists(la.getLaExpansion())
          || javaCodeCheck(la.getLaExpansion())) {

        // This handles the following cases:
        // . If syntactic lookahead is not wanted (and hence explicitly specified
        // as 0).
        // . If it is possible for the lookahead expansion to recognize the empty
        // string - in which case the lookahead trivially passes.
        // . If the lookahead expansion has a JAVACODE production that it directly
        // expands to - in which case the lookahead trivially passes.
        if (la.getActionTokens().size() == 0) {
          // In addition, if there is no semantic lookahead, then the
          // lookahead trivially succeeds. So break the main loop and
          // treat this case as the default last action.
          break;
        } else {
          // This case is when there is only semantic lookahead
          // (without any preceding syntactic lookahead). In this
          // case, an "if" statement is generated.
          switch (state) {
            case NOOPENSTM:
              retval += "\n" + "if (";
              indentAmt++;
              break;
            case OPENIF:
              retval += "\u0002\n" + "} else if (";
              break;
            case OPENSWITCH:
              retval += "\u0002\n" + "default:" + "\u0001";
              if (Options.getErrorReporting()) {
                retval += "\njj_la1[" + data.addMaskIndex() + "] = jj_gen;";
              }
              data.addMaskVals(tokenMask);
              retval += "\n" + "if (";
              indentAmt++;
          }
          genTokenSetup((la.getActionTokens().get(0)));
          for (Iterator<Token> it = la.getActionTokens().iterator(); it.hasNext();) {
            t = it.next();
            retval += getStringToPrint(t);
          }
          retval += getTrailingComments(t);
          retval += ") {\u0001" + actions[index];
          state = ParserGenerator.OPENIF;
        }

      } else if ((la.getAmount() == 1) && (la.getActionTokens().size() == 0)) {
        // Special optimal processing when the lookahead is exactly 1, and there
        // is no semantic lookahead.

        if (data.firstSet == null) {
          data.firstSet = new boolean[data.getTokenCount()];
        }
        for (int i = 0; i < data.getTokenCount(); i++) {
          data.firstSet[i] = false;
        }
        // jj2LA is set to false at the beginning of the containing "if" statement.
        // It is checked immediately after the end of the same statement to determine
        // if lookaheads are to be performed using calls to the jj2 methods.
        genFirstSet(data, la.getLaExpansion());
        // genFirstSet may find that semantic attributes are appropriate for the next
        // token. In which case, it sets jj2LA to true.
        if (!data.isJJ2LA()) {

          // This case is if there is no applicable semantic lookahead and the lookahead
          // is one (excluding the earlier cases such as JAVACODE, etc.).
          switch (state) {
            case OPENIF:
              retval += "\u0002\n" + "} else {\u0001";
              //$FALL-THROUGH$ Control flows through to next case.
            case NOOPENSTM:
              retval += "\n" + "switch (";
              if (Options.getCacheTokens()) {
                if (isJavaLanguage()) {
                  retval += "jj_nt.kind";
                } else {
                  retval += "jj_nt->kind()";
                }
                retval += ") {\u0001";
              } else {
                retval += "(jj_ntk==-1)?jj_ntk_f():jj_ntk) {\u0001";
              }
              for (int i = 0; i < data.getTokenCount(); i++) {
                casedValues[i] = false;
              }
              indentAmt++;
              tokenMask = new int[tokenMaskSize];
              for (int i = 0; i < tokenMaskSize; i++) {
                tokenMask[i] = 0;
              }
              // Don't need to do anything if state is OPENSWITCH.
          }
          for (int i = 0; i < data.getTokenCount(); i++) {
            if (data.firstSet[i]) {
              if (!casedValues[i]) {
                casedValues[i] = true;
                retval += "\u0002\ncase ";
                int j1 = i / 32;
                int j2 = i % 32;
                tokenMask[j1] |= 1 << j2;
                String s = data.getNameOfToken(i);
                if (s == null) {
                  retval += i;
                } else {
                  retval += s;
                }
                retval += ":\u0001";
              }
            }
          }
          retval += "{";
          retval += actions[index];
          retval += "\nbreak;\n}";
          state = ParserGenerator.OPENSWITCH;

        }

      } else {
        // This is the case when lookahead is determined through calls to
        // jj2 methods. The other case is when lookahead is 1, but semantic
        // attributes need to be evaluated. Hence this crazy control structure.

        data.setJJ2LA(true);
      }

      if (data.isJJ2LA()) {
        // In this case lookahead is determined by the jj2 methods.

        switch (state) {
          case NOOPENSTM:
            retval += "\n" + "if (";
            indentAmt++;
            break;
          case OPENIF:
            retval += "\u0002\n" + "} else if (";
            break;
          case OPENSWITCH:
            retval += "\u0002\n" + "default:" + "\u0001";
            if (Options.getErrorReporting()) {
              retval += "\njj_la1[" + data.addMaskIndex() + "] = jj_gen;";
            }
            data.addMaskVals(tokenMask);
            retval += "\n" + "if (";
            indentAmt++;
        }

        // At this point, la.la_expansion.internal_name must be "".
        la.getLaExpansion().internal_name = "_" + data.addJJ2Index();
        data.phase2list.add(la);


        String amount = Integer.toString(la.getAmount());
        if (isCppLanguage() && (la.getAmount() == Integer.MAX_VALUE)) {
          amount = "INT_MAX";
        }

        retval += "jj_2" + la.getLaExpansion().internal_name + "(" + amount + ")";
        if (la.getActionTokens().size() != 0) {
          // In addition, there is also a semantic lookahead. So concatenate
          // the semantic check with the syntactic one.
          retval += " && (";
          genTokenSetup((la.getActionTokens().get(0)));
          for (Iterator<Token> it = la.getActionTokens().iterator(); it.hasNext();) {
            t = it.next();
            retval += getStringToPrint(t);
          }
          retval += getTrailingComments(t);
          retval += ")";
        }
        retval += ") {\u0001" + actions[index];
        state = ParserGenerator.OPENIF;
      }

      index++;
    }

    // Generate code for the default case. Note this may not
    // be the last entry of "actions" if any condition can be
    // statically determined to be always "true".

    switch (state) {
      case NOOPENSTM:
        retval += actions[index];
        break;
      case OPENIF:
        retval += "\u0002\n" + "} else {\u0001" + actions[index];
        break;
      case OPENSWITCH:
        retval += "\u0002\n" + "default:" + "\u0001";
        if (Options.getErrorReporting()) {
          retval += "\njj_la1[" + data.addMaskIndex() + "] = jj_gen;";
          data.addMaskVals(tokenMask);
        }
        retval += actions[index];
    }
    for (int i = 0; i < indentAmt; i++) {
      retval += "\u0002\n}";
    }

    return retval;

  }

  protected abstract String generateHeaderMethod(ParserData data, BNFProduction p, Token t, String parserName);

  protected abstract void genStackCheck(boolean voidReturn);

  protected abstract void genJavaCodeProduction(ParserData data, JavaCodeProduction production);

  /**
   * The phase 1 routines generates their output into String's and dumps these String's once for
   * each method. These String's contain the special characters '\u0001' to indicate a positive
   * indent, and '\u0002' to indicate a negative indent. '\n' is used to indicate a line terminator.
   * The characters '\u0003' and '\u0004' are used to delineate portions of text where '\n's should
   * not be followed by an indentation.
   */
  private void buildPhase1Routine(ParserData data, BNFProduction p) {
    Token t = p.getReturnTypeTokens().get(0);

    boolean voidReturn = false;
    if (t.kind == JavaCCParserConstants.VOID) {
      voidReturn = true;
    }
    String error_ret = generateHeaderMethod(data, p, t, data.getParserName());

    genCode(" {");

    if ((Options.booleanValue(JavaCC.JJPARSER_CPP_STOP_ON_FIRST_ERROR) && (error_ret != null))
        || ((Options.getDepthLimit() > 0) && !voidReturn && !isJavaLanguage())) {
      genCode(error_ret);
    } else {
      error_ret = null;
    }

    genStackCheck(voidReturn);


    int indentamt = 4;
    if (Options.getDebugParser()) {
      genCodeLine("");
      if (isJavaLanguage()) {
        genCodeLine("    trace_call(\"" + Encoding.escapeUnicode(p.getLhs()) + "\");");
      } else {
        genCodeLine("    JJEnter<std::function<void()>> jjenter([this]() {trace_call  (\""
            + Encoding.escapeUnicode(p.getLhs()) + "\"); });");
        genCodeLine("    JJExit <std::function<void()>> jjexit ([this]() {trace_return(\""
            + Encoding.escapeUnicode(p.getLhs()) + "\"); });");
      }
      genCodeLine("    try {");
      indentamt = 6;
    }

    if (p.getDeclarationTokens().size() != 0) {
      genTokenSetup((p.getDeclarationTokens().get(0)));
      JavaCCToken.setRow();
      for (Iterator<Token> it = p.getDeclarationTokens().iterator(); it.hasNext();) {
        t = it.next();
        genToken(t);
      }
      genTrailingComments(t);
    }

    String code = phase1ExpansionGen(data, p.getExpansion());

    char ch = ' ';
    char prevChar;
    boolean indentOn = true;
    for (int i = 0; i < code.length(); i++) {
      prevChar = ch;
      ch = code.charAt(i);
      if ((ch == '\n') && (prevChar == '\r')) {
        // do nothing - we've already printed a new line for the '\r'
        // during the previous iteration.
      } else if ((ch == '\n') || (ch == '\r')) {
        if (indentOn) {
          genCodeLine("");
          for (int i0 = 0; i0 < indentamt; i0++) {
            genCode(" ");
          }
        } else {
          genCodeLine("");
        }
      } else if (ch == '\u0001') {
        indentamt += 2;
      } else if (ch == '\u0002') {
        indentamt -= 2;
      } else if (ch == '\u0003') {
        indentOn = false;
      } else if (ch == '\u0004') {
        indentOn = true;
      } else {
        genCode(ch);
      }
    }
    genCodeLine("");

    if (p.isJumpPatched() && !voidReturn) {
      if (isJavaLanguage()) {
        // This line is required for Java!
        genCodeLine("    throw new " + "RuntimeException" + "(\"Missing return statement in function\");");
      } else {
        genCodeLine("    throw \"Missing return statement in function\";");
      }
    }
    if (Options.getDebugParser()) {
      if (isJavaLanguage()) {
        genCodeLine("    } finally {");
        genCodeLine("      trace_return(\"" + Encoding.escapeUnicode(p.getLhs()) + "\");");
      } else {
        genCodeLine("    } catch(...) { }");
      }
      if (isJavaLanguage()) {
        genCodeLine("    }");
      }
    }
    if (!isJavaLanguage() && !voidReturn) {
      genCodeLine("assert(false);");
    }


    if (error_ret != null) {
      genCodeLine("\n#undef __ERROR_RET__\n");
    }

    if (Options.getDepthLimit() > 0 && isJavaLanguage()) {
      genCodeLine(" } finally {");
      genCodeLine("   --jj_depth;");
      genCodeLine(" }");
    }
    genCodeLine("}");
    genCodeLine("");
  }

  private String phase1ExpansionGen(ParserData data, Expansion e) {
    String retval = "";
    Token t = null;
    Lookahead[] conds;
    String[] actions;
    if (e instanceof RegularExpression) {
      RegularExpression e_nrw = (RegularExpression) e;
      retval += "\n";
      if (e_nrw.lhsTokens.size() != 0) {
        genTokenSetup((e_nrw.lhsTokens.get(0)));
        for (Iterator<Token> it = e_nrw.lhsTokens.iterator(); it.hasNext();) {
          t = it.next();
          retval += getStringToPrint(t);
        }
        retval += getTrailingComments(t);
        retval += " = ";
      }
      String tail = e_nrw.rhsToken == null ? ");" : (isJavaLanguage() ? ")." : ")->") + e_nrw.rhsToken.image + ";";
      if (e_nrw.label.equals("")) {
        Object label = data.getNameOfToken(e_nrw.ordinal);
        if (label != null) {
          retval += "jj_consume_token(" + (String) label + tail;
        } else {
          retval += "jj_consume_token(" + e_nrw.ordinal + tail;
        }
      } else {
        retval += "jj_consume_token(" + e_nrw.label + tail;
      }

      if (!isJavaLanguage() && Options.booleanValue(JavaCC.JJPARSER_CPP_STOP_ON_FIRST_ERROR)) {
        retval += "\n    { if (hasError) { return __ERROR_RET__; } }\n";
      }

    } else if (e instanceof NonTerminal) {
      NonTerminal e_nrw = (NonTerminal) e;
      retval += "\n";
      if (e_nrw.getLhsTokens().size() != 0) {
        genTokenSetup((e_nrw.getLhsTokens().get(0)));
        for (Iterator<Token> it = e_nrw.getLhsTokens().iterator(); it.hasNext();) {
          t = it.next();
          retval += getStringToPrint(t);
        }
        retval += getTrailingComments(t);
        retval += " = ";
      }
      retval += e_nrw.getName() + "(";
      if (e_nrw.getArgumentTokens().size() != 0) {
        genTokenSetup((e_nrw.getArgumentTokens().get(0)));
        for (Iterator<Token> it = e_nrw.getArgumentTokens().iterator(); it.hasNext();) {
          t = it.next();
          retval += getStringToPrint(t);
        }
        retval += getTrailingComments(t);
      }
      retval += ");";
      if (!isJavaLanguage() && Options.booleanValue(JavaCC.JJPARSER_CPP_STOP_ON_FIRST_ERROR)) {
        retval += "\n    { if (hasError) { return __ERROR_RET__; } }\n";
      }
    } else if (e instanceof Action) {
      Action e_nrw = (Action) e;
      retval += "\u0003\n";
      if (e_nrw.getActionTokens().size() != 0) {
        genTokenSetup((e_nrw.getActionTokens().get(0)));
        JavaCCToken.setColumn();
        for (Iterator<Token> it = e_nrw.getActionTokens().iterator(); it.hasNext();) {
          t = it.next();
          retval += getStringToPrint(t);
        }
        retval += getTrailingComments(t);
      }
      retval += "\u0004";
    } else if (e instanceof Choice) {
      Choice e_nrw = (Choice) e;
      conds = new Lookahead[e_nrw.getChoices().size()];
      actions = new String[e_nrw.getChoices().size() + 1];
      actions[e_nrw.getChoices().size()] = "\n" + "jj_consume_token(-1);\n"
          + (isJavaLanguage() ? "throw new ParseException();"
              : ("errorHandler->parseError(token, getToken(1), __FUNCTION__), hasError = true;"
                  + (Options.booleanValue(JavaCC.JJPARSER_CPP_STOP_ON_FIRST_ERROR) ? "return __ERROR_RET__;\n" : "")));

      // In previous line, the "throw" never throws an exception since the
      // evaluation of jj_consume_token(-1) causes ParseException to be
      // thrown first.
      Sequence nestedSeq;
      for (int i = 0; i < e_nrw.getChoices().size(); i++) {
        nestedSeq = (Sequence) (e_nrw.getChoices().get(i));
        actions[i] = phase1ExpansionGen(data, nestedSeq);
        conds[i] = (Lookahead) (nestedSeq.units.get(0));
      }
      retval = buildLookaheadChecker(data, conds, actions);
    } else if (e instanceof Sequence) {
      Sequence e_nrw = (Sequence) e;
      // We skip the first element in the following iteration since it is the
      // Lookahead object.
      for (int i = 1; i < e_nrw.units.size(); i++) {
        // For C++, since we are not using exceptions, we will protect all the
        // expansion choices with if (!error)
        boolean wrap_in_block = false;
        if (!data.isGenerated() && !isJavaLanguage()) {
          // for the last one, if it's an action, we will not protect it.
          Expansion elem = (Expansion) e_nrw.units.get(i);
          if (!(elem instanceof Action) || !(e.parent instanceof BNFProduction) || (i != (e_nrw.units.size() - 1))) {
            wrap_in_block = true;
            retval += "\nif (" + (isJavaLanguage() ? "true" : "!hasError") + ") {";
          }
        }
        retval += phase1ExpansionGen(data, (Expansion) (e_nrw.units.get(i)));
        if (wrap_in_block) {
          retval += "\n}";
        }
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
      retval += "\n";
      int labelIndex = data.nextGenSymIndex();
      if (isJavaLanguage()) {
        retval += "label_" + labelIndex + ":\n";
      }
      retval += "while (" + (isJavaLanguage() ? "true" : "!hasError") + ") {\u0001";
      retval += phase1ExpansionGen(data, nested_e);
      conds = new Lookahead[1];
      conds[0] = la;
      actions = new String[2];
      actions[0] = "\n;";

      if (isJavaLanguage()) {
        actions[1] = "\nbreak label_" + labelIndex + ";";
      } else {
        actions[1] = "\ngoto end_label_" + labelIndex + ";";
      }

      retval += buildLookaheadChecker(data, conds, actions);
      retval += "\u0002\n" + "}";
      if (!isJavaLanguage()) {
        retval += "\nend_label_" + labelIndex + ": ;";
      }
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
      retval += "\n";
      int labelIndex = data.nextGenSymIndex();
      if (isJavaLanguage()) {
        retval += "label_" + labelIndex + ":\n";
      }
      retval += "while (" + (isJavaLanguage() ? "true" : "!hasError") + ") {\u0001";
      conds = new Lookahead[1];
      conds[0] = la;
      actions = new String[2];
      actions[0] = "\n;";
      if (isJavaLanguage()) {
        actions[1] = "\nbreak label_" + labelIndex + ";";
      } else {
        actions[1] = "\ngoto end_label_" + labelIndex + ";";
      }
      retval += buildLookaheadChecker(data, conds, actions);
      retval += phase1ExpansionGen(data, nested_e);
      retval += "\u0002\n" + "}";
      if (!isJavaLanguage()) {
        retval += "\nend_label_" + labelIndex + ": ;";
      }
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
      conds = new Lookahead[1];
      conds[0] = la;
      actions = new String[2];
      actions[0] = phase1ExpansionGen(data, nested_e);
      actions[1] = "\n;";
      retval += buildLookaheadChecker(data, conds, actions);
    } else if (e instanceof TryBlock) {
      TryBlock e_nrw = (TryBlock) e;
      Expansion nested_e = e_nrw.exp;
      List<Token> list;
      retval += "\n";
      retval += "try {\u0001";
      retval += phase1ExpansionGen(data, nested_e);
      retval += "\u0002\n" + "}";
      for (int i = 0; i < e_nrw.catchblks.size(); i++) {
        retval += " catch (";
        list = e_nrw.types.get(i);
        if (list.size() != 0) {
          genTokenSetup((list.get(0)));
          for (Iterator<Token> it = list.iterator(); it.hasNext();) {
            t = it.next();
            retval += getStringToPrint(t);
          }
          retval += getTrailingComments(t);
        }
        retval += " ";
        t = (e_nrw.ids.get(i));
        genTokenSetup(t);
        retval += getStringToPrint(t);
        retval += getTrailingComments(t);
        retval += ") {\u0003\n";
        list = e_nrw.catchblks.get(i);
        if (list.size() != 0) {
          genTokenSetup((list.get(0)));
          JavaCCToken.setColumn();
          for (Iterator<Token> it = list.iterator(); it.hasNext();) {
            t = it.next();
            retval += getStringToPrint(t);
          }
          retval += getTrailingComments(t);
        }
        retval += "\u0004\n" + "}";
      }
      if (e_nrw.finallyblk != null) {
        if (isJavaLanguage()) {
          retval += " finally {\u0003\n";
        } else {
          retval += " finally {\u0003\n";
        }

        if (e_nrw.finallyblk.size() != 0) {
          genTokenSetup((e_nrw.finallyblk.get(0)));
          JavaCCToken.setColumn();
          for (Iterator<Token> it = e_nrw.finallyblk.iterator(); it.hasNext();) {
            t = it.next();
            retval += getStringToPrint(t);
          }
          retval += getTrailingComments(t);
        }
        retval += "\u0004\n" + "}";
      }
    }
    return retval;
  }

  private void buildPhase2Routine(ParserData data, Lookahead la, List<Phase3Data> phase3list,
      Hashtable<Expansion, Phase3Data> phase3table) {
    Expansion e = la.getLaExpansion();
    if (isJavaLanguage()) {
      genCodeLine("  private boolean jj_2" + e.internal_name + "(int xla)");
    } else {
      genCodeLine(" inline bool ", "jj_2" + e.internal_name + "(int xla)");
    }
    genCodeLine(" {");
    genCodeLine("    jj_la = xla; jj_lastpos = jj_scanpos = token;");

    String ret_suffix = "";
    if (Options.getDepthLimit() > 0) {
      ret_suffix = " && !jj_depth_error";
    }

    if (isJavaLanguage()) {
      genCodeLine("    try { return (!jj_3" + e.internal_name + "()" + ret_suffix + "); }");
      genCodeLine("    catch(LookaheadSuccess ls) { return true; }");
    } else {
      genCodeLine("    jj_done = false;");
      genCodeLine("    return (!jj_3" + e.internal_name + "() || jj_done)" + ret_suffix + ";");
    }
    if (Options.getErrorReporting()) {
      genCodeLine((isJavaLanguage() ? "    finally " : " ") + "{ jj_save("
          + (Integer.parseInt(e.internal_name.substring(1)) - 1) + ", xla); }");
    }
    genCodeLine("  }");
    genCodeLine("");
    Phase3Data p3d = new Phase3Data(e, la.getAmount());
    phase3list.add(p3d);
    phase3table.put(e, p3d);
  }

  private String genReturn(Expansion expansion, boolean value) {
    String retval = (value ? "true" : "false");
    if (Options.getDebugLookahead() && (expansion != null)) {
      String tracecode = "trace_return(\"" + Encoding.escapeUnicode(((NormalProduction) expansion.parent).getLhs())
          + "(LOOKAHEAD " + (value ? "FAILED" : "SUCCEEDED") + ")\");";
      if (Options.getErrorReporting()) {
        tracecode = "if (!jj_rescan) " + tracecode;
      }
      return "{ " + tracecode + " return " + retval + "; }";
    } else {
      return "return " + retval + ";";
    }
  }

  private String getTypeForToken() {
    return isJavaLanguage() ? "Token" : "Token*";
  }

  private String genjj_3Call(Expansion e) {
    if (e.internal_name.startsWith("jj_scan_token")) {
      return e.internal_name;
    } else {
      return "jj_3" + e.internal_name + "()";
    }
  }

  private void buildPhase3Routine(ParserData data, Phase3Data inf, boolean recursive_call) {
    Expansion e = inf.exp;
    Token t = null;
    if (e.internal_name.startsWith("jj_scan_token")) {
      return;
    }

    if (!recursive_call) {
      if (isJavaLanguage()) {
        genCodeLine("  private boolean jj_3" + e.internal_name + "()");
      } else {
        genCodeLine(" inline bool ", "jj_3" + e.internal_name + "()");
      }

      genCodeLine(" {");
      if (!isJavaLanguage()) {
        genCodeLine("    if (jj_done) return true;");
        if (Options.getDepthLimit() > 0) {
          genCodeLine("#define __ERROR_RET__ true");
        }
      }
      genStackCheck(false);
      data.xsp_declared = false;
      if (Options.getDebugLookahead() && (e.parent instanceof NormalProduction)) {
        genCode("    ");
        if (Options.getErrorReporting()) {
          genCode("if (!jj_rescan) ");
        }
        genCodeLine("trace_call(\"" + Encoding.escapeUnicode(((NormalProduction) e.parent).getLhs())
            + "(LOOKING AHEAD...)\");");
        data.jj3_expansion = e;
      } else {
        data.jj3_expansion = null;
      }
    }
    if (e instanceof RegularExpression) {
      RegularExpression e_nrw = (RegularExpression) e;
      if (e_nrw.label.equals("")) {
        Object label = data.getNameOfToken(e_nrw.ordinal);
        if (label != null) {
          genCodeLine("    if (jj_scan_token(" + (String) label + ")) " + genReturn(data.jj3_expansion, true));
        } else {
          genCodeLine("    if (jj_scan_token(" + e_nrw.ordinal + ")) " + genReturn(data.jj3_expansion, true));
        }
      } else {
        genCodeLine("    if (jj_scan_token(" + e_nrw.label + ")) " + genReturn(data.jj3_expansion, true));
      }
    } else if (e instanceof NonTerminal) {
      // All expansions of non-terminals have the "name" fields set. So
      // there's no need to check it below for "e_nrw" and "ntexp". In
      // fact, we rely here on the fact that the "name" fields of both these
      // variables are the same.
      NonTerminal e_nrw = (NonTerminal) e;
      NormalProduction ntprod = data.getProduction(e_nrw.getName());
      Expansion ntexp = ntprod.getExpansion();
      genCodeLine("    if (" + genjj_3Call(ntexp) + ") " + genReturn(data.jj3_expansion, true));
    } else if (e instanceof Choice) {
      Sequence nested_seq;
      Choice e_nrw = (Choice) e;
      if (e_nrw.getChoices().size() != 1) {
        if (!data.xsp_declared) {
          data.xsp_declared = true;
          genCodeLine("    " + getTypeForToken() + " xsp;");
        }
        genCodeLine("    xsp = jj_scanpos;");
      }
      for (int i = 0; i < e_nrw.getChoices().size(); i++) {
        nested_seq = (Sequence) (e_nrw.getChoices().get(i));
        Lookahead la = (Lookahead) (nested_seq.units.get(0));
        if (la.getActionTokens().size() != 0) {
          // We have semantic lookahead that must be evaluated.
          data.setLookAheadNeeded(true);
          genCodeLine("    jj_lookingAhead = true;");
          genCode("    jj_semLA = ");
          genTokenSetup((la.getActionTokens().get(0)));
          for (Iterator<Token> it = la.getActionTokens().iterator(); it.hasNext();) {
            t = it.next();
            genToken(t);
          }
          genTrailingComments(t);
          genCodeLine(";");
          genCodeLine("    jj_lookingAhead = false;");
        }
        genCode("    if (");
        if (la.getActionTokens().size() != 0) {
          genCode("!jj_semLA || ");
        }
        if (i != (e_nrw.getChoices().size() - 1)) {
          genCodeLine(genjj_3Call(nested_seq) + ") {");
          genCodeLine("    jj_scanpos = xsp;");
        } else {
          genCodeLine(genjj_3Call(nested_seq) + ") " + genReturn(data.jj3_expansion, true));
        }
      }
      for (int i = 1; i < e_nrw.getChoices().size(); i++) {
        genCodeLine(" }");
      }
    } else if (e instanceof Sequence) {
      Sequence e_nrw = (Sequence) e;
      // We skip the first element in the following iteration since it is the
      // Lookahead object.
      int cnt = inf.count;
      for (int i = 1; i < e_nrw.units.size(); i++) {
        Expansion eseq = (Expansion) (e_nrw.units.get(i));
        buildPhase3Routine(data, new Phase3Data(eseq, cnt), true);
        cnt -= ParserGenerator.minimumSize(eseq, data);
        if (cnt <= 0) {
          break;
        }
      }
    } else if (e instanceof TryBlock) {
      TryBlock e_nrw = (TryBlock) e;
      buildPhase3Routine(data, new Phase3Data(e_nrw.exp, inf.count), true);
    } else if (e instanceof OneOrMore) {
      if (!data.xsp_declared) {
        data.xsp_declared = true;
        genCodeLine("    " + getTypeForToken() + " xsp;");
      }
      OneOrMore e_nrw = (OneOrMore) e;
      Expansion nested_e = e_nrw.expansion;
      // genCodeLine(" if (jj_3" + nested_e.internal_name + "()) " + genReturn(true));
      genCodeLine("    if (" + genjj_3Call(nested_e) + ") " + genReturn(data.jj3_expansion, true));
      // genCodeLine(" if (jj_la == 0 && jj_scanpos == jj_lastpos) " +
      // genReturn(false));
      genCodeLine("    while (true) {");
      genCodeLine("      xsp = jj_scanpos;");
      // genCodeLine(" if (jj_3" + nested_e.internal_name + "()) { jj_scanpos = xsp;
      // break; }");
      genCodeLine("      if (" + genjj_3Call(nested_e) + ") { jj_scanpos = xsp; break; }");
      // genCodeLine(" if (jj_la == 0 && jj_scanpos == jj_lastpos) " +
      // genReturn(false));
      genCodeLine("    }");
    } else if (e instanceof ZeroOrMore) {
      if (!data.xsp_declared) {
        data.xsp_declared = true;
        genCodeLine("    " + getTypeForToken() + " xsp;");
      }
      ZeroOrMore e_nrw = (ZeroOrMore) e;
      Expansion nested_e = e_nrw.expansion;
      genCodeLine("    while (true) {");
      genCodeLine("      xsp = jj_scanpos;");
      // genCodeLine(" if (jj_3" + nested_e.internal_name + "()) { jj_scanpos = xsp;
      // break; }");
      genCodeLine("      if (" + genjj_3Call(nested_e) + ") { jj_scanpos = xsp; break; }");
      // genCodeLine(" if (jj_la == 0 && jj_scanpos == jj_lastpos) " +
      // genReturn(false));
      genCodeLine("    }");
    } else if (e instanceof ZeroOrOne) {
      if (!data.xsp_declared) {
        data.xsp_declared = true;
        genCodeLine("    " + getTypeForToken() + " xsp;");
      }
      ZeroOrOne e_nrw = (ZeroOrOne) e;
      Expansion nested_e = e_nrw.expansion;
      genCodeLine("    xsp = jj_scanpos;");
      genCodeLine("    if (" + genjj_3Call(nested_e) + ") jj_scanpos = xsp;");
    }
    if (!recursive_call) {
      genCodeLine("    " + genReturn(data.jj3_expansion, false));

      if (Options.getDepthLimit() > 0 && isJavaLanguage()) {
        genCodeLine(" } finally {");
        genCodeLine("   --jj_depth;");
        genCodeLine(" }");
      }
      if (!isJavaLanguage() && (Options.getDepthLimit() > 0)) {
        genCodeLine("#undef __ERROR_RET__");
      }
      genCodeLine("  }");
      genCodeLine("");
    }
  }

  private static int minimumSize(Expansion e, ParserData data) {
    return ParserGenerator.minimumSize(e, Integer.MAX_VALUE, data);
  }

  /*
   * Returns the minimum number of tokens that can parse to this expansion.
   */
  private static int minimumSize(Expansion e, int oldMin, ParserData data) {
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
      retval = ParserGenerator.minimumSize(ntexp, data);
    } else if (e instanceof Choice) {
      int min = oldMin;
      Expansion nested_e;
      Choice e_nrw = (Choice) e;
      for (int i = 0; (min > 1) && (i < e_nrw.getChoices().size()); i++) {
        nested_e = (Expansion) (e_nrw.getChoices().get(i));
        int min1 = ParserGenerator.minimumSize(nested_e, min, data);
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
        int mineseq = ParserGenerator.minimumSize(eseq, data);
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
      retval = ParserGenerator.minimumSize(e_nrw.exp, data);
    } else if (e instanceof OneOrMore) {
      OneOrMore e_nrw = (OneOrMore) e;
      retval = ParserGenerator.minimumSize(e_nrw.expansion, data);
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

  protected final void build(ParserData data) {
    for (NormalProduction p : data.getProductions()) {
      if (p instanceof JavaCodeProduction) {
        genJavaCodeProduction(data, (JavaCodeProduction) p);
      } else {
        buildPhase1Routine(data, (BNFProduction) p);
      }
    }
  }

  protected final void build2(ParserData data) {
    List<Phase3Data> phase3list = new ArrayList<>();
    Hashtable<Expansion, Phase3Data> phase3table = new Hashtable<>();
    for (Lookahead element : data.phase2list) {
      buildPhase2Routine(data, (element), phase3list, phase3table);
    }

    int phase3index = 0;
    while (phase3index < phase3list.size()) {
      for (; phase3index < phase3list.size(); phase3index++) {
        setupPhase3Builds(data, (phase3list.get(phase3index)), phase3list, phase3table);
      }
    }

    for (Phase3Data phase3Data : phase3table.values()) {
      buildPhase3Routine(data, phase3Data, false);
    }
  }

  protected final void genTrailingComments(Token t) {
    getSource().append(getTrailingComments(t));
  }

  protected final String getTrailingComments(Token t) {
    if (t.next == null) {
      return "";
    }
    return getLeadingComments(t.next);
  }

  protected final void genLeadingComments(Token t) {
    genCode(getLeadingComments(t));
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

  protected final void genTokenOnly(Token t) {
    genCode(getStringForTokenOnly(t));
  }

  private void setupPhase3Builds(ParserData data, Phase3Data inf, List<Phase3Data> phase3list,
      Hashtable<Expansion, Phase3Data> phase3table) {
    Expansion e = inf.exp;
    if (e instanceof RegularExpression) {
      // nothing to here
    } else if (e instanceof NonTerminal) {
      // All expansions of non-terminals have the "name" fields set. So
      // there's no need to check it below for "e_nrw" and "ntexp". In
      // fact, we rely here on the fact that the "name" fields of both these
      // variables are the same.
      NonTerminal e_nrw = (NonTerminal) e;
      NormalProduction ntprod = data.getProduction(e_nrw.getName());
      generate3R(data, ntprod.getExpansion(), inf, phase3list, phase3table);
    } else if (e instanceof Choice) {
      Choice e_nrw = (Choice) e;
      for (Object element : e_nrw.getChoices()) {
        generate3R(data, (Expansion) (element), inf, phase3list, phase3table);
      }
    } else if (e instanceof Sequence) {
      Sequence e_nrw = (Sequence) e;
      // We skip the first element in the following iteration since it is the
      // Lookahead object.
      int cnt = inf.count;
      for (int i = 1; i < e_nrw.units.size(); i++) {
        Expansion eseq = (Expansion) (e_nrw.units.get(i));
        setupPhase3Builds(data, new Phase3Data(eseq, cnt), phase3list, phase3table);
        cnt -= ParserGenerator.minimumSize(eseq, data);
        if (cnt <= 0) {
          break;
        }
      }
    } else if (e instanceof TryBlock) {
      TryBlock e_nrw = (TryBlock) e;
      setupPhase3Builds(data, new Phase3Data(e_nrw.exp, inf.count), phase3list, phase3table);
    } else if (e instanceof OneOrMore) {
      OneOrMore e_nrw = (OneOrMore) e;
      generate3R(data, e_nrw.expansion, inf, phase3list, phase3table);
    } else if (e instanceof ZeroOrMore) {
      ZeroOrMore e_nrw = (ZeroOrMore) e;
      generate3R(data, e_nrw.expansion, inf, phase3list, phase3table);
    } else if (e instanceof ZeroOrOne) {
      ZeroOrOne e_nrw = (ZeroOrOne) e;
      generate3R(data, e_nrw.expansion, inf, phase3list, phase3table);
    }
  }


  private void generate3R(ParserData data, Expansion e, Phase3Data inf, List<Phase3Data> phase3list,
      Hashtable<Expansion, Phase3Data> phase3table) {
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

      e.internal_name =
          "R_" + e.getProductionName() + "_" + e.getLine() + "_" + e.getColumn() + "_" + data.nextGenSymIndex();
    }

    Phase3Data p3d = phase3table.get(e);
    if ((p3d == null) || (p3d.count < inf.count)) {
      p3d = new Phase3Data(e, inf.count);
      phase3list.add(p3d);
      phase3table.put(e, p3d);
    }
  }


  /**
   * This class stores information to pass from phase 2 to phase 3.
   */
  private class Phase3Data {

    // This is the expansion to generate the jj3 method for.
    final Expansion exp;

    // This is the number of tokens that can still be consumed. This number is used to limit the
    // number of jj3 methods generated.
    final int count;

    public Phase3Data(Expansion e, int c) {
      this.exp = e;
      this.count = c;
    }
  }
}
