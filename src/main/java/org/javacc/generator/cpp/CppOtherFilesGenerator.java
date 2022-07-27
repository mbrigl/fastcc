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

package org.javacc.generator.cpp;

import org.fastcc.utils.DigestOptions;
import org.fastcc.utils.DigestWriter;
import org.fastcc.utils.Template;
import org.fastcc.utils.Version;
import org.javacc.JavaCC;
import org.javacc.JavaCCRequest;
import org.javacc.generator.LexerData;
import org.javacc.generator.OtherFilesGenerator;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.Options;
import org.javacc.parser.ParseException;
import org.javacc.parser.RStringLiteral;
import org.javacc.parser.RegExprSpec;
import org.javacc.parser.RegularExpression;
import org.javacc.parser.TokenProduction;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Generates the Constants file.
 */
public class CppOtherFilesGenerator implements OtherFilesGenerator {

  private static final String TEMPLATE = "/templates/cpp/%s.template";

  @Override
  public final void start(LexerData data, JavaCCRequest request) throws ParseException {
    if (JavaCCErrors.hasError()) {
      throw new ParseException();
    }

    CppOtherFilesGenerator.generate("JavaCC.h", JavaCC.VERSION);

    CppOtherFilesGenerator.generate("Reader.h", JavaCC.VERSION);
    CppOtherFilesGenerator.generate("StringReader.h", JavaCC.VERSION);
    CppOtherFilesGenerator.generate("StringReader.cc", JavaCC.VERSION);

    CppOtherFilesGenerator.generate("Token.h", JavaCC.VERSION);
    CppOtherFilesGenerator.generate("Token.cc", JavaCC.VERSION);
    CppOtherFilesGenerator.generate("TokenManager.h", JavaCC.VERSION);
    CppOtherFilesGenerator.generate("TokenManagerError.h", JavaCC.VERSION);
    CppOtherFilesGenerator.generate("TokenManagerError.cc", JavaCC.VERSION);
    CppOtherFilesGenerator.generate("TokenManagerErrorHandler.h", JavaCC.VERSION);
    CppOtherFilesGenerator.generate("TokenManagerErrorHandler.cc", JavaCC.VERSION);

    CppOtherFilesGenerator.generate("ParseException.h", JavaCC.VERSION);
    CppOtherFilesGenerator.generate("ParseException.cc", JavaCC.VERSION);
    CppOtherFilesGenerator.generate("ParserErrorHandler.h", JavaCC.VERSION);
    CppOtherFilesGenerator.generate("ParserErrorHandler.cc", JavaCC.VERSION);

    List<RegularExpression> expressions = new ArrayList<RegularExpression>();
    for (TokenProduction tp : request.getTokenProductions()) {
      for (RegExprSpec res : tp.respecs) {
        expressions.add(res.rexp);
      }
    }

    DigestOptions options = DigestOptions.get();
    options.put("stateCount", data.getStateCount());
    options.put("tokenCount", expressions.size() + 1);
    options.put("orderedTokens", request.getOrderedsTokens());
    options.put("regularExpression", expressions);

    options.put("getStateName", new Function<Object, String>() {

      @Override
      public String apply(Object t) {
        return String.format("const int %s = %s;", data.getStateName((int) t), t);
      }
    });
    options.put("getOrderedToken", new Function<Object, String>() {

      @Override
      public String apply(Object t) {
        RegularExpression re = (RegularExpression) t;
        return String.format("const int %s = %s;", re.label, re.ordinal);
      }
    });
    options.put("getTokenImage", new Function<Object, String>() {

      @Override
      public String apply(Object t) {
        int i = (int) t;
        StringWriter builder = new StringWriter();
        try (PrintWriter writer = new PrintWriter(builder)) {
          writer.print("" + i + "[] = ");
          if (i == 0) {
            CppOtherFilesGenerator.printCharArray(writer, "<EOF>");
          } else if (expressions.get(i - 1) instanceof RStringLiteral) {
            CppOtherFilesGenerator.printCharArray(writer, ((RStringLiteral) expressions.get(i - 1)).image);
          } else if (!expressions.get(i - 1).label.equals("")) {
            CppOtherFilesGenerator.printCharArray(writer, "<" + expressions.get(i - 1).label + ">");
          } else {
            if (expressions.get(i - 1).tpContext.kind == TokenProduction.TOKEN) {
              JavaCCErrors.warning(expressions.get(i - 1),
                  "Consider giving this non-string token a label for better error reporting.");
            }
            CppOtherFilesGenerator.printCharArray(writer, "<token of kind " + expressions.get(i - 1).ordinal + ">");
          }
        }
        return builder.toString();
      }
    });
    options.put("getTokenLabel", new Function<Object, String>() {

      @Override
      public String apply(Object t) {
        int i = (int) t;
        StringWriter builder = new StringWriter();
        try (PrintWriter writer = new PrintWriter(builder)) {
          writer.print("" + i + "[] = ");
          if (i == 0) {
            CppOtherFilesGenerator.printCharArray(writer, "<EOF>");
          } else if (expressions.get(i - 1) instanceof RStringLiteral) {
            CppOtherFilesGenerator.printCharArray(writer, "<" + ((RStringLiteral) expressions.get(i - 1)).label + ">");
          } else if (!expressions.get(i - 1).label.equals("")) {
            CppOtherFilesGenerator.printCharArray(writer, "<" + expressions.get(i - 1).label + ">");
          } else {
            if (expressions.get(i - 1).tpContext.kind == TokenProduction.TOKEN) {
              JavaCCErrors.warning(expressions.get(i - 1),
                  "Consider giving this non-string token a label for better error reporting.");
            }
            CppOtherFilesGenerator.printCharArray(writer, "<token of kind " + expressions.get(i - 1).ordinal + ">");
          }
        }
        return builder.toString();
      }
    });

    File file = new File(Options.getOutputDirectory(), request.getParserName() + "Constants.h");
    try (DigestWriter writer = DigestWriter.create(file, JavaCC.VERSION, options)) {
      Template.of(String.format(TEMPLATE, "ParserConstants.h"), writer.options()).write(writer);
    } catch (IOException e) {
      JavaCCErrors.semantic_error("Could not open file " + request.getParserName() + "Constants.h for writing.");
      throw new Error();
    }

  }

  // Used by the CPP code generatror
  static void printCharArray(PrintWriter writer, String s) {
    writer.print("{");
    for (int i = 0; i < s.length(); i++) {
      writer.print("0x" + Integer.toHexString(s.charAt(i)) + ", ");
    }
    writer.print("0}");
  }

  private static void generate(String name, Version version) {
    File file = new File(Options.getOutputDirectory(), name);
    try (DigestWriter writer = DigestWriter.create(file, version, DigestOptions.get())) {
      Template.of(String.format(TEMPLATE, name), writer.options()).write(writer);
    } catch (IOException e) {
      System.err.println("Failed to create file: " + file + e);
      JavaCCErrors.semantic_error("Could not open file: " + file + " for writing.");
      throw new Error();
    }
  }
}
