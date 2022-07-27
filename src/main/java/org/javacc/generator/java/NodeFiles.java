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
import java.util.List;

final class NodeFiles {

  private NodeFiles() {}

  static void generatePrologue(PrintWriter ostr) {
    if (!JJTreeGlobals.nodePackageName.equals("")) {
      ostr.println("package " + JJTreeGlobals.nodePackageName + ";");
      ostr.println();
      if (!JJTreeGlobals.nodePackageName.equals(JJTreeGlobals.packageName)) {
        ostr.println("import " + JJTreeGlobals.packageName + ".*;");
        ostr.println();
      }

    }
  }


  static String nodeConstants() {
    return JJTreeGlobals.parserName + "TreeConstants";
  }

  static void generateTreeConstants_java() {
    String name = NodeFiles.nodeConstants();
    File file = new File(Options.getOutputDirectory(), name + ".java");

    try (PrintWriter ostr = DigestWriter.create(file, JavaCCVersion.VERSION, DigestOptions.get())) {
      List<String> nodeIds = ASTNodeDescriptor.getNodeIds();
      List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

      NodeFiles.generatePrologue(ostr);
      ostr.println("public interface " + name);
      ostr.println("{");

      for (int i = 0; i < nodeIds.size(); ++i) {
        String n = nodeIds.get(i);
        ostr.println("  public final int " + n + " = " + i + ";");
      }

      ostr.println();
      ostr.println();

      ostr.println("  public static String[] jjtNodeName = {");
      for (String n : nodeNames) {
        ostr.println("    \"" + n + "\",");
      }
      ostr.println("  };");

      ostr.println("}");
    } catch (IOException e) {
      throw new Error(e.toString());
    }
  }


  private static String visitorClass() {
    return JJTreeGlobals.parserName + "Visitor";
  }

  static void generateVisitor_java() {
    if (!JJTreeOptions.getVisitor()) {
      return;
    }

    String name = NodeFiles.visitorClass();
    File file = new File(Options.getOutputDirectory(), name + ".java");

    try (PrintWriter ostr = DigestWriter.create(file, JavaCCVersion.VERSION, DigestOptions.get())) {
      List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

      NodeFiles.generatePrologue(ostr);
      ostr.println("public interface " + name);
      ostr.println("{");

      String ve = NodeFiles.mergeVisitorException();

      String argumentType = "Object";
      if (!JJTreeOptions.getVisitorDataType().equals("")) {
        argumentType = JJTreeOptions.getVisitorDataType();
      }

      ostr.println("  public " + JJTreeOptions.getVisitorReturnType() + " visit(Node node, " + argumentType + " data)"
          + ve + ";");
      if (JJTreeOptions.getMulti()) {
        for (String n : nodeNames) {
          if (n.equals("void")) {
            continue;
          }
          String nodeType = JJTreeOptions.getNodePrefix() + n;
          ostr.println("  public " + JJTreeOptions.getVisitorReturnType() + " " + NodeFiles.getVisitMethodName(nodeType)
              + "(" + nodeType + " node, " + argumentType + " data)" + ve + ";");
        }
      }
      ostr.println("}");
    } catch (IOException e) {
      throw new Error(e.toString());
    }
  }

  private static String defaultVisitorClass() {
    return JJTreeGlobals.parserName + "DefaultVisitor";
  }

  private static String getVisitMethodName(String className) {
    return "visit";
  }

  static void generateDefaultVisitor_java() {
    if (!JJTreeOptions.getVisitor()) {
      return;
    }

    String className = NodeFiles.defaultVisitorClass();
    File file = new File(Options.getOutputDirectory(), className + ".java");

    try (PrintWriter ostr = DigestWriter.create(file, JavaCCVersion.VERSION, DigestOptions.get())) {
      List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

      NodeFiles.generatePrologue(ostr);
      ostr.println("public class " + className + " implements " + NodeFiles.visitorClass() + "{");

      final String ve = NodeFiles.mergeVisitorException();

      String argumentType = "Object";
      if (!JJTreeOptions.getVisitorDataType().equals("")) {
        argumentType = JJTreeOptions.getVisitorDataType().trim();
      }

      final String returnType = JJTreeOptions.getVisitorReturnType().trim();
      final boolean isVoidReturnType = "void".equals(returnType);

      ostr.println("  public " + returnType + " defaultVisit(Node node, " + argumentType + " data)" + ve + "{");
      ostr.println("    node.childrenAccept(this, data);");
      ostr.print("    return");
      if (!isVoidReturnType) {
        if (returnType.equals(argumentType)) {
          ostr.print(" data");
        } else if ("boolean".equals(returnType)) {
          ostr.print(" false");
        } else if ("int".equals(returnType)) {
          ostr.print(" 0");
        } else if ("long".equals(returnType)) {
          ostr.print(" 0L");
        } else if ("double".equals(returnType)) {
          ostr.print(" 0.0d");
        } else if ("float".equals(returnType)) {
          ostr.print(" 0.0f");
        } else if ("short".equals(returnType)) {
          ostr.print(" 0");
        } else if ("byte".equals(returnType)) {
          ostr.print(" 0");
        } else if ("char".equals(returnType)) {
          ostr.print(" '\u0000'");
        } else {
          ostr.print(" null");
        }
      }
      ostr.println(";");
      ostr.println("  }");

      ostr.println("  public " + returnType + " visit(Node node, " + argumentType + " data)" + ve + "{");
      ostr.println("    " + (isVoidReturnType ? "" : "return ") + "defaultVisit(node, data);");
      ostr.println("  }");

      if (JJTreeOptions.getMulti()) {
        for (String n : nodeNames) {
          if (n.equals("void")) {
            continue;
          }
          String nodeType = JJTreeOptions.getNodePrefix() + n;
          ostr.println("  public " + returnType + " " + NodeFiles.getVisitMethodName(nodeType) + "(" + nodeType
              + " node, " + argumentType + " data)" + ve + "{");
          ostr.println("    " + (isVoidReturnType ? "" : "return ") + "defaultVisit(node, data);");
          ostr.println("  }");
        }
      }

      ostr.println("}");
    } catch (final IOException e) {
      throw new Error(e.toString());
    }
  }

  private static String mergeVisitorException() {
    String ve = JJTreeOptions.getVisitorException();
    if (!"".equals(ve)) {
      ve = " throws " + ve;
    }
    return ve;
  }


  static void generateTree_java(PrintWriter ostr, DigestOptions options) throws IOException {
    NodeFiles.generatePrologue(ostr);
    options.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);

    Template.of("/templates/Tree.template", options).write(ostr);
  }


  static void generateNode_java(PrintWriter ostr, DigestOptions options) throws IOException {
    NodeFiles.generatePrologue(ostr);

    options.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);

    Template.of("/templates/Node.template", options).write(ostr);
  }


  static void generateMULTINode_java(PrintWriter ostr, String nodeType, DigestOptions options) throws IOException {
    NodeFiles.generatePrologue(ostr);

    options.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
    options.put(JavaCC.JJTREE_NODE_TYPE, nodeType);
    options.put(JavaCC.JJTREE_VISITOR_RETURN_VOID,
        Boolean.valueOf(JJTreeOptions.getVisitorReturnType().equals("void")));

    Template.of("/templates/MultiNode.template", options).write(ostr);
  }
}
