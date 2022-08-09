// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

package org.javacc.generator;

import org.fastcc.source.SourceWriter;
import org.fastcc.utils.Encoding;
import org.javacc.JavaCCLanguage;
import org.javacc.parser.JavaCCParserConstants;
import org.javacc.parser.Options;
import org.javacc.parser.ParseException;
import org.javacc.parser.Token;

import java.io.IOException;
import java.util.Locale;

class CodeGenerator {

  private final SourceWriter   source;
  private final JavaCCLanguage language;

  /**
   * Constructs an instance of {@link CodeGenerator}.
   *
   * @param source
   * @param langauge
   */
  protected CodeGenerator(SourceWriter source, JavaCCLanguage language) {
    this.source = source;
    this.language = language;
  }

  protected SourceWriter getSource() {
    return this.source;
  }

  /**
   * Set an option.
   *
   * @param name
   * @param value
   */
  protected final void addOption(String name, Object value) {
    getSource().getOptions().put(name, value);
  }

  protected final JavaCCLanguage getLanguage() {
    return this.language;
  }

  protected final boolean isCppLanguage() {
    return getLanguage() == JavaCCLanguage.Cpp;
  }

  protected final boolean isJavaLanguage() {
    return getLanguage() == JavaCCLanguage.Java;
  }

  protected final String getLongType() {
    switch (getLanguage()) {
      case Java:
        return "long";
      case Cpp:
        return "unsigned long long";
      default:
        throw new RuntimeException("Language type not fully supported : " + Options.getOutputLanguage());
    }
  }

  public void start() throws ParseException, IOException {}

  protected final void saveOutput() {
    getSource().saveOutput(Options.getOutputDirectory());
  }

  protected final void genCode(Object... code) {
    for (Object s : code) {
      getSource().append("" + s);
    }
  }

  protected final void genCodeLine(Object... code) {
    genCode(code);
    genCode("\n");
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

  protected final void genToken(Token t) {
    genCode(getStringToPrint(t));
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

  /**
   * Generate a modifier
   */
  private void genModifier(String mod) {
    String origMod = mod.toLowerCase(Locale.ENGLISH);
    if (isJavaLanguage()) {
      genCode(mod);
    } else if (origMod.equals("public") || origMod.equals("private")) {
      genCode(origMod + ": ");
    }
    // we don't care about other mods for now.
  }

  /**
   * Generate a class with a given name, an array of superclass and another array of super interfaes
   */
  protected final void genClassStart(String mod, String name, String[] superClasses, String[] superInterfaces) {
    if (isJavaLanguage() && (mod != null)) {
      genModifier(mod);
    }
    genCode("class " + name);
    if (isJavaLanguage()) {
      if ((superClasses.length == 1) && (superClasses[0] != null)) {
        genCode(" extends " + superClasses[0]);
      }
      if (superInterfaces.length != 0) {
        genCode(" implements ");
      }
    } else {
      if ((superClasses.length > 0) || (superInterfaces.length > 0)) {
        genCode(" : ");
      }

      genCommaSeperatedString(superClasses);
    }

    genCommaSeperatedString(superInterfaces);
    genCodeLine(" {");
    if (isCppLanguage()) {
      genCodeLine("public:");
    }
  }

  private void genCommaSeperatedString(String[] strings) {
    for (int i = 0; i < strings.length; i++) {
      if (i > 0) {
        genCode(", ");
      }

      genCode(strings[i]);
    }
  }
}
