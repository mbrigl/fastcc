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

package org.javacc.generator.cpp;

import org.fastcc.source.CppWriter;
import org.fastcc.utils.Encoding;
import org.javacc.JavaCC;
import org.javacc.generator.ParserData;
import org.javacc.generator.ParserGenerator;
import org.javacc.parser.Action;
import org.javacc.parser.BNFProduction;
import org.javacc.parser.Choice;
import org.javacc.parser.CodeProduction;
import org.javacc.parser.Expansion;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.JavaCCParserConstants;
import org.javacc.parser.Lookahead;
import org.javacc.parser.NonTerminal;
import org.javacc.parser.NormalProduction;
import org.javacc.parser.OneOrMore;
import org.javacc.parser.Options;
import org.javacc.parser.RegularExpression;
import org.javacc.parser.Sequence;
import org.javacc.parser.Token;
import org.javacc.parser.TryBlock;
import org.javacc.parser.ZeroOrMore;
import org.javacc.parser.ZeroOrOne;
import org.javacc.semantic.Semanticize;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generate the parser.
 */
public class CppParserGenerator extends ParserGenerator {

  @Override
  protected void generate(ParserData data) throws IOException {
    Function<Object, String> writeMasks = i -> {
      int index = (Integer) i;
      return data.maskVals().isEmpty() ? ""
          : String.format("static unsigned int jj_la1_%s[] = {%s};", index,
              data.maskVals().stream().map(v -> "0x" + Integer.toHexString(v[index])).collect(Collectors.joining(",")));
    };
    Function<Object, String> writeProductionsImpl = i -> {
      StringWriter writer = new StringWriter();
      PrintWriter printer = new PrintWriter(writer);
      for (NormalProduction p : data.getProductions()) {
        if (p instanceof CodeProduction) {
          JavaCCErrors.semantic_error("Cannot use JAVACODE productions with C++ output (yet).");
        } else {
          String code = generatePhase1Expansion(data, p.getExpansion());
          generatePhase1((BNFProduction) p, code, data.getParserName(), printer);
        }
      }
      return writer.toString();
    };
    Function<Object, String> writeJJ_3Call = i -> {
      int index = (Integer) i;
      return "" + (index + 1);
    };

    CppWriter writer = new CppWriter(data.getParserName());
    writer.setOption("LOOKAHEAD_NEEDED", data.isLookAheadNeeded());
    writer.setOption("IS_GENERATED", data.isGenerated());
    writer.setOption("jj2Index", data.jj2Index());
    writer.setOption("maskIndex", data.maskIndex());
    writer.setOption("tokenCount", data.getTokenCount());
    writer.setOption("tokenMaskSize", ((data.getTokenCount() - 1) / 32) + 1);
    writer.setOption("writeMasks", writeMasks);
    writer.setOption("writeProductionsImpl", writeProductionsImpl);
    writer.setOption("writeJJ_3Call", writeJJ_3Call);
    writer.writeTemplate("/templates/cpp/Parser.template");

    generateHeader(data, writer);
    saveOutput(writer);
  }

  private void generateHeader(ParserData data, CppWriter cpp) throws IOException {
    cpp.switchToHeader();

    Function<Object, String> writeProductions = i -> {
      StringWriter writer = new StringWriter();
      PrintWriter printer = new PrintWriter(writer);
      for (NormalProduction p : data.getProductions()) {
        if (p instanceof BNFProduction) {
          printer.printf("  void %s();\n", p.getLhs());
        }
      }
      return writer.toString();
    };
    Function<Object, String> writeLoakAheads = i -> {
      StringWriter writer = new StringWriter();
      PrintWriter printer = new PrintWriter(writer);
      for (Lookahead la : data.getLoakaheads()) {
        generatePhase2(la.getLaExpansion(), printer);
      }
      return writer.toString();
    };
    Function<Object, String> writeExpansions = i -> {
      StringWriter writer = new StringWriter();
      PrintWriter printer = new PrintWriter(writer);
      for (Expansion e : data.getExpansions()) {
        generatePhase3Routine(data, e, data.getCount(e), printer);
      }
      return writer.toString();
    };

    cpp.setOption("writeProductions", writeProductions);
    cpp.setOption("writeLoakAheads", writeLoakAheads);
    cpp.setOption("writeExpansions", writeExpansions);
    cpp.writeTemplate("/templates/cpp/Parser.h.template");
  }

