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
import org.javacc.JavaCCLanguage;
import org.javacc.generator.JavaCCToken;
import org.javacc.generator.ParserData;
import org.javacc.generator.ParserGenerator;
import org.javacc.parser.BNFProduction;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.JavaCCParserConstants;
import org.javacc.parser.JavaCodeProduction;
import org.javacc.parser.Options;
import org.javacc.parser.ParseException;
import org.javacc.parser.Token;

import java.util.Iterator;
import java.util.List;

/**
 * Generate the parser.
 */
public class ParseGenCpp extends ParserGenerator {

  /**
   * Constructs an instance of {@link ParseGenCpp}.
   *
   * @param parserName
   */
  public ParseGenCpp(String parserName) {
    super(new CppWriter(parserName), JavaCCLanguage.Cpp);
  }

  @Override
  protected final CppWriter getSource() {
    return (CppWriter) super.getSource();
  }

  @Override
  protected void generate(ParserData data, List<String> tn) throws ParseException {
    getSource().switchToStatics();

    genCodeLine("#include \"" + data.getParserName() + ".h\"");
    genCodeLine("#include \"TokenManagerError.h\"");
    genCodeLine("#include \"" + data.getParserName() + "Tree.h\"");
    genCodeLine("");

    getSource().switchToHeader();

    // standard includes
    genCodeLine("#include \"JavaCC.h\"");
    genCodeLine("#include \"CharStream.h\"");
    genCodeLine("#include \"Token.h\"");
    genCodeLine("#include \"TokenManager.h\"");

    genCodeLine("#include \"" + data.getParserName() + "Constants.h\"");

    if (data.isGenerated()) {
      genCodeLine("#include \"JJT" + data.getParserName() + "State.h\"");
    }

    genCodeLine("#include \"DefaultParserErrorHandler.h\"");

    if (data.isGenerated()) {
      genCodeLine("#include \"" + data.getParserName() + "Tree.h\"");
    }

    if (Options.stringValue(JavaCC.JJPARSER_CPP_NAMESPACE).length() > 0) {
      genCodeLine("namespace " + Options.stringValue(JavaCC.JJPARSER_CPP_NAMESPACE) + " {");
    }

    genCodeLine("  struct JJCalls {");
    genCodeLine("    int        gen;");
    genCodeLine("    int        arg;");
    genCodeLine("    JJCalls*   next;");
    genCodeLine("    Token*     first;");
    genCodeLine("    ~JJCalls() { if (next) delete next; }");
    genCodeLine("     JJCalls() { next = nullptr; arg = 0; gen = -1; first = nullptr; }");
    genCodeLine("  };");
    genCodeLine("");

    genClassStart("", data.getParserName(), new String[] {}, new String[0]);
    getSource().switchToImpl();
    if (data.toInsertionPoint2().size() != 0) {
      Token t = null;
      genTokenSetup(data.toInsertionPoint2().get(0));
      for (Object name : data.toInsertionPoint2()) {
        t = (Token) name;
        genToken(t);
      }
    }

    getSource().switchToImpl();

    genCodeLine("");
    genCodeLine("");

    // Finally enclose the whole thing in the namespace, if specified.
    if (Options.stringValue(JavaCC.JJPARSER_CPP_NAMESPACE).length() > 0) {
      genCodeLine("namespace " + Options.stringValue(JavaCC.JJPARSER_CPP_NAMESPACE) + " {");
    }

    genCodeLine("");
    genCodeLine("");

    build(data);

    getSource().switchToHeader();
    build2(data);

    generateStaticContstants(data);

    getSource().switchToImpl();

    getSource().switchToHeader();
    genCodeLine("");
    genCodeLine("public: ");
    genCodeLine("  void setErrorHandler(ParserErrorHandler* eh) {");
    genCodeLine("    if (delete_eh) delete errorHandler;");
    genCodeLine("    errorHandler = eh;");
    genCodeLine("    delete_eh = false;");
    genCodeLine("  }");
    genCodeLine("  const ParserErrorHandler* getErrorHandler() {");
    genCodeLine("    return errorHandler;");
    genCodeLine("  }");
    genCodeLine("  static const JJChar* getTokenImage(int kind) {");
    genCodeLine("    return kind >= 0 ? tokenImages[kind] : tokenImages[0];");
    genCodeLine("  }");
    genCodeLine("  static const JJChar* getTokenLabel(int kind) {");
    genCodeLine("    return kind >= 0 ? tokenLabels[kind] : tokenLabels[0];");
    genCodeLine("  }");
    genCodeLine("");
    genCodeLine("  TokenManager*          token_source = nullptr;");
    genCodeLine("  CharStream*            jj_input_stream = nullptr;");
    genCodeLine("  Token*                 token = nullptr;  // Current token.");
    genCodeLine("  Token*                 jj_nt = nullptr;  // Next token.");
    genCodeLine("");
    genCodeLine("private: ");
    genCodeLine("  int                    jj_ntk;");
    genCodeLine("  JJCalls                jj_2_rtns[" + (data.jj2Index() + 1) + "];");
    genCodeLine("  bool                   jj_rescan;");
    genCodeLine("  int                    jj_gc;");
    genCodeLine("  Token*                 jj_scanpos;");
    genCodeLine("  Token*                 jj_lastpos;");
    genCodeLine("  int                    jj_la;");
    genCodeLine("  bool                   jj_lookingAhead;  // Whether we are looking ahead.");
    genCodeLine("  bool                   jj_semLA;");
    genCodeLine("  int                    jj_gen;");
    genCodeLine("  int                    jj_la1[" + (data.maskIndex() + 1) + "];");
    genCodeLine("  ParserErrorHandler*    errorHandler = nullptr;");
    genCodeLine("");
    genCodeLine("protected: ");
    genCodeLine("  bool                   delete_eh = false;");
    genCodeLine("  bool                   delete_tokens = true;");
    genCodeLine("  bool                   hasError;");
    genCodeLine("");

    getSource().switchToHeader(); // TEMP

    if (Options.getDepthLimit() > 0) {
      genCodeLine("  private: int jj_depth;");
      genCodeLine("  private: bool jj_depth_error;");
      genCodeLine("  friend class __jj_depth_inc;");
      genCodeLine("  class __jj_depth_inc {public:");
      genCodeLine("    " + data.getParserName() + "* parent;");
      genCodeLine("    __jj_depth_inc(" + data.getParserName() + "* p): parent(p) { parent->jj_depth++; };");
      genCodeLine("    ~__jj_depth_inc(){ parent->jj_depth--; }");
      genCodeLine("  };");
    }
    if (!Options.getStackLimit().equals("")) {
      genCodeLine("  public: size_t jj_stack_limit;");
      genCodeLine("  private: void* jj_stack_base;");
      genCodeLine("  private: bool jj_stack_error;");
    }

    genCodeLine("  Token *head; ");
    genCodeLine("public: ");
    generateHeaderMethodDefinition(" ", data.getParserName() + "(TokenManager *tokenManager)", data.getParserName());
    genCodeLine("{");
    genCodeLine("    head = nullptr;");
    genCodeLine("    ReInit(tokenManager);");
    genCodeLine("}");

    getSource().switchToHeader();
    genCodeLine("  virtual ~" + data.getParserName() + "();");
    getSource().switchToImpl();
    genCodeLine("" + data.getParserName() + "::~" + data.getParserName() + "()");
    genCodeLine("{");
    genCodeLine("  clear();");
    genCodeLine("}");
    generateHeaderMethodDefinition("void", "ReInit(TokenManager* tokenManager)", data.getParserName());
    genCodeLine("{");
    genCodeLine("    clear();");
    genCodeLine("    errorHandler = new DefaultParserErrorHandler();");
    genCodeLine("    delete_eh = true;");
    genCodeLine("    hasError = false;");
    genCodeLine("    token_source = tokenManager;");
    genCodeLine("    head = token = new Token;");
    genCodeLine("    jj_lookingAhead = false;");
    genCodeLine("    jj_rescan = false;");
    genCodeLine("    jj_done = false;");
    genCodeLine("    jj_scanpos = jj_lastpos = nullptr;");
    genCodeLine("    jj_gc = 0;");
    genCodeLine("    jj_kind = -1;");
    genCodeLine("    indent = 0;");
    genCodeLine("    trace = " + Options.getDebugParser() + ";");
    if (!Options.getStackLimit().equals("")) {
      genCodeLine("    jj_stack_limit = " + Options.getStackLimit() + ";");
      genCodeLine("    jj_stack_error = jj_stack_check(true);");
    }

    if (Options.getCacheTokens()) {
      genCodeLine("    token->next() = jj_nt = token_source->getNextToken();");
    } else {
      genCodeLine("    jj_ntk = -1;");
    }
    if (data.isGenerated()) {
      genCodeLine("    jjtree.reset();");
    }
    if (Options.getDepthLimit() > 0) {
      genCodeLine("    jj_depth = 0;");
      genCodeLine("    jj_depth_error = false;");
    }
    if (Options.getErrorReporting()) {
      genCodeLine("    jj_gen = 0;");
      if (data.maskIndex() > 0) {
        genCodeLine("    for (int i = 0; i < " + data.maskIndex() + "; i++) jj_la1[i] = -1;");
      }
    }
    genCodeLine("  }");
    genCodeLine("");

    generateHeaderMethodDefinition("void", "clear()", data.getParserName());
    genCodeLine("{");
    genCodeLine("  //Since token manager was generate from outside,");
    genCodeLine("  //parser should not take care of deleting");
    genCodeLine("  //if (token_source) delete token_source;");
    genCodeLine("  if (delete_tokens && head) {");
    genCodeLine("    Token* next;");
    genCodeLine("    Token* t = head;");
    genCodeLine("    while (t) {");
    genCodeLine("      next = t->next();");
    genCodeLine("      delete t;");
    genCodeLine("      t = next;");
    genCodeLine("    }");
    genCodeLine("  }");
    genCodeLine("  if (delete_eh) {");
    genCodeLine("    delete errorHandler, errorHandler = nullptr;");
    genCodeLine("    delete_eh = false;");
    genCodeLine("  }");
    if (Options.getDepthLimit() > 0) {
      genCodeLine("  assert(jj_depth==0);");
    }
    genCodeLine("}");
    genCodeLine("");

    if (!Options.getStackLimit().equals("")) {
      genCodeLine("");
      getSource().switchToHeader();
      genCodeLine(" virtual");
      getSource().switchToImpl();
      generateHeaderMethodDefinition("bool ", "jj_stack_check(bool init)", data.getParserName());
      genCodeLine("  {");
      genCodeLine("     if(init) {");
      genCodeLine("       jj_stack_base = nullptr;");
      genCodeLine("       return false;");
      genCodeLine("     } else {");
      genCodeLine("       volatile int q = 0;");
      genCodeLine("       if(!jj_stack_base) {");
      genCodeLine("         jj_stack_base = (void*)&q;");
      genCodeLine("         return false;");
      genCodeLine("       } else {");
      genCodeLine("         // Stack can grow in both directions, depending on arch");
      genCodeLine("         std::ptrdiff_t used = (char*)jj_stack_base-(char*)&q;");
      genCodeLine("         return (std::abs(used) > jj_stack_limit);");
      genCodeLine("       }");
      genCodeLine("     }");
      genCodeLine("  }");
    }


    generateHeaderMethodDefinition("Token *", "jj_consume_token(int kind)", data.getParserName());
    genCodeLine("  {");
    if (!Options.getStackLimit().equals("")) {
      genCodeLine("    if(kind != -1 && (jj_stack_error || jj_stack_check(false))) {");
      genCodeLine("      if (!jj_stack_error) {");
      genCodeLine("        errorHandler->handleOtherError(\"Stack overflow while trying to parse\", this);");
      genCodeLine("        jj_stack_error=true;");
      genCodeLine("      }");
      genCodeLine("      return jj_consume_token(-1);");
      genCodeLine("    }");
    }
    if (Options.getCacheTokens()) {
      genCodeLine("    Token *oldToken = token;");
      genCodeLine("    if ((token = jj_nt)->next() != nullptr) jj_nt = jj_nt->next();");
      genCodeLine("    else jj_nt = jj_nt->next() = token_source->getNextToken();");
    } else {
      genCodeLine("    Token *oldToken;");
      genCodeLine("    if ((oldToken = token)->next() != nullptr) token = token->next();");
      genCodeLine("    else token = token->next() = token_source->getNextToken();");
      genCodeLine("    jj_ntk = -1;");
    }
    genCodeLine("    if (token->kind() == kind) {");
    if (Options.getErrorReporting()) {
      genCodeLine("      jj_gen++;");
      if (data.jj2Index() != 0) {
        genCodeLine("      if (++jj_gc > 100) {");
        genCodeLine("        jj_gc = 0;");
        genCodeLine("        for (int i = 0; i < " + data.jj2Index() + "; i++) {");
        genCodeLine("          JJCalls *c = &jj_2_rtns[i];");
        genCodeLine("          while (c != nullptr) {");
        genCodeLine("            if (c->gen < jj_gen) c->first = nullptr;");
        genCodeLine("            c = c->next;");
        genCodeLine("          }");
        genCodeLine("        }");
        genCodeLine("      }");
      }
    }
    if (Options.getDebugParser()) {
      genCodeLine("      trace_token(token, \"\");");
    }
    genCodeLine("      return token;");
    genCodeLine("    }");
    if (Options.getCacheTokens()) {
      genCodeLine("    jj_nt = token;");
    }
    genCodeLine("    token = oldToken;");
    if (Options.getErrorReporting()) {
      genCodeLine("    jj_kind = kind;");
    }
    // genCodeLine(" throw generateParseException();");
    if (!Options.getStackLimit().equals("")) {
      genCodeLine("    if (!jj_stack_error) {");
    }

    genCodeLine("    const JJString expectedImage = getTokenImage(kind);");
    genCodeLine("    const JJString expectedLabel = getTokenLabel(kind);");
    genCodeLine("    const Token*   actualToken   = getToken(1);");
    genCodeLine("    const JJString actualImage   = getTokenImage(actualToken->kind());");
    genCodeLine("    const JJString actualLabel   = getTokenLabel(actualToken->kind());");
    genCodeLine(
        "    errorHandler->unexpectedToken(expectedImage, expectedLabel, actualImage, actualLabel, actualToken);");
    if (!Options.getStackLimit().equals("")) {
      genCodeLine("    }");
    }
    genCodeLine("    hasError = true;");
    genCodeLine("    return token;");
    genCodeLine("  }");
    genCodeLine("");

    if (data.jj2Index() != 0) {
      getSource().switchToImpl();
      generateHeaderMethodDefinition("bool ", "jj_scan_token(int kind)", data.getParserName());
      genCodeLine("{");
      if (!Options.getStackLimit().equals("")) {
        genCodeLine("    if(kind != -1 && (jj_stack_error || jj_stack_check(false))) {");
        genCodeLine("      if (!jj_stack_error) {");
        genCodeLine("        errorHandler->handleOtherError(\"Stack overflow while trying to parse\", this);");
        genCodeLine("        jj_stack_error=true;");
        genCodeLine("      }");
        genCodeLine("      return jj_consume_token(-1);");
        genCodeLine("    }");
      }
      genCodeLine("    if (jj_scanpos == jj_lastpos) {");
      genCodeLine("      jj_la--;");
      genCodeLine("      if (jj_scanpos->next() == nullptr) {");
      genCodeLine("        jj_lastpos = jj_scanpos = jj_scanpos->next() = token_source->getNextToken();");
      genCodeLine("      } else {");
      genCodeLine("        jj_lastpos = jj_scanpos = jj_scanpos->next();");
      genCodeLine("      }");
      genCodeLine("    } else {");
      genCodeLine("      jj_scanpos = jj_scanpos->next();");
      genCodeLine("    }");
      if (Options.getErrorReporting()) {
        genCodeLine("    if (jj_rescan) {");
        genCodeLine("      int i = 0; Token *tok = token;");
        genCodeLine("      while (tok != nullptr && tok != jj_scanpos) { i++; tok = tok->next(); }");
        genCodeLine("      if (tok != nullptr) jj_add_error_token(kind, i);");
        if (Options.getDebugLookahead()) {
          genCodeLine("    } else {");
          genCodeLine("      trace_scan(jj_scanpos, kind);");
        }
        genCodeLine("    }");
      } else if (Options.getDebugLookahead()) {
        genCodeLine("    trace_scan(jj_scanpos, kind);");
      }
      genCodeLine("    if (jj_scanpos->kind() != kind) return true;");
      // genCodeLine(" if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;");
      genCodeLine("    if (jj_la == 0 && jj_scanpos == jj_lastpos) { return jj_done = true; }");
      genCodeLine("    return false;");
      genCodeLine("  }");
      genCodeLine("");
    }
    genCodeLine("");
    genCodeLine("/** Get the next Token. */");
    generateHeaderMethodDefinition("Token *", "getNextToken()", data.getParserName());
    genCodeLine("{");
    if (Options.getCacheTokens()) {
      genCodeLine("    if ((token = jj_nt)->next() != nullptr) jj_nt = jj_nt->next();");
      genCodeLine("    else jj_nt = jj_nt->next() = token_source->getNextToken();");
    } else {
      genCodeLine("    if (token->next() != nullptr) token = token->next();");
      genCodeLine("    else token = token->next() = token_source->getNextToken();");
      genCodeLine("    jj_ntk = -1;");
    }
    if (Options.getErrorReporting()) {
      genCodeLine("    jj_gen++;");
    }
    if (Options.getDebugParser()) {
      genCodeLine("      trace_token(token, \" (in getNextToken)\");");
    }
    genCodeLine("    return token;");
    genCodeLine("  }");
    genCodeLine("");
    genCodeLine("/** Get the specific Token. */");
    generateHeaderMethodDefinition("Token *", "getToken(int index)", data.getParserName());
    genCodeLine("{");
    if (data.isLookAheadNeeded()) {
      genCodeLine("    Token *t = jj_lookingAhead ? jj_scanpos : token;");
    } else {
      genCodeLine("    Token *t = token;");
    }
    genCodeLine("    for (int i = 0; i < index; i++) {");
    genCodeLine("      if (t->next() != nullptr) t = t->next();");
    genCodeLine("      else t = t->next() = token_source->getNextToken();");
    genCodeLine("    }");
    genCodeLine("    return t;");
    genCodeLine("  }");
    genCodeLine("");
    if (!Options.getCacheTokens()) {
      generateHeaderMethodDefinition("int", "jj_ntk_f()", data.getParserName());
      genCodeLine("{");

      genCodeLine("    if ((jj_nt=token->next) == nullptr)");
      genCodeLine("      return (jj_ntk = (token->next=token_source->getNextToken())->kind);");
      genCodeLine("    else");
      genCodeLine("      return (jj_ntk = jj_nt->kind);");
      genCodeLine("  }");
      genCodeLine("");
    }

    getSource().switchToHeader();
    genCodeLine("private:");
    genCodeLine("  int jj_kind;");
    if (Options.getErrorReporting()) {
      genCodeLine("  int **jj_expentries;");
      genCodeLine("  int *jj_expentry;");
      if (data.jj2Index() != 0) {
        generateHeaderMethodDefinition("  void", "jj_add_error_token(int kind, int pos)", data.getParserName());
        genCodeLine("  {");
        genCodeLine("  }");
      }
      genCodeLine("");

      getSource().switchToHeader();
      genCodeLine("protected:");
      genCodeLine("  /** Generate ParseException. */");
      generateHeaderMethodDefinition("  virtual void ", "parseError()", data.getParserName());
      genCodeLine("   {");
      if (Options.getErrorReporting()) {
        genCodeLine(
            "      JJERR << JJWIDE(Parse error at : ) << token->beginLine() << JJWIDE(:) << token->beginColumn() << JJWIDE( after token: ) << addUnicodeEscapes(token->image()) << JJWIDE( encountered: ) << addUnicodeEscapes(getToken(1)->image()) << std::endl;");
      }
      genCodeLine("   }");
    } else {
      genCodeLine("protected:");
      genCodeLine("  /** Generate ParseException. */");
      generateHeaderMethodDefinition("virtual void ", "parseError()", data.getParserName());
      genCodeLine("   {");
      if (Options.getErrorReporting()) {
        genCodeLine(
            "      JJERR << JJWIDE(Parse error at : ) << token->beginLine() << JJWIDE(:) << token->beginColumn() << JJWIDE( after token: ) << addUnicodeEscapes(token->image()) << JJWIDE( encountered: ) << addUnicodeEscapes(getToken(1)->image()) << std::endl;");
      }
      genCodeLine("   }");
    }
    genCodeLine("");

    getSource().switchToHeader();
    genCodeLine("private:");
    genCodeLine("  int  indent; // trace indentation");
    genCodeLine("  bool trace = " + Options.getDebugParser() + ";");
    genCodeLine("  bool trace_la = " + Options.getDebugParser() + ";");
    genCodeLine("");
    genCodeLine("public:");
    generateHeaderMethodDefinition("  bool", "trace_enabled()", data.getParserName());
    genCodeLine("  {");
    genCodeLine("    return trace;");
    genCodeLine("  }");
    generateHeaderMethodDefinition("  bool", "trace_la_enabled()", data.getParserName());
    genCodeLine("  {");
    genCodeLine("    return trace_la;");
    genCodeLine("  }");
    genCodeLine("");
    if (Options.getDebugParser()) {
      getSource().switchToHeader();
      generateHeaderMethodDefinition("  void", "enable_tracing()", data.getParserName());
      genCodeLine("{");
      genCodeLine("    trace = true;");
      genCodeLine("}");
      genCodeLine("");

      getSource().switchToHeader();
      generateHeaderMethodDefinition("  void", "disable_tracing()", data.getParserName());
      genCodeLine("{");
      genCodeLine("    trace = false;");
      genCodeLine("}");
      genCodeLine("");

      getSource().switchToHeader();
      generateHeaderMethodDefinition("  void", "trace_call(const char *s)", data.getParserName());
      genCodeLine("  {");
      genCodeLine("    if (trace_enabled()) {");
      genCodeLine("      for (int i = 0; i < indent; i++) { printf(\" \"); }");
      genCodeLine("      printf(\"Call:   %s\\n\", s);");
      genCodeLine("    }");
      genCodeLine("    indent = indent + 2;");
      genCodeLine("  }");
      genCodeLine("");

      getSource().switchToHeader();
      generateHeaderMethodDefinition("  void", "trace_return(const char *s)", data.getParserName());
      genCodeLine("  {");
      genCodeLine("    indent = indent - 2;");
      genCodeLine("    if (trace_enabled()) {");
      genCodeLine("      for (int i = 0; i < indent; i++) { printf(\" \"); }");
      genCodeLine("      printf(\"Return: %s\\n\", s);");
      genCodeLine("    }");
      genCodeLine("  }");
      genCodeLine("");

      getSource().switchToHeader();
      generateHeaderMethodDefinition("  void", "trace_token(Token *t, const char *where)", data.getParserName());
      genCodeLine("  {");
      genCodeLine("    if (trace_enabled()) {");
      genCodeLine("      for (int i = 0; i < indent; i++) { printf(\" \"); }");
      genCodeLine(
          "      printf(\"Consumed token: <kind: %d(%s), \\\"%s\\\"\", t->kind, addUnicodeEscapes(tokenImage[t->kind]).c_str(), addUnicodeEscapes(t->image).c_str());");
      // genCodeLine(" if (t->kind != 0 && !tokenImage[t->kind].equals(\"\\\"\" + t->image +
      // \"\\\"\")) {");
      // genCodeLine(" System.out.print(\": \\\"\" + t->image + \"\\\"\");");
      // genCodeLine(" }");
      genCodeLine("      printf(\" at line %d column %d> %s\\n\", t->beginLine, t->beginColumn, where);");
      genCodeLine("    }");
      genCodeLine("  }");
      genCodeLine("");

      getSource().switchToHeader();
      generateHeaderMethodDefinition("  void", "trace_scan(Token *t1, int t2)", data.getParserName());
      genCodeLine("  {");
      genCodeLine("    if (trace_enabled()) {");
      genCodeLine("      for (int i = 0; i < indent; i++) { printf(\" \"); }");
      genCodeLine(
          "      printf(\"Visited token: <Kind: %d(%s), \\\"%s\\\"\", t1->kind, addUnicodeEscapes(tokenImage[t1->kind]).c_str(), addUnicodeEscapes(t1->image).c_str());");
      // genCodeLine(" if (t1->kind != 0 && !tokenImage[t1->kind].equals(\"\\\"\" + t1->image +
      // \"\\\"\")) {");
      // genCodeLine(" System.out.print(\": \\\"\" + t1->image + \"\\\"\");");
      // genCodeLine(" }");
      genCodeLine(
          "      printf(\" at line %d column %d>; Expected token: %s\\n\", t1->beginLine, t1->beginColumn, addUnicodeEscapes(tokenImage[t2]).c_str());");
      genCodeLine("    }");
      genCodeLine("  }");
      genCodeLine("");
    } else {
      getSource().switchToHeader();
      generateHeaderMethodDefinition("  void", "enable_tracing()", data.getParserName());
      genCodeLine("  {");
      genCodeLine("  }");
      getSource().switchToHeader();
      generateHeaderMethodDefinition("  void", "disable_tracing()", data.getParserName());
      genCodeLine("  {");
      genCodeLine("  }");
      getSource().switchToHeader();
      generateHeaderMethodDefinition("  void", "enable_la_tracing()", data.getParserName());
      genCodeLine("  {");
      genCodeLine("  }");
      getSource().switchToHeader();
      generateHeaderMethodDefinition("  void", "disable_la_tracing()", data.getParserName());
      genCodeLine("  {");
      genCodeLine("  }");
      genCodeLine("");
    }

    if ((data.jj2Index() != 0) && Options.getErrorReporting()) {
      generateHeaderMethodDefinition("  void", "jj_rescan_token()", data.getParserName());
      genCodeLine("{");
      genCodeLine("    jj_rescan = true;");
      genCodeLine("    for (int i = 0; i < " + data.jj2Index() + "; i++) {");
      // genCodeLine(" try {");
      genCodeLine("      JJCalls *p = &jj_2_rtns[i];");
      genCodeLine("      do {");
      genCodeLine("        if (p->gen > jj_gen) {");
      genCodeLine("          jj_la = p->arg; jj_lastpos = jj_scanpos = p->first;");
      genCodeLine("          switch (i) {");
      for (int i = 0; i < data.jj2Index(); i++) {
        genCodeLine("            case " + i + ": jj_3_" + (i + 1) + "(); break;");
      }
      genCodeLine("          }");
      genCodeLine("        }");
      genCodeLine("        p = p->next;");
      genCodeLine("      } while (p != nullptr);");
      // genCodeLine(" } catch(LookaheadSuccess ls) { }");
      genCodeLine("    }");
      genCodeLine("    jj_rescan = false;");
      genCodeLine("  }");
      genCodeLine("");

      generateHeaderMethodDefinition("  void", "jj_save(int index, int xla)", data.getParserName());
      genCodeLine("{");
      genCodeLine("    JJCalls *p = &jj_2_rtns[index];");
      genCodeLine("    while (p->gen > jj_gen) {");
      genCodeLine("      if (p->next == nullptr) { p = p->next = new JJCalls(); break; }");
      genCodeLine("      p = p->next;");
      genCodeLine("    }");
      genCodeLine("    p->gen = jj_gen + xla - jj_la; p->first = token; p->arg = xla;");
      genCodeLine("  }");
      genCodeLine("");
    }

    genCodeLine();

    // in the include file close the class signature
    getSource().switchToHeader();

    // copy other stuff
    genCodeLine("protected:");
    genCodeLine("  virtual void jjtreeOpenNodeScope(Node * node) = 0;");
    genCodeLine("  virtual void jjtreeCloseNodeScope(Node * node) = 0;");
    genCodeLine();

    if (data.isGenerated()) {
      genCodeLine("  JJT" + data.getParserName() + "State jjtree;");
    }
    genCodeLine("private:");
    genCodeLine("  bool jj_done;");
    genCodeLine("};");

    saveOutput();
  }

