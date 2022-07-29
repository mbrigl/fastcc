// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

package org.javacc.generator.java;

import org.fastcc.utils.DigestOptions;
import org.fastcc.utils.DigestWriter;
import org.fastcc.utils.Template;
import org.javacc.JavaCC;
import org.javacc.generator.JJTreeCodeGenerator;
import org.javacc.jjtree.ASTCompilationUnit;
import org.javacc.jjtree.ASTNodeDescriptor;
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
import java.util.List;
import java.util.Set;

public class JavaTreeGenerator extends JJTreeCodeGenerator {

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
          io.print(" " + JavaTreeGenerator.nodeConstants() + ", ");
          JJTreeCodeGenerator.closeJJTreeComment(io);
        } else {
          // t is pointing at the opening brace of the class body.
          JJTreeCodeGenerator.openJJTreeComment(io, null);
          io.print("implements " + JavaTreeGenerator.nodeConstants());
          JJTreeCodeGenerator.closeJJTreeComment(io);
          print(t, io, node);
        }
      } else {
        print(t, io, node);
      }

      if (t == JJTreeGlobals.parserClassBodyStart) {
        JJTreeCodeGenerator.openJJTreeComment(io, null);
        io.println();
        io.println("  protected JJT" + JJTreeGlobals.parserName + "State jjtree = new JJT" + JJTreeGlobals.parserName
            + "State();");
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
    JavaTreeGenerator.ensure(io, type);

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
      JavaTreeGenerator.ensure(io, "Tree");
    } else {
      JavaTreeGenerator.ensure(io, "Node");
    }

    if (!(nodeType.equals("Node") || JJTreeOptions.getBuildNodeFiles())) {
      return;
    }

    if (file.exists() && JavaTreeGenerator.nodesGenerated.contains(file.getName())) {
      return;
    }

    DigestOptions options = DigestOptions.get();
    try (DigestWriter writer = DigestWriter.create(file, JavaCC.VERSION, options)) {
      JavaTreeGenerator.nodesGenerated.add(file.getName());

      if (nodeType.equals("Tree")) {
        JavaTreeGenerator.generateTree_java(writer, options);
      } else if (nodeType.equals("Node")) {
        JavaTreeGenerator.generateNode_java(writer, options);
      } else {
        JavaTreeGenerator.generateMULTINode_java(writer, nodeType, options);
      }
    } catch (IOException e) {
      throw new Error(e.toString());
    }
  }

  @Override
  public final void generateJJTree() {
    JavaTreeGenerator.generateTreeConstants_java();
    JavaTreeGenerator.generateVisitor_java();
    JavaTreeGenerator.generateDefaultVisitor_java();


    File file = new File(Options.getOutputDirectory(), "JJT" + JJTreeGlobals.parserName + "State.java");
    try (DigestWriter ostr = DigestWriter.create(file, JavaCC.VERSION, DigestOptions.get())) {
      JavaTreeGenerator.generatePrologue(ostr);
      JavaTreeGenerator.insertState(ostr);
    } catch (IOException e) {
      throw new Error(e.toString());
    }
  }


  private static void insertState(PrintWriter ostr) {
    ostr.println("public class JJT" + JJTreeGlobals.parserName + "State {");

    ostr.println("  private java.util.List<Node> nodes;");
    ostr.println("  private java.util.List<Integer> marks;");

    ostr.println("");
    ostr.println("  private int sp;        // number of nodes on stack");
    ostr.println("  private int mk;        // current mark");
    ostr.println("  private boolean node_created;");
    ostr.println("");
    ostr.println("  public JJT" + JJTreeGlobals.parserName + "State() {");

    ostr.println("    nodes = new java.util.ArrayList<Node>();");
    ostr.println("    marks = new java.util.ArrayList<Integer>();");

    ostr.println("    sp = 0;");
    ostr.println("    mk = 0;");
    ostr.println("  }");
    ostr.println("");
    ostr.println("  /* Determines whether the current node was actually closed and");
    ostr.println("     pushed.  This should only be called in the final user action of a");
    ostr.println("     node scope.  */");
    ostr.println("  public boolean nodeCreated() {");
    ostr.println("    return node_created;");
    ostr.println("  }");
    ostr.println("");
    ostr.println("  /* Call this to reinitialize the node stack.  It is called");
    ostr.println("     automatically by the parser's ReInit() method. */");
    ostr.println("  public void reset() {");
    ostr.println("    nodes.clear();");
    ostr.println("    marks.clear();");
    ostr.println("    sp = 0;");
    ostr.println("    mk = 0;");
    ostr.println("  }");
    ostr.println("");
    ostr.println("  /* Returns the root node of the AST.  It only makes sense to call");
    ostr.println("     this after a successful parse. */");
    ostr.println("  public Node rootNode() {");
    ostr.println("    return nodes.get(0);");
    ostr.println("  }");
    ostr.println("");
    ostr.println("  /* Pushes a node on to the stack. */");
    ostr.println("  public void pushNode(Node n) {");
    ostr.println("    nodes.add(n);");
    ostr.println("    ++sp;");
    ostr.println("  }");
    ostr.println("");
    ostr.println("  /* Returns the node on the top of the stack, and remove it from the");
    ostr.println("     stack.  */");
    ostr.println("  public Node popNode() {");
    ostr.println("    if (--sp < mk) {");
    ostr.println("      mk = marks.remove(marks.size()-1);");
    ostr.println("    }");
    ostr.println("    return nodes.remove(nodes.size()-1);");
    ostr.println("  }");
    ostr.println("");
    ostr.println("  /* Returns the node currently on the top of the stack. */");
    ostr.println("  public Node peekNode() {");
    ostr.println("    return nodes.get(nodes.size()-1);");
    ostr.println("  }");
    ostr.println("");
    ostr.println("  /* Returns the number of children on the stack in the current node");
    ostr.println("     scope. */");
    ostr.println("  public int nodeArity() {");
    ostr.println("    return sp - mk;");
    ostr.println("  }");
    ostr.println("");
    ostr.println("");
    ostr.println("  public void clearNodeScope(Node n) {");
    ostr.println("    while (sp > mk) {");
    ostr.println("      popNode();");
    ostr.println("    }");
    ostr.println("    mk = marks.remove(marks.size()-1);");
    ostr.println("  }");
    ostr.println("");
    ostr.println("");
    ostr.println("  public void openNodeScope(Node n) {");
    ostr.println("    marks.add(mk);");
    ostr.println("    mk = sp;");
    ostr.println("    n.jjtOpen();");
    ostr.println("  }");
    ostr.println("");
    ostr.println("");
    ostr.println("  /* A definite node is constructed from a specified number of");
    ostr.println("     children.  That number of nodes are popped from the stack and");
    ostr.println("     made the children of the definite node.  Then the definite node");
    ostr.println("     is pushed on to the stack. */");
    ostr.println("  public void closeNodeScope(Node n, int num) {");
    ostr.println("    mk = marks.remove(marks.size()-1);");
    ostr.println("    while (num-- > 0) {");
    ostr.println("      Node c = popNode();");
    ostr.println("      c.jjtSetParent(n);");
    ostr.println("      n.jjtAddChild(c, num);");
    ostr.println("    }");
    ostr.println("    n.jjtClose();");
    ostr.println("    pushNode(n);");
    ostr.println("    node_created = true;");
    ostr.println("  }");
    ostr.println("");
    ostr.println("");
    ostr.println("  /* A conditional node is constructed if its condition is true.  All");
    ostr.println("     the nodes that have been pushed since the node was opened are");
    ostr.println("     made children of the conditional node, which is then pushed");
    ostr.println("     on to the stack.  If the condition is false the node is not");
    ostr.println("     constructed and they are left on the stack. */");
    ostr.println("  public void closeNodeScope(Node n, boolean condition) {");
    ostr.println("    if (condition) {");
    ostr.println("      int a = nodeArity();");
    ostr.println("      mk = marks.remove(marks.size()-1);");
    ostr.println("      while (a-- > 0) {");
    ostr.println("        Node c = popNode();");
    ostr.println("        c.jjtSetParent(n);");
    ostr.println("        n.jjtAddChild(c, a);");
    ostr.println("      }");
    ostr.println("      n.jjtClose();");
    ostr.println("      pushNode(n);");
    ostr.println("      node_created = true;");
    ostr.println("    } else {");
    ostr.println("      mk = marks.remove(marks.size()-1);");
    ostr.println("      node_created = false;");
    ostr.println("    }");
    ostr.println("  }");
    ostr.println("}");
  }

  private static void generatePrologue(PrintWriter ostr) {
    if (!JJTreeGlobals.nodePackageName.equals("")) {
      ostr.println("package " + JJTreeGlobals.nodePackageName + ";");
      ostr.println();
      if (!JJTreeGlobals.nodePackageName.equals(JJTreeGlobals.packageName)) {
        ostr.println("import " + JJTreeGlobals.packageName + ".*;");
        ostr.println();
      }

    }
  }


  private static String nodeConstants() {
    return JJTreeGlobals.parserName + "TreeConstants";
  }

  private static void generateTreeConstants_java() {
    String name = JavaTreeGenerator.nodeConstants();
    File file = new File(Options.getOutputDirectory(), name + ".java");

    try (PrintWriter ostr = DigestWriter.create(file, JavaCC.VERSION, DigestOptions.get())) {
      List<String> nodeIds = ASTNodeDescriptor.getNodeIds();
      List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

      JavaTreeGenerator.generatePrologue(ostr);
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

  private static void generateVisitor_java() {
    if (!JJTreeOptions.getVisitor()) {
      return;
    }

    String name = JavaTreeGenerator.visitorClass();
    File file = new File(Options.getOutputDirectory(), name + ".java");

    try (PrintWriter ostr = DigestWriter.create(file, JavaCC.VERSION, DigestOptions.get())) {
      List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

      JavaTreeGenerator.generatePrologue(ostr);
      ostr.println("public interface " + name);
      ostr.println("{");

      String ve = JavaTreeGenerator.mergeVisitorException();

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
          ostr.println(
              "  public " + JJTreeOptions.getVisitorReturnType() + " " + JavaTreeGenerator.getVisitMethodName(nodeType)
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

  private static void generateDefaultVisitor_java() {
    if (!JJTreeOptions.getVisitor()) {
      return;
    }

    String className = JavaTreeGenerator.defaultVisitorClass();
    File file = new File(Options.getOutputDirectory(), className + ".java");

    try (PrintWriter ostr = DigestWriter.create(file, JavaCC.VERSION, DigestOptions.get())) {
      List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

      JavaTreeGenerator.generatePrologue(ostr);
      ostr.println("public class " + className + " implements " + JavaTreeGenerator.visitorClass() + "{");

      final String ve = JavaTreeGenerator.mergeVisitorException();

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
          ostr.println("  public " + returnType + " " + JavaTreeGenerator.getVisitMethodName(nodeType) + "(" + nodeType
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


  private static void generateTree_java(PrintWriter ostr, DigestOptions options) throws IOException {
    JavaTreeGenerator.generatePrologue(ostr);
    options.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);

    Template.of("/templates/Tree.template", options).write(ostr);
  }


  private static void generateNode_java(PrintWriter ostr, DigestOptions options) throws IOException {
    JavaTreeGenerator.generatePrologue(ostr);

    options.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);

    Template.of("/templates/Node.template", options).write(ostr);
  }


  private static void generateMULTINode_java(PrintWriter ostr, String nodeType, DigestOptions options)
      throws IOException {
    JavaTreeGenerator.generatePrologue(ostr);

    options.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
    options.put(JavaCC.JJTREE_NODE_TYPE, nodeType);
    options.put(JavaCC.JJTREE_VISITOR_RETURN_VOID,
        Boolean.valueOf(JJTreeOptions.getVisitorReturnType().equals("void")));

    Template.of("/templates/MultiNode.template", options).write(ostr);
  }
}
