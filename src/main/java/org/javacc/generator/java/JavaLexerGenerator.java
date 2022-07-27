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
import org.fastcc.utils.Encoding;
import org.javacc.generator.LexerData;
import org.javacc.generator.LexerGenerator;
import org.javacc.generator.LexerStateData;
import org.javacc.lexer.NfaState;
import org.javacc.parser.Action;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.JavaCCParserConstants;
import org.javacc.parser.Options;
import org.javacc.parser.RStringLiteral.KindInfo;
import org.javacc.parser.Token;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

/**
 * Generate lexer.
 */
public class JavaLexerGenerator extends LexerGenerator {

  private static final String TEMPLATE_LEXER = "/templates/Lexer.template";


  @Override
  protected final void dumpAll(LexerData data, List<Token> insertionPoint) throws IOException {
    SourceWriter writer = new SourceWriter(data.getParserName() + "TokenManager");

    dumpClassHeader(writer, data.getParserName(), data.getTokens(), insertionPoint);

    writer.setOption("stateNames", data.stateNames);
    writer.setOption("lohiBytes", data.lohiByte.keySet());
    writer.setOption("maxOrdinal", data.maxOrdinal);
    writer.setOption("maxLexStates", data.maxLexStates);
    writer.setOption("hasEmptyMatch", data.hasEmptyMatch);
    writer.setOption("hasSkip", data.hasSkip);
    writer.setOption("hasLoop", data.hasLoop);
    writer.setOption("hasMore", data.hasMore);
    writer.setOption("hasSpecial", data.hasSpecial);
    writer.setOption("hasMoreActions", data.hasMoreActions);
    writer.setOption("hasSkipActions", data.hasSkipActions);
    writer.setOption("hasTokenActions", data.hasTokenActions);
    writer.setOption("stateSetSize", data.stateSetSize);
    writer.setOption("hasActions", data.hasMoreActions || data.hasSkipActions || data.hasTokenActions);
    writer.setOption("lexStateNameLength", data.getStateCount());
    writer.setOption("defaultLexState", data.defaultLexState);
    writer.setOption("generatedStates", data.totalNumStates);
    writer.setOption("nonAsciiTableForMethod", data.nonAsciiTableForMethod);
    writer.setOption("jjCheckNAddStatesDualNeeded", data.jjCheckNAddStatesDualNeeded);
    writer.setOption("jjCheckNAddStatesUnaryNeeded", data.jjCheckNAddStatesUnaryNeeded);

    writer.setWriter("dumpNfaAndDfa", (p, i) -> dumpNfaAndDfa(p, data.getStateData((String) i)));
    writer.setWriter("DumpSkipActions", (p, i) -> DumpSkipActions(p, data));
    writer.setWriter("DumpMoreActions", (p, i) -> DumpMoreActions(p, data));
    writer.setWriter("DumpTokenActions", (p, i) -> DumpTokenActions(p, data));
    writer.setWriter("DumpFillToken", (p, i) -> JavaLexerGenerator.DumpFillToken(p, data));
    writer.setWriter("DumpStateSets", (p, i) -> DumpStateSets(p, data));
    writer.setWriter("DumpNonAsciiMoveMethod", (p, i) -> DumpNonAsciiMoveMethod(p, (NfaState) i, data));
    writer.setWriter("DumpGetNextToken", (p, i) -> DumpGetNextToken(p, data));
    writer.setWriter("dumpStaticVarDeclarations", (p, i) -> JavaLexerGenerator.DumpStaticVarDeclarations(p, data));

    writer.setFunction("getStrLiteralImages", i -> JavaLexerGenerator.getStrLiteralImages(data));
    writer.setFunction("getStatesForState", i -> getStatesForState(data));
    writer.setFunction("getKindForState", i -> getKindForState(data));
    writer.setFunction("getLohiBytes", i -> getLohiBytes(data, (int) i));

    writer.writeTemplate(JavaLexerGenerator.TEMPLATE_LEXER);
    saveOutput(writer);
  }


  private String getLohiBytes(LexerData data, int i) {
    return "{\n   0x" + Long.toHexString(data.lohiByte.get(i)[0]) + "L, " + "0x"
        + Long.toHexString(data.lohiByte.get(i)[1]) + "L, " + "0x" + Long.toHexString(data.lohiByte.get(i)[2]) + "L, "
        + "0x" + Long.toHexString(data.lohiByte.get(i)[3]) + "L\n}";
  }

  private final void dumpClassHeader(PrintWriter writer, String parserName, List<Token> tokens,
      List<Token> insertionPoint1) {
    int i, j;


    int l = 0, kind;
    i = 1;
    for (;;) {
      if (insertionPoint1.size() <= l) {
        break;
      }

      kind = insertionPoint1.get(l).kind;
      if ((kind == JavaCCParserConstants.PACKAGE) || (kind == JavaCCParserConstants.IMPORT)) {
        if (kind == JavaCCParserConstants.IMPORT) {}
        for (; i < insertionPoint1.size(); i++) {
          kind = insertionPoint1.get(i).kind;
          if ((kind == JavaCCParserConstants.SEMICOLON) || (kind == JavaCCParserConstants.ABSTRACT)
              || (kind == JavaCCParserConstants.FINAL) || (kind == JavaCCParserConstants.PUBLIC)
              || (kind == JavaCCParserConstants.CLASS) || (kind == JavaCCParserConstants.INTERFACE)) {
            this.cline = (insertionPoint1.get(l)).beginLine;
            this.ccol = (insertionPoint1.get(l)).beginColumn;
            for (j = l; j < i; j++) {
              genToken(writer, insertionPoint1.get(j));
            }
            if (kind == JavaCCParserConstants.SEMICOLON) {
              genToken(writer, insertionPoint1.get(j));
            }
            writer.println("");
            break;
          }
        }
        l = ++i;
      } else {
        break;
      }
    }

    writer.println("");
    writer.println("/** Token Manager. */");

    writer.print("class " + parserName + "TokenManager");
    writer.print(" implements " + parserName + "Constants");

    writer.println(" {");

    if ((tokens != null) && !tokens.isEmpty()) {
      genTokenSetup(tokens.get(0));
      this.ccol = 1;

      tokens.forEach(tt -> genToken(writer, tt));

      writer.println("");
    }
  }

