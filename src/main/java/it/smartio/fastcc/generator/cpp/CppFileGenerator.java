// Copyright 2012 Google Inc. All Rights Reserved.
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

package it.smartio.fastcc.generator.cpp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import it.smartio.fastcc.FastCC;
import it.smartio.fastcc.JavaCCRequest;
import it.smartio.fastcc.generator.AbstractFileGenerator;
import it.smartio.fastcc.generator.FileGenerator;
import it.smartio.fastcc.generator.LexerData;
import it.smartio.fastcc.parser.JavaCCErrors;
import it.smartio.fastcc.parser.ParseException;
import it.smartio.fastcc.parser.RStringLiteral;
import it.smartio.fastcc.parser.RegExprSpec;
import it.smartio.fastcc.parser.RegularExpression;
import it.smartio.fastcc.parser.TokenProduction;
import it.smartio.fastcc.utils.DigestOptions;
import it.smartio.fastcc.utils.DigestWriter;
import it.smartio.fastcc.utils.TemplateOptions;

/**
 * Generates the Constants file.
 */
public class CppFileGenerator extends AbstractFileGenerator implements FileGenerator {

  /**
   * Gets the template by name.
   * 
   * @param name
   */
  protected final String getTemplate(String name) {
    return String.format("/templates/cpp/%s.template", name);
  }

  /**
   * Creates a new {@link DigestWriter}.
   * 
   * @param file
   * @param options
   */
  protected final DigestWriter createDigestWriter(File file, DigestOptions options) throws FileNotFoundException {
    return DigestWriter.createCpp(file, FastCC.VERSION, options);
  }

  @Override
  public final void handleRequest(JavaCCRequest request, LexerData context) throws ParseException {
    List<RegularExpression> expressions = new ArrayList<>();
    for (TokenProduction tp : request.getTokenProductions()) {
      for (RegExprSpec res : tp.getRespecs()) {
        expressions.add(res.rexp);
      }
    }

    TemplateOptions options = new TemplateOptions();
    options.add("STATES", context.getStateCount()).set("name", i -> context.getStateName(i));
    options.add("TOKENS", request.getOrderedsTokens()).set("ordinal", r -> r.getOrdinal()).set("label",
        r -> r.getLabel());
    options.add("REGEXPS", expressions.size() + 1).set("label", (i, w) -> getRegExp(w, false, i, expressions))
        .set("image", (i, w) -> getRegExp(w, true, i, expressions));


    generateFile("JavaCC.h", new DigestOptions(context.options()));

    generateFile("Token.h", new DigestOptions(context.options()));
    generateFile("Token.cc", new DigestOptions(context.options()));
    generateFile("TokenManager.h", new DigestOptions(context.options()));
    generateFile("TokenManagerError.h", new DigestOptions(context.options()));
    generateFile("TokenManagerError.cc", new DigestOptions(context.options()));
    generateFile("TokenManagerErrorHandler.h", new DigestOptions(context.options()));
    generateFile("TokenManagerErrorHandler.cc", new DigestOptions(context.options()));

    generateFile("Reader.h", new DigestOptions(context.options()));
    generateFile("StringReader.h", new DigestOptions(context.options()));
    generateFile("StringReader.cc", new DigestOptions(context.options()));

    generateFile("ParseException.h", new DigestOptions(context.options()));
    generateFile("ParseException.cc", new DigestOptions(context.options()));
    generateFile("ParserErrorHandler.h", new DigestOptions(context.options()));
    generateFile("ParserErrorHandler.cc", new DigestOptions(context.options()));

    generateFile("ParserConstants.h", request.getParserName() + "Constants.h",
        new DigestOptions(context.options(), options));
  }

  private static void getRegExp(PrintWriter writer, boolean isImage, int i, List<RegularExpression> expressions) {
    if (i == 0) {
      CppFileGenerator.printCharArray(writer, "<EOF>");
    } else {
      RegularExpression expr = expressions.get(i - 1);
      if (expr instanceof RStringLiteral) {
        if (isImage) {
          CppFileGenerator.printCharArray(writer, ((RStringLiteral) expr).getImage());
        } else {
          CppFileGenerator.printCharArray(writer, "<" + expr.getLabel() + ">");
        }
      } else if (expr.getLabel().isEmpty()) {
        if (expr.getTpContext().getKind() == TokenProduction.Kind.TOKEN) {
          JavaCCErrors.warning(expr, "Consider giving this non-string token a label for better error reporting.");
        }
        CppFileGenerator.printCharArray(writer, "<token of kind " + expr.getOrdinal() + ">");
      } else {
        CppFileGenerator.printCharArray(writer, "<" + expr.getLabel() + ">");
      }
    }
  }

  // Used by the CPP code generatror
  protected static void printCharArray(PrintWriter writer, String s) {
    for (int i = 0; i < s.length(); i++) {
      writer.print("0x" + Integer.toHexString(s.charAt(i)) + ", ");
    }
  }
}
