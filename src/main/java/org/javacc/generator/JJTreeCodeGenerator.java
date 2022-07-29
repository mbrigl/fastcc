// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

package org.javacc.generator;

import org.fastcc.utils.Encoding;
import org.javacc.jjtree.ASTBNFAction;
import org.javacc.jjtree.ASTBNFDeclaration;
import org.javacc.jjtree.ASTBNFNodeScope;
import org.javacc.jjtree.ASTBNFNonTerminal;
import org.javacc.jjtree.ASTBNFOneOrMore;
import org.javacc.jjtree.ASTBNFSequence;
import org.javacc.jjtree.ASTBNFTryBlock;
import org.javacc.jjtree.ASTBNFZeroOrMore;
import org.javacc.jjtree.ASTBNFZeroOrOne;
import org.javacc.jjtree.ASTExpansionNodeScope;
import org.javacc.jjtree.ASTGrammar;
import org.javacc.jjtree.ASTJavacodeBody;
import org.javacc.jjtree.ASTNodeDescriptor;
import org.javacc.jjtree.ASTProduction;
import org.javacc.jjtree.JJTreeGlobals;
import org.javacc.jjtree.JJTreeNode;
import org.javacc.jjtree.JJTreeParserDefaultVisitor;
import org.javacc.jjtree.Node;
import org.javacc.jjtree.NodeScope;
import org.javacc.jjtree.Token;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;

public abstract class JJTreeCodeGenerator extends JJTreeParserDefaultVisitor {

  @Override
  public final Object defaultVisit(Node node, Object data) {
    handleJJTreeNode((JJTreeNode) node, (PrintWriter) data);
    return null;
  }

  @Override
  public final Object visit(ASTGrammar node, Object data) {
    return node.childrenAccept(this, data);
  }

  @Override
  public final Object visit(ASTBNFAction node, Object data) {
    PrintWriter io = (PrintWriter) data;
    /*
     * Assume that this action requires an early node close, and then try to decide whether this
     * assumption is false. Do this by looking outwards through the enclosing expansion units. If we
     * ever find that we are enclosed in a unit which is not the final unit in a sequence we know
     * that an early close is not required.
     */

    NodeScope ns = NodeScope.getEnclosingNodeScope(node);
    if ((ns != null) && !ns.isVoid()) {
      boolean needClose = true;
      Node sp = node.getScopingParent(ns);

      JJTreeNode n = node;
      while (true) {
        Node p = n.jjtGetParent();
        if ((p instanceof ASTBNFSequence) || (p instanceof ASTBNFTryBlock)) {
          if (n.getOrdinal() != (p.jjtGetNumChildren() - 1)) {
            /* We're not the final unit in the sequence. */
            needClose = false;
            break;
          }
        } else if ((p instanceof ASTBNFZeroOrOne) || (p instanceof ASTBNFZeroOrMore)
            || (p instanceof ASTBNFOneOrMore)) {
          needClose = false;
          break;
        }
        if (p == sp) {
          /* No more parents to look at. */
          break;
        }
        n = (JJTreeNode) p;
      }
      if (needClose) {
        JJTreeCodeGenerator.openJJTreeComment(io, null);
        io.println();
        insertCloseNodeAction(ns, io, getIndentation(node));
        JJTreeCodeGenerator.closeJJTreeComment(io);
      }
    }

    return handleJJTreeNode((JJTreeNode) node, io);
  }

  @Override
  public final Object visit(ASTBNFDeclaration node, Object data) {
    PrintWriter io = (PrintWriter) data;
    if (!node.node_scope.isVoid()) {
      String indent = "";
      if (node.getLastToken().next == node.getFirstToken()) {
        indent = "  ";
      } else {
        for (int i = 1; i < node.getFirstToken().beginColumn; ++i) {
          indent += " ";
        }
      }

      JJTreeCodeGenerator.openJJTreeComment(io, node.node_scope.getNodeDescriptorText());
      io.println();
      insertOpenNodeCode(node.node_scope, io, indent);
      JJTreeCodeGenerator.closeJJTreeComment(io);
    }

    return handleJJTreeNode((JJTreeNode) node, io);
  }