  private static void DumpStaticVarDeclarations(PrintWriter writer, LexerData data) {
    int i;

    writer.println();
    writer.println("/** Lexer state names. */");
    writer.println("public static final String[] lexStateNames = {");
    for (i = 0; i < data.maxLexStates; i++) {
      writer.println("   \"" + data.getStateName(i) + "\",");
    }
    writer.println("};");

    {
      writer.println();
      writer.println("/** Lex State array. */");
      writer.print("public static final int[] jjnewLexState = {");

      for (i = 0; i < data.maxOrdinal; i++) {
        if ((i % 25) == 0) {
          writer.print("\n   ");
        }

        if (data.newLexState[i] == null) {
          writer.print("-1, ");
        } else {
          writer.print(data.getStateIndex(data.newLexState[i]) + ", ");
        }
      }
      writer.println("\n};");
    }

    {
      // Bit vector for TOKEN
      writer.print("static final long[] jjtoToken = {");
      for (i = 0; i < ((data.maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          writer.print("\n   ");
        }
        writer.print("0x" + Long.toHexString(data.toToken[i]) + "L, ");
      }
      writer.println("\n};");
    }

    {
      // Bit vector for SKIP
      writer.print("static final long[] jjtoSkip = {");
      for (i = 0; i < ((data.maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          writer.print("\n   ");
        }
        writer.print("0x" + Long.toHexString(data.toSkip[i]) + "L, ");
      }
      writer.println("\n};");
    }

    {
      // Bit vector for SPECIAL
      writer.print("static final long[] jjtoSpecial = {");
      for (i = 0; i < ((data.maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          writer.print("\n   ");
        }
        writer.print("0x" + Long.toHexString(data.toSpecial[i]) + "L, ");
      }
      writer.println("\n};");
    }

    {
      // Bit vector for MORE
      writer.print("static final long[] jjtoMore = {");
      for (i = 0; i < ((data.maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          writer.print("\n   ");
        }
        writer.print("0x" + Long.toHexString(data.toMore[i]) + "L, ");
      }
      writer.println("\n};");
    }
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

  private static void DumpFillToken(PrintWriter writer, LexerData data) {
    writer.println("protected Token jjFillToken()");
    writer.println("{");
    writer.println("   final Token t;");
    writer.println("   final String curTokenImage;");
    if (data.keepLineCol) {
      writer.println("   final int beginLine;");
      writer.println("   final int endLine;");
      writer.println("   final int beginColumn;");
      writer.println("   final int endColumn;");
    }

    if (data.hasEmptyMatch) {
      writer.println("   if (jjmatchedPos < 0)");
      writer.println("   {");
      writer.println("      if (image == null)");
      writer.println("         curTokenImage = \"\";");
      writer.println("      else");
      writer.println("         curTokenImage = image.toString();");

      if (data.keepLineCol) {
        writer.println("      beginLine = endLine = input_stream.getEndLine();");
        writer.println("      beginColumn = endColumn = input_stream.getEndColumn();");
      }

      writer.println("   }");
      writer.println("   else");
      writer.println("   {");
      writer.println("      String im = jjstrLiteralImages[jjmatchedKind];");
      writer.println("      curTokenImage = (im == null) ? input_stream.GetImage() : im;");

      if (data.keepLineCol) {
        writer.println("      beginLine = input_stream.getBeginLine();");
        writer.println("      beginColumn = input_stream.getBeginColumn();");
        writer.println("      endLine = input_stream.getEndLine();");
        writer.println("      endColumn = input_stream.getEndColumn();");
      }

      writer.println("   }");
    } else {
      writer.println("   String im = jjstrLiteralImages[jjmatchedKind];");
      writer.println("   curTokenImage = (im == null) ? input_stream.GetImage() : im;");
      if (data.keepLineCol) {
        writer.println("   beginLine = input_stream.getBeginLine();");
        writer.println("   beginColumn = input_stream.getBeginColumn();");
        writer.println("   endLine = input_stream.getEndLine();");
        writer.println("   endColumn = input_stream.getEndColumn();");
      }
    }

    writer.println("   t = Token.newToken(jjmatchedKind, curTokenImage);");

    if (data.keepLineCol) {
      writer.println("");
      writer.println("   t.beginLine = beginLine;");
      writer.println("   t.endLine = endLine;");
      writer.println("   t.beginColumn = beginColumn;");
      writer.println("   t.endColumn = endColumn;");
    }

    writer.println("");
    writer.println("   return t;");
    writer.println("}");
  }

  private void DumpGetNextToken(PrintWriter writer, LexerData data) {
    int i;

    writer.println("");
    writer.println("int curLexState = " + data.defaultLexState + ";");
    writer.println("int defaultLexState = " + data.defaultLexState + ";");
    writer.println("int jjnewStateCnt;");
    writer.println("int jjround;");
    writer.println("int jjmatchedPos;");
    writer.println("int jjmatchedKind;");
    writer.println("");
    writer.println("/** Get the next Token. */");
    writer.println("public Token getNextToken()");
    writer.println("{");
    if (data.hasSpecial) {
      writer.println("  Token specialToken = null;");
    }
    writer.println("  Token matchedToken;");
    writer.println("  int curPos = 0;");
    writer.println("");
    writer.println("  EOFLoop :\n  for (;;)");
    writer.println("  {");
    writer.println("   try");
    writer.println("   {");
    writer.println("      curChar = input_stream.BeginToken();");
    writer.println("   }");
    writer.println("   catch(Exception e)");
    writer.println("   {");

    if (Options.getDebugTokenManager()) {
      writer.println("      debugStream.println(\"Returning the <EOF> token.\\n\");");
    }

    writer.println("      jjmatchedKind = 0;");
    writer.println("      jjmatchedPos = -1;");
    writer.println("      matchedToken = jjFillToken();");

    if (data.hasSpecial) {
      writer.println("      matchedToken.specialToken = specialToken;");
    }

    if ((data.getNextStateForEof() != null) || (data.getActionForEof() != null)) {
      writer.println("      TokenLexicalActions(matchedToken);");
    }

    writer.println("      return matchedToken;");
    writer.println("   }");

    if (data.hasMoreActions || data.hasSkipActions || data.hasTokenActions) {
      writer.println("   image = jjimage;");
      writer.println("   image.setLength(0);");
      writer.println("   jjimageLen = 0;");
    }

    writer.println("");

    String prefix = "";
    if (data.hasMore) {
      writer.println("   for (;;)");
      writer.println("   {");
      prefix = "  ";
    }

    String endSwitch = "";
    String caseStr = "";
    // this also sets up the start state of the nfa
    if (data.maxLexStates > 1) {
      writer.println(prefix + "   switch(curLexState)");
      writer.println(prefix + "   {");
      endSwitch = prefix + "   }";
      caseStr = prefix + "     case ";
      prefix += "    ";
    }

    prefix += "   ";
    for (i = 0; i < data.maxLexStates; i++) {
      if (data.maxLexStates > 1) {
        writer.println(caseStr + i + ":");
      }

      if (data.singlesToSkip[i].HasTransitions()) {
        // added the backup(0) to make JIT happy
        writer.println(prefix + "try { input_stream.backup(0);");
        if ((data.singlesToSkip[i].asciiMoves[0] != 0L) && (data.singlesToSkip[i].asciiMoves[1] != 0L)) {
          writer.println(
              prefix + "   while ((curChar < 64" + " && (0x" + Long.toHexString(data.singlesToSkip[i].asciiMoves[0])
                  + "L & (1L << curChar)) != 0L) || \n" + prefix + "          (curChar >> 6) == 1" + " && (0x"
                  + Long.toHexString(data.singlesToSkip[i].asciiMoves[1]) + "L & (1L << (curChar & 077))) != 0L)");
        } else if (data.singlesToSkip[i].asciiMoves[1] == 0L) {
          writer.println(prefix + "   while (curChar <= " + (int) JavaLexerGenerator.MaxChar(data.singlesToSkip[i].asciiMoves[0])
              + " && (0x" + Long.toHexString(data.singlesToSkip[i].asciiMoves[0]) + "L & (1L << curChar)) != 0L)");
        } else if (data.singlesToSkip[i].asciiMoves[0] == 0L) {
          writer.println(prefix + "   while (curChar > 63 && curChar <= "
              + (JavaLexerGenerator.MaxChar(data.singlesToSkip[i].asciiMoves[1]) + 64) + " && (0x"
              + Long.toHexString(data.singlesToSkip[i].asciiMoves[1]) + "L & (1L << (curChar & 077))) != 0L)");
        }

        if (Options.getDebugTokenManager()) {
          writer.println(prefix + "{");
          writer.println("      debugStream.println("
              + (data.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
              + "\"Skipping character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \")\");");
        }
        writer.println(prefix + "      curChar = input_stream.BeginToken();");

        if (Options.getDebugTokenManager()) {
          writer.println(prefix + "}");
        }

        writer.println(prefix + "}");
        writer.println(prefix + "catch (java.io.IOException e1) { continue EOFLoop; }");
      }

      if ((data.initMatch[i] != Integer.MAX_VALUE) && (data.initMatch[i] != 0)) {
        if (Options.getDebugTokenManager()) {
          writer.println("      debugStream.println(\"   Matched the empty string as \" + tokenImage["
              + data.initMatch[i] + "] + \" token.\");");
        }

        writer.println(prefix + "jjmatchedKind = " + data.initMatch[i] + ";");
        writer.println(prefix + "jjmatchedPos = -1;");
        writer.println(prefix + "curPos = 0;");
      } else {
        writer.println(prefix + "jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
        writer.println(prefix + "jjmatchedPos = 0;");
      }

      if (Options.getDebugTokenManager()) {
        writer.println("      debugStream.println("
            + (data.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
            + "\"Current character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") "
            + "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
      }

      writer.println(prefix + "curPos = jjMoveStringLiteralDfa0_" + i + "();");
      if (data.canMatchAnyChar[i] != -1) {
        if ((data.initMatch[i] != Integer.MAX_VALUE) && (data.initMatch[i] != 0)) {
          writer.println(prefix + "if (jjmatchedPos < 0 || (jjmatchedPos == 0 && jjmatchedKind > "
              + data.canMatchAnyChar[i] + "))");
        } else {
          writer.println(prefix + "if (jjmatchedPos == 0 && jjmatchedKind > " + data.canMatchAnyChar[i] + ")");
        }
        writer.println(prefix + "{");

        if (Options.getDebugTokenManager()) {
          writer.println("           debugStream.println(\"   Current character matched as a \" + tokenImage["
              + data.canMatchAnyChar[i] + "] + \" token.\");");
        }
        writer.println(prefix + "   jjmatchedKind = " + data.canMatchAnyChar[i] + ";");

        if ((data.initMatch[i] != Integer.MAX_VALUE) && (data.initMatch[i] != 0)) {
          writer.println(prefix + "   jjmatchedPos = 0;");
        }

        writer.println(prefix + "}");
      }

      if (data.maxLexStates > 1) {
        writer.println(prefix + "break;");
      }
    }

    if (data.maxLexStates > 1) {
      writer.println(endSwitch);
    } else if (data.maxLexStates == 0) {
      writer.println("       jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
    }

    if (data.maxLexStates > 1) {
      prefix = "  ";
    } else {
      prefix = "";
    }

    if (data.maxLexStates > 0) {
      writer.println(prefix + "   if (jjmatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
      writer.println(prefix + "   {");
      writer.println(prefix + "      if (jjmatchedPos + 1 < curPos)");

      if (Options.getDebugTokenManager()) {
        writer.println(prefix + "      {");
        writer.println(prefix + "         debugStream.println("
            + "\"   Putting back \" + (curPos - jjmatchedPos - 1) + \" characters into the input stream.\");");
      }

      writer.println(prefix + "         input_stream.backup(curPos - jjmatchedPos - 1);");

      if (Options.getDebugTokenManager()) {
        writer.println(prefix + "      }");
      }

      if (Options.getDebugTokenManager()) {
        writer.println("    debugStream.println(" + "\"****** FOUND A \" + tokenImage[jjmatchedKind] + \" MATCH "
            + "(\" + TokenMgrException.addEscapes(new String(input_stream.GetSuffix(jjmatchedPos + 1))) + "
            + "\") ******\\n\");");
      }

      if (data.hasSkip || data.hasMore || data.hasSpecial) {
        writer
            .println(prefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
        writer.println(prefix + "      {");
      }

      writer.println(prefix + "         matchedToken = jjFillToken();");

      if (data.hasSpecial) {
        writer.println(prefix + "         matchedToken.specialToken = specialToken;");
      }

      if (data.hasTokenActions) {
        writer.println(prefix + "         TokenLexicalActions(matchedToken);");
      }

      if (data.maxLexStates > 1) {
        writer.println("       if (jjnewLexState[jjmatchedKind] != -1)");
        writer.println(prefix + "       curLexState = jjnewLexState[jjmatchedKind];");
      }

      writer.println(prefix + "         return matchedToken;");

      if (data.hasSkip || data.hasMore || data.hasSpecial) {
        writer.println(prefix + "      }");

        if (data.hasSkip || data.hasSpecial) {
          if (data.hasMore) {
            writer.println(
                prefix + "      else if ((jjtoSkip[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
          } else {
            writer.println(prefix + "      else");
          }

          writer.println(prefix + "      {");

          if (data.hasSpecial) {
            writer.println(
                prefix + "         if ((jjtoSpecial[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
            writer.println(prefix + "         {");

            writer.println(prefix + "            matchedToken = jjFillToken();");

            writer.println(prefix + "            if (specialToken == null)");
            writer.println(prefix + "               specialToken = matchedToken;");
            writer.println(prefix + "            else");
            writer.println(prefix + "            {");
            writer.println(prefix + "               matchedToken.specialToken = specialToken;");
            writer.println(prefix + "               specialToken = (specialToken.next = matchedToken);");
            writer.println(prefix + "            }");

            if (data.hasSkipActions) {
              writer.println(prefix + "            SkipLexicalActions(matchedToken);");
            }

            writer.println(prefix + "         }");

            if (data.hasSkipActions) {
              writer.println(prefix + "         else");
              writer.println(prefix + "            SkipLexicalActions(null);");
            }
          } else if (data.hasSkipActions) {
            writer.println(prefix + "         SkipLexicalActions(null);");
          }

          if (data.maxLexStates > 1) {
            writer.println("         if (jjnewLexState[jjmatchedKind] != -1)");
            writer.println(prefix + "         curLexState = jjnewLexState[jjmatchedKind];");
          }

          writer.println(prefix + "         continue EOFLoop;");
          writer.println(prefix + "      }");
        }

        if (data.hasMore) {
          if (data.hasMoreActions) {
            writer.println(prefix + "      MoreLexicalActions();");
          } else if (data.hasSkipActions || data.hasTokenActions) {
            writer.println(prefix + "      jjimageLen += jjmatchedPos + 1;");
          }

          if (data.maxLexStates > 1) {
            writer.println("      if (jjnewLexState[jjmatchedKind] != -1)");
            writer.println(prefix + "      curLexState = jjnewLexState[jjmatchedKind];");
          }
          writer.println(prefix + "      curPos = 0;");
          writer.println(prefix + "      jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");

          writer.println(prefix + "      try {");
          writer.println(prefix + "         curChar = input_stream.readChar();");

          if (Options.getDebugTokenManager()) {
            writer.println("   debugStream.println("
                + (data.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
                + "\"Current character : \" + "
                + "TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") "
                + "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
          }
          writer.println(prefix + "         continue;");
          writer.println(prefix + "      }");
          writer.println(prefix + "      catch (java.io.IOException e1) { }");
        }
      }

      writer.println(prefix + "   }");
      writer.println(prefix + "   int error_line = input_stream.getEndLine();");
      writer.println(prefix + "   int error_column = input_stream.getEndColumn();");
      writer.println(prefix + "   String error_after = null;");
      writer.println(prefix + "   boolean EOFSeen = false;");
      writer.println(prefix + "   try { input_stream.readChar(); input_stream.backup(1); }");
      writer.println(prefix + "   catch (java.io.IOException e1) {");
      writer.println(prefix + "      EOFSeen = true;");
      writer.println(prefix + "      error_after = curPos <= 1 ? \"\" : input_stream.GetImage();");
      writer.println(prefix + "      if (curChar == '\\n' || curChar == '\\r') {");
      writer.println(prefix + "         error_line++;");
      writer.println(prefix + "         error_column = 0;");
      writer.println(prefix + "      }");
      writer.println(prefix + "      else");
      writer.println(prefix + "         error_column++;");
      writer.println(prefix + "   }");
      writer.println(prefix + "   if (!EOFSeen) {");
      writer.println(prefix + "      input_stream.backup(1);");
      writer.println(prefix + "      error_after = curPos <= 1 ? \"\" : input_stream.GetImage();");
      writer.println(prefix + "   }");
      writer.println(prefix + "   throw new TokenMgrException("
          + "EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrException.LEXICAL_ERROR);");
    }

    if (data.hasMore) {
      writer.println(prefix + " }");
    }

    writer.println("  }");
    writer.println("}");
    writer.println("");
  }

  private void DumpSkipActions(PrintWriter writer, LexerData data) {
    Action act;

    writer.println("void SkipLexicalActions(Token matchedToken)");
    writer.println("{");
    writer.println("   switch(jjmatchedKind)");
    writer.println("   {");

    Outer:
    for (int i = 0; i < data.maxOrdinal; i++) {
      if ((data.toSkip[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (;;) {
        if ((((act = data.actions[i]) == null) || (act.getActionTokens() == null)
            || (act.getActionTokens().size() == 0)) && !data.canLoop[data.getState(i)]) {
          continue Outer;
        }

        writer.println("      case " + i + " :");

        if ((data.initMatch[data.getState(i)] == i) && data.canLoop[data.getState(i)]) {
          writer.println("         if (jjmatchedPos == -1)");
          writer.println("         {");
          writer.println("            if (jjbeenHere[" + data.getState(i) + "] &&");
          writer.println("                jjemptyLineNo[" + data.getState(i) + "] == input_stream.getBeginLine() &&");
          writer.println("                jjemptyColNo[" + data.getState(i) + "] == input_stream.getBeginColumn())");
          writer.println("               throw new TokenMgrException("
              + "(\"Error: Bailing out of infinite loop caused by repeated empty string matches "
              + "at line \" + input_stream.getBeginLine() + \", "
              + "column \" + input_stream.getBeginColumn() + \".\"), TokenMgrException.LOOP_DETECTED);");
          writer.println("            jjemptyLineNo[" + data.getState(i) + "] = input_stream.getBeginLine();");
          writer.println("            jjemptyColNo[" + data.getState(i) + "] = input_stream.getBeginColumn();");
          writer.println("            jjbeenHere[" + data.getState(i) + "] = true;");
          writer.println("         }");
        }

        if (((act = data.actions[i]) == null) || (act.getActionTokens().size() == 0)) {
          break;
        }

        writer.print("         image.append");
        if (data.getImage(i) != null) {
          writer.println("(jjstrLiteralImages[" + i + "]);");
          writer.println("        lengthOfMatch = jjstrLiteralImages[" + i + "].length();");
        } else {
          writer.println("(input_stream.GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
        }

        genTokenSetup(act.getActionTokens().get(0));
        this.ccol = 1;

        for (Token element : act.getActionTokens()) {
          genToken(writer, element);
        }
        writer.println("");

        break;
      }

      writer.println("         break;");
    }

    writer.println("      default :");
    writer.println("         break;");
    writer.println("   }");
    writer.println("}");
  }

  private void DumpMoreActions(PrintWriter writer, LexerData data) {
    Action act;

    writer.println("void MoreLexicalActions()");
    writer.println("{");
    writer.println("   jjimageLen += (lengthOfMatch = jjmatchedPos + 1);");
    writer.println("   switch(jjmatchedKind)");
    writer.println("   {");

    Outer:
    for (int i = 0; i < data.maxOrdinal; i++) {
      if ((data.toMore[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (;;) {
        if ((((act = data.actions[i]) == null) || (act.getActionTokens() == null)
            || (act.getActionTokens().size() == 0)) && !data.canLoop[data.getState(i)]) {
          continue Outer;
        }

        writer.println("      case " + i + " :");

        if ((data.initMatch[data.getState(i)] == i) && data.canLoop[data.getState(i)]) {
          writer.println("         if (jjmatchedPos == -1)");
          writer.println("         {");
          writer.println("            if (jjbeenHere[" + data.getState(i) + "] &&");
          writer.println("                jjemptyLineNo[" + data.getState(i) + "] == input_stream.getBeginLine() &&");
          writer.println("                jjemptyColNo[" + data.getState(i) + "] == input_stream.getBeginColumn())");
          writer.println("               throw new TokenMgrException("
              + "(\"Error: Bailing out of infinite loop caused by repeated empty string matches "
              + "at line \" + input_stream.getBeginLine() + \", "
              + "column \" + input_stream.getBeginColumn() + \".\"), TokenMgrException.LOOP_DETECTED);");
          writer.println("            jjemptyLineNo[" + data.getState(i) + "] = input_stream.getBeginLine();");
          writer.println("            jjemptyColNo[" + data.getState(i) + "] = input_stream.getBeginColumn();");
          writer.println("            jjbeenHere[" + data.getState(i) + "] = true;");
          writer.println("         }");
        }

        if (((act = data.actions[i]) == null) || (act.getActionTokens().size() == 0)) {
          break;
        }

        writer.print("         image.append");

        if (data.getImage(i) != null) {
          writer.println("(jjstrLiteralImages[" + i + "]);");
        } else {
          writer.println("(input_stream.GetSuffix(jjimageLen));");
        }

        writer.println("         jjimageLen = 0;");
        genTokenSetup(act.getActionTokens().get(0));
        this.ccol = 1;

        for (Token element : act.getActionTokens()) {
          genToken(writer, element);
        }
        writer.println("");

        break;
      }

      writer.println("         break;");
    }

    writer.println("      default :");
    writer.println("         break;");

    writer.println("   }");
    writer.println("}");
  }

  private void DumpTokenActions(PrintWriter writer, LexerData data) {
    Action act;
    int i;

    writer.println("void TokenLexicalActions(Token matchedToken)");
    writer.println("{");
    writer.println("   switch(jjmatchedKind)");
    writer.println("   {");

    Outer:
    for (i = 0; i < data.maxOrdinal; i++) {
      if ((data.toToken[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (;;) {
        if ((((act = data.actions[i]) == null) || (act.getActionTokens() == null)
            || (act.getActionTokens().size() == 0)) && !data.canLoop[data.getState(i)]) {
          continue Outer;
        }

        writer.println("      case " + i + " :");

        if ((data.initMatch[data.getState(i)] == i) && data.canLoop[data.getState(i)]) {
          writer.println("         if (jjmatchedPos == -1)");
          writer.println("         {");
          writer.println("            if (jjbeenHere[" + data.getState(i) + "] &&");
          writer.println("                jjemptyLineNo[" + data.getState(i) + "] == input_stream.getBeginLine() &&");
          writer.println("                jjemptyColNo[" + data.getState(i) + "] == input_stream.getBeginColumn())");
          writer.println("               throw new TokenMgrException("
              + "(\"Error: Bailing out of infinite loop caused by repeated empty string matches "
              + "at line \" + input_stream.getBeginLine() + \", "
              + "column \" + input_stream.getBeginColumn() + \".\"), TokenMgrException.LOOP_DETECTED);");
          writer.println("            jjemptyLineNo[" + data.getState(i) + "] = input_stream.getBeginLine();");
          writer.println("            jjemptyColNo[" + data.getState(i) + "] = input_stream.getBeginColumn();");
          writer.println("            jjbeenHere[" + data.getState(i) + "] = true;");
          writer.println("         }");
        }

        if (((act = data.actions[i]) == null) || (act.getActionTokens().size() == 0)) {
          break;
        }

        if (i == 0) {
          writer.println("      image.setLength(0);"); // For EOF no image is there
        } else {
          writer.print("        image.append");

          if (data.getImage(i) != null) {
            writer.println("(jjstrLiteralImages[" + i + "]);");
            writer.println("        lengthOfMatch = jjstrLiteralImages[" + i + "].length();");
          } else {
            writer.println("(input_stream.GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
          }
        }

        genTokenSetup(act.getActionTokens().get(0));
        this.ccol = 1;

        for (Token element : act.getActionTokens()) {
          genToken(writer, element);
        }
        writer.println("");

        break;
      }

      writer.println("         break;");
    }

    writer.println("      default :");
    writer.println("         break;");
    writer.println("   }");
    writer.println("}");
  }

  private void DumpStateSets(PrintWriter writer, LexerData data) {
    int cnt = 0;

    writer.print("static final int[] jjnextStates = {");
    if (data.orderedStateSet.size() > 0) {
      for (int[] set : data.orderedStateSet) {
        for (int element : set) {
          if ((cnt++ % 16) == 0) {
            writer.print("\n   ");
          }

          writer.print(element + ", ");
        }
      }
    } else {
      writer.print("0");
    }

    writer.println("\n};");
  }

  private static String getStrLiteralImages(LexerData data) {
    if (data.getImageCount() <= 0) {
      return "";
    }

    String image;
    int i;
    int charCnt = 0; // Set to zero in reInit() but just to be sure

    data.setImage(0, "");

    StringWriter buffer = new StringWriter();
    PrintWriter writer = new PrintWriter(buffer);
    for (i = 0; i < data.getImageCount(); i++) {
      if (((image = data.getImage(i)) == null)
          || (((data.toSkip[i / 64] & (1L << (i % 64))) == 0L) && ((data.toMore[i / 64] & (1L << (i % 64))) == 0L)
              && ((data.toToken[i / 64] & (1L << (i % 64))) == 0L))
          || ((data.toSkip[i / 64] & (1L << (i % 64))) != 0L) || ((data.toMore[i / 64] & (1L << (i % 64))) != 0L)
          || data.canReachOnMore[data.getState(i)]
          || ((data.ignoreCase() || data.ignoreCase[i]) && (!image.equals(image.toLowerCase(Locale.ENGLISH))
              || !image.equals(image.toUpperCase(Locale.ENGLISH))))) {
        data.setImage(i, null);
        if ((charCnt += 6) > 80) {
          writer.println("");
          charCnt = 0;
        }

        writer.print("null, ");
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
        writer.println("");
        charCnt = 0;
      }

      writer.print(toPrint);
    }

    while (++i < data.maxOrdinal) {
      if ((charCnt += 6) > 80) {
        writer.println("");
        charCnt = 0;
      }

      writer.print("null, ");
      continue;
    }
    writer.flush();
    return buffer.toString();
  }

  private void DumpStartWithStates(PrintWriter writer, LexerStateData data) {
    writer.println("private int " + "jjStartNfaWithStates" + data.lexStateSuffix + "(int pos, int kind, int state)");
    writer.println("{");
    writer.println("   jjmatchedKind = kind;");
    writer.println("   jjmatchedPos = pos;");

    if (Options.getDebugTokenManager()) {
      writer.println("   debugStream.println(\"   No more string literal token matches are possible.\");");
      writer.println("   debugStream.println(\"   Currently matched the first \" "
          + "+ (jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
    }

    writer.println("   try { curChar = input_stream.readChar(); }");
    writer.println("   catch(java.io.IOException e) { return pos + 1; }");

    if (Options.getDebugTokenManager()) {
      writer.println("   debugStream.println("
          + (data.global.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
          + "\"Current character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") "
          + "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
    }
    writer.println("   return jjMoveNfa" + data.lexStateSuffix + "(state, pos + 1);");
    writer.println("}");
  }

  @Override
  protected final void DumpHeadForCase(PrintWriter writer, int byteNum) {
    if (byteNum == 0) {
      writer.println("         long l = 1L << curChar;");
    } else if (byteNum == 1) {
      writer.println("         long l = 1L << (curChar & 077);");
    } else {
      writer.println("         int hiByte = (curChar >> 8);");
      writer.println("         int i1 = hiByte >> 6;");
      writer.println("         long l1 = 1L << (hiByte & 077);");
      writer.println("         int i2 = (curChar & 0xff) >> 6;");
      writer.println("         long l2 = 1L << (curChar & 077);");
    }

    // writer.println(" MatchLoop: do");
    writer.println("         do");
    writer.println("         {");

    writer.println("            switch(jjstateSet[--i])");
    writer.println("            {");
  }

  private void DumpNonAsciiMoveMethod(PrintWriter writer, NfaState state, LexerData data) {
    int j;
    writer.println("private static final boolean jjCanMove_" + state.nonAsciiMethod
        + "(int hiByte, int i1, int i2, long l1, long l2)");
    writer.println("{");
    writer.println("   switch(hiByte)");
    writer.println("   {");

    if ((state.loByteVec != null) && (state.loByteVec.size() > 0)) {
      for (j = 0; j < state.loByteVec.size(); j += 2) {
        writer.println("      case " + state.loByteVec.get(j).intValue() + ":");
        if (!NfaState.AllBitsSet(data.allBitVectors.get(state.loByteVec.get(j + 1).intValue()))) {
          writer.println(
              "         return ((jjbitVec" + state.loByteVec.get(j + 1).intValue() + "[i2" + "] & l2) != 0L);");
        } else {
          writer.println("            return true;");
        }
      }
    }

    writer.println("      default :");

    if ((state.nonAsciiMoveIndices != null) && ((j = state.nonAsciiMoveIndices.length) > 0)) {
      do {
        if (!NfaState.AllBitsSet(data.allBitVectors.get(state.nonAsciiMoveIndices[j - 2]))) {
          writer.println("         if ((jjbitVec" + state.nonAsciiMoveIndices[j - 2] + "[i1] & l1) != 0L)");
        }
        if (!NfaState.AllBitsSet(data.allBitVectors.get(state.nonAsciiMoveIndices[j - 1]))) {
          writer.println("            if ((jjbitVec" + state.nonAsciiMoveIndices[j - 1] + "[i2] & l2) == 0L)");
          writer.println("               return false;");
          writer.println("            else");
        }
        writer.println("            return true;");
      } while ((j -= 2) > 0);
    }

    writer.println("         return false;");
    writer.println("   }");
    writer.println("}");
  }

  private String getStatesForState(LexerData data) {
    if (data.statesForState == null) {
      assert (false) : "This should never be null.";
      return "null";
    }

    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < data.maxLexStates; i++) {
      if (data.statesForState[i] == null) {
        builder.append("{},");
        continue;
      }
      builder.append("{");
      for (int j = 0; j < data.statesForState[i].length; j++) {
        int[] stateSet = data.statesForState[i][j];
        if (stateSet == null) {
          builder.append("{ " + j + " },");
          continue;
        }
        builder.append("{ ");
        for (int element : stateSet) {
          builder.append(element + ",");
        }
        builder.append("},");
      }
      builder.append("},");
    }
    return String.format("{%s}", builder.toString());
  }

  private String getKindForState(LexerData data) {
    if (data.kinds == null) {
      return "null";
    }

    StringBuilder builder = new StringBuilder();
    boolean moreThanOne = false;
    for (int[] kind : data.kinds) {
      if (moreThanOne) {
        builder.append(",");
      }
      moreThanOne = true;
      if (kind == null) {
        builder.append("{}");
      } else {
        builder.append("{ ");
        for (int element : kind) {
          builder.append(element);
          builder.append(",");
        }
        builder.append("}");
      }
    }
    return String.format("{%s}", builder.toString());
  }

  private final void dumpNfaAndDfa(PrintWriter writer, LexerStateData stateData) {
    if (stateData.hasNFA && !stateData.isMixedState()) {
      DumpNfaStartStatesCode(writer, stateData, stateData.statesForPos);
    }

    DumpDfaCode(writer, stateData);
    if (stateData.hasNFA) {
      DumpMoveNfa(writer, stateData);
    }
  }

  private final void DumpNfaStartStatesCode(PrintWriter writer, LexerStateData data,
      Hashtable<String, long[]>[] statesForPos) {
    if (data.maxStrKind == 0) { // No need to generate this function
      return;
    }

    int i, maxKindsReqd = (data.maxStrKind / 64) + 1;
    boolean condGenerated = false;
    int ind = 0;

    StringBuilder params = new StringBuilder();
    for (i = 0; i < (maxKindsReqd - 1); i++) {
      params.append("long active" + i + ", ");
    }
    params.append("long active" + i + ")");

    writer.print("private final int jjStopStringLiteralDfa" + data.lexStateSuffix + "(int pos, " + params);
    writer.println("{");

    if (Options.getDebugTokenManager()) {
      writer.println("      debugStream.println(\"   No more string literal token matches are possible.\");");
    }

    writer.println("   switch (pos)");
    writer.println("   {");

    for (i = 0; i < (data.maxLen - 1); i++) {
      if (statesForPos[i] == null) {
        continue;
      }

      writer.println("      case " + i + ":");

      Enumeration<String> e = statesForPos[i].keys();
      while (e.hasMoreElements()) {
        String stateSetString = e.nextElement();
        long[] actives = statesForPos[i].get(stateSetString);

        for (int j = 0; j < maxKindsReqd; j++) {
          if (actives[j] == 0L) {
            continue;
          }

          if (condGenerated) {
            writer.print(" || ");
          } else {
            writer.print("         if (");
          }

          condGenerated = true;

          writer.print("(active" + j + " & 0x" + Long.toHexString(actives[j]) + "L) != 0L");
        }

        if (condGenerated) {
          writer.println(")");

          String kindStr = stateSetString.substring(0, ind = stateSetString.indexOf(", "));
          String afterKind = stateSetString.substring(ind + 2);
          int jjmatchedPos = Integer.parseInt(afterKind.substring(0, afterKind.indexOf(", ")));

          if (!kindStr.equals(String.valueOf(Integer.MAX_VALUE))) {
            writer.println("         {");
          }

          if (!kindStr.equals(String.valueOf(Integer.MAX_VALUE))) {
            if (i == 0) {
              writer.println("            jjmatchedKind = " + kindStr + ";");

              if (((data.global.initMatch[data.getStateIndex()] != 0)
                  && (data.global.initMatch[data.getStateIndex()] != Integer.MAX_VALUE))) {
                writer.println("            jjmatchedPos = 0;");
              }
            } else if (i == jjmatchedPos) {
              if (data.subStringAtPos[i]) {
                writer.println("            if (jjmatchedPos != " + i + ")");
                writer.println("            {");
                writer.println("               jjmatchedKind = " + kindStr + ";");
                writer.println("               jjmatchedPos = " + i + ";");
                writer.println("            }");
              } else {
                writer.println("            jjmatchedKind = " + kindStr + ";");
                writer.println("            jjmatchedPos = " + i + ";");
              }
            } else {
              if (jjmatchedPos > 0) {
                writer.println("            if (jjmatchedPos < " + jjmatchedPos + ")");
              } else {
                writer.println("            if (jjmatchedPos == 0)");
              }
              writer.println("            {");
              writer.println("               jjmatchedKind = " + kindStr + ";");
              writer.println("               jjmatchedPos = " + jjmatchedPos + ";");
              writer.println("            }");
            }
          }

          kindStr = stateSetString.substring(0, ind = stateSetString.indexOf(", "));
          afterKind = stateSetString.substring(ind + 2);
          stateSetString = afterKind.substring(afterKind.indexOf(", ") + 2);

          if (stateSetString.equals("null;")) {
            writer.println("            return -1;");
          } else {
            writer.println("            return " + getCompositeStateSet(data, stateSetString, true) + ";");
          }

          if (!kindStr.equals(String.valueOf(Integer.MAX_VALUE))) {
            writer.println("         }");
          }
          condGenerated = false;
        }
      }

      writer.println("         return -1;");
    }

    writer.println("      default :");
    writer.println("         return -1;");
    writer.println("   }");
    writer.println("}");

    params.setLength(0);
    params.append("(int pos, ");
    for (i = 0; i < (maxKindsReqd - 1); i++) {
      params.append("long active" + i + ", ");
    }
    params.append("long active" + i + ")");

    writer.print("private final int jjStartNfa" + data.lexStateSuffix + params);
    writer.println("{");

    if (data.isMixedState()) {
      if (data.generatedStates() != 0) {
        writer.println("   return jjMoveNfa" + data.lexStateSuffix + "(" + InitStateName(data) + ", pos + 1);");
      } else {
        writer.println("   return pos + 1;");
      }

      writer.println("}");
      return;
    }

    writer.print(
        "   return jjMoveNfa" + data.lexStateSuffix + "(" + "jjStopStringLiteralDfa" + data.lexStateSuffix + "(pos, ");
    for (i = 0; i < (maxKindsReqd - 1); i++) {
      writer.print("active" + i + ", ");
    }
    writer.print("active" + i + ")");
    writer.println(", pos + 1);");
    writer.println("}");
  }

  private final void DumpDfaCode(PrintWriter writer, LexerStateData data) {
    Hashtable<String, ?> tab;
    String key;
    KindInfo info;
    int maxLongsReqd = (data.maxStrKind / 64) + 1;
    int i, j, k;
    boolean ifGenerated;
    data.global.maxLongsReqd[data.getStateIndex()] = maxLongsReqd;

    if (data.maxLen == 0) {
      writer.println("private int " + "jjMoveStringLiteralDfa0" + data.lexStateSuffix + "()");
      DumpNullStrLiterals(writer, data);
      return;
    }

    if (!data.global.boilerPlateDumped) {
      writer.println("private int " + "jjStopAtPos(int pos, int kind)");
      writer.println("{");
      writer.println("   jjmatchedKind = kind;");
      writer.println("   jjmatchedPos = pos;");

      if (Options.getDebugTokenManager()) {
        writer.println("   debugStream.println(\"   No more string literal token matches are possible.\");");
        writer.println("   debugStream.println(\"   Currently matched the first \" + (jjmatchedPos + 1) + "
            + "\" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
      }
      writer.println("   return pos + 1;");
      writer.println("}");
      data.global.boilerPlateDumped = true;
    }

    for (i = 0; i < data.maxLen; i++) {
      boolean atLeastOne = false;
      boolean startNfaNeeded = false;
      tab = data.charPosKind.get(i);
      String[] keys = LexerGenerator.ReArrange(tab);

      StringBuilder params = new StringBuilder();
      params.append("(");
      if (i != 0) {
        if (i == 1) {
          for (j = 0; j < (maxLongsReqd - 1); j++) {
            if (i <= data.maxLenForActive[j]) {
              if (atLeastOne) {
                params.append(", ");
              } else {
                atLeastOne = true;
              }
              params.append("long active" + j);
            }
          }

          if (i <= data.maxLenForActive[j]) {
            if (atLeastOne) {
              params.append(", ");
            }
            params.append("long active" + j);
          }
        } else {
          for (j = 0; j < (maxLongsReqd - 1); j++) {
            if (i <= (data.maxLenForActive[j] + 1)) {
              if (atLeastOne) {
                params.append(", ");
              } else {
                atLeastOne = true;
              }
              params.append("long old" + j + ", " + "long active" + j);
            }
          }

          if (i <= (data.maxLenForActive[j] + 1)) {
            if (atLeastOne) {
              params.append(", ");
            }
            params.append("long old" + j + ", " + "long active" + j);
          }
        }
      }
      params.append(")");
      writer.print("private int " + "jjMoveStringLiteralDfa" + i + data.lexStateSuffix + params);
      writer.println("{");

      if (i != 0) {
        if (i > 1) {
          atLeastOne = false;
          writer.print("   if ((");

          for (j = 0; j < (maxLongsReqd - 1); j++) {
            if (i <= (data.maxLenForActive[j] + 1)) {
              if (atLeastOne) {
                writer.print(" | ");
              } else {
                atLeastOne = true;
              }
              writer.print("(active" + j + " &= old" + j + ")");
            }
          }

          if (i <= (data.maxLenForActive[j] + 1)) {
            if (atLeastOne) {
              writer.print(" | ");
            }
            writer.print("(active" + j + " &= old" + j + ")");
          }

          writer.println(") == 0L)");
          if (!data.isMixedState() && (data.generatedStates() != 0)) {
            writer.print("      return jjStartNfa" + data.lexStateSuffix + "(" + (i - 2) + ", ");
            for (j = 0; j < (maxLongsReqd - 1); j++) {
              if (i <= (data.maxLenForActive[j] + 1)) {
                writer.print("old" + j + ", ");
              } else {
                writer.print("0L, ");
              }
            }
            if (i <= (data.maxLenForActive[j] + 1)) {
              writer.println("old" + j + ");");
            } else {
              writer.println("0L);");
            }
          } else if (data.generatedStates() != 0) {
            writer.println(
                "      return jjMoveNfa" + data.lexStateSuffix + "(" + InitStateName(data) + ", " + (i - 1) + ");");
          } else {
            writer.println("      return " + i + ";");
          }
        }

        if ((i != 0) && Options.getDebugTokenManager()) {
          writer.println(
              "   if (jjmatchedKind != 0 && jjmatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
          writer.println("      debugStream.println(\"   Currently matched the first \" + "
              + "(jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
          writer.println("   debugStream.println(\"   Possible string literal matches : { \"");

          for (int vecs = 0; vecs < ((data.maxStrKind / 64) + 1); vecs++) {
            if (i <= data.maxLenForActive[vecs]) {
              writer.println(" +");
              writer.print("         jjKindsForBitVector(" + vecs + ", ");
              writer.print("active" + vecs + ") ");
            }
          }

          writer.println(" + \" } \");");
        }

        writer.println("   try { curChar = input_stream.readChar(); }");
        writer.println("   catch(java.io.IOException e) {");

        if (!data.isMixedState() && (data.generatedStates() != 0)) {
          writer.print("      jjStopStringLiteralDfa" + data.lexStateSuffix + "(" + (i - 1) + ", ");
          for (k = 0; k < (maxLongsReqd - 1); k++) {
            if (i <= data.maxLenForActive[k]) {
              writer.print("active" + k + ", ");
            } else {
              writer.print("0L, ");
            }
          }

          if (i <= data.maxLenForActive[k]) {
            writer.println("active" + k + ");");
          } else {
            writer.println("0L);");
          }


          if ((i != 0) && Options.getDebugTokenManager()) {
            writer.println(
                "      if (jjmatchedKind != 0 && jjmatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
            writer.println("         debugStream.println(\"   Currently matched the first \" + "
                + "(jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
          }

          writer.println("      return " + i + ";");
        } else if (data.generatedStates() != 0) {
          writer
              .println("   return jjMoveNfa" + data.lexStateSuffix + "(" + InitStateName(data) + ", " + (i - 1) + ");");
        } else {
          writer.println("      return " + i + ";");
        }

        writer.println("   }");
      }

      if ((i != 0) && Options.getDebugTokenManager()) {
        writer.println("   debugStream.println("
            + (data.global.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
            + "\"Current character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") "
            + "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
      }

      writer.println("   switch(curChar)");
      writer.println("   {");

      CaseLoop:
      for (String key2 : keys) {
        key = key2;
        info = (KindInfo) tab.get(key);
        ifGenerated = false;
        char c = key.charAt(0);

        if ((i == 0) && (c < 128) && (info.finalKindCnt != 0)
            && ((data.generatedStates() == 0) || !CanStartNfaUsingAscii(data, c))) {
          int kind;
          for (j = 0; j < maxLongsReqd; j++) {
            if (info.finalKinds[j] != 0L) {
              break;
            }
          }

          for (k = 0; k < 64; k++) {
            if (((info.finalKinds[j] & (1L << k)) != 0L) && !data.subString[kind = ((j * 64) + k)]) {
              if (((data.intermediateKinds != null) && (data.intermediateKinds[((j * 64) + k)] != null)
                  && (data.intermediateKinds[((j * 64) + k)][i] < ((j * 64) + k))
                  && (data.intermediateMatchedPos != null) && (data.intermediateMatchedPos[((j * 64) + k)][i] == i))
                  || ((data.global.canMatchAnyChar[data.getStateIndex()] >= 0)
                      && (data.global.canMatchAnyChar[data.getStateIndex()] < ((j * 64) + k)))) {
                break;
              } else if (((data.global.toSkip[kind / 64] & (1L << (kind % 64))) != 0L)
                  && ((data.global.toSpecial[kind / 64] & (1L << (kind % 64))) == 0L)
                  && (data.global.actions[kind] == null) && (data.global.newLexState[kind] == null)) {
                continue CaseLoop;
              }
            }
          }
        }

        // Since we know key is a single character ...
        if (data.ignoreCase()) {
          if (c != Character.toUpperCase(c)) {
            writer.println("      case " + (int) Character.toUpperCase(c) + ":");
          }

          if (c != Character.toLowerCase(c)) {
            writer.println("      case " + (int) Character.toLowerCase(c) + ":");
          }
        }

        writer.println("      case " + (int) c + ":");

        long matchedKind;
        String prefix = (i == 0) ? "         " : "            ";

        if (info.finalKindCnt != 0) {
          for (j = 0; j < maxLongsReqd; j++) {
            if ((matchedKind = info.finalKinds[j]) == 0L) {
              continue;
            }

            for (k = 0; k < 64; k++) {
              if ((matchedKind & (1L << k)) == 0L) {
                continue;
              }

              if (ifGenerated) {
                writer.print("         else if ");
              } else if (i != 0) {
                writer.print("         if ");
              }

              ifGenerated = true;

              int kindToPrint;
              if (i != 0) {
                writer.println("((active" + j + " & 0x" + Long.toHexString(1L << k) + "L) != 0L)");
              }

              if ((data.intermediateKinds != null) && (data.intermediateKinds[((j * 64) + k)] != null)
                  && (data.intermediateKinds[((j * 64) + k)][i] < ((j * 64) + k))
                  && (data.intermediateMatchedPos != null) && (data.intermediateMatchedPos[((j * 64) + k)][i] == i)) {
                JavaCCErrors.warning(" \"" + Encoding.escape(data.global.getImage((j * 64) + k))
                    + "\" cannot be matched as a string literal token " + "at line "
                    + GetLine(data.global, (j * 64) + k) + ", column " + GetColumn(data.global, (j * 64) + k)
                    + ". It will be matched as " + GetLabel(data.global, data.intermediateKinds[((j * 64) + k)][i])
                    + ".");
                kindToPrint = data.intermediateKinds[((j * 64) + k)][i];
              } else if ((i == 0) && (data.global.canMatchAnyChar[data.getStateIndex()] >= 0)
                  && (data.global.canMatchAnyChar[data.getStateIndex()] < ((j * 64) + k))) {
                JavaCCErrors.warning(" \"" + Encoding.escape(data.global.getImage((j * 64) + k))
                    + "\" cannot be matched as a string literal token " + "at line "
                    + GetLine(data.global, (j * 64) + k) + ", column " + GetColumn(data.global, (j * 64) + k)
                    + ". It will be matched as "
                    + GetLabel(data.global, data.global.canMatchAnyChar[data.getStateIndex()]) + ".");
                kindToPrint = data.global.canMatchAnyChar[data.getStateIndex()];
              } else {
                kindToPrint = (j * 64) + k;
              }

              if (!data.subString[((j * 64) + k)]) {
                int stateSetName = GetStateSetForKind(data, i, (j * 64) + k);

                if (stateSetName != -1) {
                  writer.println(prefix + "return jjStartNfaWithStates" + data.lexStateSuffix + "(" + i + ", "
                      + kindToPrint + ", " + stateSetName + ");");
                } else {
                  writer.println(prefix + "return jjStopAtPos" + "(" + i + ", " + kindToPrint + ");");
                }
              } else if (((data.global.initMatch[data.getStateIndex()] != 0)
                  && (data.global.initMatch[data.getStateIndex()] != Integer.MAX_VALUE)) || (i != 0)) {
                writer.println("         {");
                writer.println(prefix + "jjmatchedKind = " + kindToPrint + ";");
                writer.println(prefix + "jjmatchedPos = " + i + ";");
                writer.println("         }");
              } else {
                writer.println(prefix + "jjmatchedKind = " + kindToPrint + ";");
              }
            }
          }
        }

        if (info.validKindCnt != 0) {
          atLeastOne = false;

          if (i == 0) {
            writer.print("         return ");

            writer.print("jjMoveStringLiteralDfa" + (i + 1) + data.lexStateSuffix + "(");
            for (j = 0; j < (maxLongsReqd - 1); j++) {
              if ((i + 1) <= data.maxLenForActive[j]) {
                if (atLeastOne) {
                  writer.print(", ");
                } else {
                  atLeastOne = true;
                }

                writer.print("0x" + Long.toHexString(info.validKinds[j]) + "L");
              }
            }

            if ((i + 1) <= data.maxLenForActive[j]) {
              if (atLeastOne) {
                writer.print(", ");
              }

              writer.print("0x" + Long.toHexString(info.validKinds[j]) + "L");
            }
            writer.println(");");
          } else {
            writer.print("         return ");

            writer.print("jjMoveStringLiteralDfa" + (i + 1) + data.lexStateSuffix + "(");

            for (j = 0; j < (maxLongsReqd - 1); j++) {
              if ((i + 1) <= (data.maxLenForActive[j] + 1)) {
                if (atLeastOne) {
                  writer.print(", ");
                } else {
                  atLeastOne = true;
                }

                if (info.validKinds[j] != 0L) {
                  writer.print("active" + j + ", 0x" + Long.toHexString(info.validKinds[j]) + "L");
                } else {
                  writer.print("active" + j + ", 0L");
                }
              }
            }

            if ((i + 1) <= (data.maxLenForActive[j] + 1)) {
              if (atLeastOne) {
                writer.print(", ");
              }
              if (info.validKinds[j] != 0L) {
                writer.print("active" + j + ", 0x" + Long.toHexString(info.validKinds[j]) + "L");
              } else {
                writer.print("active" + j + ", 0L");
              }
            }

            writer.println(");");
          }
        } else // A very special case.
        if ((i == 0) && data.isMixedState()) {

          if (data.generatedStates() != 0) {
            writer.println("         return jjMoveNfa" + data.lexStateSuffix + "(" + InitStateName(data) + ", 0);");
          } else {
            writer.println("         return 1;");
          }
        } else if (i != 0) // No more str literals to look for
        {
          writer.println("         break;");
          startNfaNeeded = true;
        }
      }

      writer.println("      default :");

      if (Options.getDebugTokenManager()) {
        writer.println("      debugStream.println(\"   No string literal matches possible.\");");
      }

      if (data.generatedStates() != 0) {
        if (i == 0) {
          /*
           * This means no string literal is possible. Just move nfa with this guy and return.
           */
          writer.println("         return jjMoveNfa" + data.lexStateSuffix + "(" + InitStateName(data) + ", 0);");
        } else {
          writer.println("         break;");
          startNfaNeeded = true;
        }
      } else {
        writer.println("         return " + (i + 1) + ";");
      }


      writer.println("   }");

      if (i != 0) {
        if (startNfaNeeded) {
          if (!data.isMixedState() && (data.generatedStates() != 0)) {
            /*
             * Here, a string literal is successfully matched and no more string literals are
             * possible. So set the kind and state set upto and including this position for the
             * matched string.
             */

            writer.print("   return jjStartNfa" + data.lexStateSuffix + "(" + (i - 1) + ", ");
            for (k = 0; k < (maxLongsReqd - 1); k++) {
              if (i <= data.maxLenForActive[k]) {
                writer.print("active" + k + ", ");
              } else {
                writer.print("0L, ");
              }
            }
            if (i <= data.maxLenForActive[k]) {
              writer.println("active" + k + ");");
            } else {
              writer.println("0L);");
            }
          } else if (data.generatedStates() != 0) {
            writer.println("   return jjMoveNfa" + data.lexStateSuffix + "(" + InitStateName(data) + ", " + i + ");");
          } else {
            writer.println("   return " + (i + 1) + ";");
          }
        }
      }

      writer.println("}");
    }

    if (!data.isMixedState() && (data.generatedStates() != 0) && data.createStartNfa) {
      DumpStartWithStates(writer, data);
    }
  }

  private final void DumpMoveNfa(PrintWriter writer, LexerStateData data) {
    int i;
    int[] kindsForStates = null;

    if (data.global.kinds == null) {
      data.global.kinds = new int[data.global.maxLexStates][];
      data.global.statesForState = new int[data.global.maxLexStates][][];
    }

    ReArrange(data);

    for (i = 0; i < data.getAllStateCount(); i++) {
      NfaState temp = data.getAllState(i);

      if ((temp.lexState != data.getStateIndex()) || !temp.HasTransitions() || temp.dummy || (temp.stateName == -1)) {
        continue;
      }

      if (kindsForStates == null) {
        kindsForStates = new int[data.generatedStates()];
        data.global.statesForState[data.getStateIndex()] =
            new int[Math.max(data.generatedStates(), data.dummyStateIndex + 1)][];
      }

      kindsForStates[temp.stateName] = temp.lookingFor;
      data.global.statesForState[data.getStateIndex()][temp.stateName] = temp.compositeStates;
    }

    Enumeration<String> e = data.stateNameForComposite.keys();

    while (e.hasMoreElements()) {
      String s = e.nextElement();
      int state = data.stateNameForComposite.get(s).intValue();

      if (state >= data.generatedStates()) {
        data.global.statesForState[data.getStateIndex()][state] = data.getNextStates(s);
      }
    }

    if (!data.stateSetsToFix.isEmpty()) {
      FixStateSets(data);
    }

    data.global.kinds[data.getStateIndex()] = kindsForStates;

    writer.println("private int " + "jjMoveNfa" + data.lexStateSuffix + "(int startState, int curPos)");
    writer.println("{");
    if (data.generatedStates() == 0) {
      writer.println("   return curPos;");
      writer.println("}");
      return;
    }

    if (data.isMixedState()) {
      writer.println("   int strKind = jjmatchedKind;");
      writer.println("   int strPos = jjmatchedPos;");
      writer.println("   int seenUpto;");
      writer.println("   input_stream.backup(seenUpto = curPos + 1);");
      writer.println("   try { curChar = input_stream.readChar(); }");
      writer.println("   catch(java.io.IOException e) { throw new Error(\"Internal Error\"); }");
      writer.println("   curPos = 0;");
    }

    writer.println("   int startsAt = 0;");
    writer.println("   jjnewStateCnt = " + data.generatedStates() + ";");
    writer.println("   int i = 1;");
    writer.println("   jjstateSet[0] = startState;");

    if (Options.getDebugTokenManager()) {
      writer.println("      debugStream.println(\"   Starting NFA to match one of : \" + "
          + "jjKindsForStateVector(curLexState, jjstateSet, 0, 1));");
    }

    if (Options.getDebugTokenManager()) {
      writer.println("      debugStream.println("
          + (data.global.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
          + "\"Current character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") "
          + "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
    }

    writer.println("   int kind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
    writer.println("   for (;;)");
    writer.println("   {");
    writer.println("      if (++jjround == 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
    writer.println("         ReInitRounds();");
    writer.println("      if (curChar < 64)");
    writer.println("      {");

    DumpAsciiMoves(writer, data, 0);

    writer.println("      }");

    writer.println("      else if (curChar < 128)");

    writer.println("      {");

    DumpAsciiMoves(writer, data, 1);

    writer.println("      }");

    writer.println("      else");
    writer.println("      {");

    DumpCharAndRangeMoves(writer, data);

    writer.println("      }");

    writer.println("      if (kind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
    writer.println("      {");
    writer.println("         jjmatchedKind = kind;");
    writer.println("         jjmatchedPos = curPos;");
    writer.println("         kind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
    writer.println("      }");
    writer.println("      ++curPos;");

    if (Options.getDebugTokenManager()) {
      writer.println(
          "      if (jjmatchedKind != 0 && jjmatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
      writer.println("         debugStream.println("
          + "\"   Currently matched the first \" + (jjmatchedPos + 1) + \" characters as"
          + " a \" + tokenImage[jjmatchedKind] + \" token.\");");
    }

    writer.println(
        "      if ((i = jjnewStateCnt) == (startsAt = " + data.generatedStates() + " - (jjnewStateCnt = startsAt)))");
    if (data.isMixedState()) {
      writer.println("         break;");
    } else {
      writer.println("         return curPos;");
    }

    if (Options.getDebugTokenManager()) {
      writer.println("      debugStream.println(\"   Possible kinds of longer matches : \" + "
          + "jjKindsForStateVector(curLexState, jjstateSet, startsAt, i));");
    }

    writer.println("      try { curChar = input_stream.readChar(); }");

    if (data.isMixedState()) {
      writer.println("      catch(java.io.IOException e) { break; }");
    } else {
      writer.println("      catch(java.io.IOException e) { return curPos; }");
    }

    if (Options.getDebugTokenManager()) {
      writer.println("      debugStream.println("
          + (data.global.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
          + "\"Current character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") "
          + "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
    }

    writer.println("   }");

    if (data.isMixedState()) {
      writer.println("   if (jjmatchedPos > strPos)");
      writer.println("      return curPos;");
      writer.println("");
      writer.println("   int toRet = Math.max(curPos, seenUpto);");
      writer.println("");
      writer.println("   if (curPos < toRet)");
      writer.println("      for (i = toRet - Math.min(curPos, seenUpto); i-- > 0; )");
      writer.println("         try { curChar = input_stream.readChar(); }");
      writer.println("         catch(java.io.IOException e) { "
          + "throw new Error(\"Internal Error : Please send a bug report.\"); }");
      writer.println("");
      writer.println("   if (jjmatchedPos < strPos)");
      writer.println("   {");
      writer.println("      jjmatchedKind = strKind;");
      writer.println("      jjmatchedPos = strPos;");
      writer.println("   }");
      writer.println("   else if (jjmatchedPos == strPos && jjmatchedKind > strKind)");
      writer.println("      jjmatchedKind = strKind;");
      writer.println("");
      writer.println("   return toRet;");
    }
    writer.println("}");
  }
}
