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
import org.javacc.generator.LexerData;
import org.javacc.generator.LexerGenerator;
import org.javacc.generator.LexerStateData;
import org.javacc.lexer.NfaState;
import org.javacc.parser.Action;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.Options;
import org.javacc.parser.RStringLiteral.KindInfo;
import org.javacc.parser.Token;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

/**
 * Generate lexer.
 */
public class CppLexerGenerator extends LexerGenerator {

  @Override
  protected final void dumpAll(LexerData data, List<Token> insertionPoint) throws IOException {
    CppWriter writer = new CppWriter(data.getParserName() + "TokenManager");

    writer.setOption("stateNames", data.stateNames);
    writer.setOption("lohiBytes", data.lohiByte.keySet());
    writer.setOption("defaultLexState", data.defaultLexState);
    writer.setOption("maxOrdinal", data.maxOrdinal);
    writer.setOption("maxLexStates", data.maxLexStates);
    writer.setOption("keepLineCol", data.keepLineCol);
    writer.setOption("hasEmptyMatch", data.hasEmptyMatch);
    writer.setOption("hasSkip", data.hasSkip);
    writer.setOption("hasMore", data.hasMore);
    writer.setOption("hasSpecial", data.hasSpecial);
    writer.setOption("hasLoop", data.hasLoop);
    writer.setOption("hasMoreActions", data.hasMoreActions);
    writer.setOption("hasSkipActions", data.hasSkipActions);
    writer.setOption("hasTokenActions", data.hasTokenActions);
    writer.setOption("stateSetSize", data.stateSetSize);
    writer.setOption("lexStateName", data.getStateName(data.getStateCount() - 1));
    writer.setOption("hasActions", data.hasMoreActions || data.hasSkipActions || data.hasTokenActions);
    writer.setOption("jjCheckNAddStatesUnaryNeeded", data.jjCheckNAddStatesUnaryNeeded);
    writer.setOption("jjCheckNAddStatesDualNeeded", data.jjCheckNAddStatesDualNeeded);
    writer.setOption("nonAsciiTableForMethod", data.nonAsciiTableForMethod);

    writer.setFunction("getNonAsciiMethod", i -> "" + ((NfaState) i).nonAsciiMethod);
    writer.setFunction("getLohiBytes", i -> CppLexerGenerator.getLohiBytes(data, (int) i));

    writer.setWriter("dumpNfaAndDfaHeader", (w, k) -> dumpNfaAndDfaHeader(w, data.getStateData((String) k)));
    writer.setWriter("DumpStrLiteralImages", (w, k) -> DumpStrLiteralImages(w, data));
    writer.setWriter("DumpStateSets", (w, k) -> DumpStateSets(w, data));
    writer.setWriter("DumpStatesForStateCPP", (w, k) -> DumpStatesForStateCPP(w, data));
    writer.setWriter("DumpStatesForKind", (w, k) -> DumpStatesForKind(w, data));
    writer.setWriter("DumpStaticVarDeclarations", (w, k) -> DumpStaticVarDeclarations(w, data));
    writer.setWriter("dumpNfaAndDfa", (w, k) -> dumpNfaAndDfa(w, data.getStateData((String) k)));
    writer.setWriter("DumpNonAsciiMoveMethod", (w, k) -> DumpNonAsciiMoveMethod(w, data, (NfaState) k));
    writer.setWriter("DumpGetNextToken", (w, k) -> DumpGetNextToken(w, data));
    writer.setWriter("DumpSkipActions", (w, k) -> DumpSkipActions(w, data));
    writer.setWriter("DumpMoreActions", (w, k) -> DumpMoreActions(w, data));
    writer.setWriter("DumpTokenActions", (w, k) -> DumpTokenActions(w, data));

    writer.writeTemplate("/templates/cpp/Lexer.template");

    writer.switchToHeader();
    data.boilerPlateDumped = false;
    writer.writeTemplate("/templates/cpp/Lexer.h.template");

    saveOutput(writer);
  }


  private static String getLohiBytes(LexerData data, int i) {
    return "{\n   0x" + Long.toHexString(data.lohiByte.get(i)[0]) + "L, " + "0x"
        + Long.toHexString(data.lohiByte.get(i)[1]) + "L, " + "0x" + Long.toHexString(data.lohiByte.get(i)[2]) + "L, "
        + "0x" + Long.toHexString(data.lohiByte.get(i)[3]) + "L\n}";
  }

