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

import org.fastcc.utils.DigestOptions;
import org.fastcc.utils.DigestWriter;
import org.fastcc.utils.Template;
import org.javacc.JavaCC;
import org.javacc.JavaCCVersion;
import org.javacc.jjtree.ASTNodeDescriptor;
import org.javacc.jjtree.JJTreeGlobals;
import org.javacc.jjtree.JJTreeOptions;
import org.javacc.parser.Options;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CPPNodeFiles {

  private CPPNodeFiles() {}

  private static List<String> headersForJJTreeH = new ArrayList<>();

  private static Set<String>  nodesToGenerate   = new HashSet<>();

  static void addType(String type) {
    if (!type.equals("Node")) {
      CPPNodeFiles.nodesToGenerate.add(type);
    }
  }

  private static String nodeIncludeFile() {
    return new File(Options.getOutputDirectory(), "Node.h").getAbsolutePath();
  }

  private static String nodeImplFile() {
    return new File(Options.getOutputDirectory(), "Node.cc").getAbsolutePath();
  }

  private static String jjtreeIncludeFile() {
    return new File(Options.getOutputDirectory(), JJTreeGlobals.parserName + "Tree.h").getAbsolutePath();
  }


  private static String jjtreeIncludeFile(String s) {
    return new File(Options.getOutputDirectory(), s + ".h").getAbsolutePath();
  }

  private static String jjtreeImplFile(String s) {
    return new File(Options.getOutputDirectory(), s + ".cc").getAbsolutePath();
  }


  private static String visitorIncludeFile() {
    String name = CPPNodeFiles.visitorClass();
    return new File(Options.getOutputDirectory(), name + ".h").getAbsolutePath();
  }

  static void generateTreeClasses() {
    CPPNodeFiles.generateNodeHeader();
    CPPNodeFiles.generateNodeImpl();
    CPPNodeFiles.generateMultiTreeImpl();
    CPPNodeFiles.generateOneTreeInterface();
    // generateOneTreeImpl();
    CPPNodeFiles.generateTreeInterface();
  }

  private static void generateNodeHeader() {
    DigestOptions optionMap = DigestOptions.get();
    optionMap.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_TYPE, CPPNodeFiles.getVisitorReturnType());
    optionMap.put(JavaCC.JJTREE_VISITOR_DATA_TYPE, CPPNodeFiles.getVisitorArgumentType());
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_VOID,
        Boolean.valueOf(CPPNodeFiles.getVisitorReturnType().equals("void")));

    File file = new File(CPPNodeFiles.nodeIncludeFile());
    try (DigestWriter writer = DigestWriter.create(file, JavaCCVersion.VERSION, optionMap)) {
      CPPNodeFiles.generateFile(writer, "/templates/cpp/Node.h.template", writer.options());
    } catch (IOException e) {
      throw new Error(e.toString());
    }
  }

  private static void generateNodeImpl() {
    DigestOptions optionMap = DigestOptions.get();
    optionMap.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_TYPE, CPPNodeFiles.getVisitorReturnType());
    optionMap.put(JavaCC.JJTREE_VISITOR_DATA_TYPE, CPPNodeFiles.getVisitorArgumentType());
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_VOID,
        Boolean.valueOf(CPPNodeFiles.getVisitorReturnType().equals("void")));

    File file = new File(CPPNodeFiles.nodeImplFile());
    try (DigestWriter writer = DigestWriter.create(file, JavaCCVersion.VERSION, optionMap)) {
      CPPNodeFiles.generateFile(writer, "/templates/cpp/Node.cc.template", writer.options());
    } catch (IOException e) {
      throw new Error(e.toString());
    }
  }

  private static void generateTreeInterface() {
    String node = "Tree";
    DigestOptions optionMap = DigestOptions.get();
    optionMap.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_TYPE, CPPNodeFiles.getVisitorReturnType());
    optionMap.put(JavaCC.JJTREE_VISITOR_DATA_TYPE, CPPNodeFiles.getVisitorArgumentType());
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_VOID,
        Boolean.valueOf(CPPNodeFiles.getVisitorReturnType().equals("void")));
    optionMap.put(JavaCC.JJTREE_NODE_TYPE, node);

    File file = new File(CPPNodeFiles.jjtreeIncludeFile(node));
    try (DigestWriter writer = DigestWriter.create(file, JavaCCVersion.VERSION, optionMap)) {
      CPPNodeFiles.generateFile(writer, "/templates/cpp/Tree.h.template", writer.options());
    } catch (IOException e) {
      throw new Error(e.toString());
    }
  }

  private static void generateMultiTreeImpl() {
    for (String node : CPPNodeFiles.nodesToGenerate) {
      File file = new File(CPPNodeFiles.jjtreeImplFile(node));
      DigestOptions optionMap = DigestOptions.get();
      optionMap.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
      optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_TYPE, CPPNodeFiles.getVisitorReturnType());
      optionMap.put(JavaCC.JJTREE_VISITOR_DATA_TYPE, CPPNodeFiles.getVisitorArgumentType());
      optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_VOID,
          Boolean.valueOf(CPPNodeFiles.getVisitorReturnType().equals("void")));
      optionMap.put(JavaCC.JJTREE_NODE_TYPE, node);

      try (DigestWriter writer = DigestWriter.create(file, JavaCCVersion.VERSION, optionMap)) {
        CPPNodeFiles.generateFile(writer, "/templates/cpp/MultiNode.cc.template", writer.options());
      } catch (IOException e) {
        throw new Error(e.toString());
      }
    }
  }


  private static void generateOneTreeInterface() {
    DigestOptions optionMap = DigestOptions.get();
    optionMap.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_TYPE, CPPNodeFiles.getVisitorReturnType());
    optionMap.put(JavaCC.JJTREE_VISITOR_DATA_TYPE, CPPNodeFiles.getVisitorArgumentType());
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_VOID,
        Boolean.valueOf(CPPNodeFiles.getVisitorReturnType().equals("void")));

    File file = new File(CPPNodeFiles.jjtreeIncludeFile());
    try (DigestWriter writer = DigestWriter.create(file, JavaCCVersion.VERSION, optionMap)) {
      // PrintWriter ostr = outputFile.getPrintWriter();
      file.getName().replace('.', '_').toUpperCase();
      writer.println("#ifndef JAVACC_ONE_TREE_H");
      writer.println("#define JAVACC_ONE_TREE_H");
      writer.println();
      writer.println("#include \"Node.h\"");
      for (String s : CPPNodeFiles.nodesToGenerate) {
        writer.println("#include \"" + s + ".h\"");
      }
      writer.println("#endif");
    } catch (IOException e) {
      throw new Error(e.toString());
    }
  }


  private static String nodeConstants() {
    return JJTreeGlobals.parserName + "TreeConstants";
  }

  static void generateTreeConstants() {
    String name = CPPNodeFiles.nodeConstants();
    File file = new File(Options.getOutputDirectory(), name + ".h");
    CPPNodeFiles.headersForJJTreeH.add(file.getName());

    try (DigestWriter ostr = DigestWriter.create(file, JavaCCVersion.VERSION, DigestOptions.get())) {
      List<String> nodeIds = ASTNodeDescriptor.getNodeIds();
      List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

      ostr.println("#ifndef JAVACC_" + file.getName().replace('.', '_').toUpperCase());
      ostr.println("#define JAVACC_" + file.getName().replace('.', '_').toUpperCase());

      ostr.println("\n#include \"JavaCC.h\"");
      boolean hasNamespace = ((String) ostr.options().get(JavaCC.JJPARSER_CPP_NAMESPACE)).length() > 0;
      if (hasNamespace) {
        ostr.println("namespace " + ostr.options().get(JavaCC.JJPARSER_CPP_NAMESPACE) + " {");
      }
      ostr.println("enum {");
      for (int i = 0; i < nodeIds.size(); ++i) {
        String n = nodeIds.get(i);
        ostr.println("  " + n + " = " + i + ",");
      }

      ostr.println("};");
      ostr.println();

      for (int i = 0; i < nodeNames.size(); ++i) {
        ostr.println("  static JJChar jjtNodeName_arr_" + i + "[] = ");
        String n = nodeNames.get(i);
        // ostr.println(" (JJChar*)\"" + n + "\",");
        OtherFilesGenCPP.printCharArray(ostr, n);
        ostr.println(";");
      }
      ostr.println("  static JJString jjtNodeName[] = {");
      for (int i = 0; i < nodeNames.size(); i++) {
        ostr.println("jjtNodeName_arr_" + i + ", ");
      }
      ostr.println("  };");

      if (hasNamespace) {
        ostr.println("}");
      }


      ostr.println("#endif");
    } catch (IOException e) {
      throw new Error(e.toString());
    }
  }


  private static String visitorClass() {
    return JJTreeGlobals.parserName + "Visitor";
  }

  private static String getVisitMethodName(String className) {
    return "visit";
  }

  private static String getVisitorArgumentType() {
    String ret = Options.stringValue(JavaCC.JJTREE_VISITOR_DATA_TYPE);
    return (ret == null) || ret.equals("") || ret.equals("Object") ? "void *" : ret;
  }

  private static String getVisitorReturnType() {
    String ret = Options.stringValue(JavaCC.JJTREE_VISITOR_RETURN_TYPE);
    return (ret == null) || ret.equals("") || ret.equals("Object") ? "void " : ret;
  }

  static void generateVisitors() {
    if (!JJTreeOptions.getVisitor()) {
      return;
    }

    File file = new File(CPPNodeFiles.visitorIncludeFile());
    try (DigestWriter ostr = DigestWriter.create(file, JavaCCVersion.VERSION, DigestOptions.get())) {
      CPPNodeFiles.visitorClass();
      ostr.println("#ifndef " + file.getName().replace('.', '_').toUpperCase());
      ostr.println("#define " + file.getName().replace('.', '_').toUpperCase());
      ostr.println("\n#include \"JavaCC.h\"");
      ostr.println("#include \"" + JJTreeGlobals.parserName + "Tree.h" + "\"");

      boolean hasNamespace = ((String) ostr.options().get(JavaCC.JJPARSER_CPP_NAMESPACE)).length() > 0;
      if (hasNamespace) {
        ostr.println("namespace " + ostr.options().get(JavaCC.JJPARSER_CPP_NAMESPACE) + " {");
      }

      CPPNodeFiles.generateVisitorInterface(ostr);
      CPPNodeFiles.generateDefaultVisitor(ostr);

      if (hasNamespace) {
        ostr.println("}");
      }

      ostr.println("#endif");
    } catch (IOException ioe) {
      throw new Error(ioe.toString());
    }
  }

  private static void generateVisitorInterface(PrintWriter ostr) {
    String name = CPPNodeFiles.visitorClass();
    List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

    ostr.println("class " + name);
    ostr.println("{");

    String argumentType = CPPNodeFiles.getVisitorArgumentType();
    String returnType = CPPNodeFiles.getVisitorReturnType();
    if (!JJTreeOptions.getVisitorDataType().equals("")) {
      argumentType = JJTreeOptions.getVisitorDataType();
    }
    ostr.println("  public:");

    ostr.println("  virtual " + returnType + " visit(const Node *node, " + argumentType + " data) = 0;");
    if (JJTreeOptions.getMulti()) {
      for (String n : nodeNames) {
        if (n.equals("void")) {
          continue;
        }
        String nodeType = JJTreeOptions.getNodePrefix() + n;
        ostr.println("  virtual " + returnType + " " + CPPNodeFiles.getVisitMethodName(nodeType) + "(const " + nodeType
            + " *node, " + argumentType + " data) = 0;");
      }
    }

    ostr.println("  virtual ~" + name + "() { }");
    ostr.println("};");
  }

  private static String defaultVisitorClass() {
    return JJTreeGlobals.parserName + "DefaultVisitor";
  }

  private static void generateDefaultVisitor(PrintWriter ostr) {
    String className = CPPNodeFiles.defaultVisitorClass();
    List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

    ostr.println("class " + className + " : public " + CPPNodeFiles.visitorClass() + " {");

    String argumentType = CPPNodeFiles.getVisitorArgumentType();
    String ret = CPPNodeFiles.getVisitorReturnType();

    ostr.println("public:");
    ostr.println("  virtual " + ret + " defaultVisit(const Node *node, " + argumentType + " data) = 0;");
    // ostr.println(" node->childrenAccept(this, data);");
    // ostr.println(" return" + (ret.trim().equals("void") ? "" : " data") + ";");
    // ostr.println(" }");

    ostr.println("  virtual " + ret + " visit(const Node *node, " + argumentType + " data) {");
    ostr.println("    " + (ret.trim().equals("void") ? "" : "return ") + "defaultVisit(node, data);");
    ostr.println("}");

    if (JJTreeOptions.getMulti()) {
      for (String n : nodeNames) {
        if (n.equals("void")) {
          continue;
        }
        String nodeType = JJTreeOptions.getNodePrefix() + n;
        ostr.println("  virtual " + ret + " " + CPPNodeFiles.getVisitMethodName(nodeType) + "(const " + nodeType
            + " *node, " + argumentType + " data) {");
        ostr.println("    " + (ret.trim().equals("void") ? "" : "return ") + "defaultVisit(node, data);");
        ostr.println("  }");
      }
    }
    ostr.println("  ~" + className + "() { }");
    ostr.println("};");
  }

  static void generateFile(PrintWriter writer, String template, Map<String, Object> options) throws IOException {
    Template.of(template, options).write(writer);
  }
}
