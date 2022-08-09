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

import org.fastcc.utils.Encoding;
import org.javacc.JavaCCRequest;
import org.javacc.parser.JavaCCParserConstants;
import org.javacc.parser.Token;

import java.io.PrintWriter;
import java.util.List;

/**
 * This package contains data created as a result of parsing and semanticizing a JavaCC input file.
 * This data is what is used by the back-ends of JavaCC as well as any other back-end of JavaCC
 * related tools such as JJTree.
 */
public class JavaCCTokenInsertion extends JavaCCToken {

  private static void printToken(Token t, PrintWriter ostr) {
    Token tt = t.specialToken;
    if (tt != null) {
      while (tt.specialToken != null) {
        tt = tt.specialToken;
      }
      while (tt != null) {
        JavaCCTokenInsertion.printTokenOnly(tt, ostr);
        tt = tt.next;
      }
    }
    JavaCCTokenInsertion.printTokenOnly(t, ostr);
  }

  private static void printTokenOnly(Token t, PrintWriter ostr) {
    for (; JavaCCToken.cline < t.beginLine; JavaCCToken.cline++) {
      ostr.println("");
      JavaCCToken.ccol = 1;
    }
    for (; JavaCCToken.ccol < t.beginColumn; JavaCCToken.ccol++) {
      ostr.print(" ");
    }
    if ((t.kind == JavaCCParserConstants.STRING_LITERAL) || (t.kind == JavaCCParserConstants.CHARACTER_LITERAL)) {
      ostr.print(Encoding.escapeUnicode(t.image));
    } else {
      ostr.print(t.image);
    }
    JavaCCToken.cline = t.endLine;
    JavaCCToken.ccol = t.endColumn + 1;
    char last = t.image.charAt(t.image.length() - 1);
    if ((last == '\n') || (last == '\r')) {
      JavaCCToken.cline++;
      JavaCCToken.ccol = 1;
    }
  }

  private static String printLeadingComments(Token t) {
    String retval = "";
    if (t.specialToken == null) {
      return retval;
    }
    Token tt = t.specialToken;
    while (tt.specialToken != null) {
      tt = tt.specialToken;
    }
    while (tt != null) {
      retval += JavaCCToken.printTokenOnly(tt);
      tt = tt.next;
    }
    if ((JavaCCToken.ccol != 1) && (JavaCCToken.cline != t.beginLine)) {
      retval += "\n";
      JavaCCToken.cline++;
      JavaCCToken.ccol = 1;
    }
    return retval;
  }

  private static void printTrailingComments(Token t, PrintWriter ostr) {
    if (t.next == null) {
      return;
    }
    JavaCCTokenInsertion.printLeadingComments(t.next);
  }

  public static void printTokenSetup(PrintWriter writer, JavaCCRequest request) {
    Token t = null;
    List<Token> tokens = request.toInsertionPoint1();
    if ((tokens.size() != 0) && (tokens.get(0).kind == JavaCCParserConstants.PACKAGE)) {
      for (int i = 1; i < tokens.size(); i++) {
        if (tokens.get(i).kind == JavaCCParserConstants.SEMICOLON) {
          JavaCCToken.printTokenSetup(tokens.get(0));
          for (int j = 0; j <= i; j++) {
            t = tokens.get(j);
            JavaCCTokenInsertion.printToken(t, writer);
          }
          JavaCCTokenInsertion.printTrailingComments(t, writer);
          writer.println("");
          writer.println("");
          break;
        }
      }
    }
  }

  public static void print(PrintWriter writer, JavaCCRequest request) {
    List<Token> tokens = request.toInsertionPoint1();
    if ((tokens.size() != 0) && (tokens.get(0).kind == JavaCCParserConstants.PACKAGE)) {
      for (int i = 1; i < tokens.size(); i++) {
        if (tokens.get(i).kind == JavaCCParserConstants.SEMICOLON) {
          JavaCCToken.cline = tokens.get(0).beginLine;
          JavaCCToken.ccol = tokens.get(0).beginColumn;
          for (int j = 0; j <= i; j++) {
            JavaCCTokenInsertion.printToken(tokens.get(j), writer);
          }
          writer.println("");
          writer.println("");
          break;
        }
      }
    }
  }
}