  @Override
  public final Object visit(ASTBNFNodeScope node, Object data) {
    PrintWriter io = (PrintWriter) data;
    if (node.node_scope.isVoid()) {
      return handleJJTreeNode((JJTreeNode) node, io);
    }

    String indent = getIndentation(node.expansion_unit);

    JJTreeCodeGenerator.openJJTreeComment(io, node.node_scope.getNodeDescriptor().getDescriptor());
    io.println();
    tryExpansionUnit(node.node_scope, io, indent, node.expansion_unit);
    return null;
  }

  @Override
  public final Object visit(ASTExpansionNodeScope node, Object data) {
    PrintWriter io = (PrintWriter) data;
    String indent = getIndentation(node.expansion_unit);
    JJTreeCodeGenerator.openJJTreeComment(io, node.node_scope.getNodeDescriptor().getDescriptor());
    io.println();
    insertOpenNodeAction(node.node_scope, io, indent);
    tryExpansionUnit(node.node_scope, io, indent, node.expansion_unit);

    // Print the "whiteOut" equivalent of the Node descriptor to preserve
    // line numbers in the generated file.
    ((ASTNodeDescriptor) node.jjtGetChild(1)).jjtAccept(this, io);
    return null;
  }

  @Override
  public final Object visit(ASTJavacodeBody node, Object data) {
    PrintWriter io = (PrintWriter) data;
    if (node.node_scope.isVoid()) {
      return handleJJTreeNode((JJTreeNode) node, io);
    }

    Token first = node.getFirstToken();

    String indent = "";
    for (int i = 4; i < first.beginColumn; ++i) {
      indent += " ";
    }

    JJTreeCodeGenerator.openJJTreeComment(io, node.node_scope.getNodeDescriptorText());
    io.println();
    insertOpenNodeCode(node.node_scope, io, indent);
    tryTokenSequence(node.node_scope, io, indent, first, node.getLastToken());
    return null;
  }

  /*
   * This method prints the tokens corresponding to this node recursively calling the print methods
   * of its children. Overriding this print method in appropriate nodes gives the output the added
   * stuff not in the input.
   */

  private Object handleJJTreeNode(JJTreeNode node, PrintWriter io) {
    if (node.getLastToken().next == node.getFirstToken()) {
      return null;
    }

    Token t1 = node.getFirstToken();
    Token t = new Token();
    t.next = t1;
    JJTreeNode n;
    for (int ord = 0; ord < node.jjtGetNumChildren(); ord++) {
      n = (JJTreeNode) node.jjtGetChild(ord);
      while (true) {
        t = t.next;
        if (t == n.getFirstToken()) {
          break;
        }
        print(t, io, node);
      }
      n.jjtAccept(this, io);
      t = n.getLastToken();
    }
    while (t != node.getLastToken()) {
      t = t.next;
      print(t, io, node);
    }

    return null;
  }


  protected static void openJJTreeComment(PrintWriter io, String arg) {
    if (arg != null) {
      io.print("/*@bgen(jjtree) " + arg + " */");
    } else {
      io.print("/*@bgen(jjtree)*/");
    }
  }

  protected static void closeJJTreeComment(PrintWriter io) {
    io.print("/*@egen*/");
  }

  private final String getIndentation(JJTreeNode n) {
    return getIndentation(n, 0);
  }

  private final String getIndentation(JJTreeNode n, int offset) {
    String s = "";
    for (int i = offset + 1; i < n.getFirstToken().beginColumn; ++i) {
      s += " ";
    }
    return s;
  }

  protected final void print(Token t, PrintWriter io, String in, String out) {
    Token tt = t.specialToken;
    if (tt != null) {
      while (tt.specialToken != null) {
        tt = tt.specialToken;
      }
      while (tt != null) {
        io.print(Encoding.escapeUnicode(tt.image));
        tt = tt.next;
      }
    }
    String i = t.image;
    if ((in != null) && i.equals(in)) {
      i = out;
    }
    io.print(Encoding.escapeUnicode(i));
  }