  // Print method header and return the ERROR_RETURN string.
  @Override
  protected final String generateHeaderMethod(ParserData data, BNFProduction p, Token t, String parserName) {
    StringBuilder sig = new StringBuilder();
    String ret, params;

    String method_name = p.getLhs();
    boolean void_ret = false;
    boolean ptr_ret = false;

    genTokenSetup(t);
    JavaCCToken.setColumn();
    getLeadingComments(t);
    JavaCCToken.set(t);
    sig.append(t.image);
    if (t.kind == JavaCCParserConstants.VOID) {
      void_ret = true;
    }
    if (t.kind == JavaCCParserConstants.STAR) {
      ptr_ret = true;
    }

    for (int i = 1; i < p.getReturnTypeTokens().size(); i++) {
      t = (p.getReturnTypeTokens().get(i));
      sig.append(getStringToPrint(t));
      if (t.kind == JavaCCParserConstants.VOID) {
        void_ret = true;
      }
      if (t.kind == JavaCCParserConstants.STAR) {
        ptr_ret = true;
      }
    }

    getTrailingComments(t);
    ret = sig.toString();

    sig.setLength(0);
    sig.append("(");
    if (p.getParameterListTokens().size() != 0) {
      genTokenSetup((p.getParameterListTokens().get(0)));
      for (Iterator<Token> it = p.getParameterListTokens().iterator(); it.hasNext();) {
        t = it.next();
        sig.append(getStringToPrint(t));
      }
      sig.append(getTrailingComments(t));
    }
    sig.append(")");
    params = sig.toString();

    // For now, just ignore comments
    generateHeaderMethodDefinition(ret, p.getLhs() + params, parserName);

    // Generate a default value for error return.
    String default_return;
    if (ptr_ret) {
      default_return = "NULL";
    } else if (void_ret) {
      default_return = "";
    } else {
      default_return = "0"; // 0 converts to most (all?) basic types.
    }

    StringBuilder ret_val = new StringBuilder("\n#if !defined ERROR_RET_" + method_name + "\n");
    ret_val.append("#define ERROR_RET_" + method_name + " " + default_return + "\n");
    ret_val.append("#endif\n");
    ret_val.append("#define __ERROR_RET__ ERROR_RET_" + method_name + "\n");

    return ret_val.toString();
  }

