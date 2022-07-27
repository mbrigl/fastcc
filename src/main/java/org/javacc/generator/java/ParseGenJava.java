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
import org.javacc.generator.ParserGenerator;
import org.javacc.parser.BNFProduction;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.JavaCCParserConstants;
import org.javacc.parser.Options;
import org.javacc.parser.ParseException;
import org.javacc.parser.Token;

import java.util.Iterator;
import java.util.List;

/**
 * Generate the parser.
 */
public class ParseGenJava extends ParserGenerator {

  /**
   * Constructs an instance of {@link ParseGenJava}.
   *
   * @param request
   * @param context
   */
  public ParseGenJava(JavaCCRequest request, JavaCCContext context) {
    super(new SourceWriter(request.getParserName()), request, context);
  }

  @Override
  public void start() throws ParseException {
    Token t = null;

    if (JavaCCErrors.hasError()) {
      throw new ParseException();
    }

    final List<String> tn = getParserContext().getToolNames();
    tn.add(JavaCC.TOOLNAME);

    // This is the first line generated -- the the comment line at the top of the generated parser
    genCodeLine("/* " + JavaCCToken.getIdString(tn) + " */");

    boolean implementsExists = false;
    // final boolean extendsExists = false;

    if (getParserRequest().toInsertionPoint1().size() != 0) {
      Object firstToken = getParserRequest().toInsertionPoint1().get(0);
      genTokenSetup((Token) firstToken);
      this.ccol = 1;
      for (final Iterator<Token> it = getParserRequest().toInsertionPoint1().iterator(); it.hasNext();) {
        t = it.next();
        if (t.kind == JavaCCParserConstants.IMPLEMENTS) {
          implementsExists = true;
        } else if (t.kind == JavaCCParserConstants.CLASS) {
          implementsExists = false;
        }

        genToken(t);
      }
    }

    if (implementsExists) {
      genCode(", ");
    } else {
      genCode(" implements ");
    }
    genCode(getParserRequest().getParserName() + "Constants ");
    if (getParserRequest().toInsertionPoint2().size() != 0) {
      genTokenSetup(getParserRequest().toInsertionPoint2().get(0));
      for (Token token : getParserRequest().toInsertionPoint2()) {
        genToken(token);
      }
    }

    if (!Options.isLegacy()) {
      genCodeLine("");
      genCodeLine("  protected final Node rootNode() { return jjtree.rootNode(); }");
      genCodeLine("  protected void jjtreeOpenNodeScope(Node node) throws ParseException {}");
      genCodeLine("  protected void jjtreeCloseNodeScope(Node node) throws ParseException {}");
      genCodeLine();
    }

    build();
    build2();

    if (getParserRequest().isStatic()) {
      genCodeLine("  static private boolean jj_initialized_once = false;");
    }
    genCodeLine("  /** Generated Token Manager. */");
    genCodeLine("  " + getJavaStatic() + "public " + getParserRequest().getParserName() + "TokenManager token_source;");
    genCodeLine("  " + getJavaStatic() + "JavaCharStream jj_input_stream;");
    genCodeLine("  /** Current token. */");
    genCodeLine("  " + getJavaStatic() + "public Token token;");
    genCodeLine("  /** Next token. */");
    genCodeLine("  " + getJavaStatic() + "public Token jj_nt;");
    if (!Options.getCacheTokens()) {
      genCodeLine("  " + getJavaStatic() + "private int jj_ntk;");
    }
    if (Options.getDepthLimit() > 0) {
      genCodeLine("  " + getJavaStatic() + "private int jj_depth;");
    }
    if (jj2Index() != 0) {
      genCodeLine("  " + getJavaStatic() + "private Token jj_scanpos, jj_lastpos;");
      genCodeLine("  " + getJavaStatic() + "private int jj_la;");
      if (lookAheadNeeded()) {
        genCodeLine("  /** Whether we are looking ahead. */");
        genCodeLine("  " + getJavaStatic() + "private boolean jj_lookingAhead = false;");
        genCodeLine("  " + getJavaStatic() + "private boolean jj_semLA;");
      }
    }
    if (Options.getErrorReporting()) {
      genCodeLine("  " + getJavaStatic() + "private int jj_gen;");
      genCodeLine("  " + getJavaStatic() + "final private int[] jj_la1 = new int[" + maskIndex() + "];");
      final int tokenMaskSize = ((getParserRequest().getTokenCount() - 1) / 32) + 1;
      for (int i = 0; i < tokenMaskSize; i++) {
        genCodeLine("  static private int[] jj_la1_" + i + ";");
      }
      genCodeLine("  static {");
      for (int i = 0; i < tokenMaskSize; i++) {
        genCodeLine("	   jj_la1_init_" + i + "();");
      }
      genCodeLine("	}");
      for (int i = 0; i < tokenMaskSize; i++) {
        genCodeLine("	private static void jj_la1_init_" + i + "() {");
        genCode("	   jj_la1_" + i + " = new int[] {");
        for (int[] maskVal : maskVals()) {
          genCode("0x" + Integer.toHexString(maskVal[i]) + ",");
        }
        genCodeLine("};");
        genCodeLine("	}");
      }
    }
    if ((jj2Index() != 0) && Options.getErrorReporting()) {
      genCodeLine("  " + getJavaStatic() + "final private JJCalls[] jj_2_rtns = new JJCalls[" + jj2Index() + "];");
      genCodeLine("  " + getJavaStatic() + "private boolean jj_rescan = false;");
      genCodeLine("  " + getJavaStatic() + "private int jj_gc = 0;");
    }
    genCodeLine("");

    if (Options.getDebugParser()) {
      genCodeLine("  {");
      genCodeLine("      enable_tracing();");
      genCodeLine("  }");
    }

    final String readerInterfaceName = "Provider";
    final String stringReaderClass = "StringProvider";


    genCodeLine("  /** Constructor. */");
    genCodeLine("  public " + getParserRequest().getParserName() + "(" + readerInterfaceName + " stream) {");
    if (getParserRequest().isStatic()) {
      genCodeLine("	 if (jj_initialized_once) {");
      genCodeLine("	   System.out.println(\"ERROR: Second call to constructor of static parser. \");");
      genCodeLine("	   System.out.println(\"	   You must either use ReInit() or "
          + "set the JavaCC option STATIC to false\");");
      genCodeLine("	   System.out.println(\"	   during parser generation.\");");
      genCodeLine("	   throw new RuntimeException();");
      genCodeLine("	 }");
      genCodeLine("	 jj_initialized_once = true;");
    }
    genCodeLine("	 jj_input_stream = new JavaCharStream(stream, 1, 1);");
    genCodeLine("	 token_source = new " + getParserRequest().getParserName() + "TokenManager(jj_input_stream);");
    genCodeLine("	 token = new Token();");
    if (Options.getCacheTokens()) {
      genCodeLine("	 token.next = jj_nt = token_source.getNextToken();");
    } else {
      genCodeLine("	 jj_ntk = -1;");
    }
    if (Options.getDepthLimit() > 0) {
      genCodeLine("    jj_depth = -1;");
    }
    if (Options.getErrorReporting()) {
      genCodeLine("	 jj_gen = 0;");
      if (maskIndex() > 0) {
        genCodeLine("	 for (int i = 0; i < " + maskIndex() + "; i++) jj_la1[i] = -1;");
      }
      if (jj2Index() != 0) {
        genCodeLine("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
      }
    }
    genCodeLine("  }");
    genCodeLine("");

    // Add-in a string based constructor because its convenient (modern only to prevent
    // regressions)
    genCodeLine("  /** Constructor. */");
    genCodeLine(
        "  public " + getParserRequest().getParserName() + "(String dsl) throws ParseException, TokenMgrException {");
    genCodeLine("	   this(new " + stringReaderClass + "(dsl));");
    genCodeLine("  }");
    genCodeLine("");

    genCodeLine("  public void ReInit(String s) {");
    genCodeLine("	  ReInit(new " + stringReaderClass + "(s));");
    genCodeLine("  }");


    genCodeLine("  /** Reinitialise. */");
    genCodeLine("  " + getJavaStatic() + "public void ReInit(" + readerInterfaceName + " stream) {");
    genCodeLine("	if (jj_input_stream == null) {");
    genCodeLine("	   jj_input_stream = new JavaCharStream(stream, 1, 1);");
    genCodeLine("	} else {");
    genCodeLine("	   jj_input_stream.ReInit(stream, 1, 1);");
    genCodeLine("	}");

    genCodeLine("	if (token_source == null) {");
    genCodeLine(" token_source = new " + getParserRequest().getParserName() + "TokenManager(jj_input_stream);");
    genCodeLine("	}");
    genCodeLine("");
    genCodeLine("	 token_source.ReInit(jj_input_stream);");

    genCodeLine("	 token = new Token();");
    if (Options.getCacheTokens()) {
      genCodeLine("	 token.next = jj_nt = token_source.getNextToken();");
    } else {
      genCodeLine("	 jj_ntk = -1;");
    }
    if (Options.getDepthLimit() > 0) {
      genCodeLine("    jj_depth = -1;");
    }
    if (getParserContext().isGenerated()) {
      genCodeLine("	 jjtree.reset();");
    }
    if (Options.getErrorReporting()) {
      genCodeLine("	 jj_gen = 0;");
      if (maskIndex() > 0) {
        genCodeLine("	 for (int i = 0; i < " + maskIndex() + "; i++) jj_la1[i] = -1;");
      }
      if (jj2Index() != 0) {
        genCodeLine("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
      }
    }
    genCodeLine("  }");
    genCodeLine("");
    genCodeLine("  /** Constructor with generated Token Manager. */");
    genCodeLine("  public " + getParserRequest().getParserName() + "(" + getParserRequest().getParserName()
        + "TokenManager tm) {");
    if (getParserRequest().isStatic()) {
      genCodeLine("	 if (jj_initialized_once) {");
      genCodeLine("	   System.out.println(\"ERROR: Second call to constructor of static parser. \");");
      genCodeLine("	   System.out.println(\"	   You must either use ReInit() or "
          + "set the JavaCC option STATIC to false\");");
      genCodeLine("	   System.out.println(\"	   during parser generation.\");");
      genCodeLine("	   throw new RuntimeException();");
      genCodeLine("	 }");
      genCodeLine("	 jj_initialized_once = true;");
    }
    genCodeLine("	 token_source = tm;");
    genCodeLine("	 token = new Token();");
    if (Options.getCacheTokens()) {
      genCodeLine("	 token.next = jj_nt = token_source.getNextToken();");
    } else {
      genCodeLine("	 jj_ntk = -1;");
    }
    if (Options.getDepthLimit() > 0) {
      genCodeLine("    jj_depth = -1;");
    }
    if (Options.getErrorReporting()) {
      genCodeLine("	 jj_gen = 0;");
      if (maskIndex() > 0) {
        genCodeLine("	 for (int i = 0; i < " + maskIndex() + "; i++) jj_la1[i] = -1;");
      }
      if (jj2Index() != 0) {
        genCodeLine("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
      }
    }
    genCodeLine("  }");
    genCodeLine("");
    genCodeLine("  /** Reinitialise. */");
    genCodeLine("  public void ReInit(" + getParserRequest().getParserName() + "TokenManager tm) {");
    genCodeLine("	 token_source = tm;");
    genCodeLine("	 token = new Token();");
    if (Options.getCacheTokens()) {
      genCodeLine("	 token.next = jj_nt = token_source.getNextToken();");
    } else {
      genCodeLine("	 jj_ntk = -1;");
    }
    if (Options.getDepthLimit() > 0) {
      genCodeLine("    jj_depth = -1;");
    }
    if (getParserContext().isGenerated()) {
      genCodeLine("	 jjtree.reset();");
    }
    if (Options.getErrorReporting()) {
      genCodeLine("	 jj_gen = 0;");
      if (maskIndex() > 0) {
        genCodeLine("	 for (int i = 0; i < " + maskIndex() + "; i++) jj_la1[i] = -1;");
      }
      if (jj2Index() != 0) {
        genCodeLine("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
      }
    }
    genCodeLine("  }");
    genCodeLine("");
    genCodeLine("  " + getJavaStatic() + "private Token jj_consume_token(int kind) throws ParseException {");
    if (Options.getCacheTokens()) {
      genCodeLine("	 Token oldToken = token;");
      genCodeLine("	 if ((token = jj_nt).next != null) jj_nt = jj_nt.next;");
      genCodeLine("	 else jj_nt = jj_nt.next = token_source.getNextToken();");
    } else {
      genCodeLine("	 Token oldToken;");
      genCodeLine("	 if ((oldToken = token).next != null) token = token.next;");
      genCodeLine("	 else token = token.next = token_source.getNextToken();");
      genCodeLine("	 jj_ntk = -1;");
    }
    genCodeLine("	 if (token.kind == kind) {");
    if (Options.getErrorReporting()) {
      genCodeLine("	   jj_gen++;");
      if (jj2Index() != 0) {
        genCodeLine("	   if (++jj_gc > 100) {");
        genCodeLine("		 jj_gc = 0;");
        genCodeLine("		 for (int i = 0; i < jj_2_rtns.length; i++) {");
        genCodeLine("		   JJCalls c = jj_2_rtns[i];");
        genCodeLine("		   while (c != null) {");
        genCodeLine("			 if (c.gen < jj_gen) c.first = null;");
        genCodeLine("			 c = c.next;");
        genCodeLine("		   }");
        genCodeLine("		 }");
        genCodeLine("	   }");
      }
    }
    if (Options.getDebugParser()) {
      genCodeLine("	   trace_token(token, \"\");");
    }
    genCodeLine("	   return token;");
    genCodeLine("	 }");
    if (Options.getCacheTokens()) {
      genCodeLine("	 jj_nt = token;");
    }
    genCodeLine("	 token = oldToken;");
    if (Options.getErrorReporting()) {
      genCodeLine("	 jj_kind = kind;");
    }
    genCodeLine("	 throw generateParseException();");
    genCodeLine("  }");
    genCodeLine("");
    if (jj2Index() != 0) {
      genCodeLine("  @SuppressWarnings(\"serial\")");
      genCodeLine("  static private final class LookaheadSuccess extends java.lang.RuntimeException {");
      genCodeLine("    @Override");
      genCodeLine("    public Throwable fillInStackTrace() {");
      genCodeLine("      return this;");
      genCodeLine("    }");
      genCodeLine("  }");
      genCodeLine("  static private final LookaheadSuccess jj_ls = new LookaheadSuccess();");
      genCodeLine("  " + getJavaStatic() + "private boolean jj_scan_token(int kind) {");
      genCodeLine("	 if (jj_scanpos == jj_lastpos) {");
      genCodeLine("	   jj_la--;");
      genCodeLine("	   if (jj_scanpos.next == null) {");
      genCodeLine("		 jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();");
      genCodeLine("	   } else {");
      genCodeLine("		 jj_lastpos = jj_scanpos = jj_scanpos.next;");
      genCodeLine("	   }");
      genCodeLine("	 } else {");
      genCodeLine("	   jj_scanpos = jj_scanpos.next;");
      genCodeLine("	 }");
      if (Options.getErrorReporting()) {
        genCodeLine("	 if (jj_rescan) {");
        genCodeLine("	   int i = 0; Token tok = token;");
        genCodeLine("	   while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }");
        genCodeLine("	   if (tok != null) jj_add_error_token(kind, i);");
        if (Options.getDebugLookahead()) {
          genCodeLine("	 } else {");
          genCodeLine("	   trace_scan(jj_scanpos, kind);");
        }
        genCodeLine("	 }");
      } else if (Options.getDebugLookahead()) {
        genCodeLine("	 trace_scan(jj_scanpos, kind);");
      }
      genCodeLine("	 if (jj_scanpos.kind != kind) return true;");
      genCodeLine("	 if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;");
      genCodeLine("	 return false;");
      genCodeLine("  }");
      genCodeLine("");
    }
    genCodeLine("");
    genCodeLine("/** Get the next Token. */");
    genCodeLine("  " + getJavaStatic() + "final public Token getNextToken() {");
    if (Options.getCacheTokens()) {
      genCodeLine("	 if ((token = jj_nt).next != null) jj_nt = jj_nt.next;");
      genCodeLine("	 else jj_nt = jj_nt.next = token_source.getNextToken();");
    } else {
      genCodeLine("	 if (token.next != null) token = token.next;");
      genCodeLine("	 else token = token.next = token_source.getNextToken();");
      genCodeLine("	 jj_ntk = -1;");
    }
    if (Options.getErrorReporting()) {
      genCodeLine("	 jj_gen++;");
    }
    if (Options.getDebugParser()) {
      genCodeLine("	   trace_token(token, \" (in getNextToken)\");");
    }
    genCodeLine("	 return token;");
    genCodeLine("  }");
    genCodeLine("");
    genCodeLine("/** Get the specific Token. */");
    genCodeLine("  " + getJavaStatic() + "final public Token getToken(int index) {");
    if (lookAheadNeeded()) {
      genCodeLine("	 Token t = jj_lookingAhead ? jj_scanpos : token;");
    } else {
      genCodeLine("	 Token t = token;");
    }
    genCodeLine("	 for (int i = 0; i < index; i++) {");
    genCodeLine("	   if (t.next != null) t = t.next;");
    genCodeLine("	   else t = t.next = token_source.getNextToken();");
    genCodeLine("	 }");
    genCodeLine("	 return t;");
    genCodeLine("  }");
    genCodeLine("");
    if (!Options.getCacheTokens()) {
      genCodeLine("  " + getJavaStatic() + "private int jj_ntk_f() {");
      genCodeLine("	 if ((jj_nt=token.next) == null)");
      genCodeLine("	   return (jj_ntk = (token.next=token_source.getNextToken()).kind);");
      genCodeLine("	 else");
      genCodeLine("	   return (jj_ntk = jj_nt.kind);");
      genCodeLine("  }");
      genCodeLine("");
    }

    if (Options.getErrorReporting()) {
      genCodeLine(
          "  " + getJavaStatic() + "private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();");
      genCodeLine("  " + getJavaStatic() + "private int[] jj_expentry;");
      genCodeLine("  " + getJavaStatic() + "private int jj_kind = -1;");
      if (jj2Index() != 0) {
        genCodeLine("  " + getJavaStatic() + "private int[] jj_lasttokens = new int[100];");
        genCodeLine("  " + getJavaStatic() + "private int jj_endpos;");
        genCodeLine("");
        genCodeLine("  " + getJavaStatic() + "private void jj_add_error_token(int kind, int pos) {");
        genCodeLine("	 if (pos >= 100) {");
        genCodeLine("		return;");
        genCodeLine("	 }");
        genCodeLine("");
        genCodeLine("	 if (pos == jj_endpos + 1) {");
        genCodeLine("	   jj_lasttokens[jj_endpos++] = kind;");
        genCodeLine("	 } else if (jj_endpos != 0) {");
        genCodeLine("	   jj_expentry = new int[jj_endpos];");
        genCodeLine("");
        genCodeLine("	   for (int i = 0; i < jj_endpos; i++) {");
        genCodeLine("		 jj_expentry[i] = jj_lasttokens[i];");
        genCodeLine("	   }");
        genCodeLine("");
        genCodeLine("	   for (int[] oldentry : jj_expentries) {");
        genCodeLine("		 if (oldentry.length == jj_expentry.length) {");
        genCodeLine("		   boolean isMatched = true;");
        genCodeLine("");
        genCodeLine("		   for (int i = 0; i < jj_expentry.length; i++) {");
        genCodeLine("			 if (oldentry[i] != jj_expentry[i]) {");
        genCodeLine("			   isMatched = false;");
        genCodeLine("			   break;");
        genCodeLine("			 }");
        genCodeLine("");
        genCodeLine("		   }");
        genCodeLine("		   if (isMatched) {");
        genCodeLine("			 jj_expentries.add(jj_expentry);");
        genCodeLine("			 break;");
        genCodeLine("		   }");
        genCodeLine("		 }");
        genCodeLine("	   }");
        genCodeLine("");
        genCodeLine("	   if (pos != 0) {");
        genCodeLine("		 jj_lasttokens[(jj_endpos = pos) - 1] = kind;");
        genCodeLine("	   }");
        genCodeLine("	 }");
        genCodeLine("  }");
      }
      genCodeLine("");
      genCodeLine("  /** Generate ParseException. */");
      genCodeLine("  " + getJavaStatic() + "public ParseException generateParseException() {");
      genCodeLine("	 jj_expentries.clear();");
      genCodeLine("	 boolean[] la1tokens = new boolean[" + getParserRequest().getTokenCount() + "];");
      genCodeLine("	 if (jj_kind >= 0) {");
      genCodeLine("	   la1tokens[jj_kind] = true;");
      genCodeLine("	   jj_kind = -1;");
      genCodeLine("	 }");
      genCodeLine("	 for (int i = 0; i < " + maskIndex() + "; i++) {");
      genCodeLine("	   if (jj_la1[i] == jj_gen) {");
      genCodeLine("		 for (int j = 0; j < 32; j++) {");
      for (int i = 0; i < (((getParserRequest().getTokenCount() - 1) / 32) + 1); i++) {
        genCodeLine("		   if ((jj_la1_" + i + "[i] & (1<<j)) != 0) {");
        genCode("			 la1tokens[");
        if (i != 0) {
          genCode((32 * i) + "+");
        }
        genCodeLine("j] = true;");
        genCodeLine("		   }");
      }
      genCodeLine("		 }");
      genCodeLine("	   }");
      genCodeLine("	 }");
      genCodeLine("	 for (int i = 0; i < " + getParserRequest().getTokenCount() + "; i++) {");
      genCodeLine("	   if (la1tokens[i]) {");
      genCodeLine("		 jj_expentry = new int[1];");
      genCodeLine("		 jj_expentry[0] = i;");
      genCodeLine("		 jj_expentries.add(jj_expentry);");
      genCodeLine("	   }");
      genCodeLine("	 }");
      if (jj2Index() != 0) {
        genCodeLine("	 jj_endpos = 0;");
        genCodeLine("	 jj_rescan_token();");
        genCodeLine("	 jj_add_error_token(0, 0);");
      }
      genCodeLine("	 int[][] exptokseq = new int[jj_expentries.size()][];");
      genCodeLine("	 for (int i = 0; i < jj_expentries.size(); i++) {");
      genCodeLine("	   exptokseq[i] = jj_expentries.get(i);");
      genCodeLine("	 }");


      // Add the lexical state onto the exception message
      genCodeLine("	 return new ParseException(token, exptokseq, tokenImage, token_source == null ? null : "
          + getParserRequest().getParserName() + "TokenManager.lexStateNames[token_source.curLexState]);");

      genCodeLine("  }");
    } else {
      genCodeLine("  /** Generate ParseException. */");
      genCodeLine("  " + getJavaStatic() + "public ParseException generateParseException() {");
      genCodeLine("	 Token errortok = token.next;");
      if (Options.getKeepLineColumn()) {
        genCodeLine("	 int line = errortok.beginLine, column = errortok.beginColumn;");
      }
      genCodeLine("	 String mess = (errortok.kind == 0) ? tokenImage[0] : errortok.image;");
      if (Options.getKeepLineColumn()) {
        genCodeLine("	 return new ParseException(" + "\"Parse error at line \" + line + \", column \" + column + \".  "
            + "Encountered: \" + mess);");
      } else {
        genCodeLine("	 return new ParseException(\"Parse error at <unknown location>.  " + "Encountered: \" + mess);");
      }
      genCodeLine("  }");
    }
    genCodeLine("");

    genCodeLine("  " + getJavaStatic() + "private boolean trace_enabled;");
    genCodeLine("");
    genCodeLine("/** Trace enabled. */");
    genCodeLine("  " + getJavaStatic() + "final public boolean trace_enabled() {");
    genCodeLine("	 return trace_enabled;");
    genCodeLine("  }");
    genCodeLine("");

    if (Options.getDebugParser()) {
      genCodeLine("  " + getJavaStatic() + "private int trace_indent = 0;");

      genCodeLine("/** Enable tracing. */");
      genCodeLine("  " + getJavaStatic() + "final public void enable_tracing() {");
      genCodeLine("	 trace_enabled = true;");
      genCodeLine("  }");
      genCodeLine("");
      genCodeLine("/** Disable tracing. */");
      genCodeLine("  " + getJavaStatic() + "final public void disable_tracing() {");
      genCodeLine("	 trace_enabled = false;");
      genCodeLine("  }");
      genCodeLine("");
      genCodeLine("  " + getJavaStatic() + "protected void trace_call(String s) {");
      genCodeLine("	 if (trace_enabled) {");
      genCodeLine("	   for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
      genCodeLine("	   System.out.println(\"Call:	\" + s);");
      genCodeLine("	 }");
      genCodeLine("	 trace_indent = trace_indent + 2;");
      genCodeLine("  }");
      genCodeLine("");
      genCodeLine("  " + getJavaStatic() + "protected void trace_return(String s) {");
      genCodeLine("	 trace_indent = trace_indent - 2;");
      genCodeLine("	 if (trace_enabled) {");
      genCodeLine("	   for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
      genCodeLine("	   System.out.println(\"Return: \" + s);");
      genCodeLine("	 }");
      genCodeLine("  }");
      genCodeLine("");
      genCodeLine("  " + getJavaStatic() + "protected void trace_token(Token t, String where) {");
      genCodeLine("	 if (trace_enabled) {");
      genCodeLine("	   for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
      genCodeLine("	   System.out.print(\"Consumed token: <\" + tokenImage[t.kind]);");
      genCodeLine("	   if (t.kind != 0 && !tokenImage[t.kind].equals(\"\\\"\" + t.image + \"\\\"\")) {");
      genCodeLine("		 System.out.print(\": \\\"\" + TokenMgrException.addEscapes(" + "t.image) + \"\\\"\");");
      genCodeLine("	   }");
      genCodeLine(
          "	   System.out.println(\" at line \" + t.beginLine + " + "\" column \" + t.beginColumn + \">\" + where);");
      genCodeLine("	 }");
      genCodeLine("  }");
      genCodeLine("");
      genCodeLine("  " + getJavaStatic() + "protected void trace_scan(Token t1, int t2) {");
      genCodeLine("	 if (trace_enabled) {");
      genCodeLine("	   for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
      genCodeLine("	   System.out.print(\"Visited token: <\" + tokenImage[t1.kind]);");
      genCodeLine("	   if (t1.kind != 0 && !tokenImage[t1.kind].equals(\"\\\"\" + t1.image + \"\\\"\")) {");
      genCodeLine("		 System.out.print(\": \\\"\" + TokenMgrException.addEscapes(" + "t1.image) + \"\\\"\");");
      genCodeLine("	   }");
      genCodeLine("	   System.out.println(\" at line \" + t1.beginLine + \""
          + " column \" + t1.beginColumn + \">; Expected token: <\" + tokenImage[t2] + \">\");");
      genCodeLine("	 }");
      genCodeLine("  }");
      genCodeLine("");
    } else {
      genCodeLine("  /** Enable tracing. */");
      genCodeLine("  " + getJavaStatic() + "final public void enable_tracing() {");
      genCodeLine("  }");
      genCodeLine("");
      genCodeLine("  /** Disable tracing. */");
      genCodeLine("  " + getJavaStatic() + "final public void disable_tracing() {");
      genCodeLine("  }");
      genCodeLine("");
    }

    if ((jj2Index() != 0) && Options.getErrorReporting()) {
      genCodeLine("  " + getJavaStatic() + "private void jj_rescan_token() {");
      genCodeLine("	 jj_rescan = true;");
      genCodeLine("	 for (int i = 0; i < " + jj2Index() + "; i++) {");
      genCodeLine("	   try {");
      genCodeLine("		 JJCalls p = jj_2_rtns[i];");
      genCodeLine("");
      genCodeLine("		 do {");
      genCodeLine("		   if (p.gen > jj_gen) {");
      genCodeLine("			 jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;");
      genCodeLine("			 switch (i) {");
      for (int i = 0; i < jj2Index(); i++) {
        genCodeLine("			   case " + i + ": jj_3_" + (i + 1) + "(); break;");
      }
      genCodeLine("			 }");
      genCodeLine("		   }");
      genCodeLine("		   p = p.next;");
      genCodeLine("		 } while (p != null);");
      genCodeLine("");
      genCodeLine("		 } catch(LookaheadSuccess ls) { }");
      genCodeLine("	 }");
      genCodeLine("	 jj_rescan = false;");
      genCodeLine("  }");
      genCodeLine("");
      genCodeLine("  " + getJavaStatic() + "private void jj_save(int index, int xla) {");
      genCodeLine("	 JJCalls p = jj_2_rtns[index];");
      genCodeLine("	 while (p.gen > jj_gen) {");
      genCodeLine("	   if (p.next == null) { p = p.next = new JJCalls(); break; }");
      genCodeLine("	   p = p.next;");
      genCodeLine("	 }");
      genCodeLine("");
      genCodeLine("	 p.gen = jj_gen + xla - jj_la; ");
      genCodeLine("	 p.first = token;");
      genCodeLine("	 p.arg = xla;");
      genCodeLine("  }");
      genCodeLine("");
    }

    if ((jj2Index() != 0) && Options.getErrorReporting()) {
      genCodeLine("  static final class JJCalls {");
      genCodeLine("	 int gen;");
      genCodeLine("	 Token first;");
      genCodeLine("	 int arg;");
      genCodeLine("	 JJCalls next;");
      genCodeLine("  }");
      genCodeLine("");
    }

    if (getParserRequest().fromInsertionPoint2().size() != 0) {
      genTokenSetup(getParserRequest().fromInsertionPoint2().get(0));
      this.ccol = 1;
      for (Object name : getParserRequest().fromInsertionPoint2()) {
        t = (Token) name;
        genToken(t);
      }
      genTrailingComments(t);
    }
    genCodeLine("");

    saveOutput();
  }

  @Override
  protected final String generateHeaderMethod(BNFProduction p, Token t) {
    genTokenSetup(t);
    JavaCCToken.setColumn();
    genLeadingComments(t);
    genCode("  " + getJavaStatic() + "final " + (p.getAccessMod() != null ? p.getAccessMod() : "public") + " ");
    JavaCCToken.set(t);
    genTokenOnly(t);
    for (int i = 1; i < p.getReturnTypeTokens().size(); i++) {
      t = (p.getReturnTypeTokens().get(i));
      genToken(t);
    }
    genTrailingComments(t);
    genCode(" " + p.getLhs() + "(");
    if (p.getParameterListTokens().size() != 0) {
      genTokenSetup((p.getParameterListTokens().get(0)));
      for (Iterator<Token> it = p.getParameterListTokens().iterator(); it.hasNext();) {
        t = it.next();
        genToken(t);
      }
      genTrailingComments(t);
    }
    genCode(")");
    genCode(" throws ParseException");

    for (Iterator<List<Token>> it = p.getThrowsList().iterator(); it.hasNext();) {
      genCode(", ");
      List<Token> name = it.next();
      for (Iterator<Token> it2 = name.iterator(); it2.hasNext();) {
        t = it2.next();
        genCode(t.image);
      }
    }

    return null;
  }
}
