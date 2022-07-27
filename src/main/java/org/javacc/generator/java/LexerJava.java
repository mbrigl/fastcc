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

    // TODO :: CBA -- Require Unification of output language specific processing into a single Enum
    // class
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

    genClassStart(null, getTokenManager(), new String[] {},
        new String[] { getLexerData().request.getParserName() + "Constants" });

    if ((getLexerData().request.getTokens() != null) && !getLexerData().request.getTokens().isEmpty()) {
      genTokenSetup(getLexerData().request.getTokens().get(0));
      this.ccol = 1;

      getLexerData().request.getTokens().forEach(tt -> genToken(tt));

      genCodeLine("");
    }

    genCodeLine("");
    genCodeLine("  /** Debug output. */");
    genCodeLine("  public " + getStatic() + " java.io.PrintStream debugStream = System.out;");
    genCodeLine("  /** Set debug output. */");
    genCodeLine("  public " + getStatic() + " void setDebugStream(java.io.PrintStream ds) { debugStream = ds; }");
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

    if (this.hasLoop) {
      genCodeLine(getStatic() + "int[] jjemptyLineNo = new int[" + this.maxLexStates + "];");
      genCodeLine(getStatic() + "int[] jjemptyColNo = new int[" + this.maxLexStates + "];");
      genCodeLine(getStatic() + "boolean[] jjbeenHere = new boolean[" + this.maxLexStates + "];");
    }

    DumpSkipActions();
    DumpMoreActions();
    DumpTokenActions();

    PrintBoilerPlate();

    String charStreamName = "JavaCharStream";

    writeTemplate(LexerJava.BOILERPLATER_METHOD_RESOURCE_URL,
        Map.of("charStreamName", charStreamName, "lexStateNameLength", getLexerData().getStateCount(),
            "defaultLexState", this.defaultLexState, "noDfa", Options.getNoDfa(), "generatedStates",
            this.totalNumStates));

    DumpStaticVarDeclarations(charStreamName);
    genCodeLine(/* { */ "}");

    saveOutput();
  }

  private void CheckEmptyStringMatch() {
    int i, j, k, len;
    boolean[] seen = new boolean[this.maxLexStates];
    boolean[] done = new boolean[this.maxLexStates];
    String cycle;
    String reList;

    Outer:
    for (i = 0; i < this.maxLexStates; i++) {
      if (done[i] || (this.initMatch[i] == 0) || (this.initMatch[i] == Integer.MAX_VALUE)
          || (this.canMatchAnyChar[i] != -1)) {
        continue;
      }

      done[i] = true;
      len = 0;
      cycle = "";
      reList = "";

      for (k = 0; k < this.maxLexStates; k++) {
        seen[k] = false;
      }

      j = i;
      seen[i] = true;
      cycle += getLexerData().getStateName(j) + "-->";
      while (this.newLexState[this.initMatch[j]] != null) {
        cycle += this.newLexState[this.initMatch[j]];
        if (seen[j = getLexerData().getStateIndex(this.newLexState[this.initMatch[j]])]) {
          break;
        }

        cycle += "-->";
        done[j] = true;
        seen[j] = true;
        if ((this.initMatch[j] == 0) || (this.initMatch[j] == Integer.MAX_VALUE) || (this.canMatchAnyChar[j] != -1)) {
          continue Outer;
        }
        if (len != 0) {
          reList += "; ";
        }
        reList += "line " + this.rexprs[this.initMatch[j]].getLine() + ", column "
            + this.rexprs[this.initMatch[j]].getColumn();
        len++;
      }

      if (this.newLexState[this.initMatch[j]] == null) {
        cycle += getLexerData().getStateName(getLexerData().getState(this.initMatch[j]));
      }

      for (k = 0; k < this.maxLexStates; k++) {
        this.canLoop[k] |= seen[k];
      }

      this.hasLoop = true;
      if (len == 0) {
        JavaCCErrors.warning(this.rexprs[this.initMatch[i]],
            "Regular expression"
                + ((this.rexprs[this.initMatch[i]].label.equals("")) ? ""
                    : (" for " + this.rexprs[this.initMatch[i]].label))
                + " can be matched by the empty string (\"\") in lexical state " + getLexerData().getStateName(i)
                + ". This can result in an endless loop of " + "empty string matches.");
      } else {
        JavaCCErrors.warning(this.rexprs[this.initMatch[i]],
            "Regular expression"
                + ((this.rexprs[this.initMatch[i]].label.equals("")) ? ""
                    : (" for " + this.rexprs[this.initMatch[i]].label))
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
    for (i = 0; i < this.maxLexStates; i++) {
      genCodeLine("   \"" + getLexerData().getStateName(i) + "\",");
    }
    genCodeLine("};");

    {
      genCodeLine("");
      genCodeLine("/** Lex State array. */");
      genCode("public static final int[] jjnewLexState = {");

      for (i = 0; i < this.maxOrdinal; i++) {
        if ((i % 25) == 0) {
          genCode("\n   ");
        }

        if (this.newLexState[i] == null) {
          genCode("-1, ");
        } else {
          genCode(getLexerData().getStateIndex(this.newLexState[i]) + ", ");
        }
      }
      genCodeLine("\n};");
    }

    {
      // Bit vector for TOKEN
      genCode("static final long[] jjtoToken = {");
      for (i = 0; i < ((this.maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          genCode("\n   ");
        }
        genCode("0x" + Long.toHexString(this.toToken[i]) + "L, ");
      }
      genCodeLine("\n};");
    }

    {
      // Bit vector for SKIP
      genCode("static final long[] jjtoSkip = {");
      for (i = 0; i < ((this.maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          genCode("\n   ");
        }
        genCode("0x" + Long.toHexString(this.toSkip[i]) + "L, ");
      }
      genCodeLine("\n};");
    }

    {
      // Bit vector for SPECIAL
      genCode("static final long[] jjtoSpecial = {");
      for (i = 0; i < ((this.maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          genCode("\n   ");
        }
        genCode("0x" + Long.toHexString(this.toSpecial[i]) + "L, ");
      }
      genCodeLine("\n};");
    }

    {
      // Bit vector for MORE
      genCode("static final long[] jjtoMore = {");
      for (i = 0; i < ((this.maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          genCode("\n   ");
        }
        genCode("0x" + Long.toHexString(this.toMore[i]) + "L, ");
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
    genCodeLine(getStatic() + "protected Token jjFillToken()");
    genCodeLine("{");
    genCodeLine("   final Token t;");
    genCodeLine("   final String curTokenImage;");
    if (this.keepLineCol) {
      genCodeLine("   final int beginLine;");
      genCodeLine("   final int endLine;");
      genCodeLine("   final int beginColumn;");
      genCodeLine("   final int endColumn;");
    }

    if (this.hasEmptyMatch) {
      genCodeLine("   if (jjmatchedPos < 0)");
      genCodeLine("   {");
      genCodeLine("      if (image == null)");
      genCodeLine("         curTokenImage = \"\";");
      genCodeLine("      else");
      genCodeLine("         curTokenImage = image.toString();");

      if (this.keepLineCol) {
        genCodeLine("      beginLine = endLine = input_stream.getEndLine();");
        genCodeLine("      beginColumn = endColumn = input_stream.getEndColumn();");
      }

      genCodeLine("   }");
      genCodeLine("   else");
      genCodeLine("   {");
      genCodeLine("      String im = jjstrLiteralImages[jjmatchedKind];");
      genCodeLine("      curTokenImage = (im == null) ? input_stream.GetImage() : im;");

      if (this.keepLineCol) {
        genCodeLine("      beginLine = input_stream.getBeginLine();");
        genCodeLine("      beginColumn = input_stream.getBeginColumn();");
        genCodeLine("      endLine = input_stream.getEndLine();");
        genCodeLine("      endColumn = input_stream.getEndColumn();");
      }

      genCodeLine("   }");
    } else {
      genCodeLine("   String im = jjstrLiteralImages[jjmatchedKind];");
      genCodeLine("   curTokenImage = (im == null) ? input_stream.GetImage() : im;");
      if (this.keepLineCol) {
        genCodeLine("   beginLine = input_stream.getBeginLine();");
        genCodeLine("   beginColumn = input_stream.getBeginColumn();");
        genCodeLine("   endLine = input_stream.getEndLine();");
        genCodeLine("   endColumn = input_stream.getEndColumn();");
      }
    }

    genCodeLine("   t = Token.newToken(jjmatchedKind, curTokenImage);");

    if (this.keepLineCol) {
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
    genCodeLine(getStatic() + "int curLexState = " + this.defaultLexState + ";");
    genCodeLine(getStatic() + "int defaultLexState = " + this.defaultLexState + ";");
    genCodeLine(getStatic() + "int jjnewStateCnt;");
    genCodeLine(getStatic() + "int jjround;");
    genCodeLine(getStatic() + "int jjmatchedPos;");
    genCodeLine(getStatic() + "int jjmatchedKind;");
    genCodeLine("");
    genCodeLine("/** Get the next Token. */");
    genCodeLine("public " + getStatic() + "Token getNextToken()" + " ");
    genCodeLine("{");
    if (this.hasSpecial) {
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

    if (this.hasSpecial) {
      genCodeLine("      matchedToken.specialToken = specialToken;");
    }

    if ((getLexerData().request.getNextStateForEof() != null) || (getLexerData().request.getActionForEof() != null)) {
      genCodeLine("      TokenLexicalActions(matchedToken);");
    }

    genCodeLine("      return matchedToken;");
    genCodeLine("   }");

    if (this.hasMoreActions || this.hasSkipActions || this.hasTokenActions) {
      genCodeLine("   image = jjimage;");
      genCodeLine("   image.setLength(0);");
      genCodeLine("   jjimageLen = 0;");
    }

    genCodeLine("");

    String prefix = "";
    if (this.hasMore) {
      genCodeLine("   for (;;)");
      genCodeLine("   {");
      prefix = "  ";
    }

    String endSwitch = "";
    String caseStr = "";
    // this also sets up the start state of the nfa
    if (this.maxLexStates > 1) {
      genCodeLine(prefix + "   switch(curLexState)");
      genCodeLine(prefix + "   {");
      endSwitch = prefix + "   }";
      caseStr = prefix + "     case ";
      prefix += "    ";
    }

    prefix += "   ";
    for (i = 0; i < this.maxLexStates; i++) {
      if (this.maxLexStates > 1) {
        genCodeLine(caseStr + i + ":");
      }

      if (this.singlesToSkip[i].HasTransitions()) {
        // added the backup(0) to make JIT happy
        genCodeLine(prefix + "try { input_stream.backup(0);");
        if ((this.singlesToSkip[i].asciiMoves[0] != 0L) && (this.singlesToSkip[i].asciiMoves[1] != 0L)) {
          genCodeLine(
              prefix + "   while ((curChar < 64" + " && (0x" + Long.toHexString(this.singlesToSkip[i].asciiMoves[0])
                  + "L & (1L << curChar)) != 0L) || \n" + prefix + "          (curChar >> 6) == 1" + " && (0x"
                  + Long.toHexString(this.singlesToSkip[i].asciiMoves[1]) + "L & (1L << (curChar & 077))) != 0L)");
        } else if (this.singlesToSkip[i].asciiMoves[1] == 0L) {
          genCodeLine(prefix + "   while (curChar <= " + (int) LexerJava.MaxChar(this.singlesToSkip[i].asciiMoves[0])
              + " && (0x" + Long.toHexString(this.singlesToSkip[i].asciiMoves[0]) + "L & (1L << curChar)) != 0L)");
        } else if (this.singlesToSkip[i].asciiMoves[0] == 0L) {
          genCodeLine(prefix + "   while (curChar > 63 && curChar <= "
              + (LexerJava.MaxChar(this.singlesToSkip[i].asciiMoves[1]) + 64) + " && (0x"
              + Long.toHexString(this.singlesToSkip[i].asciiMoves[1]) + "L & (1L << (curChar & 077))) != 0L)");
        }

        if (Options.getDebugTokenManager()) {
          genCodeLine(prefix + "{");
          genCodeLine("      debugStream.println("
              + (this.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
              + "\"Skipping character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \")\");");
        }
        genCodeLine(prefix + "      curChar = input_stream.BeginToken();");

        if (Options.getDebugTokenManager()) {
          genCodeLine(prefix + "}");
        }

        genCodeLine(prefix + "}");
        genCodeLine(prefix + "catch (java.io.IOException e1) { continue EOFLoop; }");
      }

      if ((this.initMatch[i] != Integer.MAX_VALUE) && (this.initMatch[i] != 0)) {
        if (Options.getDebugTokenManager()) {
          genCodeLine("      debugStream.println(\"   Matched the empty string as \" + tokenImage[" + this.initMatch[i]
              + "] + \" token.\");");
        }

        genCodeLine(prefix + "jjmatchedKind = " + this.initMatch[i] + ";");
        genCodeLine(prefix + "jjmatchedPos = -1;");
        genCodeLine(prefix + "curPos = 0;");
      } else {
        genCodeLine(prefix + "jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
        genCodeLine(prefix + "jjmatchedPos = 0;");
      }

      if (Options.getDebugTokenManager()) {
        genCodeLine("      debugStream.println("
            + (this.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
            + "\"Current character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") "
            + "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
      }

      genCodeLine(prefix + "curPos = jjMoveStringLiteralDfa0_" + i + "();");
      if (this.canMatchAnyChar[i] != -1) {
        if ((this.initMatch[i] != Integer.MAX_VALUE) && (this.initMatch[i] != 0)) {
          genCodeLine(prefix + "if (jjmatchedPos < 0 || (jjmatchedPos == 0 && jjmatchedKind > "
              + this.canMatchAnyChar[i] + "))");
        } else {
          genCodeLine(prefix + "if (jjmatchedPos == 0 && jjmatchedKind > " + this.canMatchAnyChar[i] + ")");
        }
        genCodeLine(prefix + "{");

        if (Options.getDebugTokenManager()) {
          genCodeLine("           debugStream.println(\"   Current character matched as a \" + tokenImage["
              + this.canMatchAnyChar[i] + "] + \" token.\");");
        }
        genCodeLine(prefix + "   jjmatchedKind = " + this.canMatchAnyChar[i] + ";");

        if ((this.initMatch[i] != Integer.MAX_VALUE) && (this.initMatch[i] != 0)) {
          genCodeLine(prefix + "   jjmatchedPos = 0;");
        }

        genCodeLine(prefix + "}");
      }

      if (this.maxLexStates > 1) {
        genCodeLine(prefix + "break;");
      }
    }

    if (this.maxLexStates > 1) {
      genCodeLine(endSwitch);
    } else if (this.maxLexStates == 0) {
      genCodeLine("       jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
    }

    if (this.maxLexStates > 1) {
      prefix = "  ";
    } else {
      prefix = "";
    }

    if (this.maxLexStates > 0) {
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

      if (this.hasSkip || this.hasMore || this.hasSpecial) {
        genCodeLine(prefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
        genCodeLine(prefix + "      {");
      }

      genCodeLine(prefix + "         matchedToken = jjFillToken();");

      if (this.hasSpecial) {
        genCodeLine(prefix + "         matchedToken.specialToken = specialToken;");
      }

      if (this.hasTokenActions) {
        genCodeLine(prefix + "         TokenLexicalActions(matchedToken);");
      }

      if (this.maxLexStates > 1) {
        genCodeLine("       if (jjnewLexState[jjmatchedKind] != -1)");
        genCodeLine(prefix + "       curLexState = jjnewLexState[jjmatchedKind];");
      }

      genCodeLine(prefix + "         return matchedToken;");

      if (this.hasSkip || this.hasMore || this.hasSpecial) {
        genCodeLine(prefix + "      }");

        if (this.hasSkip || this.hasSpecial) {
          if (this.hasMore) {
            genCodeLine(
                prefix + "      else if ((jjtoSkip[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
          } else {
            genCodeLine(prefix + "      else");
          }

          genCodeLine(prefix + "      {");

          if (this.hasSpecial) {
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

            if (this.hasSkipActions) {
              genCodeLine(prefix + "            SkipLexicalActions(matchedToken);");
            }

            genCodeLine(prefix + "         }");

            if (this.hasSkipActions) {
              genCodeLine(prefix + "         else");
              genCodeLine(prefix + "            SkipLexicalActions(null);");
            }
          } else if (this.hasSkipActions) {
            genCodeLine(prefix + "         SkipLexicalActions(null);");
          }

          if (this.maxLexStates > 1) {
            genCodeLine("         if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine(prefix + "         curLexState = jjnewLexState[jjmatchedKind];");
          }

          genCodeLine(prefix + "         continue EOFLoop;");
          genCodeLine(prefix + "      }");
        }

        if (this.hasMore) {
          if (this.hasMoreActions) {
            genCodeLine(prefix + "      MoreLexicalActions();");
          } else if (this.hasSkipActions || this.hasTokenActions) {
            genCodeLine(prefix + "      jjimageLen += jjmatchedPos + 1;");
          }

          if (this.maxLexStates > 1) {
            genCodeLine("      if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine(prefix + "      curLexState = jjnewLexState[jjmatchedKind];");
          }
          genCodeLine(prefix + "      curPos = 0;");
          genCodeLine(prefix + "      jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");

          genCodeLine(prefix + "      try {");
          genCodeLine(prefix + "         curChar = input_stream.readChar();");

          if (Options.getDebugTokenManager()) {
            genCodeLine("   debugStream.println("
                + (this.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
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

    if (this.hasMore) {
      genCodeLine(prefix + " }");
    }

    genCodeLine("  }");
    genCodeLine("}");
    genCodeLine("");
  }

  private void DumpSkipActions() {
    Action act;

    genCodeLine(getStatic() + "void SkipLexicalActions(Token matchedToken)");
    genCodeLine("{");
    genCodeLine("   switch(jjmatchedKind)");
    genCodeLine("   {");

    Outer:
    for (int i = 0; i < this.maxOrdinal; i++) {
      if ((this.toSkip[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (;;) {
        if ((((act = this.actions[i]) == null) || (act.getActionTokens() == null)
            || (act.getActionTokens().size() == 0)) && !this.canLoop[getLexerData().getState(i)]) {
          continue Outer;
        }

        genCodeLine("      case " + i + " :");

        if ((this.initMatch[getLexerData().getState(i)] == i) && this.canLoop[getLexerData().getState(i)]) {
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

        if (((act = this.actions[i]) == null) || (act.getActionTokens().size() == 0)) {
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

    genCodeLine(getStatic() + "void MoreLexicalActions()");
    genCodeLine("{");
    genCodeLine("   jjimageLen += (lengthOfMatch = jjmatchedPos + 1);");
    genCodeLine("   switch(jjmatchedKind)");
    genCodeLine("   {");

    Outer:
    for (int i = 0; i < this.maxOrdinal; i++) {
      if ((this.toMore[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (;;) {
        if ((((act = this.actions[i]) == null) || (act.getActionTokens() == null)
            || (act.getActionTokens().size() == 0)) && !this.canLoop[getLexerData().getState(i)]) {
          continue Outer;
        }

        genCodeLine("      case " + i + " :");

        if ((this.initMatch[getLexerData().getState(i)] == i) && this.canLoop[getLexerData().getState(i)]) {
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

        if (((act = this.actions[i]) == null) || (act.getActionTokens().size() == 0)) {
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

    genCodeLine(getStatic() + "void TokenLexicalActions(Token matchedToken)");
    genCodeLine("{");
    genCodeLine("   switch(jjmatchedKind)");
    genCodeLine("   {");

    Outer:
    for (i = 0; i < this.maxOrdinal; i++) {
      if ((this.toToken[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (;;) {
        if ((((act = this.actions[i]) == null) || (act.getActionTokens() == null)
            || (act.getActionTokens().size() == 0)) && !this.canLoop[getLexerData().getState(i)]) {
          continue Outer;
        }

        genCodeLine("      case " + i + " :");

        if ((this.initMatch[getLexerData().getState(i)] == i) && this.canLoop[getLexerData().getState(i)]) {
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

        if (((act = this.actions[i]) == null) || (act.getActionTokens().size() == 0)) {
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
    genCodeLine(getJavaStatic() + "private void " + "jjCheckNAdd(int state)");
    genCodeLine("{");
    genCodeLine("   if (jjrounds[state] != jjround)");
    genCodeLine("   {");
    genCodeLine("      jjstateSet[jjnewStateCnt++] = state;");
    genCodeLine("      jjrounds[state] = jjround;");
    genCodeLine("   }");
    genCodeLine("}");

    genCodeLine(getJavaStatic() + "private void " + "jjAddStates(int start, int end)");
    genCodeLine("{");
    genCodeLine("   do {");
    genCodeLine("      jjstateSet[jjnewStateCnt++] = jjnextStates[start];");
    genCodeLine("   } while (start++ != end);");
    genCodeLine("}");

    genCodeLine(getJavaStatic() + "private void " + "jjCheckNAddTwoStates(int state1, int state2)");
    genCodeLine("{");
    genCodeLine("   jjCheckNAdd(state1);");
    genCodeLine("   jjCheckNAdd(state2);");
    genCodeLine("}");
    genCodeLine("");

    if (getLexerData().jjCheckNAddStatesDualNeeded) {
      genCodeLine(getJavaStatic() + "private void " + "jjCheckNAddStates(int start, int end)");
      genCodeLine("{");
      genCodeLine("   do {");
      genCodeLine("      jjCheckNAdd(jjnextStates[start]);");
      genCodeLine("   } while (start++ != end);");
      genCodeLine("}");
      genCodeLine("");
    }

    if (getLexerData().jjCheckNAddStatesUnaryNeeded) {
      genCodeLine(getJavaStatic() + "private void " + "jjCheckNAddStates(int start)");
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

    for (int i = 0; i < this.maxLexStates; i++) {
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
          || (((this.toSkip[i / 64] & (1L << (i % 64))) == 0L) && ((this.toMore[i / 64] & (1L << (i % 64))) == 0L)
              && ((this.toToken[i / 64] & (1L << (i % 64))) == 0L))
          || ((this.toSkip[i / 64] & (1L << (i % 64))) != 0L) || ((this.toMore[i / 64] & (1L << (i % 64))) != 0L)
          || this.canReachOnMore[getLexerData().getState(i)]
          || ((getLexerData().ignoreCase() || this.ignoreCase[i]) && (!image.equals(image.toLowerCase(Locale.ENGLISH))
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

    while (++i < this.maxOrdinal) {
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
