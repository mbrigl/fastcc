// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)


package org.javacc.generator.cpp;

import org.fastcc.utils.DigestOptions;
import org.fastcc.utils.DigestWriter;
import org.fastcc.utils.Template;
import org.fastcc.utils.Version;
import org.javacc.JavaCC;
import org.javacc.JavaCCVersion;
import org.javacc.jjtree.JJTreeGlobals;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.Options;

import java.io.File;
import java.io.IOException;

/**
 * Generate CharStream, TokenManager and Exceptions.
 */
public class CPPFiles {

  private static void genFile(String name, Version version, String[] parameters) {
    File file = new File(Options.getOutputDirectory(), name);
    try (DigestWriter writer = DigestWriter.create(file, version, DigestOptions.get())) {
      Template.of("/templates/cpp/" + name + ".template", writer.options()).write(writer);
    } catch (IOException e) {
      System.err.println("Failed to create file: " + file + e);
      JavaCCErrors.semantic_error("Could not open file: " + file + " for writing.");
      throw new Error();
    }
  }

  static void gen_CharStream() {
    String[] parameters = { JavaCC.JJPARSER_STATIC };
    CPPFiles.genFile("CharStream.h", JavaCCVersion.VERSION, parameters);
    CPPFiles.genFile("DefaultCharStream.h", JavaCCVersion.VERSION, parameters);
    CPPFiles.genFile("DefaultCharStream.cc", JavaCCVersion.VERSION, parameters);
  }

  static void gen_ParseException() {
    String[] parameters = { JavaCC.JJPARSER_STATIC };
    CPPFiles.genFile("ParseException.h", JavaCCVersion.VERSION, parameters);
    CPPFiles.genFile("ParseException.cc", JavaCCVersion.VERSION, parameters);
    CPPFiles.genFile("ParserErrorHandler.h", JavaCCVersion.VERSION, parameters);
    CPPFiles.genFile("DefaultParserErrorHandler.h", JavaCCVersion.VERSION, parameters);
    CPPFiles.genFile("DefaultParserErrorHandler.cc", JavaCCVersion.VERSION, parameters);
  }

  static void gen_TokenMgrError() {
    String[] parameters = { JavaCC.JJPARSER_STATIC };
    CPPFiles.genFile("TokenManagerError.h", JavaCCVersion.VERSION, parameters);
    CPPFiles.genFile("TokenManagerError.cc", JavaCCVersion.VERSION, parameters);
    CPPFiles.genFile("TokenManagerErrorHandler.h", JavaCCVersion.VERSION, parameters);
  }

  static void gen_Token() {
    String[] parameters = { JavaCC.JJPARSER_STATIC };
    CPPFiles.genFile("Token.h", JavaCCVersion.VERSION, parameters);
    CPPFiles.genFile("Token.cc", JavaCCVersion.VERSION, parameters);
  }

  static void gen_TokenManager() {
    String[] parameters = { JavaCC.JJPARSER_STATIC };
    CPPFiles.genFile("TokenManager.h", JavaCCVersion.VERSION, parameters);
  }

  static void gen_JavaCCDefs() {
    String[] parameters = { JavaCC.JJPARSER_STATIC };
    CPPFiles.genFile("JavaCC.h", JavaCCVersion.VERSION, parameters);
  }

  static void gen_ErrorHandler() {
    String[] parameters = { JavaCC.JJPARSER_STATIC };
    CPPFiles.genFile("DefaultTokenManagerErrorHandler.h", JavaCCVersion.VERSION, parameters);
    CPPFiles.genFile("DefaultTokenManagerErrorHandler.cc", JavaCCVersion.VERSION, parameters);
  }


  private static void generateTreeState() throws IOException {
    DigestOptions options = DigestOptions.get();
    options.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
    String filePrefix =
        new File(Options.getOutputDirectory(), "JJT" + JJTreeGlobals.parserName + "State").getAbsolutePath();


    File file = new File(filePrefix + ".h");
    try (DigestWriter writer = DigestWriter.create(file, JavaCCVersion.VERSION, options)) {
      CPPNodeFiles.generateFile(writer, "/templates/cpp/JJTTreeState.h.template", writer.options());
    }

    file = new File(filePrefix + ".cc");
    try (DigestWriter writer = DigestWriter.create(file, JavaCCVersion.VERSION, options)) {
      CPPNodeFiles.generateFile(writer, "/templates/cpp/JJTTreeState.cc.template", writer.options());
    }
  }

  public static void generateJJTree() throws IOException {
    CPPNodeFiles.generateTreeClasses();
    CPPNodeFiles.generateTreeConstants();
    CPPNodeFiles.generateVisitors();
    CPPFiles.generateTreeState();
  }
}
