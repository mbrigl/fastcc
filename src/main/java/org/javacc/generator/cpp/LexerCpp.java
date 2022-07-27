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
import org.javacc.JavaCC;
import org.javacc.JavaCCContext;
import org.javacc.JavaCCRequest;
import org.javacc.generator.LexerGenerator;
import org.javacc.parser.Action;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.Options;
import org.javacc.parser.Token;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Generate lexer.
 */
public class LexerCpp extends LexerGenerator {

  /**
   * Constructs an instance of {@link LexerCpp}.
   *
   * @param request
   * @param context
   */
  public LexerCpp(JavaCCRequest request, JavaCCContext context) {
    super(new CppWriter(request.getParserName() + "TokenManager"), request, context);
  }

  @Override
  protected final CppWriter getSource() {
    return (CppWriter) super.getSource();
  }

  @Override
  protected void PrintClassHead() {
    getSource().switchToStatics();

    genCodeLine("#include \"" + getLexerData().request.getParserName() + "TokenManager.h\"");
    genCodeLine("#include \"TokenManagerError.h\"");
    genCodeLine("#include \"DefaultTokenManagerErrorHandler.h\"");
    genCodeLine("");
    genCodeLine("");

    // standard includes
    getSource().switchToHeader();
    genCodeLine("");
    genCodeLine("#include \"JavaCC.h\"");
    genCodeLine("#include \"CharStream.h\"");
    genCodeLine("#include \"Token.h\"");
    genCodeLine("#include \"ParserErrorHandler.h\"");
    genCodeLine("#include \"TokenManager.h\"");
    genCodeLine("#include \"" + getLexerData().request.getParserName() + "Constants.h\"");

    genCodeLine("");

    if (Options.stringValue(JavaCC.JJPARSER_CPP_NAMESPACE).length() > 0) {
      genCodeLine("");
      genCodeLine("");
      genCodeLine("namespace " + Options.stringValue(JavaCC.JJPARSER_CPP_NAMESPACE) + " {");
    }

    genCodeLine("");
    genClassStart(null, getLexerData().request.getParserName() + "TokenManager", new String[] {},
        new String[] { "public TokenManager" });

    if ((getLexerData().request.getTokens() != null) && !getLexerData().request.getTokens().isEmpty()) {
      genTokenSetup(getLexerData().request.getTokens().get(0));
      this.ccol = 1;

      getSource().switchToImpl();
      getLexerData().request.getTokens().forEach(tt -> genToken(tt));

      getSource().switchToHeader();
      genCodeLine("  void CommonTokenAction(Token* token);");
      genCodeLine("");
    }

    genCodeLine("");
    genCodeLine("  FILE *debugStream;");

    getSource().switchToImpl();

    // Finally enclose the whole thing in the namespace, if specified.
    if (Options.stringValue(JavaCC.JJPARSER_CPP_NAMESPACE).length() > 0) {
      genCodeLine("namespace " + Options.stringValue(JavaCC.JJPARSER_CPP_NAMESPACE) + " {");
    }

    generateMethodDefHeaderCpp("  void ", "setDebugStream(FILE *ds)");
    genCodeLine("{ debugStream = ds; }");

    getSource().switchToImpl();
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
      getSource().switchToStatics();
      genCodeLine("static int  jjemptyLineNo[" + data2().maxLexStates + "];");
      genCodeLine("static int  jjemptyColNo[" + data2().maxLexStates + "];");
      genCodeLine("static bool jjbeenHere[" + data2().maxLexStates + "];");
      getSource().switchToImpl();
    }

    if (data2().hasSkipActions) {
      DumpSkipActions();
    }
    if (data2().hasMoreActions) {
      DumpMoreActions();
    }
    if (data2().hasTokenActions) {
      DumpTokenActions();
    }

    PrintBoilerPlateCPP();

