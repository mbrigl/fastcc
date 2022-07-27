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

package org.javacc.generator.java;

import org.fastcc.source.SourceWriter;
import org.javacc.JavaCC;
import org.javacc.JavaCCContext;
import org.javacc.JavaCCRequest;
import org.javacc.generator.JavaCCToken;
import org.javacc.generator.LexerGenerator;
import org.javacc.parser.Action;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.JavaCCParserConstants;
import org.javacc.parser.Options;
import org.javacc.parser.Token;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generate lexer.
 */
public class LexerJava extends LexerGenerator {

  private static final String DUMP_STATIC_VAR_DECLARATIONS_TEMPLATE_RESOURCE_URL =
      "/templates/DumpStaticVarDeclarations.template";
  private static final String BOILERPLATER_METHOD_RESOURCE_URL                   =
      "/templates/TokenManagerBoilerPlateMethods.template";


  private final JavaCCContext context;

  /**
   * Constructs an instance of {@link LexerJava}.
   *
   * @param request
   * @param context
   */
  public LexerJava(JavaCCRequest request, JavaCCContext context) {
    super(new SourceWriter(request.getParserName() + "TokenManager"), request, context);
    this.context = context;
  }

  @Override
  protected final void PrintClassHead() {
    int i, j;
    boolean bHasImport = false;

    List<String> tn = context.getToolNames();
    tn.add(JavaCC.TOOLNAME);
    genCodeLine("/* " + JavaCCToken.getIdString(tn) + " */");

    int l = 0, kind;
    i = 1;
    for (;;) {
      if (getLexerData().request.toInsertionPoint1().size() <= l) {
        break;
      }

      kind = getLexerData().request.toInsertionPoint1().get(l).kind;
      if ((kind == JavaCCParserConstants.PACKAGE) || (kind == JavaCCParserConstants.IMPORT)) {
        if (kind == JavaCCParserConstants.IMPORT) {
          bHasImport = true;
        }
        for (; i < getLexerData().request.toInsertionPoint1().size(); i++) {
          kind = getLexerData().request.toInsertionPoint1().get(i).kind;
          if ((kind == JavaCCParserConstants.SEMICOLON) || (kind == JavaCCParserConstants.ABSTRACT)
              || (kind == JavaCCParserConstants.FINAL) || (kind == JavaCCParserConstants.PUBLIC)
              || (kind == JavaCCParserConstants.CLASS) || (kind == JavaCCParserConstants.INTERFACE)) {
            this.cline = (getLexerData().request.toInsertionPoint1().get(l)).beginLine;
            this.ccol = (getLexerData().request.toInsertionPoint1().get(l)).beginColumn;
            for (j = l; j < i; j++) {
              genToken(getLexerData().request.toInsertionPoint1().get(j));
            }
            if (kind == JavaCCParserConstants.SEMICOLON) {
              genToken(getLexerData().request.toInsertionPoint1().get(j));
            }
            genCodeLine("");
            break;
          }
        }
        l = ++i;
      } else {
        break;
      }
    }

    genCodeLine("");
    genCodeLine("/** Token Manager. */");

    if (bHasImport) {
      genCodeLine("@SuppressWarnings (\"unused\")");
    }

    genClassStart(null, getLexerData().request.getParserName() + "TokenManager", new String[] {},
        new String[] { getLexerData().request.getParserName() + "Constants" });

    if ((getLexerData().request.getTokens() != null) && !getLexerData().request.getTokens().isEmpty()) {
      genTokenSetup(getLexerData().request.getTokens().get(0));
      this.ccol = 1;

      getLexerData().request.getTokens().forEach(tt -> genToken(tt));

      genCodeLine("");
    }

    genCodeLine("");
    genCodeLine("  /** Debug output. */");
    genCodeLine("  public java.io.PrintStream debugStream = System.out;");
    genCodeLine("  /** Set debug output. */");
    genCodeLine("  public void setDebugStream(java.io.PrintStream ds) { debugStream = ds; }");
  }

  @Override
  protected final void dumpAll() throws IOException {
    CheckEmptyStringMatch();
    DumpStrLiteralImages();
    DumpFillToken();
    DumpStateSets();
    DumpNonAsciiMoveMethods();
    DumpGetNextToken();

    if (Options.getDebugTokenManager()) {
      DumpStatesForKind();
    }

    if (data2().hasLoop) {
      genCodeLine("int[] jjemptyLineNo = new int[" + data2().maxLexStates + "];");
      genCodeLine("int[] jjemptyColNo = new int[" + data2().maxLexStates + "];");
      genCodeLine("boolean[] jjbeenHere = new boolean[" + data2().maxLexStates + "];");
    }

    DumpSkipActions();
    DumpMoreActions();
    DumpTokenActions();

    PrintBoilerPlate();

    String charStreamName = "JavaCharStream";

    writeTemplate(LexerJava.BOILERPLATER_METHOD_RESOURCE_URL,
        Map.of("charStreamName", charStreamName, "lexStateNameLength", getLexerData().getStateCount(),
            "defaultLexState", data2().defaultLexState, "noDfa", Options.getNoDfa(), "generatedStates",
            data2().totalNumStates));

    DumpStaticVarDeclarations(charStreamName);
    genCodeLine(/* { */ "}");

    saveOutput();
  }