  private void DumpStaticVarDeclarations(PrintWriter writer, LexerData data) {
    int i;

    writer.println("");
    writer.println("/** Lexer state names. */");
    String[] stateNames = new String[data.getStateCount()];
    for (int index = 0; index < data.getStateCount(); index++) {
      stateNames[index] = data.getStateName(index);
    }
    genStringLiteralArrayCPP(writer, "lexStateNames", stateNames);

    if (data.maxLexStates > 1) {
      writer.println("");
      writer.println("/** Lex State array. */");
      writer.print("static const int jjnewLexState[] = {");

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

    if (data.hasSkip || data.hasMore || data.hasSpecial) {
      // Bit vector for TOKEN
      writer.print("static const unsigned long long jjtoToken[] = {");
      for (i = 0; i < ((data.maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          writer.print("\n   ");
        }
        writer.print("0x" + Long.toHexString(data.toToken[i]) + "L, ");
      }
      writer.println("\n};");
    }

    if (data.hasSkip || data.hasSpecial) {
      // Bit vector for SKIP
      writer.print("static const unsigned long long jjtoSkip[] = {");
      for (i = 0; i < ((data.maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          writer.print("\n   ");
        }
        writer.print("0x" + Long.toHexString(data.toSkip[i]) + "L, ");
      }
      writer.println("\n};");
    }

    if (data.hasSpecial) {
      // Bit vector for SPECIAL
      writer.print("static const unsigned long long jjtoSpecial[] = {");
      for (i = 0; i < ((data.maxOrdinal / 64) + 1); i++) {
        if ((i % 4) == 0) {
          writer.print("\n   ");
        }
        writer.print("0x" + Long.toHexString(data.toSpecial[i]) + "L, ");
      }
      writer.println("\n};");
    }
  }

  private void DumpGetNextToken(PrintWriter writer, LexerData data) {
    int i;
    // writer.println("const int defaultLexState = " + defaultLexState + ";");
    writer.println("/** Get the next Token. */");
    writer.print("\nToken * " + data.getParserName() + "TokenManager::getNextToken()");
    writer.println("{");
    if (data.hasSpecial) {
      writer.println("  Token *specialToken = nullptr;");
    }
    writer.println("  Token *matchedToken = nullptr;");
    writer.println("  int curPos = 0;");
    writer.println("");
    writer.println("  for (;;)");
    writer.println("  {");
    writer.println("   EOFLoop: ");
    // writer.println(" {");
    // writer.println(" curChar = reader->beginToken();");
    // writer.println(" }");
    writer.println("   if (reader->endOfInput())");
    writer.println("   {");
    // writer.println(" reader->backup(1);");

    if (Options.getDebugTokenManager()) {
      writer.println("      fprintf(debugStream, \"Returning the <EOF> token.\\n\");");
    }

    writer.println("      jjmatchedKind = 0;");
    writer.println("      jjmatchedPos = -1;");
    writer.println("      matchedToken = jjFillToken();");

    if (data.hasSpecial) {
      writer.println("      matchedToken->specialToken = specialToken;");
    }

    if ((data.getNextStateForEof() != null) || (data.getActionForEof() != null)) {
      writer.println("      TokenLexicalActions(matchedToken);");
    }

    writer.println("      return matchedToken;");
    writer.println("   }");
    writer.println("   curChar = reader->beginToken();");

    if (data.hasMoreActions || data.hasSkipActions || data.hasTokenActions) {
      writer.println("   image = jjimage;");
      writer.println("   image.clear();");
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
        writer.println(prefix + "{ reader->backup(0);");
        if ((data.singlesToSkip[i].asciiMoves[0] != 0L) && (data.singlesToSkip[i].asciiMoves[1] != 0L)) {
          writer.println(
              prefix + "   while ((curChar < 64" + " && (0x" + Long.toHexString(data.singlesToSkip[i].asciiMoves[0])
                  + "L & (1L << curChar)) != 0L) || \n" + prefix + "          (curChar >> 6) == 1" + " && (0x"
                  + Long.toHexString(data.singlesToSkip[i].asciiMoves[1]) + "L & (1L << (curChar & 077))) != 0L)");
        } else if (data.singlesToSkip[i].asciiMoves[1] == 0L) {
          writer.println(prefix + "   while (curChar <= " + (int) CppLexerGenerator.MaxChar(data.singlesToSkip[i].asciiMoves[0])
              + " && (0x" + Long.toHexString(data.singlesToSkip[i].asciiMoves[0]) + "L & (1L << curChar)) != 0L)");
        } else if (data.singlesToSkip[i].asciiMoves[0] == 0L) {
          writer.println(prefix + "   while (curChar > 63 && curChar <= "
              + (CppLexerGenerator.MaxChar(data.singlesToSkip[i].asciiMoves[1]) + 64) + " && (0x"
              + Long.toHexString(data.singlesToSkip[i].asciiMoves[1]) + "L & (1L << (curChar & 077))) != 0L)");
        }

        writer.println(prefix + "{");
        if (Options.getDebugTokenManager()) {
          if (data.maxLexStates > 1) {
            writer.println(
                "      fprintf(debugStream, \"<%s>\" , addUnicodeEscapes(lexStateNames[curLexState]).c_str());");
          }

          writer.println("      fprintf(debugStream, \"Skipping character : %c(%d)\\n\", curChar, (int)curChar);");
        }

        writer.println(prefix + "if (reader->endOfInput()) { goto EOFLoop; }");
        writer.println(prefix + "curChar = reader->beginToken();");
        writer.println(prefix + "}");
        writer.println(prefix + "}");
      }

      if ((data.initMatch[i] != Integer.MAX_VALUE) && (data.initMatch[i] != 0)) {
        if (Options.getDebugTokenManager()) {
          writer.println(
              "      fprintf(debugStream, \"   Matched the empty string as %s token.\\n\", addUnicodeEscapes(tokenImage["
                  + data.initMatch[i] + "]).c_str());");
        }

        writer.println(prefix + "jjmatchedKind = " + data.initMatch[i] + ";");
        writer.println(prefix + "jjmatchedPos = -1;");
        writer.println(prefix + "curPos = 0;");
      } else {
        writer.println(prefix + "jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
        writer.println(prefix + "jjmatchedPos = 0;");
      }

      if (Options.getDebugTokenManager()) {
        writer.println("   fprintf(debugStream, " + "\"<%s>Current character : %c(%d) at line %d column %d\\n\","
            + "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, "
            + "reader->getEndLine(), reader->getEndColumn());");
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
          writer.println(
              "           fprintf(debugStream, \"   Current character matched as a %s token.\\n\", addUnicodeEscapes(tokenImage["
                  + data.canMatchAnyChar[i] + "]).c_str());");
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
        writer.println(prefix + "         fprintf(debugStream, "
            + "\"   Putting back %d characters into the input stream.\\n\", (curPos - jjmatchedPos - 1));");
      }

      writer.println(prefix + "         reader->backup(curPos - jjmatchedPos - 1);");

      if (Options.getDebugTokenManager()) {
        writer.println(prefix + "      }");
      }

      if (Options.getDebugTokenManager()) {
        writer.println("    fprintf(debugStream, "
            + "\"****** FOUND A %d(%s) MATCH (%s) ******\\n\", jjmatchedKind, addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str(), addUnicodeEscapes(reader->GetSuffix(jjmatchedPos + 1)).c_str());");
      }

      if (data.hasSkip || data.hasMore || data.hasSpecial) {
        writer
            .println(prefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " + "(1L << (jjmatchedKind & 077))) != 0L)");
        writer.println(prefix + "      {");
      }

      writer.println(prefix + "         matchedToken = jjFillToken();");

      if (data.hasSpecial) {
        writer.println(prefix + "         matchedToken->specialToken = specialToken;");
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

            writer.println(prefix + "            if (specialToken == nullptr)");
            writer.println(prefix + "               specialToken = matchedToken;");
            writer.println(prefix + "            else");
            writer.println(prefix + "            {");
            writer.println(prefix + "               matchedToken->specialToken = specialToken;");
            writer.println(prefix + "               specialToken = (specialToken->next = matchedToken);");
            writer.println(prefix + "            }");

            if (data.hasSkipActions) {
              writer.println(prefix + "            SkipLexicalActions(matchedToken);");
            }

            writer.println(prefix + "         }");

            if (data.hasSkipActions) {
              writer.println(prefix + "         else");
              writer.println(prefix + "            SkipLexicalActions(nullptr);");
            }
          } else if (data.hasSkipActions) {
            writer.println(prefix + "         SkipLexicalActions(nullptr);");
          }

          if (data.maxLexStates > 1) {
            writer.println("         if (jjnewLexState[jjmatchedKind] != -1)");
            writer.println(prefix + "         curLexState = jjnewLexState[jjmatchedKind];");
          }

          writer.println(prefix + "         goto EOFLoop;");
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

          writer.println(prefix + "   if (!reader->endOfInput()) {");
          writer.println(prefix + "         curChar = reader->read(); // TOL: Support Unicode");

          if (Options.getDebugTokenManager()) {
            writer.println("   fprintf(debugStream, " + "\"<%s>Current character : %c(%d) at line %d column %d\\n\","
                + "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, "
                + "reader->getEndLine(), reader->getEndColumn());");
          }
          writer.println(prefix + "   continue;");
          writer.println(prefix + " }");
        }
      }

      writer.println(prefix + "   }");
      writer.println(prefix + "   int error_line = reader->getEndLine();");
      writer.println(prefix + "   int error_column = reader->getEndColumn();");
      writer.println(prefix + "   JJString error_after = JJEMPTY;");
      writer.println(prefix + "   bool EOFSeen = false;");
      writer.println(prefix + "   if (reader->endOfInput()) {");
      writer.println(prefix + "      EOFSeen = true;");
      writer.println(prefix + "      error_after = curPos <= 1 ? JJEMPTY : reader->getImage();");
      writer.println(prefix + "      if (curChar == '\\n' || curChar == '\\r') {");
      writer.println(prefix + "         error_line++;");
      writer.println(prefix + "         error_column = 0;");
      writer.println(prefix + "      }");
      writer.println(prefix + "      else");
      writer.println(prefix + "         error_column++;");
      writer.println(prefix + "   }");
      writer.println(prefix + "   if (!EOFSeen) {");
      writer.println(prefix + "      error_after = curPos <= 1 ? JJEMPTY : reader->getImage();");
      writer.println(prefix + "   }");
      writer.println(prefix
          + "   errorHandler->lexicalError(EOFSeen, curLexState, error_line, error_column, error_after, curChar);");
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
    writer.print("\nvoid " + data.getParserName() + "TokenManager::SkipLexicalActions(Token *matchedToken)");

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

        writer.println("      case " + i + " : {");

        if ((data.initMatch[data.getState(i)] == i) && data.canLoop[data.getState(i)]) {
          writer.println("         if (jjmatchedPos == -1)");
          writer.println("         {");
          writer.println("            if (jjbeenHere[" + data.getState(i) + "] &&");
          writer.println("                jjemptyLineNo[" + data.getState(i) + "] == reader->getBeginLine() &&");
          writer.println("                jjemptyColNo[" + data.getState(i) + "] == reader->getBeginColumn())");
          writer.println(
              "               errorHandler->lexicalError(JJString(\"(\"Error: Bailing out of infinite loop caused by repeated empty string matches \" + \"at line \" + reader->getBeginLine() + \", \" + \"column \" + reader->getBeginColumn() + \".\")), this);");
          writer.println("            jjemptyLineNo[" + data.getState(i) + "] = reader->getBeginLine();");
          writer.println("            jjemptyColNo[" + data.getState(i) + "] = reader->getBeginColumn();");
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
          writer.println("(reader->GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
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
      writer.println("       }");
    }

    writer.println("      default :");
    writer.println("         break;");
    writer.println("   }");
    writer.println("}");
  }

  private void DumpMoreActions(PrintWriter writer, LexerData data) {
    Action act;
    writer.print("\nvoid " + data.getParserName() + "TokenManager::MoreLexicalActions()");

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

        writer.println("      case " + i + " : {");

        if ((data.initMatch[data.getState(i)] == i) && data.canLoop[data.getState(i)]) {
          writer.println("         if (jjmatchedPos == -1)");
          writer.println("         {");
          writer.println("            if (jjbeenHere[" + data.getState(i) + "] &&");
          writer.println("                jjemptyLineNo[" + data.getState(i) + "] == reader->getBeginLine() &&");
          writer.println("                jjemptyColNo[" + data.getState(i) + "] == reader->getBeginColumn())");
          writer.println(
              "               errorHandler->lexicalError(JJString(\"(\"Error: Bailing out of infinite loop caused by repeated empty string matches \" + \"at line \" + reader->getBeginLine() + \", \" + \"column \" + reader->getBeginColumn() + \".\")), this);");
          writer.println("            jjemptyLineNo[" + data.getState(i) + "] = reader->getBeginLine();");
          writer.println("            jjemptyColNo[" + data.getState(i) + "] = reader->getBeginColumn();");
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
          writer.println("(reader->GetSuffix(jjimageLen));");
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
      writer.println("       }");
    }

    writer.println("      default :");
    writer.println("         break;");

    writer.println("   }");
    writer.println("}");
  }

  private void DumpTokenActions(PrintWriter writer, LexerData data) {
    Action act;
    int i;
    writer.print("\nvoid " + data.getParserName() + "TokenManager::TokenLexicalActions(Token *matchedToken)");
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

        writer.println("      case " + i + " : {");

        if ((data.initMatch[data.getState(i)] == i) && data.canLoop[data.getState(i)]) {
          writer.println("         if (jjmatchedPos == -1)");
          writer.println("         {");
          writer.println("            if (jjbeenHere[" + data.getState(i) + "] &&");
          writer.println("                jjemptyLineNo[" + data.getState(i) + "] == reader->getBeginLine() &&");
          writer.println("                jjemptyColNo[" + data.getState(i) + "] == reader->getBeginColumn())");
          writer.println(
              "               errorHandler->lexicalError(JJString(\"Error: Bailing out of infinite loop caused by repeated empty string matches "
                  + "at line \" + reader->getBeginLine() + \", "
                  + "column \" + reader->getBeginColumn() + \".\"), this);");
          writer.println("            jjemptyLineNo[" + data.getState(i) + "] = reader->getBeginLine();");
          writer.println("            jjemptyColNo[" + data.getState(i) + "] = reader->getBeginColumn();");
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
            writer.println("(reader->GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));");
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
      writer.println("       }");
    }

    writer.println("      default :");
    writer.println("         break;");
    writer.println("   }");
    writer.println("}");
  }

  private void genStringLiteralArrayCPP(PrintWriter writer, String varName, String[] arr) {
    // First generate char array vars
    for (int i = 0; i < arr.length; i++) {
      writer.println("static const JJChar " + varName + "_arr_" + i + "[] = ");
      genStringLiteralInCPP(writer, arr[i]);
      writer.println(";");
    }

    writer.println("static const JJString " + varName + "[] = {");
    for (int i = 0; i < arr.length; i++) {
      writer.println(varName + "_arr_" + i + ", ");
    }
    writer.println("};");
  }

  private void genStringLiteralInCPP(PrintWriter writer, String s) {
    // String literals in CPP become char arrays
    writer.print("{");
    for (int i = 0; i < s.length(); i++) {
      writer.print("0x" + Integer.toHexString(s.charAt(i)) + ", ");
    }
    writer.print("0}");
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

  private void DumpStatesForKind(PrintWriter writer, LexerData data) {
    boolean moreThanOne = false;
    int cnt = 0;

    writer.print("static const int kindForState[" + data.stateSetSize + "][" + data.stateSetSize + "] = ");

    if (data.kinds == null) {
      writer.println("null;");
      return;
    } else {
      writer.println("{");
    }

    for (int[] kind : data.kinds) {
      if (moreThanOne) {
        writer.println(",");
      }
      moreThanOne = true;

      if (kind == null) {
        writer.println("{}");
      } else {
        cnt = 0;
        writer.print("{ ");
        for (int element : kind) {
          if ((cnt % 15) == 0) {
            writer.print("\n  ");
          } else if (cnt > 1) {
            writer.print(" ");
          }

          writer.print(element);
          writer.print(", ");

        }

        writer.print("}");
      }
    }
    writer.println("\n};");
  }

  private void DumpStatesForStateCPP(PrintWriter writer, LexerData data) {
    if (data.statesForState == null) {
      assert (false) : "This should never be null.";
      return;
    }

    for (int i = 0; i < data.maxLexStates; i++) {
      if (data.statesForState[i] == null) {
        continue;
      }

      for (int j = 0; j < data.statesForState[i].length; j++) {
        int[] stateSet = data.statesForState[i][j];

        writer.print("const int stateSet_" + i + "_" + j + "[" + data.stateSetSize + "] = ");
        if (stateSet == null) {
          writer.println("   { " + j + " };");
          continue;
        }

        writer.print("   { ");

        for (int element : stateSet) {
          writer.print(element + ", ");
        }

        writer.println("};");
      }
    }

    for (int i = 0; i < data.maxLexStates; i++) {
      writer.println("const int *stateSet_" + i + "[] = {");
      if (data.statesForState[i] == null) {
        writer.println(" NULL, ");
        writer.println("};");
        continue;
      }

      for (int j = 0; j < data.statesForState[i].length; j++) {
        writer.print("stateSet_" + i + "_" + j + ",");
      }
      writer.println("};");
    }

    writer.print("const int** statesForState[] = { ");
    for (int i = 0; i < data.maxLexStates; i++) {
      writer.println("stateSet_" + i + ", ");
    }

    writer.println("\n};");
  }

  private void DumpStateSets(PrintWriter writer, LexerData data) {
    int cnt = 0;

    writer.print("static const int jjnextStates[] = {");
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

  private void DumpStrLiteralImages(PrintWriter writer, LexerData data) {
    // For C++
    String image;
    int i;
    int charCnt = 0; // Set to zero in reInit() but just to be sure

    int literalCount = 0;

    if (data.getImageCount() <= 0) {
      writer.println("static const JJString jjstrLiteralImages[] = {};");
      return;
    }

    data.setImage(0, "");
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

        writer.println("static JJChar jjstrLiteralChars_" + literalCount++ + "[] = {0};");
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
        writer.println("");
        charCnt = 0;
      }

      writer.println(toPrint);
    }

    while (++i < data.maxOrdinal) {
      if ((charCnt += 6) > 80) {
        writer.println("");
        charCnt = 0;
      }

      writer.println("static JJChar jjstrLiteralChars_" + literalCount++ + "[] = {0};");
      continue;
    }

    // Generate the array here.
    writer.println("static const JJString " + "jjstrLiteralImages[] = {");
    for (int j = 0; j < literalCount; j++) {
      writer.println("jjstrLiteralChars_" + j + ", ");
    }
    writer.println("};");
  }

  private void DumpStartWithStates(PrintWriter writer, LexerStateData data) {
    writer.print("\nint " + data.getParserName() + "TokenManager::jjStartNfaWithStates" + data.lexStateSuffix
        + "(int pos, int kind, int state)");

    writer.println("{");
    writer.println("   jjmatchedKind = kind;");
    writer.println("   jjmatchedPos = pos;");

    if (Options.getDebugTokenManager()) {
      writer.println("   fprintf(debugStream, \"   No more string literal token matches are possible.\");");
      writer.println(
          "   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
    }

    writer.println("   if (reader->endOfInput()) { return pos + 1; }");
    writer.println("   curChar = reader->read(); // TOL: Support Unicode");

    if (Options.getDebugTokenManager()) {
      writer.println("   fprintf(debugStream, " + "\"<%s>Current character : %c(%d) at line %d column %d\\n\","
          + "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, "
          + "reader->getEndLine(), reader->getEndColumn());");
    }
    writer.println("   return jjMoveNfa" + data.lexStateSuffix + "(state, pos + 1);");
    writer.println("}");
  }


  @Override
  protected final void DumpHeadForCase(PrintWriter writer, int byteNum) {
    if (byteNum == 0) {
      writer.println("         unsigned long long l = 1L << curChar;");
      writer.println("         (void)l;");
    } else if (byteNum == 1) {
      writer.println("         unsigned long long l = 1L << (curChar & 077);");
      writer.println("         (void)l;");
    } else {
      writer.println("         int hiByte = (curChar >> 8);");
      writer.println("         int i1 = hiByte >> 6;");
      writer.println("         unsigned long long l1 = 1L << (hiByte & 077);");
      writer.println("         int i2 = (curChar & 0xff) >> 6;");
      writer.println("         unsigned long long l2 = 1L << (curChar & 077);");
    }

    // writer.println(" MatchLoop: do");
    writer.println("         do");
    writer.println("         {");

    writer.println("            switch(jjstateSet[--i])");
    writer.println("            {");
  }

  private final void DumpNonAsciiMoveMethod(PrintWriter writer, LexerData data, NfaState state) {
    int j;
    writer.print("\nbool " + data.getParserName() + "TokenManager::jjCanMove_" + +state.nonAsciiMethod
        + "(int hiByte, int i1, int i2, unsigned long long l1, unsigned long long l2)");


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

  private final void dumpNfaAndDfa(PrintWriter writer, LexerStateData stateData) {
    if (stateData.hasNFA && !stateData.isMixedState()) {
      DumpNfaStartStatesCode(writer, stateData, stateData.statesForPos);
    }
    DumpDfaCode(writer, stateData);
    if (stateData.hasNFA) {
      DumpMoveNfa(writer, stateData);
    }
  }

  private final void dumpNfaAndDfaHeader(PrintWriter writer, LexerStateData stateData) {
    if (stateData.hasNFA && !stateData.isMixedState() && stateData.maxStrKind > 0) {
      int i, maxKindsReqd = (stateData.maxStrKind / 64) + 1;
      StringBuilder params = new StringBuilder();
      for (i = 0; i < (maxKindsReqd - 1); i++) {
        params.append("unsigned long long active" + i + ", ");
      }
      params.append("unsigned long long active" + i + ")");
      writer.println("int jjStopStringLiteralDfa" + stateData.lexStateSuffix + "(int pos, " + params + ";");

      params.setLength(0);
      params.append("(int pos, ");
      for (i = 0; i < (maxKindsReqd - 1); i++) {
        params.append("unsigned long long active" + i + ", ");
      }
      params.append("unsigned long long active" + i + ")");
      writer.println("int jjStartNfa" + stateData.lexStateSuffix + params + ";");
    }

    if (stateData.maxLen == 0) {
      writer.println(" int  jjMoveStringLiteralDfa0" + stateData.lexStateSuffix + "();");
    } else if (!stateData.global.boilerPlateDumped) {
      writer.println(" int " + " jjStopAtPos(int pos, int kind);");
      stateData.global.boilerPlateDumped = true;
    }

    DumpDfaCodeHeader(writer, stateData);
    if (!stateData.isMixedState() && (stateData.generatedStates() != 0) && stateData.createStartNfa) {
      writer.println("int jjStartNfaWithStates" + stateData.lexStateSuffix + "int pos, int kind, int state);");
    }
    if (stateData.hasNFA) {
      writer.println("int jjMoveNfa" + stateData.lexStateSuffix + "(int startState, int curPos);");
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
      params.append("unsigned long long active" + i + ", ");
    }
    params.append("unsigned long long active" + i + ")");

    writer.print("\nint " + data.getParserName() + "TokenManager::jjStopStringLiteralDfa" + data.lexStateSuffix
        + "(int pos, " + params);

    writer.println("{");

    if (Options.getDebugTokenManager()) {
      writer.println("      fprintf(debugStream, \"   No more string literal token matches are possible.\");");
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
      params.append("unsigned long long active" + i + ", ");
    }
    params.append("unsigned long long active" + i + ")");

    writer.print("\nint " + data.getParserName() + "TokenManager::jjStartNfa" + data.lexStateSuffix + params);
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
      writer.print(
          "\n  int " + data.getParserName() + "TokenManager::jjMoveStringLiteralDfa0" + data.lexStateSuffix + "()");
      DumpNullStrLiterals(writer, data);
      return;
    }

    if (!data.global.boilerPlateDumped) {
      writer.print("\n int  " + data.getParserName() + "TokenManager::jjStopAtPos(int pos, int kind)");
      writer.println("{");
      writer.println("   jjmatchedKind = kind;");
      writer.println("   jjmatchedPos = pos;");

      if (Options.getDebugTokenManager()) {
        writer.println("   fprintf(debugStream, \"   No more string literal token matches are possible.\");");
        writer.println(
            "   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
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
              params.append("unsigned long long active" + j);
            }
          }

          if (i <= data.maxLenForActive[j]) {
            if (atLeastOne) {
              params.append(", ");
            }
            params.append("unsigned long long active" + j);
          }
        } else {
          for (j = 0; j < (maxLongsReqd - 1); j++) {
            if (i <= (data.maxLenForActive[j] + 1)) {
              if (atLeastOne) {
                params.append(", ");
              } else {
                atLeastOne = true;
              }
              params.append("unsigned long long old" + j + ", " + "unsigned long long active" + j);
            }
          }

          if (i <= (data.maxLenForActive[j] + 1)) {
            if (atLeastOne) {
              params.append(", ");
            }
            params.append("unsigned long long old" + j + ", " + "unsigned long long active" + j);
          }
        }
      }
      params.append(")");
      writer.print("\n int  " + data.getParserName() + "TokenManager::jjMoveStringLiteralDfa" + i + data.lexStateSuffix
          + params);
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
          writer.println(
              "      fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\", (jjmatchedPos + 1), addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
          writer.println("   fprintf(debugStream, \"   Possible string literal matches : { \");");

          StringBuilder fmt = new StringBuilder();
          StringBuilder args = new StringBuilder();
          for (int vecs = 0; vecs < ((data.maxStrKind / 64) + 1); vecs++) {
            if (i <= data.maxLenForActive[vecs]) {
              if (fmt.length() > 0) {
                fmt.append(", ");
                args.append(", ");
              }

              fmt.append("%s");
              args.append("         jjKindsForBitVector(" + vecs + ", ");
              args.append("active" + vecs + ").c_str() ");
            }
          }

          fmt.append("}\\n");
          writer.println("    fprintf(debugStream, \"" + fmt + "\"," + args + ");");
        }

        writer.println("   if (reader->endOfInput()) {");

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
            writer.println(
                "      fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\", (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
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

      if ((i != 0)) {
        writer.println("   curChar = reader->readChar();");
      }

      if ((i != 0) && Options.getDebugTokenManager()) {
        writer.println("   fprintf(debugStream, " + "\"<%s>Current character : %c(%d) at line %d column %d\\n\","
            + "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, "
            + "reader->getEndLine(), reader->getEndColumn());");
      }

      writer.println("   switch(curChar)");
      writer.println("   {");

      for (String key2 : keys) {
        key = key2;
        info = (KindInfo) tab.get(key);
        ifGenerated = false;
        char c = key.charAt(0);

        if ((i == 0) && (c < 128) && (info.finalKindCnt != 0)
            && ((data.generatedStates() == 0) || !CanStartNfaUsingAscii(data, c))) {
          for (j = 0; j < maxLongsReqd; j++) {
            if (info.finalKinds[j] != 0L) {
              break;
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
        writer.println("      fprintf(debugStream, \"   No string literal matches possible.\\n\");");
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


  private final void DumpDfaCodeHeader(PrintWriter writer, LexerStateData data) {
    if (data.maxLen == 0) {
      return;
    }
    int maxLongsReqd = (data.maxStrKind / 64) + 1;
    int j;
    for (int i = 0; i < data.maxLen; i++) {
      boolean atLeastOne = false;
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
              params.append("unsigned long long active" + j);
            }
          }

          if (i <= data.maxLenForActive[j]) {
            if (atLeastOne) {
              params.append(", ");
            }
            params.append("unsigned long long active" + j);
          }
        } else {
          for (j = 0; j < (maxLongsReqd - 1); j++) {
            if (i <= (data.maxLenForActive[j] + 1)) {
              if (atLeastOne) {
                params.append(", ");
              } else {
                atLeastOne = true;
              }
              params.append("unsigned long long old" + j + ", " + "unsigned long long active" + j);
            }
          }

          if (i <= (data.maxLenForActive[j] + 1)) {
            if (atLeastOne) {
              params.append(", ");
            }
            params.append("unsigned long long old" + j + ", " + "unsigned long long active" + j);
          }
        }
      }
      params.append(")");
      writer.println(" int  jjMoveStringLiteralDfa" + i + data.lexStateSuffix + params + ";");
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

    writer.print("\nint " + data.getParserName() + "TokenManager::jjMoveNfa" + data.lexStateSuffix
        + "(int startState, int curPos)");
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
      writer.println("   reader->backup(seenUpto = curPos + 1);");
      writer.println("   assert(!reader->endOfInput());");
      writer.println("   curChar = reader->read(); // TOL: Support Unicode");
      writer.println("   curPos = 0;");
    }

    writer.println("   int startsAt = 0;");
    writer.println("   jjnewStateCnt = " + data.generatedStates() + ";");
    writer.println("   int i = 1;");
    writer.println("   jjstateSet[0] = startState;");

    if (Options.getDebugTokenManager()) {
      writer.println(
          "      fprintf(debugStream, \"   Starting NFA to match one of : %s\\n\", jjKindsForStateVector(curLexState, jjstateSet, 0, 1).c_str());");
    }

    if (Options.getDebugTokenManager()) {
      writer.println("   fprintf(debugStream, " + "\"<%s>Current character : %c(%d) at line %d column %d\\n\","
          + "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, "
          + "reader->getEndLine(), reader->getEndColumn());");
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
      writer.println(
          "   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
    }

    writer.println("      if ((i = jjnewStateCnt), (jjnewStateCnt = startsAt), (i == (startsAt = "
        + data.generatedStates() + " - startsAt)))");
    if (data.isMixedState()) {
      writer.println("         break;");
    } else {
      writer.println("         return curPos;");
    }

    if (Options.getDebugTokenManager()) {
      writer.println(
          "      fprintf(debugStream, \"   Possible kinds of longer matches : %s\\n\", jjKindsForStateVector(curLexState, jjstateSet, startsAt, i).c_str());");
    }

    if (data.isMixedState()) {
      writer.println("      if (reader->endOfInput()) { break; }");
    } else {
      writer.println("      if (reader->endOfInput()) { return curPos; }");
    }
    writer.println("      curChar = reader->read(); // TOL: Support Unicode");

    if (Options.getDebugTokenManager()) {
      writer.println("   fprintf(debugStream, " + "\"<%s>Current character : %c(%d) at line %d column %d\\n\","
          + "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, "
          + "reader->getEndLine(), reader->getEndColumn());");
    }

    writer.println("   }");

    if (data.isMixedState()) {
      writer.println("   if (jjmatchedPos > strPos)");
      writer.println("      return curPos;");
      writer.println("");
      writer.println("   int toRet = MAX(curPos, seenUpto);");
      writer.println("");
      writer.println("   if (curPos < toRet)");
      writer.println("      for (i = toRet - MIN(curPos, seenUpto); i-- > 0; )");
      writer.println("        {  assert(!reader->endOfInput());");
      writer.println("           curChar = reader->read(); } // TOL: Support Unicode");
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
