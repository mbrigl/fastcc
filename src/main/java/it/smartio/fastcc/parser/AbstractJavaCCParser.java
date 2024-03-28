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

package it.smartio.fastcc.parser;

import java.util.ArrayList;
import java.util.List;

import it.smartio.fastcc.JJLanguage;

/**
 * Utilities.
 */
abstract class AbstractJavaCCParser implements JavaCCParserConstants {

  private JavaCCData data;
  private int        nextFreeLexState;

  /**
   * This int variable is incremented while parsing local lookaheads. Hence it keeps track of
   * *syntactic* lookahead nesting. This is used to provide warnings when actions and nested
   * lookaheads are used in syntactic lookahead productions. This is to prevent typos such as
   * leaving out the comma in LOOKAHEAD( foo(), {check()} ).
   */
  protected int      inLocalLA;

  // Set to true when the parser is within an action.
  protected boolean inAction;

  /**
   * This flag keeps track of whether or not return and throw statements have been patched during
   * the parsing of a production. The value of this flag is assigned the field with the same name in
   * BNFProduction.java.
   */
  protected boolean jumpPatched;

  /**
   * Constructs an instance of {@link AbstractJavaCCParser}.
   *
   */
  protected AbstractJavaCCParser() {
    this.nextFreeLexState = 1;
    this.inLocalLA = 0;
    this.inAction = false;
    this.jumpPatched = false;
  }

  /**
   * Gets the {@link #options}.
   */
  public Options getOptions() {
    throw new UnsupportedOperationException();
  }

  public void initialize(JavaCCData data) {
    this.data = data;
  }

  protected void setParserName() {
    this.data.setParser(getToken(0).image);
  }

  protected void addproduction(NormalProduction p) {
    this.data.addNormalProduction(p);
  }

  protected void production_addexpansion(BNFProduction p, Expansion e) {
    e.parent = p;
    p.setExpansion(e);
  }

  protected void addregexpr(TokenProduction p) {
    this.data.addTokenProduction(p);
    if (p.getLexStates() == null) {
      return;
    }
    for (int i = 0; i < p.getLexStates().length; i++) {
      for (int j = 0; j < i; j++) {
        if (p.getLexStates()[i].equals(p.getLexStates()[j])) {
          JavaCCErrors.parse_error(p, "Multiple occurrence of \"" + p.getLexStates()[i] + "\" in lexical state list.");
        }
      }
      if (this.data.hasLexState(p.getLexStates()[i])) {
        this.data.setLexState(p.getLexStates()[i], this.nextFreeLexState++);
      }
    }
  }

  protected void add_inline_regexpr(RegularExpression r) {
    if (!(r instanceof REndOfFile)) {
      TokenProduction p = new TokenProduction();
      p.setExplicit(false);
      p.setLexStates(new String[] { "DEFAULT" });
      p.setKind(TokenProduction.Kind.TOKEN);
      RegExprSpec res = new RegExprSpec();
      res.rexp = r;
      res.rexp.setTpContext(p);
      res.act = new Action();
      res.nextState = null;
      res.nsTok = null;
      p.getRespecs().add(res);
      this.data.addTokenProduction(p);
    }
  }

  private static boolean hexchar(char ch) {
    if ((ch >= '0') && (ch <= '9')) {
      return true;
    }
    if ((ch >= 'A') && (ch <= 'F')) {
      return true;
    }
    if ((ch >= 'a') && (ch <= 'f')) {
      return true;
    }
    return false;
  }

  private static int hexval(char ch) {
    if ((ch >= '0') && (ch <= '9')) {
      return (ch) - ('0');
    }
    if ((ch >= 'A') && (ch <= 'F')) {
      return ((ch) - ('A')) + 10;
    }
    return ((ch) - ('a')) + 10;
  }

  protected String remove_escapes_and_quotes(Token t, String str) {
    String retval = "";
    int index = 1;
    char ch, ch1;
    int ordinal;
    while (index < (str.length() - 1)) {
      if (str.charAt(index) != '\\') {
        retval += str.charAt(index);
        index++;
        continue;
      }
      index++;
      ch = str.charAt(index);
      if (ch == 'b') {
        retval += '\b';
        index++;
        continue;
      }
      if (ch == 't') {
        retval += '\t';
        index++;
        continue;
      }
      if (ch == 'n') {
        retval += '\n';
        index++;
        continue;
      }
      if (ch == 'f') {
        retval += '\f';
        index++;
        continue;
      }
      if (ch == 'r') {
        retval += '\r';
        index++;
        continue;
      }
      if (ch == '"') {
        retval += '\"';
        index++;
        continue;
      }
      if (ch == '\'') {
        retval += '\'';
        index++;
        continue;
      }
      if (ch == '\\') {
        retval += '\\';
        index++;
        continue;
      }
      if ((ch >= '0') && (ch <= '7')) {
        ordinal = (ch) - ('0');
        index++;
        ch1 = str.charAt(index);
        if ((ch1 >= '0') && (ch1 <= '7')) {
          ordinal = ((ordinal * 8) + (ch1)) - ('0');
          index++;
          ch1 = str.charAt(index);
          if ((ch <= '3') && (ch1 >= '0') && (ch1 <= '7')) {
            ordinal = ((ordinal * 8) + (ch1)) - ('0');
            index++;
          }
        }
        retval += (char) ordinal;
        continue;
      }
      if (ch == 'u') {
        index++;
        ch = str.charAt(index);
        if (AbstractJavaCCParser.hexchar(ch)) {
          ordinal = AbstractJavaCCParser.hexval(ch);
          index++;
          ch = str.charAt(index);
          if (AbstractJavaCCParser.hexchar(ch)) {
            ordinal = (ordinal * 16) + AbstractJavaCCParser.hexval(ch);
            index++;
            ch = str.charAt(index);
            if (AbstractJavaCCParser.hexchar(ch)) {
              ordinal = (ordinal * 16) + AbstractJavaCCParser.hexval(ch);
              index++;
              ch = str.charAt(index);
              if (AbstractJavaCCParser.hexchar(ch)) {
                ordinal = (ordinal * 16) + AbstractJavaCCParser.hexval(ch);
                index++;
                continue;
              }
            }
          }
        }
        JavaCCErrors.parse_error(t, "Encountered non-hex character '" + ch + "' at position " + index + " of string "
            + "- Unicode escape must have 4 hex digits after it.");
        return retval;
      }
      JavaCCErrors.parse_error(t, "Illegal escape sequence '\\" + ch + "' at position " + index + " of string.");
      return retval;
    }
    return retval;
  }