    writeTemplate("/templates/cpp/TokenManagerBoilerPlateMethods.template",
        Map.of("charStreamName", "CharStream", "parserClassName", getLexerData().request.getParserName(),
            "defaultLexState", "defaultLexState", "lexStateNameLength", getLexerData().getStateCount(), "lexStateName",
            getLexerData().getStateName(getLexerData().getStateCount() - 1)));

    dumpBoilerPlateInHeader();

    // in the include file close the class signature
    DumpStaticVarDeclarations(); // static vars actually inst

    getSource().switchToHeader(); // remaining variables
    writeTemplate("/templates/cpp/DumpVarDeclarations.template",
        Map.of("charStreamName", "CharStream", "lexStateNameLength", getLexerData().getStateCount()));
    genCodeLine(/* { */ "};");

    getSource().switchToStatics();
    saveOutput();
  }

  private void dumpBoilerPlateInHeader() {
    getSource().switchToHeader();
    genCodeLine("  CharStream*        input_stream;");
    genCodeLine("");

    genCodeLine("private:");
    genCodeLine("  void ReInitRounds();");
    genCodeLine("");
    genCodeLine("public:");
    genCodeLine("  " + getLexerData().request.getParserName() + "TokenManager(CharStream* stream, int lexState = "
        + data2().defaultLexState + ");");
    genCodeLine("  virtual ~" + getLexerData().request.getParserName() + "TokenManager();");
    genCodeLine("");
    genCodeLine("protected:");
    genCodeLine("  void ReInit(CharStream* stream, int lexState = " + data2().defaultLexState + ");");
    genCodeLine("  void SwitchTo(int lexState);");
    genCodeLine("  void clear();");
    genCodeLine("  const JJSimpleString jjKindsForBitVector(int i, unsigned long long vec);");
    genCodeLine("  const JJSimpleString jjKindsForStateVector(int lexState, int vec[], int start, int end);");
    genCodeLine("");
  }

  private void DumpStaticVarDeclarations() throws IOException {
    int i;

    getSource().switchToStatics(); // remaining variables
    genCodeLine("");
    genCodeLine("/** Lexer state names. */");
    String[] stateNames = new String[getLexerData().getStateCount()];
    for (int index = 0; index < getLexerData().getStateCount(); index++) {
      stateNames[index] = getLexerData().getStateName(index);
    }
    genStringLiteralArrayCPP("lexStateNames", stateNames);

    if (data2().maxLexStates > 1) {
      genCodeLine("");
      genCodeLine("/** Lex State array. */");
      genCode("static const int jjnewLexState[] = {");

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

    if (data2().hasSkip || data2().hasMore || data2().hasSpecial) {
      // Bit vector for TOKEN
      genCode("static const unsigned long long jjtoToken[] = {");
      for (i = 0; i < ((data2().maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          genCode("\n   ");
        }
        genCode("0x" + Long.toHexString(data2().toToken[i]) + "L, ");
      }
      genCodeLine("\n};");
    }

    if (data2().hasSkip || data2().hasSpecial) {
      // Bit vector for SKIP
      genCode("static const unsigned long long jjtoSkip[] = {");
      for (i = 0; i < ((data2().maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          genCode("\n   ");
        }
        genCode("0x" + Long.toHexString(data2().toSkip[i]) + "L, ");
      }
      genCodeLine("\n};");
    }

    if (data2().hasSpecial) {
      // Bit vector for SPECIAL
      genCode("static const unsigned long long jjtoSpecial[] = {");
      for (i = 0; i < ((data2().maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          genCode("\n   ");
        }
        genCode("0x" + Long.toHexString(data2().toSpecial[i]) + "L, ");
      }
      genCodeLine("\n};");
    }
  }

  private void DumpFillToken() {
    generateMethodDefHeaderCpp("Token *", "jjFillToken()");
    genCodeLine("{");
    genCodeLine("   Token *t;");
    genCodeLine("   JJString curTokenImage;");
    if (data2().keepLineCol) {
      genCodeLine("   int beginLine   = -1;");
      genCodeLine("   int endLine     = -1;");
      genCodeLine("   int beginColumn = -1;");
      genCodeLine("   int endColumn   = -1;");
    }

    if (data2().hasEmptyMatch) {
      genCodeLine("   if (jjmatchedPos < 0)");
      genCodeLine("   {");
      genCodeLine("       curTokenImage = image.c_str();");

      if (data2().keepLineCol) {
        genCodeLine("   if (input_stream->getTrackLineColumn()) {");
        genCodeLine("      beginLine = endLine = input_stream->getEndLine();");
        genCodeLine("      beginColumn = endColumn = input_stream->getEndColumn();");
        genCodeLine("   }");
      }

      genCodeLine("   }");
      genCodeLine("   else");
      genCodeLine("   {");
      genCodeLine("      JJString im = jjstrLiteralImages[jjmatchedKind];");
      genCodeLine("      curTokenImage = (im.length() == 0) ? input_stream->getImage() : im;");

      if (data2().keepLineCol) {
        genCodeLine("   if (input_stream->getTrackLineColumn()) {");
        genCodeLine("      beginLine = input_stream->getBeginLine();");
        genCodeLine("      beginColumn = input_stream->getBeginColumn();");
        genCodeLine("      endLine = input_stream->getEndLine();");
        genCodeLine("      endColumn = input_stream->getEndColumn();");
        genCodeLine("   }");
      }

      genCodeLine("   }");
    } else {
      genCodeLine("   JJString im = jjstrLiteralImages[jjmatchedKind];");
      genCodeLine("   curTokenImage = (im.length() == 0) ? input_stream->getImage() : im;");
      if (data2().keepLineCol) {
        genCodeLine("   if (input_stream->getTrackLineColumn()) {");
        genCodeLine("     beginLine = input_stream->getBeginLine();");
        genCodeLine("     beginColumn = input_stream->getBeginColumn();");
        genCodeLine("     endLine = input_stream->getEndLine();");
        genCodeLine("     endColumn = input_stream->getEndColumn();");
        genCodeLine("   }");
      }
    }

    genCodeLine("   t = Token::newToken(jjmatchedKind, curTokenImage);");

    if (data2().keepLineCol) {
      genCodeLine("");
      // genCodeLine(" if (input_stream->getTrackLineColumn()) {");
      genCodeLine("   t->beginLine() = beginLine;");
      genCodeLine("   t->endLine() = endLine;");
      genCodeLine("   t->beginColumn() = beginColumn;");
      genCodeLine("   t->endColumn() = endColumn;");
      // genCodeLine(" }");
    }

    genCodeLine("");
    genCodeLine("   return t;");
    genCodeLine("}");
  }

  private void DumpGetNextToken() {
    int i;

    getSource().switchToHeader();
    genCodeLine("");
    genCodeLine("public:");
    genCodeLine("  int defaultLexState;");
    genCodeLine("  int curLexState = 0;");
    genCodeLine("  int jjnewStateCnt = 0;");
    genCodeLine("  int jjround = 0;");
    genCodeLine("  int jjmatchedPos = 0;");
    genCodeLine("  int jjmatchedKind = 0;");
    genCodeLine("");
    getSource().switchToImpl();
    // genCodeLine("const int defaultLexState = " + defaultLexState + ";");
    genCodeLine("/** Get the next Token. */");
    generateMethodDefHeaderCpp("Token *", "getNextToken()");
    genCodeLine("{");
    if (data2().hasSpecial) {
      genCodeLine("  Token *specialToken = nullptr;");
    }
    genCodeLine("  Token *matchedToken = nullptr;");
    genCodeLine("  int curPos = 0;");
    genCodeLine("");
    genCodeLine("  for (;;)");
    genCodeLine("  {");
    genCodeLine("   EOFLoop: ");
    // genCodeLine(" {");
    // genCodeLine(" curChar = input_stream->beginToken();");
    // genCodeLine(" }");
    genCodeLine("   if (input_stream->endOfInput())");
    genCodeLine("   {");
    // genCodeLine(" input_stream->backup(1);");

    if (Options.getDebugTokenManager()) {
      genCodeLine("      fprintf(debugStream, \"Returning the <EOF> token.\\n\");");
    }

    genCodeLine("      jjmatchedKind = 0;");
    genCodeLine("      jjmatchedPos = -1;");
    genCodeLine("      matchedToken = jjFillToken();");

    if (data2().hasSpecial) {
      genCodeLine("      matchedToken->specialToken = specialToken;");
    }

    if ((getLexerData().request.getNextStateForEof() != null) || (getLexerData().request.getActionForEof() != null)) {
      genCodeLine("      TokenLexicalActions(matchedToken);");
    }

    genCodeLine("      return matchedToken;");
    genCodeLine("   }");
    genCodeLine("   curChar = input_stream->beginToken();");

    if (data2().hasMoreActions || data2().hasSkipActions || data2().hasTokenActions) {
      genCodeLine("   image = jjimage;");
      genCodeLine("   image.clear();");
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
        genCodeLine(prefix + "{ input_stream->backup(0);");
        if ((data2().singlesToSkip[i].asciiMoves[0] != 0L) && (data2().singlesToSkip[i].asciiMoves[1] != 0L)) {
          genCodeLine(
              prefix + "   while ((curChar < 64" + " && (0x" + Long.toHexString(data2().singlesToSkip[i].asciiMoves[0])
                  + "L & (1L << curChar)) != 0L) || \n" + prefix + "          (curChar >> 6) == 1" + " && (0x"
                  + Long.toHexString(data2().singlesToSkip[i].asciiMoves[1]) + "L & (1L << (curChar & 077))) != 0L)");
        } else if (data2().singlesToSkip[i].asciiMoves[1] == 0L) {
          genCodeLine(prefix + "   while (curChar <= " + (int) LexerCpp.MaxChar(data2().singlesToSkip[i].asciiMoves[0])
              + " && (0x" + Long.toHexString(data2().singlesToSkip[i].asciiMoves[0]) + "L & (1L << curChar)) != 0L)");
        } else if (data2().singlesToSkip[i].asciiMoves[0] == 0L) {
          genCodeLine(prefix + "   while (curChar > 63 && curChar <= "
              + (LexerCpp.MaxChar(data2().singlesToSkip[i].asciiMoves[1]) + 64) + " && (0x"
              + Long.toHexString(data2().singlesToSkip[i].asciiMoves[1]) + "L & (1L << (curChar & 077))) != 0L)");
        }

        genCodeLine(prefix + "{");
        if (Options.getDebugTokenManager()) {
          if (data2().maxLexStates > 1) {
            genCodeLine(
                "      fprintf(debugStream, \"<%s>\" , addUnicodeEscapes(lexStateNames[curLexState]).c_str());");
          }

          genCodeLine("      fprintf(debugStream, \"Skipping character : %c(%d)\\n\", curChar, (int)curChar);");
        }

        genCodeLine(prefix + "if (input_stream->endOfInput()) { goto EOFLoop; }");
        genCodeLine(prefix + "curChar = input_stream->beginToken();");
        genCodeLine(prefix + "}");
        genCodeLine(prefix + "}");
      }

      if ((data2().initMatch[i] != Integer.MAX_VALUE) && (data2().initMatch[i] != 0)) {
        if (Options.getDebugTokenManager()) {
          genCodeLine(
              "      fprintf(debugStream, \"   Matched the empty string as %s token.\\n\", addUnicodeEscapes(tokenImage["
                  + data2().initMatch[i] + "]).c_str());");
        }

        genCodeLine(prefix + "jjmatchedKind = " + data2().initMatch[i] + ";");
        genCodeLine(prefix + "jjmatchedPos = -1;");
        genCodeLine(prefix + "curPos = 0;");
      } else {
        genCodeLine(prefix + "jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
        genCodeLine(prefix + "jjmatchedPos = 0;");
      }

      if (Options.getDebugTokenManager()) {
        genCodeLine("   fprintf(debugStream, " + "\"<%s>Current character : %c(%d) at line %d column %d\\n\","
            + "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, "
            + "input_stream->getEndLine(), input_stream->getEndColumn());");
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
          genCodeLine(
              "           fprintf(debugStream, \"   Current character matched as a %s token.\\n\", addUnicodeEscapes(tokenImage["
                  + data2().canMatchAnyChar[i] + "]).c_str());");
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
        genCodeLine(prefix + "         fprintf(debugStream, "
            + "\"   Putting back %d characters into the input stream.\\n\", (curPos - jjmatchedPos - 1));");
      }

      genCodeLine(prefix + "         input_stream->backup(curPos - jjmatchedPos - 1);");

      if (Options.getDebugTokenManager()) {
        genCodeLine(prefix + "      }");
      }

      if (Options.getDebugTokenManager()) {
        genCodeLine("    fprintf(debugStream, "
            + "\"****** FOUND A %d(%s) MATCH (%s) ******\\n\", jjmatchedKind, addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str(), addUnicodeEscapes(input_stream->GetSuffix(jjmatchedPos + 1)).c_str());");
      }

      if (data2().hasSkip || data2().hasMore || data2().hasSpecial) {
        genCodeLine(prefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
        genCodeLine(prefix + "      {");
      }

      genCodeLine(prefix + "         matchedToken = jjFillToken();");

      if (data2().hasSpecial) {
        genCodeLine(prefix + "         matchedToken->specialToken = specialToken;");
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

            genCodeLine(prefix + "            if (specialToken == nullptr)");
            genCodeLine(prefix + "               specialToken = matchedToken;");
            genCodeLine(prefix + "            else");
            genCodeLine(prefix + "            {");
            genCodeLine(prefix + "               matchedToken->specialToken = specialToken;");
            genCodeLine(prefix + "               specialToken = (specialToken->next = matchedToken);");
            genCodeLine(prefix + "            }");

            if (data2().hasSkipActions) {
              genCodeLine(prefix + "            SkipLexicalActions(matchedToken);");
            }

            genCodeLine(prefix + "         }");

            if (data2().hasSkipActions) {
              genCodeLine(prefix + "         else");
              genCodeLine(prefix + "            SkipLexicalActions(nullptr);");
            }
          } else if (data2().hasSkipActions) {
            genCodeLine(prefix + "         SkipLexicalActions(nullptr);");
          }

          if (data2().maxLexStates > 1) {
            genCodeLine("         if (jjnewLexState[jjmatchedKind] != -1)");
            genCodeLine(prefix + "         curLexState = jjnewLexState[jjmatchedKind];");
          }

          genCodeLine(prefix + "         goto EOFLoop;");
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

          genCodeLine(prefix + "   if (!input_stream->endOfInput()) {");
          genCodeLine(prefix + "         curChar = input_stream->readUnicode(); // TOL: Support Unicode");

          if (Options.getDebugTokenManager()) {
            genCodeLine("   fprintf(debugStream, " + "\"<%s>Current character : %c(%d) at line %d column %d\\n\","
                + "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, "
                + "input_stream->getEndLine(), input_stream->getEndColumn());");
          }
          genCodeLine(prefix + "   continue;");
          genCodeLine(prefix + " }");
        }
      }

      genCodeLine(prefix + "   }");
      genCodeLine(prefix + "   int error_line = input_stream->getEndLine();");
      genCodeLine(prefix + "   int error_column = input_stream->getEndColumn();");
      genCodeLine(prefix + "   JJString error_after = JJEMPTY;");
      genCodeLine(prefix + "   bool EOFSeen = false;");
      genCodeLine(prefix + "   if (input_stream->endOfInput()) {");
      genCodeLine(prefix + "      EOFSeen = true;");
      genCodeLine(prefix + "      error_after = curPos <= 1 ? JJEMPTY : input_stream->getImage();");
      genCodeLine(prefix + "      if (curChar == '\\n' || curChar == '\\r') {");
      genCodeLine(prefix + "         error_line++;");
      genCodeLine(prefix + "         error_column = 0;");
      genCodeLine(prefix + "      }");
      genCodeLine(prefix + "      else");
      genCodeLine(prefix + "         error_column++;");
      genCodeLine(prefix + "   }");
      genCodeLine(prefix + "   if (!EOFSeen) {");
      genCodeLine(prefix + "      error_after = curPos <= 1 ? JJEMPTY : input_stream->getImage();");
      genCodeLine(prefix + "   }");
      genCodeLine(prefix
          + "   errorHandler->lexicalError(EOFSeen, curLexState, error_line, error_column, error_after, curChar);");
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

    generateMethodDefHeaderCpp("void ", "SkipLexicalActions(Token *matchedToken)");
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

        genCodeLine("      case " + i + " : {");

        if ((data2().initMatch[getLexerData().getState(i)] == i) && data2().canLoop[getLexerData().getState(i)]) {
          genCodeLine("         if (jjmatchedPos == -1)");
          genCodeLine("         {");
          genCodeLine("            if (jjbeenHere[" + getLexerData().getState(i) + "] &&");
          genCodeLine(
              "                jjemptyLineNo[" + getLexerData().getState(i) + "] == input_stream->getBeginLine() &&");
          genCodeLine(
              "                jjemptyColNo[" + getLexerData().getState(i) + "] == input_stream->getBeginColumn())");
          genCodeLine(
              "               errorHandler->lexicalError(JJString(\"(\"Error: Bailing out of infinite loop caused by repeated empty string matches \" + \"at line \" + input_stream->getBeginLine() + \", \" + \"column \" + input_stream->getBeginColumn() + \".\")), this);");
          genCodeLine("            jjemptyLineNo[" + getLexerData().getState(i) + "] = input_stream->getBeginLine();");
          genCodeLine("            jjemptyColNo[" + getLexerData().getState(i) + "] = input_stream->getBeginColumn();");
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
          genCodeLine("(input_stream->GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
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
      genCodeLine("       }");
    }

    genCodeLine("      default :");
    genCodeLine("         break;");
    genCodeLine("   }");
    genCodeLine("}");
  }

  private void DumpMoreActions() {
    Action act;

    generateMethodDefHeaderCpp("void ", "MoreLexicalActions()");
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

        genCodeLine("      case " + i + " : {");

        if ((data2().initMatch[getLexerData().getState(i)] == i) && data2().canLoop[getLexerData().getState(i)]) {
          genCodeLine("         if (jjmatchedPos == -1)");
          genCodeLine("         {");
          genCodeLine("            if (jjbeenHere[" + getLexerData().getState(i) + "] &&");
          genCodeLine(
              "                jjemptyLineNo[" + getLexerData().getState(i) + "] == input_stream->getBeginLine() &&");
          genCodeLine(
              "                jjemptyColNo[" + getLexerData().getState(i) + "] == input_stream->getBeginColumn())");
          genCodeLine(
              "               errorHandler->lexicalError(JJString(\"(\"Error: Bailing out of infinite loop caused by repeated empty string matches \" + \"at line \" + input_stream->getBeginLine() + \", \" + \"column \" + input_stream->getBeginColumn() + \".\")), this);");
          genCodeLine("            jjemptyLineNo[" + getLexerData().getState(i) + "] = input_stream->getBeginLine();");
          genCodeLine("            jjemptyColNo[" + getLexerData().getState(i) + "] = input_stream->getBeginColumn();");
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
          genCodeLine("(input_stream->GetSuffix(jjimageLen));");
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
      genCodeLine("       }");
    }

    genCodeLine("      default :");
    genCodeLine("         break;");

    genCodeLine("   }");
    genCodeLine("}");
  }

  private void DumpTokenActions() {
    Action act;
    int i;

    generateMethodDefHeaderCpp("void ", "TokenLexicalActions(Token *matchedToken)");
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

        genCodeLine("      case " + i + " : {");

        if ((data2().initMatch[getLexerData().getState(i)] == i) && data2().canLoop[getLexerData().getState(i)]) {
          genCodeLine("         if (jjmatchedPos == -1)");
          genCodeLine("         {");
          genCodeLine("            if (jjbeenHere[" + getLexerData().getState(i) + "] &&");
          genCodeLine(
              "                jjemptyLineNo[" + getLexerData().getState(i) + "] == input_stream->getBeginLine() &&");
          genCodeLine(
              "                jjemptyColNo[" + getLexerData().getState(i) + "] == input_stream->getBeginColumn())");
          genCodeLine(
              "               errorHandler->lexicalError(JJString(\"Error: Bailing out of infinite loop caused by repeated empty string matches "
                  + "at line \" + input_stream->getBeginLine() + \", "
                  + "column \" + input_stream->getBeginColumn() + \".\"), this);");
          genCodeLine("            jjemptyLineNo[" + getLexerData().getState(i) + "] = input_stream->getBeginLine();");
          genCodeLine("            jjemptyColNo[" + getLexerData().getState(i) + "] = input_stream->getBeginColumn();");
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
            genCodeLine("(input_stream->GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
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
      genCodeLine("       }");
    }

    genCodeLine("      default :");
    genCodeLine("         break;");
    genCodeLine("   }");
    genCodeLine("}");
  }

  private void genStringLiteralArrayCPP(String varName, String[] arr) {
    // First generate char array vars
    for (int i = 0; i < arr.length; i++) {
      genCodeLine("static const JJChar " + varName + "_arr_" + i + "[] = ");
      genStringLiteralInCPP(arr[i]);
      genCodeLine(";");
    }

    genCodeLine("static const JJString " + varName + "[] = {");
    for (int i = 0; i < arr.length; i++) {
      genCodeLine(varName + "_arr_" + i + ", ");
    }
    genCodeLine("};");
  }

  private void genStringLiteralInCPP(String s) {
    // String literals in CPP become char arrays
    genCode("{");
    for (int i = 0; i < s.length(); i++) {
      genCode("0x" + Integer.toHexString(s.charAt(i)) + ", ");
    }
    genCode("0}");
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

  // private static boolean boilerPlateDumped = false;
  private void PrintBoilerPlateCPP() {
    getSource().switchToHeader();
    genCodeLine("#define jjCheckNAdd(state)\\");
    genCodeLine("{\\");
    genCodeLine("   if (jjrounds[state] != jjround)\\");
    genCodeLine("   {\\");
    genCodeLine("      jjstateSet[jjnewStateCnt++] = state;\\");
    genCodeLine("      jjrounds[state] = jjround;\\");
    genCodeLine("   }\\");
    genCodeLine("}");

    genCodeLine("#define jjAddStates(start, end)\\");
    genCodeLine("{\\");
    genCodeLine("   for (int x = start; x <= end; x++) {\\");
    genCodeLine("      jjstateSet[jjnewStateCnt++] = jjnextStates[x];\\");
    genCodeLine("   } /*while (start++ != end);*/\\");
    genCodeLine("}");

    genCodeLine("#define jjCheckNAddTwoStates(state1, state2)\\");
    genCodeLine("{\\");
    genCodeLine("   jjCheckNAdd(state1);\\");
    genCodeLine("   jjCheckNAdd(state2);\\");
    genCodeLine("}");
    genCodeLine("");

    if (getLexerData().jjCheckNAddStatesDualNeeded) {
      genCodeLine("#define jjCheckNAddStates(start, end)\\");
      genCodeLine("{\\");
      genCodeLine("   for (int x = start; x <= end; x++) {\\");
      genCodeLine("      jjCheckNAdd(jjnextStates[x]);\\");
      genCodeLine("   } /*while (start++ != end);*/\\");
      genCodeLine("}");
      genCodeLine("");
    }

    if (getLexerData().jjCheckNAddStatesUnaryNeeded) {
      genCodeLine("#define jjCheckNAddStates(start)\\");
      genCodeLine("{\\");
      genCodeLine("   jjCheckNAdd(jjnextStates[start]);\\");
      genCodeLine("   jjCheckNAdd(jjnextStates[start + 1]);\\");
      genCodeLine("}");
      genCodeLine("");
    }
    getSource().switchToImpl();
  }

  private void DumpStatesForKind() {
    DumpStatesForStateCPP();
    boolean moreThanOne = false;
    int cnt = 0;

    getSource().switchToStatics();
    genCode("static const int kindForState[" + data2().stateSetSize + "][" + data2().stateSetSize + "] = ");

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
    getSource().switchToImpl();
  }

  private void DumpStatesForStateCPP() {
    if (getLexerData().statesForState == null) {
      assert (false) : "This should never be null.";
      genCodeLine("null;");
      return;
    }

    getSource().switchToStatics();
    for (int i = 0; i < data2().maxLexStates; i++) {
      if (getLexerData().statesForState[i] == null) {
        continue;
      }

      for (int j = 0; j < getLexerData().statesForState[i].length; j++) {
        int[] stateSet = getLexerData().statesForState[i][j];

        genCode("const int stateSet_" + i + "_" + j + "[" + data2().stateSetSize + "] = ");
        if (stateSet == null) {
          genCodeLine("   { " + j + " };");
          continue;
        }

        genCode("   { ");

        for (int element : stateSet) {
          genCode(element + ", ");
        }

        genCodeLine("};");
      }
    }

    for (int i = 0; i < data2().maxLexStates; i++) {
      genCodeLine("const int *stateSet_" + i + "[] = {");
      if (getLexerData().statesForState[i] == null) {
        genCodeLine(" NULL, ");
        genCodeLine("};");
        continue;
      }

      for (int j = 0; j < getLexerData().statesForState[i].length; j++) {
        genCode("stateSet_" + i + "_" + j + ",");
      }
      genCodeLine("};");
    }

    genCode("const int** statesForState[] = { ");
    for (int i = 0; i < data2().maxLexStates; i++) {
      genCodeLine("stateSet_" + i + ", ");
    }

    genCodeLine("\n};");
    getSource().switchToImpl();
  }

  private void DumpStateSets() {
    int cnt = 0;

    getSource().switchToStatics();
    genCode("static const int jjnextStates[] = {");
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
    getSource().switchToImpl();
  }

  private void DumpStrLiteralImages() {
    // For C++
    String image;
    int i;
    int charCnt = 0; // Set to zero in reInit() but just to be sure

    genCodeLine("");
    genCodeLine("/** Token literal values. */");
    int literalCount = 0;
    getSource().switchToStatics();

    if (getLexerData().getImageCount() <= 0) {
      genCodeLine("static const JJString jjstrLiteralImages[] = {};");
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

        genCodeLine("static JJChar jjstrLiteralChars_" + literalCount++ + "[] = {0};");
        continue;
      }

      String toPrint = "static JJChar jjstrLiteralChars_" + literalCount++ + "[] = {";
      for (int j = 0; j < image.length(); j++) {
        String hexVal = Integer.toHexString(image.charAt(j));
        toPrint += "0x" + hexVal + ", ";
      }

      // Null char
      toPrint += "0};";

      if ((charCnt += toPrint.length()) >= 80) {
        genCodeLine("");
        charCnt = 0;
      }

      genCodeLine(toPrint);
    }

    while (++i < data2().maxOrdinal) {
      if ((charCnt += 6) > 80) {
        genCodeLine("");
        charCnt = 0;
      }

      genCodeLine("static JJChar jjstrLiteralChars_" + literalCount++ + "[] = {0};");
      continue;
    }

    // Generate the array here.
    genCodeLine("static const JJString " + "jjstrLiteralImages[] = {");
    for (int j = 0; j < literalCount; j++) {
      genCodeLine("jjstrLiteralChars_" + j + ", ");
    }
    genCodeLine("};");
  }
}