  /**
   * The phase 1 routines generates their output into String's and dumps these String's once for
   * each method. These String's contain the special characters '\u0001' to indicate a positive
   * indent, and '\u0002' to indicate a negative indent. '\n' is used to indicate a line terminator.
   * The characters '\u0003' and '\u0004' are used to delineate portions of text where '\n's should
   * not be followed by an indentation.
   */
  private void generatePhase1(BNFProduction p, String code, String parserName, PrintWriter writer) {
    Token t = p.getReturnTypeTokens().get(0);

    boolean voidReturn = (t.kind == JavaCCParserConstants.VOID);
    String error_ret = genHeaderMethod(p, t, parserName, writer);

    writer.print(" {");

    if ((Options.booleanValue(JavaCC.JJPARSER_CPP_STOP_ON_FIRST_ERROR) && (error_ret != null))
        || ((Options.getDepthLimit() > 0) && !voidReturn)) {
      writer.print(error_ret);
    } else {
      error_ret = null;
    }

    genStackCheck(voidReturn, writer);

    int indentamt = 4;
    if (Options.getDebugParser()) {
      writer.println();
      writer.println("    JJEnter<std::function<void()>> jjenter([this]() {trace_call  (\""
          + Encoding.escapeUnicode(p.getLhs()) + "\"); });");
      writer.println("    JJExit <std::function<void()>> jjexit ([this]() {trace_return(\""
          + Encoding.escapeUnicode(p.getLhs()) + "\"); });");
      writer.println("    try {");
      indentamt = 6;
    }

    if (p.getDeclarationTokens().size() != 0) {
      genTokenSetup((p.getDeclarationTokens().get(0)));
      for (Iterator<Token> it = p.getDeclarationTokens().iterator(); it.hasNext();) {
        t = it.next();
        writer.print(getStringToPrint(t));
      }
      writer.print(getTrailingComments(t));
    }

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
          writer.println();
          for (int i0 = 0; i0 < indentamt; i0++) {
            writer.print(" ");
          }
        } else {
          writer.println();
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
        writer.print(ch);
      }
    }
    writer.println();

    if (p.isJumpPatched() && !voidReturn) {
      writer.println("    throw \"Missing return statement in function\";");
    }
    if (Options.getDebugParser()) {
      writer.println("    } catch(...) { }");
    }
    if (!voidReturn) {
      writer.println("assert(false);");
    }
    if (error_ret != null) {
      writer.println("\n#undef __ERROR_RET__");
    }
    writer.println("}");
    writer.println();
  }

  private String generatePhase1Expansion(ParserData data, Expansion e) {
    String retval = "";
    Token t = null;
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
      String tail = e_nrw.rhsToken == null ? ");" : ")->" + e_nrw.rhsToken.image + ";";
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

      if (Options.booleanValue(JavaCC.JJPARSER_CPP_STOP_ON_FIRST_ERROR)) {
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
      if (Options.booleanValue(JavaCC.JJPARSER_CPP_STOP_ON_FIRST_ERROR)) {
        retval += "\n    { if (hasError) { return __ERROR_RET__; } }\n";
      }
    } else if (e instanceof Action) {
      Action e_nrw = (Action) e;
      retval += "\u0003\n";
      if (e_nrw.getActionTokens().size() != 0) {
        genTokenSetup((e_nrw.getActionTokens().get(0)));
        for (Iterator<Token> it = e_nrw.getActionTokens().iterator(); it.hasNext();) {
          t = it.next();
          retval += getStringToPrint(t);
        }
        retval += getTrailingComments(t);
      }
      retval += "\u0004";
    } else if (e instanceof Choice) {
      Choice e_nrw = (Choice) e;
      Lookahead[] conds = data.getLoakaheads(e);
      String[] actions = new String[e_nrw.getChoices().size() + 1];
      actions[e_nrw.getChoices().size()] =
          "\n" + "jj_consume_token(-1);\nerrorHandler->parseError(token, getToken(1), __FUNCTION__), hasError = true;"
              + (Options.booleanValue(JavaCC.JJPARSER_CPP_STOP_ON_FIRST_ERROR) ? "return __ERROR_RET__;\n" : "");

      // In previous line, the "throw" never throws an exception since the
      // evaluation of jj_consume_token(-1) causes ParseException to be
      // thrown first.
      Sequence nestedSeq;
      for (int i = 0; i < e_nrw.getChoices().size(); i++) {
        nestedSeq = (Sequence) (e_nrw.getChoices().get(i));
        actions[i] = generatePhase1Expansion(data, nestedSeq);
      }
      retval = genLookaheadChecker(data, conds, actions);
    } else if (e instanceof Sequence) {
      Sequence e_nrw = (Sequence) e;
      // We skip the first element in the following iteration since it is the
      // Lookahead object.
      for (int i = 1; i < e_nrw.units.size(); i++) {
        // For C++, since we are not using exceptions, we will protect all the
        // expansion choices with if (!error)
        boolean wrap_in_block = false;
        if (!data.isGenerated()) {
          // for the last one, if it's an action, we will not protect it.
          Expansion elem = (Expansion) e_nrw.units.get(i);
          if (!(elem instanceof Action) || !(e.parent instanceof BNFProduction) || (i != (e_nrw.units.size() - 1))) {
            wrap_in_block = true;
            retval += "\nif (!hasError) {";
          }
        }
        retval += generatePhase1Expansion(data, (Expansion) (e_nrw.units.get(i)));
        if (wrap_in_block) {
          retval += "\n}";
        }
      }
    } else if (e instanceof OneOrMore) {
      OneOrMore e_nrw = (OneOrMore) e;
      Expansion nested_e = e_nrw.expansion;
      retval += "\n";
      int labelIndex = nextLabelIndex();
      retval += "while (!hasError) {\u0001";
      retval += generatePhase1Expansion(data, nested_e);
      Lookahead[] conds = data.getLoakaheads(e);
      String[] actions = { "\n;", "\ngoto end_label_" + labelIndex + ";" };

      retval += genLookaheadChecker(data, conds, actions);
      retval += "\u0002\n" + "}";
      retval += "\nend_label_" + labelIndex + ": ;";
    } else if (e instanceof ZeroOrMore) {
      ZeroOrMore e_nrw = (ZeroOrMore) e;
      Expansion nested_e = e_nrw.expansion;
      retval += "\n";
      int labelIndex = nextLabelIndex();
      retval += "while (!hasError) {\u0001";
      Lookahead[] conds = data.getLoakaheads(e);
      String[] actions = { "\n;", "\ngoto end_label_" + labelIndex + ";" };
      retval += genLookaheadChecker(data, conds, actions);
      retval += generatePhase1Expansion(data, nested_e);
      retval += "\u0002\n" + "}";
      retval += "\nend_label_" + labelIndex + ": ;";
    } else if (e instanceof ZeroOrOne) {
      ZeroOrOne e_nrw = (ZeroOrOne) e;
      Expansion nested_e = e_nrw.expansion;
      Lookahead[] conds = data.getLoakaheads(e);
      String[] actions = { generatePhase1Expansion(data, nested_e), "\n;" };
      retval += genLookaheadChecker(data, conds, actions);
    } else if (e instanceof TryBlock) {
      TryBlock e_nrw = (TryBlock) e;
      Expansion nested_e = e_nrw.exp;
      List<Token> list;
      retval += "\n";
      retval += "try {\u0001";
      retval += generatePhase1Expansion(data, nested_e);
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
          for (Iterator<Token> it = list.iterator(); it.hasNext();) {
            t = it.next();
            retval += getStringToPrint(t);
          }
          retval += getTrailingComments(t);
        }
        retval += "\u0004\n" + "}";
      }
      if (e_nrw.finallyblk != null) {
        retval += " finally {\u0003\n";

        if (e_nrw.finallyblk.size() != 0) {
          genTokenSetup((e_nrw.finallyblk.get(0)));
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

  private String genLookaheadChecker(ParserData data, Lookahead[] conds, String[] actions) {
    // The state variables.
    LookaheadState state = LookaheadState.NOOPENSTM;
    int indentAmt = 0;
    boolean[] casedValues = new boolean[data.getTokenCount()];
    String retval = "";
    Lookahead la = null;
    Token t = null;

    // Iterate over all the conditions.
    int index = 0;
    boolean jj2LA = false;
    while (index < conds.length) {

      la = conds[index];
      jj2LA = false;

      if ((la.getAmount() == 0) || Semanticize.emptyExpansionExists(la.getLaExpansion())) {

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
                retval += "\njj_la1[" + data.getIndex(la) + "] = jj_gen;";
              }
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
              retval += "\u0002\n" + "} else {\u0001";
              //$FALL-THROUGH$ Control flows through to next case.
            case NOOPENSTM:
              retval += "\n" + "switch (";
              if (Options.getCacheTokens()) {
                retval += "jj_nt->kind()";
                retval += ") {\u0001";
              } else {
                retval += "(jj_ntk==-1)?jj_ntk_f():jj_ntk) {\u0001";
              }
              for (int i = 0; i < data.getTokenCount(); i++) {
                casedValues[i] = false;
              }
              indentAmt++;
              // Don't need to do anything if state is OPENSWITCH.
            default:
          }
          for (int i = 0; i < data.getTokenCount(); i++) {
            if (firstSet[i] && !casedValues[i]) {
              casedValues[i] = true;
              retval += "\u0002\ncase ";
              String s = data.getNameOfToken(i);
              if (s == null) {
                retval += i;
              } else {
                retval += s;
              }
              retval += ":\u0001";
            }
          }
          retval += "{";
          retval += actions[index];
          retval += "\nbreak;\n}";
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
              retval += "\njj_la1[" + data.getIndex(la) + "] = jj_gen;";
            }
            retval += "\n" + "if (";
            indentAmt++;
        }

        String amount = Integer.toString(la.getAmount());
        if (la.getAmount() == Integer.MAX_VALUE) {
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
        state = LookaheadState.OPENIF;
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
          retval += "\njj_la1[" + data.getIndex(la) + "] = jj_gen;";
        }
        retval += actions[index];
    }
    for (int i = 0; i < indentAmt; i++) {
      retval += "\u0002\n}";
    }
    return retval;
  }

  private final String genHeaderMethod(BNFProduction p, Token t, String parserName, PrintWriter writer) {
    StringBuilder sig = new StringBuilder();
    String ret, params;

    String method_name = p.getLhs();
    boolean void_ret = false;
    boolean ptr_ret = false;

    genTokenSetup(t);
    getLeadingComments(t);
    sig.append(t.image);
    if (t.kind == JavaCCParserConstants.VOID) {
      void_ret = true;
    }
    if (t.kind == JavaCCParserConstants.STAR) {
      ptr_ret = true;
    }

    for (int i = 1; i < p.getReturnTypeTokens().size(); i++) {
      t = (p.getReturnTypeTokens().get(i));
      sig.append(getStringToPrint(t));
      if (t.kind == JavaCCParserConstants.VOID) {
        void_ret = true;
      }
      if (t.kind == JavaCCParserConstants.STAR) {
        ptr_ret = true;
      }
    }

    getTrailingComments(t);
    ret = sig.toString();

    sig.setLength(0);
    sig.append("(");
    if (p.getParameterListTokens().size() != 0) {
      genTokenSetup((p.getParameterListTokens().get(0)));
      for (Iterator<Token> it = p.getParameterListTokens().iterator(); it.hasNext();) {
        t = it.next();
        sig.append(getStringToPrint(t));
      }
      sig.append(getTrailingComments(t));
    }
    sig.append(")");
    params = sig.toString();

    // For now, just ignore comments
    writer.print("\n" + ret + " " + parserName + "::" + p.getLhs() + params);

    // Generate a default value for error return.
    String default_return;
    if (ptr_ret) {
      default_return = "NULL";
    } else if (void_ret) {
      default_return = "";
    } else {
      default_return = "0"; // 0 converts to most (all?) basic types.
    }

    StringBuilder ret_val = new StringBuilder("\n#if !defined ERROR_RET_" + method_name + "\n");
    ret_val.append("#define ERROR_RET_" + method_name + " " + default_return + "\n");
    ret_val.append("#endif\n");
    ret_val.append("#define __ERROR_RET__ ERROR_RET_" + method_name + "\n");

    return ret_val.toString();
  }

  private void genStackCheck(boolean voidReturn, PrintWriter writer) {
    if (Options.getDepthLimit() > 0) {
      if (!voidReturn) {
        writer.println("if(jj_depth_error){ return __ERROR_RET__; }");
      } else {
        writer.println("if(jj_depth_error){ return; }");
      }
      writer.println("__jj_depth_inc __jj_depth_counter(this);");
      writer.println("if(jj_depth > " + Options.getDepthLimit() + ") {");
      writer.println("  jj_depth_error = true;");
      writer.println("  jj_consume_token(-1);");
      writer.println("  errorHandler->parseError(token, getToken(1), __FUNCTION__), hasError = true;");
      if (!voidReturn) {
        writer.println("  return __ERROR_RET__;"); // Non-recoverable error
      } else {
        writer.println("  return;"); // Non-recoverable error
      }
      writer.println("}");
    }
  }

  private void generatePhase2(Expansion e, PrintWriter writer) {
    writer.println("  inline bool jj_2" + e.internal_name + "(int xla) {");
    writer.println("    jj_la = xla; jj_lastpos = jj_scanpos = token;");

    String ret_suffix = "";
    if (Options.getDepthLimit() > 0) {
      ret_suffix = " && !jj_depth_error";
    }

    writer.println("    jj_done = false;");
    writer.println("    return (!jj_3" + e.internal_name + "() || jj_done)" + ret_suffix + ";");
    if (Options.getErrorReporting()) {
      writer.println("    { jj_save(" + (Integer.parseInt(e.internal_name.substring(1)) - 1) + ", xla); }");
    }
    writer.println("  }");
    writer.println();
  }

  private void generatePhase3Routine(ParserData data, Expansion e, int count, PrintWriter writer) {
    if (e.internal_name.startsWith("jj_scan_token")) {
      return;
    }

    writer.println(" inline bool jj_3" + e.internal_name + "()");
    writer.println(" {\n");
    writer.println("    if (jj_done) return true;");
    if (Options.getDepthLimit() > 0) {
      writer.println("#define __ERROR_RET__ true");
    }
    genStackCheck(false, writer);
    boolean xsp_declared = false;
    Expansion jj3_expansion = null;
    if (Options.getDebugLookahead() && (e.parent instanceof NormalProduction)) {
      String prefix = "    ";
      if (Options.getErrorReporting()) {
        prefix += "if (!jj_rescan) ";
      }
      writer.println(prefix + "trace_call(\"" + Encoding.escapeUnicode(((NormalProduction) e.parent).getLhs())
          + "(LOOKING AHEAD...)\");");
      jj3_expansion = e;
    }

    buildPhase3RoutineRecursive(data, jj3_expansion, xsp_declared, e, count, writer);

    writer.println("    " + genReturn(jj3_expansion, false));
    if (Options.getDepthLimit() > 0) {
      writer.println("#undef __ERROR_RET__");
    }
    writer.println("  }");
    writer.println();
  }

  private boolean buildPhase3RoutineRecursive(ParserData data, Expansion jj3_expansion, boolean xsp_declared,
      Expansion e, int count, PrintWriter writer) {
    if (e.internal_name.startsWith("jj_scan_token")) {
      return xsp_declared;
    }
    Token t = null;
    if (e instanceof RegularExpression) {
      RegularExpression e_nrw = (RegularExpression) e;
      if (e_nrw.label.equals("")) {
        Object label = data.getNameOfToken(e_nrw.ordinal);
        if (label != null) {
          writer.println("    if (jj_scan_token(" + (String) label + ")) " + genReturn(jj3_expansion, true));
        } else {
          writer.println("    if (jj_scan_token(" + e_nrw.ordinal + ")) " + genReturn(jj3_expansion, true));
        }
      } else {
        writer.println("    if (jj_scan_token(" + e_nrw.label + ")) " + genReturn(jj3_expansion, true));
      }
    } else if (e instanceof NonTerminal) {
      // All expansions of non-terminals have the "name" fields set. So
      // there's no need to check it below for "e_nrw" and "ntexp". In
      // fact, we rely here on the fact that the "name" fields of both these
      // variables are the same.
      NonTerminal e_nrw = (NonTerminal) e;
      NormalProduction ntprod = data.getProduction(e_nrw.getName());
      Expansion ntexp = ntprod.getExpansion();
      writer.println("    if (" + genjj_3Call(ntexp) + ") " + genReturn(jj3_expansion, true));
    } else if (e instanceof Choice) {
      Sequence nested_seq;
      Choice e_nrw = (Choice) e;
      if (e_nrw.getChoices().size() != 1) {
        if (!xsp_declared) {
          xsp_declared = true;
          writer.println("    Token* xsp;");
        }
        writer.println("    xsp = jj_scanpos;");
      }
      for (int i = 0; i < e_nrw.getChoices().size(); i++) {
        nested_seq = (Sequence) (e_nrw.getChoices().get(i));
        Lookahead la = (Lookahead) (nested_seq.units.get(0));
        if (la.getActionTokens().size() != 0) {
          writer.println("    jj_lookingAhead = true;");
          writer.print("    jj_semLA = ");
          genTokenSetup((la.getActionTokens().get(0)));
          for (Iterator<Token> it = la.getActionTokens().iterator(); it.hasNext();) {
            t = it.next();
            writer.print(getStringToPrint(t));
          }
          writer.print(getTrailingComments(t));
          writer.println(";");
          writer.println("    jj_lookingAhead = false;");
        }
        writer.print("    if (");
        if (la.getActionTokens().size() != 0) {
          writer.print("!jj_semLA || ");
        }
        if (i != (e_nrw.getChoices().size() - 1)) {
          writer.println(genjj_3Call(nested_seq) + ") {");
          writer.println("    jj_scanpos = xsp;");
        } else {
          writer.println(genjj_3Call(nested_seq) + ") " + genReturn(jj3_expansion, true));
        }
      }
      for (int i = 1; i < e_nrw.getChoices().size(); i++) {
        writer.println(" }");
      }
    } else if (e instanceof Sequence) {
      Sequence e_nrw = (Sequence) e;
      // We skip the first element in the following iteration since it is the
      // Lookahead object.
      int cnt = count;
      for (int i = 1; i < e_nrw.units.size(); i++) {
        Expansion eseq = (Expansion) (e_nrw.units.get(i));
        xsp_declared = buildPhase3RoutineRecursive(data, jj3_expansion, xsp_declared, eseq, cnt, writer);
        cnt -= ParserGenerator.minimumSize(data, eseq);
        if (cnt <= 0) {
          break;
        }
      }
    } else if (e instanceof TryBlock) {
      TryBlock e_nrw = (TryBlock) e;
      xsp_declared = buildPhase3RoutineRecursive(data, jj3_expansion, xsp_declared, e_nrw.exp, count, writer);
    } else if (e instanceof OneOrMore) {
      if (!xsp_declared) {
        xsp_declared = true;
        writer.println("    Token* xsp;");
      }
      OneOrMore e_nrw = (OneOrMore) e;
      Expansion nested_e = e_nrw.expansion;
      writer.println("    if (" + genjj_3Call(nested_e) + ") " + genReturn(jj3_expansion, true));
      writer.println("    while (true) {");
      writer.println("      xsp = jj_scanpos;");
      writer.println("      if (" + genjj_3Call(nested_e) + ") { jj_scanpos = xsp; break; }");
      writer.println("    }");
    } else if (e instanceof ZeroOrMore) {
      if (!xsp_declared) {
        xsp_declared = true;
        writer.println("    Token* xsp;");
      }
      ZeroOrMore e_nrw = (ZeroOrMore) e;
      Expansion nested_e = e_nrw.expansion;
      writer.println("    while (true) {");
      writer.println("      xsp = jj_scanpos;");
      writer.println("      if (" + genjj_3Call(nested_e) + ") { jj_scanpos = xsp; break; }");
      writer.println("    }");
    } else if (e instanceof ZeroOrOne) {
      if (!xsp_declared) {
        xsp_declared = true;
        writer.println("    Token* xsp;");
      }
      ZeroOrOne e_nrw = (ZeroOrOne) e;
      Expansion nested_e = e_nrw.expansion;
      writer.println("    xsp = jj_scanpos;");
      writer.println("    if (" + genjj_3Call(nested_e) + ") jj_scanpos = xsp;");
    }

    return xsp_declared;
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

  private String genjj_3Call(Expansion e) {
    return e.internal_name.startsWith("jj_scan_token") ? e.internal_name : "jj_3" + e.internal_name + "()";
  }
}