  protected char character_descriptor_assign(Token t, String s) {
    if (s.length() != 1) {
      JavaCCErrors.parse_error(t, "String in character list may contain only one character.");
      return ' ';
    } else {
      return s.charAt(0);
    }
  }

  protected char character_descriptor_assign(Token t, String s, String left) {
    if (s.length() != 1) {
      JavaCCErrors.parse_error(t, "String in character list may contain only one character.");
      return ' ';
    } else if ((left.charAt(0)) > (s.charAt(0))) {
      JavaCCErrors.parse_error(t, "Right end of character range \'" + s
          + "\' has a lower ordinal value than the left end of character range \'" + left + "\'.");
      return left.charAt(0);
    } else {
      return s.charAt(0);
    }
  }

  protected void makeTryBlock(Token tryLoc, Container result, Container nestedExp, List<List<Token>> types,
      List<Token> ids, List<List<Token>> catchblks, List<Token> finallyblk) {
    if ((catchblks.size() == 0) && (finallyblk == null)) {
      JavaCCErrors.parse_error(tryLoc, "Try block must contain at least one catch or finally block.");
      return;
    }
    TryBlock tblk = new TryBlock(ids, types, catchblks, finallyblk);
    tblk.setLine(tryLoc.beginLine);
    tblk.setColumn(tryLoc.beginColumn);
    tblk.setExpansion((Expansion) nestedExp.member);
    tblk.getExpansion().parent = tblk;
    tblk.getExpansion().ordinal = 0;
    result.member = tblk;
  }

  protected final boolean isCpp() {
    getOptions();
    return Options.getOutputLanguage() == JJLanguage.Cpp;
  }

  /*
   * Returns true if the next token is not in the FOLLOW list of "expansion". It is used to decide
   * when the end of an "expansion" has been reached.
   */
  protected boolean notTailOfExpansionUnit() {
    Token t;
    t = getToken(1);
    if ((t.kind == JavaCCParserConstants.BIT_OR) || (t.kind == JavaCCParserConstants.COMMA)
        || (t.kind == JavaCCParserConstants.RPAREN) || (t.kind == JavaCCParserConstants.RBRACE)
        || (t.kind == JavaCCParserConstants.RBRACKET)) {
      return false;
    }
    return true;
  }

  protected void eatUptoCloseBrace(List<Token> tokens) {
    int b = 1;
    Token t;
    while (((t = getToken(1)).kind != JavaCCParserConstants.RBRACE) || (--b != 0)) {
      if (tokens != null) {
        tokens.add(t);
      }
      if (t.kind == JavaCCParserConstants.EOF) {
        break;
      }
      if (t.kind == JavaCCParserConstants.LBRACE) {
        b++;
      }
      getNextToken(); // eat it
    }
  }

  protected void eatUptoRParen(List<Token> tokens) {
    int b = 1;
    Token t;
    while (((t = getToken(1)).kind != JavaCCParserConstants.RPAREN) || (--b != 0)) {
      if (tokens != null) {
        tokens.add(t);
      }
      if (t.kind == JavaCCParserConstants.EOF) {
        break;
      }
      if (t.kind == JavaCCParserConstants.LPAREN) {
        b++;
      }
      getNextToken(); // eat it
    }
  }

  protected abstract Token getNextToken();

  protected abstract Token getToken(int index);
  
  protected void Arguments() throws ParseException {
    Arguments(new ArrayList<>());
  }
  
  protected void Block() throws ParseException {
    Block(new ArrayList<>());
  }
  
    protected void Expression() throws ParseException {
    Expression(new ArrayList<>());
  }
  
  protected void FormalParameters() throws ParseException {
    FormalParameters(new ArrayList<>());
  }
  
  protected void Name() throws ParseException {
    Name(new ArrayList<>());
  }
  
  protected void ResultType() throws ParseException {
    ResultType(new ArrayList<>());
  }
  
  protected void TypeArguments() throws ParseException {
    TypeArguments(new ArrayList<>());
  }

  protected abstract void Arguments(List<Token> tokens) throws ParseException;
  protected abstract void Block(List<Token> tokens) throws ParseException;
  protected abstract void Expression(List<Token> tokens) throws ParseException;
  protected abstract void FormalParameters(List<Token> tokens) throws ParseException;
  protected abstract void Name(List<Token> tokens) throws ParseException;
  protected abstract void ResultType(List<Token> tokens) throws ParseException;
  protected abstract void TypeArguments(List<Token> tokens) throws ParseException;
}