  private void CheckEmptyStringMatch() {
    int i, j, k, len;
    boolean[] seen = new boolean[data2().maxLexStates];
    boolean[] done = new boolean[data2().maxLexStates];
    String cycle;
    String reList;

    Outer:
    for (i = 0; i < data2().maxLexStates; i++) {
      if (done[i] || (data2().initMatch[i] == 0) || (data2().initMatch[i] == Integer.MAX_VALUE)
          || (data2().canMatchAnyChar[i] != -1)) {
        continue;
      }

      done[i] = true;
      len = 0;
      cycle = "";
      reList = "";

      for (k = 0; k < data2().maxLexStates; k++) {
        seen[k] = false;
      }

      j = i;
      seen[i] = true;
      cycle += getLexerData().getStateName(j) + "-->";
      while (data2().newLexState[data2().initMatch[j]] != null) {
        cycle += data2().newLexState[data2().initMatch[j]];
        if (seen[j = getLexerData().getStateIndex(data2().newLexState[data2().initMatch[j]])]) {
          break;
        }

        cycle += "-->";
        done[j] = true;
        seen[j] = true;
        if ((data2().initMatch[j] == 0) || (data2().initMatch[j] == Integer.MAX_VALUE)
            || (data2().canMatchAnyChar[j] != -1)) {
          continue Outer;
        }
        if (len != 0) {
          reList += "; ";
        }
        reList += "line " + data2().rexprs[data2().initMatch[j]].getLine() + ", column "
            + data2().rexprs[data2().initMatch[j]].getColumn();
        len++;
      }

      if (data2().newLexState[data2().initMatch[j]] == null) {
        cycle += getLexerData().getStateName(getLexerData().getState(data2().initMatch[j]));
      }

      for (k = 0; k < data2().maxLexStates; k++) {
        data2().canLoop[k] |= seen[k];
      }

      data2().hasLoop = true;
      if (len == 0) {
        JavaCCErrors.warning(data2().rexprs[data2().initMatch[i]],
            "Regular expression"
                + ((data2().rexprs[data2().initMatch[i]].label.equals("")) ? ""
                    : (" for " + data2().rexprs[data2().initMatch[i]].label))
                + " can be matched by the empty string (\"\") in lexical state " + getLexerData().getStateName(i)
                + ". This can result in an endless loop of " + "empty string matches.");
      } else {
        JavaCCErrors.warning(data2().rexprs[data2().initMatch[i]],
            "Regular expression"
                + ((data2().rexprs[data2().initMatch[i]].label.equals("")) ? ""
                    : (" for " + data2().rexprs[data2().initMatch[i]].label))
                + " can be matched by the empty string (\"\") in lexical state " + getLexerData().getStateName(i)
                + ". This regular expression along with the " + "regular expressions at " + reList
                + " forms the cycle \n   " + cycle + "\ncontaining regular expressions with empty matches."
                + " This can result in an endless loop of empty string matches.");
      }
    }
  }