  /*
   * Indicates whether the token should be replaced by white space or replaced with the actual node
   * variable.
   */
  private boolean whitingOut = false;

  protected void print(Token t, PrintWriter io, JJTreeNode node) {
    Token tt = t.specialToken;
    if (tt != null) {
      while (tt.specialToken != null) {
        tt = tt.specialToken;
      }
      while (tt != null) {
        io.print(Encoding.escapeUnicode(node.translateImage(tt)));
        tt = tt.next;
      }
    }

    /*
     * If we're within a node scope we modify the source in the following ways:
     *
     * 1) we rename all references to `jjtThis' to be references to the actual node variable.
     *
     * 2) we replace all calls to `jjtree.currentNode()' with references to the node variable.
     */

    NodeScope s = NodeScope.getEnclosingNodeScope(node);
    if (s == null) {
      /*
       * Not within a node scope so we don't need to modify the source.
       */
      io.print(Encoding.escapeUnicode(node.translateImage(t)));
      return;
    }

    if (t.image.equals("jjtThis")) {
      io.print(s.getNodeVariable());
      return;
    } else if (t.image.equals("jjtree")) {
      if (t.next.image.equals(".")) {
        if (t.next.next.image.equals("currentNode")) {
          if (t.next.next.next.image.equals("(")) {
            if (t.next.next.next.next.image.equals(")")) {
              /*
               * Found `jjtree.currentNode()' so go into white out mode. We'll stay in this mode
               * until we find the closing parenthesis.
               */
              this.whitingOut = true;
            }
          }
        }
      }
    }
    if (this.whitingOut) {
      if (t.image.equals("jjtree")) {
        io.print(s.getNodeVariable());
        io.print(" ");
      } else if (t.image.equals(")")) {
        io.print(" ");
        this.whitingOut = false;
      } else {
        for (int i = 0; i < t.image.length(); ++i) {
          io.print(" ");
        }
      }
      return;
    }

    io.print(Encoding.escapeUnicode(node.translateImage(t)));
  }


  private final void insertOpenNodeAction(NodeScope ns, PrintWriter io, String indent) {
    io.println(indent + "{");
    insertOpenNodeCode(ns, io, indent + "  ");
    io.println(indent + "}");
  }


  private final void insertCloseNodeAction(NodeScope ns, PrintWriter io, String indent) {
    io.println(indent + "{");
    insertCloseNodeCode(ns, io, indent + "  ", false);
    io.println(indent + "}");
  }

  protected abstract void insertOpenNodeCode(NodeScope ns, PrintWriter io, String indent);

  protected abstract void insertCloseNodeCode(NodeScope ns, PrintWriter io, String indent, boolean isFinal);

  protected abstract void insertCatchBlocks(NodeScope ns, PrintWriter io, Enumeration<String> thrown_names,
      String indent);

  protected abstract void tryTokenSequence(NodeScope ns, PrintWriter io, String indent, Token first, Token last);


  protected static void findThrown(NodeScope ns, Hashtable<String, String> thrown_set, JJTreeNode expansion_unit) {
    if (expansion_unit instanceof ASTBNFNonTerminal) {
      /*
       * Should really make the nonterminal explicitly maintain its name.
       */
      String nt = expansion_unit.getFirstToken().image;
      ASTProduction prod = JJTreeGlobals.productions.get(nt);
      if (prod != null) {
        Enumeration<String> e = prod.throws_list.elements();
        while (e.hasMoreElements()) {
          String t = e.nextElement();
          thrown_set.put(t, t);
        }
      }
    }
    for (int i = 0; i < expansion_unit.jjtGetNumChildren(); ++i) {
      JJTreeNode n = (JJTreeNode) expansion_unit.jjtGetChild(i);
      JJTreeCodeGenerator.findThrown(ns, thrown_set, n);
    }
  }


  protected abstract void tryExpansionUnit(NodeScope ns, PrintWriter io, String indent, JJTreeNode expansion_unit);

  public abstract void generateJJTree();
}
