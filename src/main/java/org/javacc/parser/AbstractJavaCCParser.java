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

package org.javacc.parser;

import java.util.List;

/**
 * Utilities.
 */
abstract class AbstractJavaCCParser implements JavaCCParserConstants {

  static final class ModifierSet {

    /* Definitions of the bits in the modifiers field. */
    static final int PUBLIC       = 0x0001;
    static final int PROTECTED    = 0x0002;
    static final int PRIVATE      = 0x0004;
    static final int ABSTRACT     = 0x0008;
    static final int STATIC       = 0x0010;
    static final int FINAL        = 0x0020;
    static final int SYNCHRONIZED = 0x0040;
    static final int NATIVE       = 0x0080;
    static final int TRANSIENT    = 0x0100;
    static final int VOLATILE     = 0x0200;
    static final int STRICTFP     = 0x1000;
  }


  private JavaCCData  data;
  private List<Token> add_cu_token_here;
  private Token       first_cu_token;
  private boolean     insertionpoint1set;
  private boolean     insertionpoint2set;
  private int         nextFreeLexState;


  // The name of the parser class.
  protected String parser_class_name;

  // This flag is set to true when the part between PARSER_BEGIN and PARSER_END is being parsed.
  protected boolean processing_cu;

  // The level of class nesting.
  protected int class_nesting;

  /**
   * This int variable is incremented while parsing local lookaheads. Hence it keeps track of
   * *syntactic* lookahead nesting. This is used to provide warnings when actions and nested
   * lookaheads are used in syntactic lookahead productions. This is to prevent typos such as
   * leaving out the comma in LOOKAHEAD( foo(), {check()} ).
   */
  protected int inLocalLA;

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
    first_cu_token = null;
    insertionpoint1set = false;
    insertionpoint2set = false;
    nextFreeLexState = 1;
    processing_cu = false;
    class_nesting = 0;
    inLocalLA = 0;
    inAction = false;
    jumpPatched = false;
  }

  public void initialize(JavaCCData data) {
    this.data = data;
    this.add_cu_token_here = data.toInsertionPoint1();
  }

  protected void addcuname(String id) {
    this.data.setParser(id);
  }

  protected void compare(Token t, String id1, String id2) {
    if (!id2.equals(id1)) {
      JavaCCErrors.parse_error(t, "Name " + id2 + " must be the same as that used at PARSER_BEGIN (" + id1 + ")");
    }
  }

  protected void setinsertionpoint(Token t, int no) {
    do {
      this.add_cu_token_here.add(this.first_cu_token);
      this.first_cu_token = this.first_cu_token.next;
    } while (this.first_cu_token != t);
    if (no == 1) {
      if (this.insertionpoint1set) {
        JavaCCErrors.parse_error(t, "Multiple declaration of parser class.");
      } else {
        this.insertionpoint1set = true;
        this.add_cu_token_here = this.data.toInsertionPoint2();
      }
    } else {
      this.add_cu_token_here = this.data.fromInsertionPoint2();
      this.insertionpoint2set = true;
    }
    this.first_cu_token = t;
  }

  protected void insertionpointerrors(Token t) {
    while (this.first_cu_token != t) {
      this.add_cu_token_here.add(this.first_cu_token);
      this.first_cu_token = this.first_cu_token.next;
    }
    if (!this.insertionpoint1set || !this.insertionpoint2set) {
      JavaCCErrors.parse_error(t, "Parser class has not been defined between PARSER_BEGIN and PARSER_END.");
    }
  }

  protected void set_initial_cu_token(Token t) {
    this.first_cu_token = t;
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
    if (p.lexStates == null) {
      return;
    }
    for (int i = 0; i < p.lexStates.length; i++) {
      for (int j = 0; j < i; j++) {
        if (p.lexStates[i].equals(p.lexStates[j])) {
          JavaCCErrors.parse_error(p, "Multiple occurrence of \"" + p.lexStates[i] + "\" in lexical state list.");
        }
      }
      if (data.hasLexState(p.lexStates[i])) {
        data.setLexState(p.lexStates[i], this.nextFreeLexState++);
      }
    }
  }

  protected void add_token_manager_decls(Token t, List<Token> decls) {
    this.data.setTokens(t, decls);
  }

  protected void add_inline_regexpr(RegularExpression r) {
    if (!(r instanceof REndOfFile)) {
      TokenProduction p = new TokenProduction();
      p.isExplicit = false;
      p.lexStates = new String[] { "DEFAULT" };
      p.kind = TokenProduction.TOKEN;
      RegExprSpec res = new RegExprSpec();
      res.rexp = r;
      res.rexp.tpContext = p;
      res.act = new Action();
      res.nextState = null;
      res.nsTok = null;
      p.respecs.add(res);
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
    TryBlock tblk = new TryBlock();
    tblk.setLine(tryLoc.beginLine);
    tblk.setColumn(tryLoc.beginColumn);
    tblk.exp = (Expansion) (nestedExp.member);
    tblk.exp.parent = tblk;
    tblk.exp.ordinal = 0;
    tblk.types = types;
    tblk.ids = ids;
    tblk.catchblks = catchblks;
    tblk.finallyblk = finallyblk;
    result.member = tblk;
  }

  protected final boolean isJavaLanguage() {
    return Options.getOutputLanguage().equalsIgnoreCase(Options.OUTPUT_LANGUAGE__JAVA);
  }

  /*
   * Returns true if the next token is not in the FOLLOW list of "expansion". It is used to decide
   * when the end of an "expansion" has been reached.
   */
  protected boolean notTailOfExpansionUnit() {
    Token t;
    t = getToken(1);
    if (t.kind == JavaCCParserConstants.BIT_OR || t.kind == JavaCCParserConstants.COMMA
        || t.kind == JavaCCParserConstants.RPAREN || t.kind == JavaCCParserConstants.RBRACE
        || t.kind == JavaCCParserConstants.RBRACKET)
      return false;
    return true;
  }

  /*
   * return true if the token is allowed in a ResultType. Used to mark a c++ result type as an error
   * for a java grammar
   */
  protected boolean isAllowed(Token t) {
    if (isJavaLanguage() && (t.kind == JavaCCParserConstants.STAR || t.kind == JavaCCParserConstants.BIT_AND
        || t.kind == JavaCCParserConstants.CONST))
      return false;
    else
      return true;
  }

  protected void eatUptoCloseBrace(List<Token> tokens) {
    int b = 1;
    Token t;
    while ((t = getToken(1)).kind != JavaCCParserConstants.RBRACE || --b != 0) {
      if (tokens != null) {
        tokens.add(t);
      }
      if (t.kind == JavaCCParserConstants.EOF)
        break;
      if (t.kind == JavaCCParserConstants.LBRACE)
        b++;
      getNextToken(); // eat it
    }
  }

  protected void eatUptoRParen(List<Token> tokens) {
    int b = 1;
    Token t;
    while ((t = getToken(1)).kind != JavaCCParserConstants.RPAREN || --b != 0) {
      if (tokens != null) {
        tokens.add(t);
      }
      if (t.kind == JavaCCParserConstants.EOF)
        break;
      if (t.kind == JavaCCParserConstants.LPAREN)
        b++;
      getNextToken(); // eat it
    }
  }

  protected abstract Token getNextToken();

  protected abstract Token getToken(int index);
}
