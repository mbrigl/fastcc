// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

package org.javacc.generator.java;

import org.fastcc.utils.DigestOptions;
import org.fastcc.utils.DigestWriter;
import org.javacc.JavaCCVersion;
import org.javacc.generator.JJTreeCodeGenerator;
import org.javacc.jjtree.ASTCompilationUnit;
import org.javacc.jjtree.JJTreeGlobals;
import org.javacc.jjtree.JJTreeNode;
import org.javacc.jjtree.JJTreeOptions;
import org.javacc.jjtree.NodeScope;
import org.javacc.jjtree.Token;
import org.javacc.parser.Options;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class JavaCodeGenerator extends JJTreeCodeGenerator {

  @Override
  public Object visit(ASTCompilationUnit node, Object data) {
    PrintWriter io = (PrintWriter) data;
    Token t = node.getFirstToken();

    while (true) {
      if (t == JJTreeGlobals.parserImports) {
        if (!JJTreeGlobals.nodePackageName.equals("")
            && !JJTreeGlobals.nodePackageName.equals(JJTreeGlobals.packageName)) {
          io.println("");
          io.println("import " + JJTreeGlobals.nodePackageName + ".*;");
        }
      }

      if (t == JJTreeGlobals.parserImplements) {
        if (t.image.equals("implements")) {
          print(t, io, node);
          JJTreeCodeGenerator.openJJTreeComment(io, null);
          io.print(" " + NodeFiles.nodeConstants() + ", ");
          JJTreeCodeGenerator.closeJJTreeComment(io);
        } else {
          // t is pointing at the opening brace of the class body.
          JJTreeCodeGenerator.openJJTreeComment(io, null);
          io.print("implements " + NodeFiles.nodeConstants());
          JJTreeCodeGenerator.closeJJTreeComment(io);
          print(t, io, node);
        }
      } else {
        print(t, io, node);
      }

      if (t == JJTreeGlobals.parserClassBodyStart) {
        JJTreeCodeGenerator.openJJTreeComment(io, null);
        String s = Options.getStatic() ? "static " : "";
        io.println();
        io.println("  protected " + s + JJTreeState.nameState() + " jjtree = new " + JJTreeState.nameState() + "();");
        io.println();
        JJTreeCodeGenerator.closeJJTreeComment(io);
      }

      if (t == node.getLastToken()) {
        return null;
      }
      t = t.next;
    }
  }

  @Override
  protected final void insertOpenNodeCode(NodeScope ns, PrintWriter io, String indent) {
    String type = ns.node_descriptor.getNodeType();
    final String nodeClass;
    if ((JJTreeOptions.getNodeClass().length() > 0) && !JJTreeOptions.getMulti()) {
      nodeClass = JJTreeOptions.getNodeClass();
    } else {
      nodeClass = type;
    }

    // Ensure that there is a template definition file for the node type.
    JavaCodeGenerator.ensure(io, type);

    io.print(indent + nodeClass + " " + ns.nodeVar + " = ");
    if (JJTreeOptions.getNodeFactory().equals("*")) {
      // Old-style multiple-implementations.
      io.println("(" + nodeClass + ")" + nodeClass + ".jjtCreate(" + ns.node_descriptor.getNodeId() + ");");
    } else if (JJTreeOptions.getNodeFactory().length() > 0) {
      io.println("(" + nodeClass + ")" + JJTreeOptions.getNodeFactory() + ".jjtCreate(" + ns.node_descriptor.getNodeId()
          + ");");
    } else {
      io.println("new " + nodeClass + "(" + ns.node_descriptor.getNodeId() + ");");
    }

    if (ns.usesCloseNodeVar()) {
      io.println(indent + "boolean " + ns.closedVar + " = true;");
    }
    io.println(indent + ns.node_descriptor.openNode(ns.nodeVar));
    if (JJTreeOptions.getNodeScopeHook()) {
      io.println(indent + "jjtreeOpenNodeScope(" + ns.nodeVar + ");");
    }

    if (JJTreeOptions.getTrackTokens()) {
      io.println(indent + ns.nodeVar + ".jjtSetFirstToken(getToken(1));");
    }
  }

  @Override
  protected final void insertCloseNodeCode(NodeScope ns, PrintWriter io, String indent, boolean isFinal) {
    String closeNode = ns.node_descriptor.closeNode(ns.nodeVar);
    io.println(indent + closeNode);
    if (ns.usesCloseNodeVar() && !isFinal) {
      io.println(indent + ns.closedVar + " = false;");
    }
    if (JJTreeOptions.getNodeScopeHook()) {
      closeNode.lastIndexOf(",");
      io.println(indent + "if (jjtree.nodeCreated()) {");
      io.println(indent + " jjtreeCloseNodeScope(" + ns.nodeVar + ");");
      io.println(indent + "}");
    }

    if (JJTreeOptions.getTrackTokens()) {
      io.println(indent + ns.nodeVar + ".jjtSetLastToken(getToken(0));");
    }
  }

  @Override
  protected final void insertCatchBlocks(NodeScope ns, PrintWriter io, Enumeration<String> thrown_names,
      String indent) {
    String thrown;
    if (thrown_names.hasMoreElements()) {
      io.println(indent + "} catch (Throwable " + ns.exceptionVar + ") {");

      if (ns.usesCloseNodeVar()) {
        io.println(indent + "  if (" + ns.closedVar + ") {");
        io.println(indent + "    jjtree.clearNodeScope(" + ns.nodeVar + ");");
        io.println(indent + "    " + ns.closedVar + " = false;");
        io.println(indent + "  } else {");
        io.println(indent + "    jjtree.popNode();");
        io.println(indent + "  }");
      }

      while (thrown_names.hasMoreElements()) {
        thrown = thrown_names.nextElement();
        io.println(indent + "  if (" + ns.exceptionVar + " instanceof " + thrown + ") {");
        io.println(indent + "    throw (" + thrown + ")" + ns.exceptionVar + ";");
        io.println(indent + "  }");
      }
      /*
       * This is either an Error or an undeclared Exception. If it's an Error then the cast is good,
       * otherwise we want to force the user to declare it by crashing on the bad cast.
       */
      io.println(indent + "  throw (Error)" + ns.exceptionVar + ";");
    }
  }

  @Override
  protected final void tryTokenSequence(NodeScope ns, PrintWriter io, String indent, Token first, Token last) {
    io.println(indent + "try {");
    JJTreeCodeGenerator.closeJJTreeComment(io);

    /*
     * Print out all the tokens, converting all references to `jjtThis' into the current node
     * variable.
     */
    for (Token t = first; t != last.next; t = t.next) {
      print(t, io, "jjtThis", ns.nodeVar);
    }

    JJTreeCodeGenerator.openJJTreeComment(io, null);
    io.println();

    Enumeration<String> thrown_names = ns.production.throws_list.elements();
    insertCatchBlocks(ns, io, thrown_names, indent);

    io.println(indent + "} finally {");
    if (ns.usesCloseNodeVar()) {
      io.println(indent + "  if (" + ns.closedVar + ") {");
      insertCloseNodeCode(ns, io, indent + "    ", true);
      io.println(indent + "  }");
    }
    io.println(indent + "}");
    JJTreeCodeGenerator.closeJJTreeComment(io);
  }

  @Override
  protected final void tryExpansionUnit(NodeScope ns, PrintWriter io, String indent, JJTreeNode expansion_unit) {
    io.println(indent + "try {");
    JJTreeCodeGenerator.closeJJTreeComment(io);

    expansion_unit.jjtAccept(this, io);

    JJTreeCodeGenerator.openJJTreeComment(io, null);
    io.println();

    Hashtable<String, String> thrown_set = new Hashtable<>();
    JJTreeCodeGenerator.findThrown(ns, thrown_set, expansion_unit);
    Enumeration<String> thrown_names = thrown_set.elements();
    insertCatchBlocks(ns, io, thrown_names, indent);

    io.println(indent + "} finally {");
    if (ns.usesCloseNodeVar()) {
      io.println(indent + "  if (" + ns.closedVar + ") {");
      insertCloseNodeCode(ns, io, indent + "    ", true);
      io.println(indent + "  }");
    }
    io.println(indent + "}");
    JJTreeCodeGenerator.closeJJTreeComment(io);
  }


  private static Set<String> nodesGenerated = new HashSet<>();

  private static void ensure(PrintWriter io, String nodeType) {
    File file = new File(Options.getOutputDirectory(), nodeType + ".java");

    if (nodeType.equals("Tree")) {} else if (nodeType.equals("Node")) {
      JavaCodeGenerator.ensure(io, "Tree");
    } else {
      JavaCodeGenerator.ensure(io, "Node");
    }

    if (!(nodeType.equals("Node") || JJTreeOptions.getBuildNodeFiles())) {
      return;
    }

    if (file.exists() && JavaCodeGenerator.nodesGenerated.contains(file.getName())) {
      return;
    }

    DigestOptions options = DigestOptions.get();
    try (DigestWriter writer = DigestWriter.create(file, JavaCCVersion.VERSION, options)) {
      JavaCodeGenerator.nodesGenerated.add(file.getName());

      if (nodeType.equals("Tree")) {
        NodeFiles.generateTree_java(writer, options);
      } else if (nodeType.equals("Node")) {
        NodeFiles.generateNode_java(writer, options);
      } else {
        NodeFiles.generateMULTINode_java(writer, nodeType, options);
      }
    } catch (IOException e) {
      throw new Error(e.toString());
    }
  }
}
