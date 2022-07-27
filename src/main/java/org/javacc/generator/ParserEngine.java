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
import org.javacc.generator.cpp.CPPCodeGenerator;
import org.javacc.generator.cpp.CPPFiles;
import org.javacc.generator.cpp.LexerCpp;
import org.javacc.generator.cpp.OtherFilesGenCPP;
import org.javacc.generator.cpp.ParseGenCpp;
import org.javacc.generator.java.JJTreeState;
import org.javacc.generator.java.JavaCodeGenerator;
import org.javacc.generator.java.LexerJava;
import org.javacc.generator.java.OtherFilesGenJava;
import org.javacc.generator.java.ParseGenJava;
import org.javacc.jjtree.ASTGrammar;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;

/**
 * The {@link ParserEngine} class.
 */
public class ParserEngine {

  private final JavaCCContext context;

  /**
   * Constructs an instance of {@link ParserEngine}.
   *
   * @param context
   */
  private ParserEngine(JavaCCContext context) {
    this.context = context;
  }

  public final void generate(JavaCCRequest request) throws IOException, ParseException {
    switch (context.getLanguage()) {
      case Java:
        LexerGenerator generator = new LexerJava(request, context);
        generator.start();
        new ParseGenJava(request, context).start();
        OtherFilesGenJava.start(generator.getLexerData(), context);
        break;

      case Cpp:
        generator = new LexerCpp(request, context);
        generator.start();
        new ParseGenCpp(request, context).start();
        OtherFilesGenCPP.start(generator.getLexerData(), context);
        break;
      default:
    }
  }

  /**
   * Create a new instance of {@link ParserEngine}.
   *
   * @param node
   * @param writer
   */
  public void generateJJTree(ASTGrammar node, PrintWriter writer) throws IOException {
    switch (context.getLanguage()) {
      case Java:
        node.jjtAccept(new JavaCodeGenerator(), writer);
        JJTreeState.generateJJTree();
        break;
      case Cpp:
        node.jjtAccept(new CPPCodeGenerator(), writer);
        CPPFiles.generateJJTree();
        break;
      default:
        throw new RuntimeException("Language type not supported for JJTree : " + context.getLanguage());
    }
  }

  /**
   * Create a new instance of {@link ParserEngine}.
   *
   * @param context
   */
  public static ParserEngine create(JavaCCContext context) {
    switch (context.getLanguage()) {
      case Cpp:
      case Java:
        return new ParserEngine(context);
      default:
        throw new RuntimeException("Language '" + context.getLanguage() + "' type not supported!");
    }
  }
}