  private void DumpStaticVarDeclarations(String charStreamName) throws IOException {
    int i;

    genCodeLine("");
    genCodeLine("/** Lexer state names. */");
    genCodeLine("public static final String[] lexStateNames = {");
    for (i = 0; i < data2().maxLexStates; i++) {
      genCodeLine("   \"" + getLexerData().getStateName(i) + "\",");
    }
    genCodeLine("};");

    {
      genCodeLine("");
      genCodeLine("/** Lex State array. */");
      genCode("public static final int[] jjnewLexState = {");

      for (i = 0; i < data2().maxOrdinal; i++) {
        if ((i % 25) == 0) {
          genCode("\n   ");
        }

        if (data2().newLexState[i] == null) {
          genCode("-1, ");
        } else {
          genCode(getLexerData().getStateIndex(data2().newLexState[i]) + ", ");
        }
      }
      genCodeLine("\n};");
    }

    {
      // Bit vector for TOKEN
      genCode("static final long[] jjtoToken = {");
      for (i = 0; i < ((data2().maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          genCode("\n   ");
        }
        genCode("0x" + Long.toHexString(data2().toToken[i]) + "L, ");
      }
      genCodeLine("\n};");
    }

    {
      // Bit vector for SKIP
      genCode("static final long[] jjtoSkip = {");
      for (i = 0; i < ((data2().maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          genCode("\n   ");
        }
        genCode("0x" + Long.toHexString(data2().toSkip[i]) + "L, ");
      }
      genCodeLine("\n};");
    }

    {
      // Bit vector for SPECIAL
      genCode("static final long[] jjtoSpecial = {");
      for (i = 0; i < ((data2().maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          genCode("\n   ");
        }
        genCode("0x" + Long.toHexString(data2().toSpecial[i]) + "L, ");
      }
      genCodeLine("\n};");
    }

    {
      // Bit vector for MORE
      genCode("static final long[] jjtoMore = {");
      for (i = 0; i < ((data2().maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          genCode("\n   ");
        }
        genCode("0x" + Long.toHexString(data2().toMore[i]) + "L, ");
      }
      genCodeLine("\n};");
    }

    writeTemplate(LexerJava.DUMP_STATIC_VAR_DECLARATIONS_TEMPLATE_RESOURCE_URL,
        Map.of("charStreamName", charStreamName, "protected", "protected", "private", "private", "final", "final",
            "lexStateNameLength", getLexerData().getStateCount()));
  }

  // Assumes l != 0L
  private static char MaxChar(long l) {
    for (int i = 64; i-- > 0;) {
      if ((l & (1L << i)) != 0L) {
        return (char) i;
      }
    }

    return 0xffff;
  }

  private void DumpFillToken() {
    genCodeLine("protected Token jjFillToken()");
    genCodeLine("{");
    genCodeLine("   final Token t;");
    genCodeLine("   final String curTokenImage;");
    if (data2().keepLineCol) {
      genCodeLine("   final int beginLine;");
      genCodeLine("   final int endLine;");
      genCodeLine("   final int beginColumn;");
      genCodeLine("   final int endColumn;");
    }

    if (data2().hasEmptyMatch) {
      genCodeLine("   if (jjmatchedPos < 0)");
      genCodeLine("   {");
      genCodeLine("      if (image == null)");
      genCodeLine("         curTokenImage = \"\";");
      genCodeLine("      else");
      genCodeLine("         curTokenImage = image.toString();");

      if (data2().keepLineCol) {
        genCodeLine("      beginLine = endLine = input_stream.getEndLine();");
        genCodeLine("      beginColumn = endColumn = input_stream.getEndColumn();");
      }

      genCodeLine("   }");
      genCodeLine("   else");
      genCodeLine("   {");
      genCodeLine("      String im = jjstrLiteralImages[jjmatchedKind];");
      genCodeLine("      curTokenImage = (im == null) ? input_stream.GetImage() : im;");

      if (data2().keepLineCol) {
        genCodeLine("      beginLine = input_stream.getBeginLine();");
        genCodeLine("      beginColumn = input_stream.getBeginColumn();");
        genCodeLine("      endLine = input_stream.getEndLine();");
        genCodeLine("      endColumn = input_stream.getEndColumn();");
      }

      genCodeLine("   }");
    } else {
      genCodeLine("   String im = jjstrLiteralImages[jjmatchedKind];");
      genCodeLine("   curTokenImage = (im == null) ? input_stream.GetImage() : im;");
      if (data2().keepLineCol) {
        genCodeLine("   beginLine = input_stream.getBeginLine();");
        genCodeLine("   beginColumn = input_stream.getBeginColumn();");
        genCodeLine("   endLine = input_stream.getEndLine();");
        genCodeLine("   endColumn = input_stream.getEndColumn();");
      }
    }

    genCodeLine("   t = Token.newToken(jjmatchedKind, curTokenImage);");

    if (data2().keepLineCol) {
      genCodeLine("");
      genCodeLine("   t.beginLine = beginLine;");
      genCodeLine("   t.endLine = endLine;");
      genCodeLine("   t.beginColumn = beginColumn;");
      genCodeLine("   t.endColumn = endColumn;");
    }

    genCodeLine("");
    genCodeLine("   return t;");
    genCodeLine("}");
  }

  private void DumpGetNextToken() {
    int i;

    genCodeLine("");
    genCodeLine("int curLexState = " + data2().defaultLexState + ";");
    genCodeLine("int defaultLexState = " + data2().defaultLexState + ";");
    genCodeLine("int jjnewStateCnt;");
    genCodeLine("int jjround;");
    genCodeLine("int jjmatchedPos;");
    genCodeLine("int jjmatchedKind;");
    genCodeLine("");
    genCodeLine("/** Get the next Token. */");
    genCodeLine("public Token getNextToken()" + " ");
    genCodeLine("{");
    if (data2().hasSpecial) {
      genCodeLine("  Token specialToken = null;");
    }
    genCodeLine("  Token matchedToken;");
    genCodeLine("  int curPos = 0;");
    genCodeLine("");
    genCodeLine("  EOFLoop :\n  for (;;)");
    genCodeLine("  {");
    genCodeLine("   try");
    genCodeLine("   {");
    genCodeLine("      curChar = input_stream.BeginToken();");
    genCodeLine("   }");
    genCodeLine("   catch(Exception e)");
    genCodeLine("   {");

    if (Options.getDebugTokenManager()) {
      genCodeLine("      debugStream.println(\"Returning the <EOF> token.\\n\");");
    }

    genCodeLine("      jjmatchedKind = 0;");
    genCodeLine("      jjmatchedPos = -1;");
    genCodeLine("      matchedToken = jjFillToken();");

    if (data2().hasSpecial) {
      genCodeLine("      matchedToken.specialToken = specialToken;");
    }

    if ((getLexerData().request.getNextStateForEof() != null) || (getLexerData().request.getActionForEof() != null)) {
      genCodeLine("      TokenLexicalActions(matchedToken);");
    }

    genCodeLine("      return matchedToken;");
    genCodeLine("   }");

    if (data2().hasMoreActions || data2().hasSkipActions || data2().hasTokenActions) {
      genCodeLine("   image = jjimage;");
      genCodeLine("   image.setLength(0);");
      genCodeLine("   jjimageLen = 0;");
    }

    genCodeLine("");

    String prefix = "";
    if (data2().hasMore) {
      genCodeLine("   for (;;)");
      genCodeLine("   {");
      prefix = "  ";
    }

    String endSwitch = "";
    String caseStr = "";
    // this also sets up the start state of the nfa
    if (data2().maxLexStates > 1) {
      genCodeLine(prefix + "   switch(curLexState)");
      genCodeLine(prefix + "   {");
      endSwitch = prefix + "   }";
      caseStr = prefix + "     case ";
      prefix += "    ";
    }

    prefix += "   ";
    for (i = 0; i < data2().maxLexStates; i++) {
      if (data2().maxLexStates > 1) {
        genCodeLine(caseStr + i + ":");
      }

      if (data2().singlesToSkip[i].HasTransitions()) {
        // added the backup(0) to make JIT happy
        genCodeLine(prefix + "try { input_stream.backup(0);");
        if ((data2().singlesToSkip[i].asciiMoves[0] != 0L) && (data2().singlesToSkip[i].asciiMoves[1] != 0L)) {
          genCodeLine(
              prefix + "   while ((curChar < 64" + " && (0x" + Long.toHexString(data2().singlesToSkip[i].asciiMoves[0])
                  + "L & (1L << curChar)) != 0L) || \n" + prefix + "          (curChar >> 6) == 1" + " && (0x"
                  + Long.toHexString(data2().singlesToSkip[i].asciiMoves[1]) + "L & (1L << (curChar & 077))) != 0L)");
        } else if (data2().singlesToSkip[i].asciiMoves[1] == 0L) {
          genCodeLine(prefix + "   while (curChar <= " + (int) LexerJava.MaxChar(data2().singlesToSkip[i].asciiMoves[0])
              + " && (0x" + Long.toHexString(data2().singlesToSkip[i].asciiMoves[0]) + "L & (1L << curChar)) != 0L)");
        } else if (data2().singlesToSkip[i].asciiMoves[0] == 0L) {
          genCodeLine(prefix + "   while (curChar > 63 && curChar <= "
              + (LexerJava.MaxChar(data2().singlesToSkip[i].asciiMoves[1]) + 64) + " && (0x"
              + Long.toHexString(data2().singlesToSkip[i].asciiMoves[1]) + "L & (1L << (curChar & 077))) != 0L)");
        }

        if (Options.getDebugTokenManager()) {
          genCodeLine(prefix + "{");
          genCodeLine("      debugStream.println("
              + (data2().maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
              + "\"Skipping character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \")\");");
        }
        genCodeLine(prefix + "      curChar = input_stream.BeginToken();");

        if (Options.getDebugTokenManager()) {
          genCodeLine(prefix + "}");
        }

        genCodeLine(prefix + "}");
        genCodeLine(prefix + "catch (java.io.IOException e1) { continue EOFLoop; }");
      }

      if ((data2().initMatch[i] != Integer.MAX_VALUE) && (data2().initMatch[i] != 0)) {
        if (Options.getDebugTokenManager()) {
          genCodeLine("      debugStream.println(\"   Matched the empty string as \" + tokenImage["
              + data2().initMatch[i] + "] + \" token.\");");
        }

        genCodeLine(prefix + "jjmatchedKind = " + data2().initMatch[i] + ";");
        genCodeLine(prefix + "jjmatchedPos = -1;");
        genCodeLine(prefix + "curPos = 0;");
      } else {
        genCodeLine(prefix + "jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
        genCodeLine(prefix + "jjmatchedPos = 0;");
      }

      if (Options.getDebugTokenManager()) {
        genCodeLine("      debugStream.println("
            + (data2().maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
            + "\"Current character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") "
            + "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
      }

      genCodeLine(prefix + "curPos = jjMoveStringLiteralDfa0_" + i + "();");
      if (data2().canMatchAnyChar[i] != -1) {
        if ((data2().initMatch[i] != Integer.MAX_VALUE) && (data2().initMatch[i] != 0)) {
          genCodeLine(prefix + "if (jjmatchedPos < 0 || (jjmatchedPos == 0 && jjmatchedKind > "
              + data2().canMatchAnyChar[i] + "))");
        } else {
          genCodeLine(prefix + "if (jjmatchedPos == 0 && jjmatchedKind > " + data2().canMatchAnyChar[i] + ")");
        }
        genCodeLine(prefix + "{");

        if (Options.getDebugTokenManager()) {
          genCodeLine("           debugStream.println(\"   Current character matched as a \" + tokenImage["
              + data2().canMatchAnyChar[i] + "] + \" token.\");");
        }
        genCodeLine(prefix + "   jjmatchedKind = " + data2().canMatchAnyChar[i] + ";");

        if ((data2().initMatch[i] != Integer.MAX_VALUE) && (data2().initMatch[i] != 0)) {
          genCodeLine(prefix + "   jjmatchedPos = 0;");
        }

        genCodeLine(prefix + "}");
      }

      if (data2().maxLexStates > 1) {
        genCodeLine(prefix + "break;");
      }
    }

    if (data2().maxLexStates > 1) {
      genCodeLine(endSwitch);
    } else if (data2().maxLexStates == 0) {
      genCodeLine("       jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
    }

    if (data2().maxLexStates > 1) {
      prefix = "  ";
    } else {
      prefix = "";
    }

    if (data2().maxLexStates > 0) {
      genCodeLine(prefix + "   if (jjmatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
      genCodeLine(prefix + "   {");
      genCodeLine(prefix + "      if (jjmatchedPos + 1 < curPos)");

      if (Options.getDebugTokenManager()) {
        genCodeLine(prefix + "      {");
        genCodeLine(prefix + "         debugStream.println("
            + "\"   Putting back \" + (curPos - jjmatchedPos - 1) + \" characters into the input stream.\");");
      }

      genCodeLine(prefix + "         input_stream.backup(curPos - jjmatchedPos - 1);");

      if (Options.getDebugTokenManager()) {
        genCodeLine(prefix + "      }");
      }

      if (Options.getDebugTokenManager()) {
        genCodeLine("    debugStream.println(" + "\"****** FOUND A \" + tokenImage[jjmatchedKind] + \" MATCH "
            + "(\" + TokenMgrException.addEscapes(new String(input_stream.GetSuffix(jjmatchedPos + 1))) + "
            + "\") ******\\n\");");
      }

      if (data2().hasSkip || data2().hasMore || data2().hasSpecial) {
        genCodeLine(prefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
        genCodeLine(prefix + "      {");
      }

      genCodeLine(prefix + "         matchedToken = jjFillToken();");

      if (data2().hasSpecial) {
        genCodeLine(prefix + "         matchedToken.specialToken = specialToken;");
      }

      if (data2().hasTokenActions) {
        genCodeLine(prefix + "         TokenLexicalActions(matchedToken);");
      }

      if (data2().maxLexStates > 1) {
        genCodeLine("       if (jjnewLexState[jjmatchedKind] != -1)");
        genCodeLine(prefix + "       curLexState = jjnewLexState[jjmatchedKind];");
      }

      genCodeLine(prefix + "         return matchedToken;");

      if (data2().hasSkip || data2().hasMore || data2().hasSpecial) {
        genCodeLine(prefix + "      }");

        if (data2().hasSkip || data2().hasSpecial) {
          if (data2().hasMore) {
            genCodeLine(
                prefix + "      else if ((jjtoSkip[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
          } else {
            genCodeLine(prefix + "      else");
          }

          genCodeLine(prefix + "      {");

          if (data2().hasSpecial) {
            genCodeLine(
                prefix + "         if ((jjtoSpecial[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
            genCodeLine(prefix + "         {");

            genCodeLine(prefix + "            matchedToken = jjFillToken();");

            genCodeLine(prefix + "            if (specialToken == null)");
            genCodeLine(prefix + "               specialToken = matchedToken;");
            genCodeLine(prefix + "            else");
            genCodeLine(prefix + "            {");
            genCodeLine(prefix + "               matchedToken.specialToken = specialToken;");
            genCodeLine(prefix + "               specialToken = (specialToken.next = matchedToken);");
            genCodeLine(prefix + "            }");

            if (data2().hasSkipActions) {
              genCodeLine(prefix + "            SkipLexicalActions(matchedToken);");
            }

            genCodeLine(prefix + "         }");

            if (data2().hasSkipActions) {
              genCodeLine(prefix + "         else");
              genCodeLine(prefix + "            SkipLexicalActions(null);");
            }
          } else if (data2().hasSkipActions) {
            genCodeLine(prefix + "         SkipLexicalActions(null);");
          }

          if (data2().maxLexStates > 1) {
            genCodeLine("         if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine(prefix + "         curLexState = jjnewLexState[jjmatchedKind];");
          }

          genCodeLine(prefix + "         continue EOFLoop;");
          genCodeLine(prefix + "      }");
        }

        if (data2().hasMore) {
          if (data2().hasMoreActions) {
            genCodeLine(prefix + "      MoreLexicalActions();");
          } else if (data2().hasSkipActions || data2().hasTokenActions) {
            genCodeLine(prefix + "      jjimageLen += jjmatchedPos + 1;");
          }

          if (data2().maxLexStates > 1) {
            genCodeLine("      if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine(prefix + "      curLexState = jjnewLexState[jjmatchedKind];");
          }
          genCodeLine(prefix + "      curPos = 0;");
          genCodeLine(prefix + "      jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");

          genCodeLine(prefix + "      try {");
          genCodeLine(prefix + "         curChar = input_stream.readChar();");

          if (Options.getDebugTokenManager()) {
            genCodeLine("   debugStream.println("
                + (data2().maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
                + "\"Current character : \" + "
                + "TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") "
                + "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
          }
          genCodeLine(prefix + "         continue;");
          genCodeLine(prefix + "      }");
          genCodeLine(prefix + "      catch (java.io.IOException e1) { }");
        }
      }

      genCodeLine(prefix + "   }");
      genCodeLine(prefix + "   int error_line = input_stream.getEndLine();");
      genCodeLine(prefix + "   int error_column = input_stream.getEndColumn();");
      genCodeLine(prefix + "   String error_after = null;");
      genCodeLine(prefix + "   boolean EOFSeen = false;");
      genCodeLine(prefix + "   try { input_stream.readChar(); input_stream.backup(1); }");
      genCodeLine(prefix + "   catch (java.io.IOException e1) {");
      genCodeLine(prefix + "      EOFSeen = true;");
      genCodeLine(prefix + "      error_after = curPos <= 1 ? \"\" : input_stream.GetImage();");
      genCodeLine(prefix + "      if (curChar == '\\n' || curChar == '\\r') {");
      genCodeLine(prefix + "         error_line++;");
      genCodeLine(prefix + "         error_column = 0;");
      genCodeLine(prefix + "      }");
      genCodeLine(prefix + "      else");
      genCodeLine(prefix + "         error_column++;");
      genCodeLine(prefix + "   }");
      genCodeLine(prefix + "   if (!EOFSeen) {");
      genCodeLine(prefix + "      input_stream.backup(1);");
      genCodeLine(prefix + "      error_after = curPos <= 1 ? \"\" : input_stream.GetImage();");
      genCodeLine(prefix + "   }");
      genCodeLine(prefix + "   throw new TokenMgrException("
          + "EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrException.LEXICAL_ERROR);");
    }

    if (data2().hasMore) {
      genCodeLine(prefix + " }");
    }

    genCodeLine("  }");
    genCodeLine("}");
    genCodeLine("");
  }

  private void DumpSkipActions() {
    Action act;

    genCodeLine("void SkipLexicalActions(Token matchedToken)");
    genCodeLine("{");
    genCodeLine("   switch(jjmatchedKind)");
    genCodeLine("   {");

    Outer:
    for (int i = 0; i < data2().maxOrdinal; i++) {
      if ((data2().toSkip[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (;;) {
        if ((((act = data2().actions[i]) == null) || (act.getActionTokens() == null)
            || (act.getActionTokens().size() == 0)) && !data2().canLoop[getLexerData().getState(i)]) {
          continue Outer;
        }

        genCodeLine("      case " + i + " :");

        if ((data2().initMatch[getLexerData().getState(i)] == i) && data2().canLoop[getLexerData().getState(i)]) {
          genCodeLine("         if (jjmatchedPos == -1)");
          genCodeLine("         {");
          genCodeLine("            if (jjbeenHere[" + getLexerData().getState(i) + "] &&");
          genCodeLine(
              "                jjemptyLineNo[" + getLexerData().getState(i) + "] == input_stream.getBeginLine() &&");
          genCodeLine(
              "                jjemptyColNo[" + getLexerData().getState(i) + "] == input_stream.getBeginColumn())");
          genCodeLine("               throw new TokenMgrException("
              + "(\"Error: Bailing out of infinite loop caused by repeated empty string matches "
              + "at line \" + input_stream.getBeginLine() + \", "
              + "column \" + input_stream.getBeginColumn() + \".\"), TokenMgrException.LOOP_DETECTED);");
          genCodeLine("            jjemptyLineNo[" + getLexerData().getState(i) + "] = input_stream.getBeginLine();");
          genCodeLine("            jjemptyColNo[" + getLexerData().getState(i) + "] = input_stream.getBeginColumn();");
          genCodeLine("            jjbeenHere[" + getLexerData().getState(i) + "] = true;");
          genCodeLine("         }");
        }

        if (((act = data2().actions[i]) == null) || (act.getActionTokens().size() == 0)) {
          break;
        }

        genCode("         image.append");
        if (getLexerData().getImage(i) != null) {
          genCodeLine("(jjstrLiteralImages[" + i + "]);");
          genCodeLine("        lengthOfMatch = jjstrLiteralImages[" + i + "].length();");
        } else {
          genCodeLine("(input_stream.GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
        }

        genTokenSetup(act.getActionTokens().get(0));
        this.ccol = 1;

        for (Token element : act.getActionTokens()) {
          genToken(element);
        }
        genCodeLine("");

        break;
      }

      genCodeLine("         break;");
    }

    genCodeLine("      default :");
    genCodeLine("         break;");
    genCodeLine("   }");
    genCodeLine("}");
  }

  private void DumpMoreActions() {
    Action act;

    genCodeLine("void MoreLexicalActions()");
    genCodeLine("{");
    genCodeLine("   jjimageLen += (lengthOfMatch = jjmatchedPos + 1);");
    genCodeLine("   switch(jjmatchedKind)");
    genCodeLine("   {");

    Outer:
    for (int i = 0; i < data2().maxOrdinal; i++) {
      if ((data2().toMore[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (;;) {
        if ((((act = data2().actions[i]) == null) || (act.getActionTokens() == null)
            || (act.getActionTokens().size() == 0)) && !data2().canLoop[getLexerData().getState(i)]) {
          continue Outer;
        }

        genCodeLine("      case " + i + " :");

        if ((data2().initMatch[getLexerData().getState(i)] == i) && data2().canLoop[getLexerData().getState(i)]) {
          genCodeLine("         if (jjmatchedPos == -1)");
          genCodeLine("         {");
          genCodeLine("            if (jjbeenHere[" + getLexerData().getState(i) + "] &&");
          genCodeLine(
              "                jjemptyLineNo[" + getLexerData().getState(i) + "] == input_stream.getBeginLine() &&");
          genCodeLine(
              "                jjemptyColNo[" + getLexerData().getState(i) + "] == input_stream.getBeginColumn())");
          genCodeLine("               throw new TokenMgrException("
              + "(\"Error: Bailing out of infinite loop caused by repeated empty string matches "
              + "at line \" + input_stream.getBeginLine() + \", "
              + "column \" + input_stream.getBeginColumn() + \".\"), TokenMgrException.LOOP_DETECTED);");
          genCodeLine("            jjemptyLineNo[" + getLexerData().getState(i) + "] = input_stream.getBeginLine();");
          genCodeLine("            jjemptyColNo[" + getLexerData().getState(i) + "] = input_stream.getBeginColumn();");
          genCodeLine("            jjbeenHere[" + getLexerData().getState(i) + "] = true;");
          genCodeLine("         }");
        }

        if (((act = data2().actions[i]) == null) || (act.getActionTokens().size() == 0)) {
          break;
        }

        genCode("         image.append");

        if (getLexerData().getImage(i) != null) {
          genCodeLine("(jjstrLiteralImages[" + i + "]);");
        } else {
          genCodeLine("(input_stream.GetSuffix(jjimageLen));");
        }

        genCodeLine("         jjimageLen = 0;");
        genTokenSetup(act.getActionTokens().get(0));
        this.ccol = 1;

        for (Token element : act.getActionTokens()) {
          genToken(element);
        }
        genCodeLine("");

        break;
      }

      genCodeLine("         break;");
    }

    genCodeLine("      default :");
    genCodeLine("         break;");

    genCodeLine("   }");
    genCodeLine("}");
  }

  private void DumpTokenActions() {
    Action act;
    int i;

    genCodeLine("void TokenLexicalActions(Token matchedToken)");
    genCodeLine("{");
    genCodeLine("   switch(jjmatchedKind)");
    genCodeLine("   {");

    Outer:
    for (i = 0; i < data2().maxOrdinal; i++) {
      if ((data2().toToken[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (;;) {
        if ((((act = data2().actions[i]) == null) || (act.getActionTokens() == null)
            || (act.getActionTokens().size() == 0)) && !data2().canLoop[getLexerData().getState(i)]) {
          continue Outer;
        }

        genCodeLine("      case " + i + " :");

        if ((data2().initMatch[getLexerData().getState(i)] == i) && data2().canLoop[getLexerData().getState(i)]) {
          genCodeLine("         if (jjmatchedPos == -1)");
          genCodeLine("         {");
          genCodeLine("            if (jjbeenHere[" + getLexerData().getState(i) + "] &&");
          genCodeLine(
              "                jjemptyLineNo[" + getLexerData().getState(i) + "] == input_stream.getBeginLine() &&");
          genCodeLine(
              "                jjemptyColNo[" + getLexerData().getState(i) + "] == input_stream.getBeginColumn())");
          genCodeLine("               throw new TokenMgrException("
              + "(\"Error: Bailing out of infinite loop caused by repeated empty string matches "
              + "at line \" + input_stream.getBeginLine() + \", "
              + "column \" + input_stream.getBeginColumn() + \".\"), TokenMgrException.LOOP_DETECTED);");
          genCodeLine("            jjemptyLineNo[" + getLexerData().getState(i) + "] = input_stream.getBeginLine();");
          genCodeLine("            jjemptyColNo[" + getLexerData().getState(i) + "] = input_stream.getBeginColumn();");
          genCodeLine("            jjbeenHere[" + getLexerData().getState(i) + "] = true;");
          genCodeLine("         }");
        }

        if (((act = data2().actions[i]) == null) || (act.getActionTokens().size() == 0)) {
          break;
        }

        if (i == 0) {
          genCodeLine("      image.setLength(0);"); // For EOF no image is there
        } else {
          genCode("        image.append");

          if (getLexerData().getImage(i) != null) {
            genCodeLine("(jjstrLiteralImages[" + i + "]);");
            genCodeLine("        lengthOfMatch = jjstrLiteralImages[" + i + "].length();");
          } else {
            genCodeLine("(input_stream.GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
          }
        }

        genTokenSetup(act.getActionTokens().get(0));
        this.ccol = 1;

        for (Token element : act.getActionTokens()) {
          genToken(element);
        }
        genCodeLine("");

        break;
      }

      genCodeLine("         break;");
    }

    genCodeLine("      default :");
    genCodeLine("         break;");
    genCodeLine("   }");
    genCodeLine("}");
  }


  // private static boolean boilerPlateDumped = false;
  private void PrintBoilerPlate() {
    genCodeLine("private void " + "jjCheckNAdd(int state)");
    genCodeLine("{");
    genCodeLine("   if (jjrounds[state] != jjround)");
    genCodeLine("   {");
    genCodeLine("      jjstateSet[jjnewStateCnt++] = state;");
    genCodeLine("      jjrounds[state] = jjround;");
    genCodeLine("   }");
    genCodeLine("}");

    genCodeLine("private void " + "jjAddStates(int start, int end)");
    genCodeLine("{");
    genCodeLine("   do {");
    genCodeLine("      jjstateSet[jjnewStateCnt++] = jjnextStates[start];");
    genCodeLine("   } while (start++ != end);");
    genCodeLine("}");

    genCodeLine("private void " + "jjCheckNAddTwoStates(int state1, int state2)");
    genCodeLine("{");
    genCodeLine("   jjCheckNAdd(state1);");
    genCodeLine("   jjCheckNAdd(state2);");
    genCodeLine("}");
    genCodeLine("");

    if (getLexerData().jjCheckNAddStatesDualNeeded) {
      genCodeLine("private void " + "jjCheckNAddStates(int start, int end)");
      genCodeLine("{");
      genCodeLine("   do {");
      genCodeLine("      jjCheckNAdd(jjnextStates[start]);");
      genCodeLine("   } while (start++ != end);");
      genCodeLine("}");
      genCodeLine("");
    }

    if (getLexerData().jjCheckNAddStatesUnaryNeeded) {
      genCodeLine("private void " + "jjCheckNAddStates(int start)");
      genCodeLine("{");
      genCodeLine("   jjCheckNAdd(jjnextStates[start]);");
      genCodeLine("   jjCheckNAdd(jjnextStates[start + 1]);");
      genCodeLine("}");
      genCodeLine("");
    }
  }

  private void DumpStatesForKind() {
    DumpStatesForState();
    boolean moreThanOne = false;
    int cnt = 0;

    genCode("protected static final int[][] kindForState = ");

    if (getLexerData().kinds == null) {
      genCodeLine("null;");
      return;
    } else {
      genCodeLine("{");
    }

    for (int[] kind : getLexerData().kinds) {
      if (moreThanOne) {
        genCodeLine(",");
      }
      moreThanOne = true;

      if (kind == null) {
        genCodeLine("{}");
      } else {
        cnt = 0;
        genCode("{ ");
        for (int element : kind) {
          if ((cnt % 15) == 0) {
            genCode("\n  ");
          } else if (cnt > 1) {
            genCode(" ");
          }

          genCode(element);
          genCode(", ");

        }

        genCode("}");
      }
    }
    genCodeLine("\n};");
  }


  private void DumpStatesForState() {
    genCode("protected static final int[][][] statesForState = ");

    if (getLexerData().statesForState == null) {
      assert (false) : "This should never be null.";
      genCodeLine("null;");
      return;
    } else {
      genCodeLine("{");
    }

    for (int i = 0; i < data2().maxLexStates; i++) {
      if (getLexerData().statesForState[i] == null) {
        genCodeLine(" {},");
        continue;
      }

      genCodeLine(" {");

      for (int j = 0; j < getLexerData().statesForState[i].length; j++) {
        int[] stateSet = getLexerData().statesForState[i][j];

        if (stateSet == null) {
          genCodeLine("   { " + j + " },");
          continue;
        }

        genCode("   { ");

        for (int element : stateSet) {
          genCode(element + ", ");
        }

        genCodeLine("},");
      }

      genCodeLine("},");
    }

    genCodeLine("\n};");
  }

  private void DumpStateSets() {
    int cnt = 0;

    genCode("static final int[] jjnextStates = {");
    if (getLexerData().orderedStateSet.size() > 0) {
      for (int[] set : getLexerData().orderedStateSet) {
        for (int element : set) {
          if ((cnt++ % 16) == 0) {
            genCode("\n   ");
          }

          genCode(element + ", ");
        }
      }
    } else {
      genCode("0");
    }

    genCodeLine("\n};");
  }

  private void DumpStrLiteralImages() {
    String image;
    int i;
    int charCnt = 0; // Set to zero in reInit() but just to be sure

    genCodeLine("");
    genCodeLine("/** Token literal values. */");
    genCodeLine("public static final String[] jjstrLiteralImages = {");

    if (getLexerData().getImageCount() <= 0) {
      genCodeLine("};");
      return;
    }

    getLexerData().setImage(0, "");
    for (i = 0; i < getLexerData().getImageCount(); i++) {
      if (((image = getLexerData().getImage(i)) == null)
          || (((data2().toSkip[i / 64] & (1L << (i % 64))) == 0L) && ((data2().toMore[i / 64] & (1L << (i % 64))) == 0L)
              && ((data2().toToken[i / 64] & (1L << (i % 64))) == 0L))
          || ((data2().toSkip[i / 64] & (1L << (i % 64))) != 0L) || ((data2().toMore[i / 64] & (1L << (i % 64))) != 0L)
          || data2().canReachOnMore[getLexerData().getState(i)]
          || ((getLexerData().ignoreCase() || data2().ignoreCase[i])
              && (!image.equals(image.toLowerCase(Locale.ENGLISH))
                  || !image.equals(image.toUpperCase(Locale.ENGLISH))))) {
        getLexerData().setImage(i, null);
        if ((charCnt += 6) > 80) {
          genCodeLine("");
          charCnt = 0;
        }

        genCode("null, ");
        continue;
      }

      String toPrint = "\"";
      for (int j = 0; j < image.length(); j++) {
        if (image.charAt(j) <= 0xff) {
          toPrint += ("\\" + Integer.toOctalString(image.charAt(j)));
        } else {
          String hexVal = Integer.toHexString(image.charAt(j));
          if (hexVal.length() == 3) {
            hexVal = "0" + hexVal;
          }
          toPrint += ("\\u" + hexVal);
        }
      }

      toPrint += ("\", ");

      if ((charCnt += toPrint.length()) >= 80) {
        genCodeLine("");
        charCnt = 0;
      }

      genCode(toPrint);
    }

    while (++i < data2().maxOrdinal) {
      if ((charCnt += 6) > 80) {
        genCodeLine("");
        charCnt = 0;
      }

      genCode("null, ");
      continue;
    }

    genCodeLine("};");
  }
}
