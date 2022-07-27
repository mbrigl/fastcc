// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

package org.javacc.generator;

import org.fastcc.source.SourceWriter;
import org.fastcc.utils.Encoding;
import org.javacc.parser.JavaCCParserConstants;
import org.javacc.parser.Options;
import org.javacc.parser.Token;

class CodeGenerator {

  protected final void saveOutput(SourceWriter writer) {
    writer.saveOutput(Options.getOutputDirectory());
  }

  public int cline, ccol;

  protected void genTokenSetup(Token t) {
    Token tt = t;

    while (tt.specialToken != null) {
      tt = tt.specialToken;
    }

    this.cline = tt.beginLine;
    this.ccol = tt.beginColumn;
  }

  protected final String getStringToPrint(Token t) {
    String retval = "";
    Token tt = t.specialToken;
    if (tt != null) {
      while (tt.specialToken != null) {
        tt = tt.specialToken;
      }
      while (tt != null) {
        retval += getStringForTokenOnly(tt);
        tt = tt.next;
      }
    }

    return retval + getStringForTokenOnly(t);
  }

  protected String getStringForTokenOnly(Token t) {
    String retval = "";
    for (; this.cline < t.beginLine; this.cline++) {
      retval += "\n";
      this.ccol = 1;
    }
    for (; this.ccol < t.beginColumn; this.ccol++) {
      retval += " ";
    }
    if ((t.kind == JavaCCParserConstants.STRING_LITERAL) || (t.kind == JavaCCParserConstants.CHARACTER_LITERAL)) {
      retval += Encoding.escapeUnicode(t.image);
    } else {
      retval += t.image;
    }
    this.cline = t.endLine;
    this.ccol = t.endColumn + 1;
    if (t.image.length() > 0) {
      char last = t.image.charAt(t.image.length() - 1);
      if ((last == '\n') || (last == '\r')) {
        this.cline++;
        this.ccol = 1;
      }
    }
    return retval;
  }
}
