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

package org.javacc.jjtree;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class ASTNodeDescriptor extends JJTreeNode {

  ASTNodeDescriptor(int id) {
    super(id);
  }

  private boolean faked = false;

  static ASTNodeDescriptor indefinite(String s) {
    ASTNodeDescriptor nd = new ASTNodeDescriptor(JJTreeParserTreeConstants.JJTNODEDESCRIPTOR);
    nd.name = s;
    nd.setNodeIdValue();
    nd.faked = true;
    return nd;
  }


  private static List<String>              nodeIds   = new ArrayList<>();
  private static List<String>              nodeNames = new ArrayList<>();
  private static Hashtable<String, String> nodeSeen  = new Hashtable<>();

  public static List<String> getNodeIds() {
    return ASTNodeDescriptor.nodeIds;
  }

  public static List<String> getNodeNames() {
    return ASTNodeDescriptor.nodeNames;
  }

  void setNodeIdValue() {
    String k = getNodeId();
    if (!ASTNodeDescriptor.nodeSeen.containsKey(k)) {
      ASTNodeDescriptor.nodeSeen.put(k, k);
      ASTNodeDescriptor.nodeNames.add(this.name);
      ASTNodeDescriptor.nodeIds.add(k);
    }
  }

  public String getNodeId() {
    return "JJT" + this.name.toUpperCase().replace('.', '_');
  }


  String                      name;
  boolean                     isGT;
  ASTNodeDescriptorExpression expression;


  boolean isVoid() {
    return this.name.equals("void");
  }

  @Override
  public String toString() {
    if (this.faked) {
      return "(faked) " + this.name;
    } else {
      return super.toString() + ": " + this.name;
    }
  }


  public String getDescriptor() {
    if (this.expression == null) {
      return this.name;
    } else {
      return "#" + this.name + "(" + (this.isGT ? ">" : "") + expression_text() + ")";
    }
  }

  public String getNodeType() {
    return JJTreeOptions.getMulti() ? JJTreeOptions.getNodePrefix() + this.name : "Node";
  }


  public String openNode(String nodeVar) {
    return "jjtree.openNodeScope(" + nodeVar + ");";
  }


  private String expression_text() {
    if (this.expression.getFirstToken().image.equals(")") && this.expression.getLastToken().image.equals("(")) {
      return "true";
    }

    String s = "";
    Token t = this.expression.getFirstToken();
    while (true) {
      s += " " + t.image;
      if (t == this.expression.getLastToken()) {
        break;
      }
      t = t.next;
    }
    return s;
  }


  public String closeNode(String nodeVar) {
    if (this.expression == null) {
      return "jjtree.closeNodeScope(" + nodeVar + ", true);";
    } else if (this.isGT) {
      return "jjtree.closeNodeScope(" + nodeVar + ", jjtree.nodeArity() >" + expression_text() + ");";
    } else {
      return "jjtree.closeNodeScope(" + nodeVar + ", " + expression_text() + ");";
    }
  }

  @Override
  public String translateImage(Token t) {
    return whiteOut(t);
  }

  @Override
  public final Object jjtAccept(JJTreeParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}