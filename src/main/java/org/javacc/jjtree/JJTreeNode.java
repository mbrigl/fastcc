/* Copyright (c) 2006, Sun Microsystems, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.javacc.jjtree;

public class JJTreeNode extends Node {

  private int myOrdinal;

  JJTreeNode(int id) {
    super(id);
  }

  JJTreeNode(JJTreeParser p, int i) {
    this(i);
  }


  @Override
  public void jjtAddChild(Node n, int i) {
    super.jjtAddChild(n, i);
    ((JJTreeNode) n).setOrdinal(i);
  }

  public int getOrdinal() {
    return this.myOrdinal;
  }

  public void setOrdinal(int o) {
    this.myOrdinal = o;
  }


  /*****************************************************************
   *
   * The following is added manually to enhance all tree nodes with attributes that store the first
   * and last tokens corresponding to each node, as well as to print the tokens back to the
   * specified output stream.
   *
   *****************************************************************/

  private Token first, last;

  public Token getFirstToken() {
    return this.first;
  }

  public void setFirstToken(Token t) {
    this.first = t;
  }

  public Token getLastToken() {
    return this.last;
  }

  public void setLastToken(Token t) {
    this.last = t;
  }

  public String translateImage(Token t) {
    return t.image;
  }

  String whiteOut(Token t) {
    StringBuilder sb = new StringBuilder(t.image.length());

    for (int i = 0; i < t.image.length(); ++i) {
      char ch = t.image.charAt(i);
      if ((ch != '\t') && (ch != '\n') && (ch != '\r') && (ch != '\f')) {
        sb.append(' ');
      } else {
        sb.append(ch);
      }
    }

    return sb.toString();
  }
}
