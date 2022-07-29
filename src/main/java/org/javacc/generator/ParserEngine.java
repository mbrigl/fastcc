/*
 * Copyright (c) 2001-2021 Territorium Online Srl / TOL GmbH. All Rights Reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code as defined in and that are
 * subject to the Territorium Online License Version 1.0. You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at http://www.tol.info/license/
 * and read it before using this file.
 *
 * The Original Code and all software distributed under the License are distributed on an 'AS IS'
 * basis, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, AND TERRITORIUM ONLINE HEREBY
 * DISCLAIMS ALL SUCH WARRANTIES, INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT. Please see the License for
 * the specific language governing rights and limitations under the License.
 */

package org.javacc.generator;

import org.javacc.JavaCCContext;
import org.javacc.JavaCCRequest;
import org.javacc.generator.cpp.CppTreeGenerator;
import org.javacc.generator.cpp.CppLexerGenerator;
import org.javacc.generator.cpp.CppOtherFilesGenerator;
import org.javacc.generator.cpp.CppParserGenerator;
import org.javacc.generator.java.JavaTreeGenerator;
import org.javacc.generator.java.JavaLexerGenerator;
import org.javacc.generator.java.JavaOtherFilesGenerator;
import org.javacc.generator.java.JavaParserGenerator;
import org.javacc.jjtree.ASTGrammar;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;

/**
 * The {@link ParserEngine} class.
 */
public class ParserEngine {

  private final JavaCCContext context;

  private LexerGenerator      lexerGenerator;
  private ParserGenerator     parserGenerator;
  private JJTreeCodeGenerator treeGenerator;
  private OtherFilesGenerator otherFilesGenerator;

  /**
   * Constructs an instance of {@link ParserEngine}.
   *
   * @param context
   */
  private ParserEngine(JavaCCContext context) {
    this.context = context;
  }

  public final void generate(JavaCCRequest request) throws IOException, ParseException {
    LexerData data = new LexerBuilder().build(request);
    lexerGenerator.start(data, context);
    parserGenerator.start(request, context);
    otherFilesGenerator.start(data, request);
  }

  /**
   * Create a new instance of {@link ParserEngine}.
   *
   * @param node
   * @param writer
   */
  public void generateJJTree(ASTGrammar node, PrintWriter writer) throws IOException {
    node.jjtAccept(treeGenerator, writer);
    treeGenerator.generateJJTree();
  }

  /**
   * Create a new instance of {@link ParserEngine}.
   *
   * @param context
   */
  public static ParserEngine create(JavaCCContext context) {
    ParserEngine engine = new ParserEngine(context);
    switch (context.getLanguage()) {
      case Cpp:
        engine.lexerGenerator = new CppLexerGenerator();
        engine.parserGenerator = new CppParserGenerator();
        engine.treeGenerator = new CppTreeGenerator();
        engine.otherFilesGenerator = new CppOtherFilesGenerator();
        break;
      case Java:
        engine.lexerGenerator = new JavaLexerGenerator();
        engine.parserGenerator = new JavaParserGenerator();
        engine.treeGenerator = new JavaTreeGenerator();
        engine.otherFilesGenerator = new JavaOtherFilesGenerator();
        break;
      default:
        throw new RuntimeException("Language '" + context.getLanguage() + "' type not supported!");
    }
    return engine;
  }
}
