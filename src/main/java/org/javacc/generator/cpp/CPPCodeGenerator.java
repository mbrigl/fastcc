// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

package org.javacc.generator.cpp;

import org.javacc.generator.JJTreeCodeGenerator;
import org.javacc.jjtree.ASTCompilationUnit;
import org.javacc.jjtree.JJTreeNode;
import org.javacc.jjtree.JJTreeOptions;
import org.javacc.jjtree.JJTreeParserConstants;
import org.javacc.jjtree.NodeScope;
import org.javacc.jjtree.Token;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;

public class CPPCodeGenerator extends JJTreeCodeGenerator {

  @Override
  public Object visit(ASTCompilationUnit node, Object data) {
    PrintWriter io = (PrintWriter) data;
    Token t = node.getFirstToken();

    while (true) {
      print(t, io, node);
      if (t == node.getLastToken()) {
        break;
      }
      if (t.kind == JJTreeParserConstants._PARSER_BEGIN) {
        // eat PARSER_BEGIN "(" <ID> ")"
        print(t.next, io, node);
        print(t.next.next, io, node);
        print(t = t.next.next.next, io, node);
      }

      t = t.next;
    }
    return null;
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

    CPPNodeFiles.addType(type);

    io.print(indent + nodeClass + " *" + ns.nodeVar + " = ");
    if (JJTreeOptions.getNodeFactory().equals("*")) {
      // Old-style multiple-implementations.
      io.println("(" + nodeClass + "*)" + nodeClass + "::jjtCreate(" + ns.node_descriptor.getNodeId() + ");");
    } else if (JJTreeOptions.getNodeFactory().length() > 0) {
      io.println("(" + nodeClass + "*)nodeFactory->jjtCreate(" + ns.node_descriptor.getNodeId() + ");");
    } else {
      io.println("new " + nodeClass + "(" + ns.node_descriptor.getNodeId() + ");");
    }

    if (ns.usesCloseNodeVar()) {
      io.println(indent + "bool " + ns.closedVar + " = true;");
    }
    io.println(indent + ns.node_descriptor.openNode(ns.nodeVar));
    if (JJTreeOptions.getNodeScopeHook()) {
      io.println(indent + "jjtreeOpenNodeScope(" + ns.nodeVar + ");");
    }

    if (JJTreeOptions.getTrackTokens()) {
      io.println(indent + ns.nodeVar + "->jjtSetFirstToken(getToken(1));");
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
      io.println(indent + "if (jjtree.nodeCreated()) {");
      io.println(indent + " jjtreeCloseNodeScope(" + ns.nodeVar + ");");
      io.println(indent + "}");
    }

    if (JJTreeOptions.getTrackTokens()) {
      io.println(indent + ns.nodeVar + "->jjtSetLastToken(getToken(0));");
    }
  }

  @Override
  protected final void insertCatchBlocks(NodeScope ns, PrintWriter io, Enumeration<String> thrown_names,
      String indent) {
    // if (thrown_names.hasMoreElements()) {
    io.println(indent + "} catch (...) {"); // " + ns.exceptionVar + ") {");

    if (ns.usesCloseNodeVar()) {
      io.println(indent + "  if (" + ns.closedVar + ") {");
      io.println(indent + "    jjtree.clearNodeScope(" + ns.nodeVar + ");");
      io.println(indent + "    " + ns.closedVar + " = false;");
      io.println(indent + "  } else {");
      io.println(indent + "    jjtree.popNode();");
      io.println(indent + "  }");
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

    io.println(indent + "} {");
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

    io.println(indent + "} {");
    if (ns.usesCloseNodeVar()) {
      io.println(indent + "  if (" + ns.closedVar + ") {");
      insertCloseNodeCode(ns, io, indent + "    ", true);
      io.println(indent + "  }");
    }
    io.println(indent + "}");
    JJTreeCodeGenerator.closeJJTreeComment(io);
  }


}
