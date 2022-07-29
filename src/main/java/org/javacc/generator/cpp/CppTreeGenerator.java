// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

package org.javacc.generator.cpp;

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
import org.javacc.jjtree.JJTreeParserConstants;
import org.javacc.jjtree.NodeScope;
import org.javacc.jjtree.Token;
import org.javacc.parser.Options;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CppTreeGenerator extends JJTreeCodeGenerator {

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

    CppTreeGenerator.addType(type);

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

  @Override
  public final void generateJJTree() {
    CppTreeGenerator.generateTreeClasses();
    CppTreeGenerator.generateTreeConstants();
    CppTreeGenerator.generateVisitors();
    CppTreeGenerator.generateTreeState();
  }


  private static void generateTreeState() {
    DigestOptions options = DigestOptions.get();
    options.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
    String filePrefix = new File(Options.getOutputDirectory(), "TreeState").getAbsolutePath();


    File file = new File(filePrefix + ".h");
    try (DigestWriter writer = DigestWriter.create(file, JavaCC.VERSION, options)) {
      CppTreeGenerator.generateFile(writer, "/templates/cpp/TreeState.h.template", writer.options());
    } catch (IOException e) {
      e.printStackTrace();
    }

    file = new File(filePrefix + ".cc");
    try (DigestWriter writer = DigestWriter.create(file, JavaCC.VERSION, options)) {
      CppTreeGenerator.generateFile(writer, "/templates/cpp/TreeState.cc.template", writer.options());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static List<String> headersForJJTreeH = new ArrayList<>();

  private static Set<String>  nodesToGenerate   = new HashSet<>();

  private static void addType(String type) {
    if (!type.equals("Node")) {
      CppTreeGenerator.nodesToGenerate.add(type);
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
    String name = CppTreeGenerator.visitorClass();
    return new File(Options.getOutputDirectory(), name + ".h").getAbsolutePath();
  }

  private static void generateTreeClasses() {
    CppTreeGenerator.generateNodeHeader();
    CppTreeGenerator.generateNodeImpl();
    CppTreeGenerator.generateMultiTreeImpl();
    CppTreeGenerator.generateOneTreeInterface();
    // generateOneTreeImpl();
    CppTreeGenerator.generateTreeInterface();
  }

  private static void generateNodeHeader() {
    DigestOptions optionMap = DigestOptions.get();
    optionMap.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_TYPE, CppTreeGenerator.getVisitorReturnType());
    optionMap.put(JavaCC.JJTREE_VISITOR_DATA_TYPE, CppTreeGenerator.getVisitorArgumentType());
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_VOID,
        Boolean.valueOf(CppTreeGenerator.getVisitorReturnType().equals("void")));

    File file = new File(CppTreeGenerator.nodeIncludeFile());
    try (DigestWriter writer = DigestWriter.create(file, JavaCC.VERSION, optionMap)) {
      CppTreeGenerator.generateFile(writer, "/templates/cpp/Node.h.template", writer.options());
    } catch (IOException e) {
      throw new Error(e.toString());
    }
  }

  private static void generateNodeImpl() {
    DigestOptions optionMap = DigestOptions.get();
    optionMap.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_TYPE, CppTreeGenerator.getVisitorReturnType());
    optionMap.put(JavaCC.JJTREE_VISITOR_DATA_TYPE, CppTreeGenerator.getVisitorArgumentType());
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_VOID,
        Boolean.valueOf(CppTreeGenerator.getVisitorReturnType().equals("void")));

    File file = new File(CppTreeGenerator.nodeImplFile());
    try (DigestWriter writer = DigestWriter.create(file, JavaCC.VERSION, optionMap)) {
      CppTreeGenerator.generateFile(writer, "/templates/cpp/Node.cc.template", writer.options());
    } catch (IOException e) {
      throw new Error(e.toString());
    }
  }

  private static void generateTreeInterface() {
    String node = "Tree";
    DigestOptions optionMap = DigestOptions.get();
    optionMap.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_TYPE, CppTreeGenerator.getVisitorReturnType());
    optionMap.put(JavaCC.JJTREE_VISITOR_DATA_TYPE, CppTreeGenerator.getVisitorArgumentType());
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_VOID,
        Boolean.valueOf(CppTreeGenerator.getVisitorReturnType().equals("void")));
    optionMap.put(JavaCC.JJTREE_NODE_TYPE, node);

    File file = new File(CppTreeGenerator.jjtreeIncludeFile(node));
    try (DigestWriter writer = DigestWriter.create(file, JavaCC.VERSION, optionMap)) {
      CppTreeGenerator.generateFile(writer, "/templates/cpp/Tree.h.template", writer.options());
    } catch (IOException e) {
      throw new Error(e.toString());
    }
  }

  private static void generateMultiTreeImpl() {
    for (String node : CppTreeGenerator.nodesToGenerate) {
      File file = new File(CppTreeGenerator.jjtreeImplFile(node));
      DigestOptions optionMap = DigestOptions.get();
      optionMap.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
      optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_TYPE, CppTreeGenerator.getVisitorReturnType());
      optionMap.put(JavaCC.JJTREE_VISITOR_DATA_TYPE, CppTreeGenerator.getVisitorArgumentType());
      optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_VOID,
          Boolean.valueOf(CppTreeGenerator.getVisitorReturnType().equals("void")));
      optionMap.put(JavaCC.JJTREE_NODE_TYPE, node);

      try (DigestWriter writer = DigestWriter.create(file, JavaCC.VERSION, optionMap)) {
        CppTreeGenerator.generateFile(writer, "/templates/cpp/MultiNode.cc.template", writer.options());
      } catch (IOException e) {
        throw new Error(e.toString());
      }
    }
  }


  private static void generateOneTreeInterface() {
    DigestOptions optionMap = DigestOptions.get();
    optionMap.put(JavaCC.PARSER_NAME, JJTreeGlobals.parserName);
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_TYPE, CppTreeGenerator.getVisitorReturnType());
    optionMap.put(JavaCC.JJTREE_VISITOR_DATA_TYPE, CppTreeGenerator.getVisitorArgumentType());
    optionMap.put(JavaCC.JJTREE_VISITOR_RETURN_VOID,
        Boolean.valueOf(CppTreeGenerator.getVisitorReturnType().equals("void")));

    File file = new File(CppTreeGenerator.jjtreeIncludeFile());
    try (DigestWriter writer = DigestWriter.create(file, JavaCC.VERSION, optionMap)) {
      // PrintWriter ostr = outputFile.getPrintWriter();
      file.getName().replace('.', '_').toUpperCase();
      writer.println("#ifndef JAVACC_ONE_TREE_H");
      writer.println("#define JAVACC_ONE_TREE_H");
      writer.println();
      writer.println("#include \"Node.h\"");
      for (String s : CppTreeGenerator.nodesToGenerate) {
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

  private static void generateTreeConstants() {
    String name = CppTreeGenerator.nodeConstants();
    File file = new File(Options.getOutputDirectory(), name + ".h");
    CppTreeGenerator.headersForJJTreeH.add(file.getName());

    try (DigestWriter ostr = DigestWriter.create(file, JavaCC.VERSION, DigestOptions.get())) {
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
        ostr.println("    " + n + " = " + i + ",");
      }

      ostr.println("};");
      ostr.println();

      for (int i = 0; i < nodeNames.size(); ++i) {
        ostr.print("static JJChar jjtNodeName_arr_" + i + "[] = ");
        String n = nodeNames.get(i);
        // ostr.println(" (JJChar*)\"" + n + "\",");
        CppOtherFilesGenerator.printCharArray(ostr, n);
        ostr.println(";");
      }
      ostr.println("static JJString jjtNodeName[] = {");
      for (int i = 0; i < nodeNames.size(); i++) {
        ostr.println("    jjtNodeName_arr_" + i + ",");
      }
      ostr.println("};");

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

  private static void generateVisitors() {
    if (!JJTreeOptions.getVisitor()) {
      return;
    }

    File file = new File(CppTreeGenerator.visitorIncludeFile());
    try (DigestWriter ostr = DigestWriter.create(file, JavaCC.VERSION, DigestOptions.get())) {
      CppTreeGenerator.visitorClass();
      ostr.println("#ifndef " + file.getName().replace('.', '_').toUpperCase());
      ostr.println("#define " + file.getName().replace('.', '_').toUpperCase());
      ostr.println("\n#include \"JavaCC.h\"");
      ostr.println("#include \"" + JJTreeGlobals.parserName + "Tree.h" + "\"");

      boolean hasNamespace = ((String) ostr.options().get(JavaCC.JJPARSER_CPP_NAMESPACE)).length() > 0;
      if (hasNamespace) {
        ostr.println("namespace " + ostr.options().get(JavaCC.JJPARSER_CPP_NAMESPACE) + " {");
      }

      CppTreeGenerator.generateVisitorInterface(ostr);
      CppTreeGenerator.generateDefaultVisitor(ostr);

      if (hasNamespace) {
        ostr.println("}");
      }

      ostr.println("#endif");
    } catch (IOException ioe) {
      throw new Error(ioe.toString());
    }
  }

  private static void generateVisitorInterface(PrintWriter ostr) {
    String name = CppTreeGenerator.visitorClass();
    List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

    ostr.println("class " + name);
    ostr.println("{");

    String argumentType = CppTreeGenerator.getVisitorArgumentType();
    String returnType = CppTreeGenerator.getVisitorReturnType();
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
        ostr.println("  virtual " + returnType + " " + CppTreeGenerator.getVisitMethodName(nodeType) + "(const "
            + nodeType + " *node, " + argumentType + " data) = 0;");
      }
    }

    ostr.println("  virtual ~" + name + "() { }");
    ostr.println("};");
  }

  private static String defaultVisitorClass() {
    return JJTreeGlobals.parserName + "DefaultVisitor";
  }

  private static void generateDefaultVisitor(PrintWriter ostr) {
    String className = CppTreeGenerator.defaultVisitorClass();
    List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

    ostr.println("class " + className + " : public " + CppTreeGenerator.visitorClass() + " {");

    String argumentType = CppTreeGenerator.getVisitorArgumentType();
    String ret = CppTreeGenerator.getVisitorReturnType();

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
        ostr.println("  virtual " + ret + " " + CppTreeGenerator.getVisitMethodName(nodeType) + "(const " + nodeType
            + " *node, " + argumentType + " data) {");
        ostr.println("    " + (ret.trim().equals("void") ? "" : "return ") + "defaultVisit(node, data);");
        ostr.println("  }");
      }
    }
    ostr.println("  ~" + className + "() { }");
    ostr.println("};");
  }

  private static void generateFile(PrintWriter writer, String template, Map<String, Object> options)
      throws IOException {
    Template.of(template, options).write(writer);
  }
}