  private void generateStaticContstants(ParserData data) {
    int tokenMaskSize = ((data.getTokenCount() - 1) / 32) + 1;
    if (Options.getErrorReporting() && (tokenMaskSize > 0)) {
      getSource().switchToStatics();
      for (int i = 0; i < tokenMaskSize; i++) {
        if (data.maskVals().size() > 0) {
          genCodeLine("static unsigned int jj_la1_" + i + "[] = {");
          for (int[] maskVal : data.maskVals()) {
            genCode("0x" + Integer.toHexString(maskVal[i]) + ",");
          }
          genCodeLine("};");
        }
      }
    }
  }

  private void generateHeaderMethodDefinition(String qualifiedModsAndRetType, String nameAndParams, String parserName) {
    ((CppWriter) getSource()).switchToHeader();
    genCodeLine(qualifiedModsAndRetType + " " + nameAndParams + ";");

    String modsAndRetType = null;
    int i = qualifiedModsAndRetType.lastIndexOf(':');
    if (i >= 0) {
      modsAndRetType = qualifiedModsAndRetType.substring(i + 1);
    }

    if (modsAndRetType != null) {
      i = modsAndRetType.lastIndexOf("virtual");
      if (i >= 0) {
        modsAndRetType = modsAndRetType.substring(i + "virtual".length());
      }
    }
    i = qualifiedModsAndRetType.lastIndexOf("virtual");
    if (i >= 0) {
      qualifiedModsAndRetType = qualifiedModsAndRetType.substring(i + "virtual".length());
    }
    ((CppWriter) getSource()).switchToImpl();
    genCode("\n" + qualifiedModsAndRetType + " " + parserName + "::" + nameAndParams);
  }

  @Override
  protected final void genStackCheck(boolean voidReturn) {
    if (Options.getDepthLimit() > 0) {
      if (!voidReturn) {
        genCodeLine("if(jj_depth_error){ return __ERROR_RET__; }");
      } else {
        genCodeLine("if(jj_depth_error){ return; }");
      }
      genCodeLine("__jj_depth_inc __jj_depth_counter(this);");
      genCodeLine("if(jj_depth > " + Options.getDepthLimit() + ") {");
      genCodeLine("  jj_depth_error = true;");
      genCodeLine("  jj_consume_token(-1);");
      genCodeLine("  errorHandler->parseError(token, getToken(1), __FUNCTION__), hasError = true;");
      if (!voidReturn) {
        genCodeLine("  return __ERROR_RET__;"); // Non-recoverable error
      } else {
        genCodeLine("  return;"); // Non-recoverable error
      }
      genCodeLine("}");
    }
  }

  @Override
  protected final void genJavaCodeProduction(ParserData data, JavaCodeProduction production) {
    JavaCCErrors.semantic_error("Cannot use JAVACODE productions with C++ output (yet).");
  }
}
