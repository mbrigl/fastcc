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

package it.smartio.fastcc.parser;

/**
 * Describes expansions - entities that may occur on the right hand sides of productions. This is
 * the base class of a bunch of other more specific classes.
 */

public class Expansion {

  /**
   * The line and column number of the construct that corresponds most closely to this node.
   */
  private int   line;
  private int   column;

  /**
   * An internal name for this expansion. This is used to generate parser routines.
   */
  public String internal_name = "";


  /**
   * The parent of this expansion node. In case this is the top level expansion of the production it
   * is a reference to the production node otherwise it is a reference to another Expansion node. In
   * case this is the top level of a lookahead expansion,then the parent is null.
   */
  public Object  parent;

  /**
   * The ordinal of this node with respect to its parent.
   */
  public int     ordinal;

  /**
   * To avoid right-recursive loops when calculating follow sets, we use a generation number which
   * indicates if this expansion was visited by LookaheadWalk.genFollowSet in the same generation.
   * New generations are obtained by incrementing the static counter below, and the current
   * generation is stored in the non-static variable below.
   */
  public long    myGeneration  = 0;

  /**
   * This flag is used for bookkeeping by the minimumSize method in class ParseEngine.
   */
  public boolean inMinimumSize = false;

  /**
   * A reimplementing of Object.hashCode() to be deterministic. This uses the line and column fields
   * to generate an arbitrary number - we assume that this method is called only after line and
   * column are set to their actual values.
   */
  @Override
  public int hashCode() {
    return getLine() + getColumn();
  }

  private String getSimpleName() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1); // strip the package name
  }

  @Override
  public String toString() {
    return "[" + getLine() + "," + getColumn() + " " + System.identityHashCode(this) + " " + getSimpleName() + "]";
  }

  public final String getProductionName() {
    Object next = this;
    // Limit the number of iterations in case there's a cycle
    for (int i = 0; (i < 42) && (next != null); i++) {
      if (next instanceof BNFProduction) {
        return ((BNFProduction) next).getLhs();
      } else if (next instanceof Expansion) {
        next = ((Expansion) next).parent;
      } else {
        return null;
      }
    }
    return null;
  }

  /**
   * @return the line
   */
  public int getLine() {
    return this.line;
  }

  /**
   * @return the column
   */
  public int getColumn() {
    return this.column;
  }

  /**
   * Sets the position in the source.
   * 
   * @param token
   * @param token
   */
  public void setLocation(Expansion expansion) {
    this.line = expansion.getLine();
    this.column = expansion.getColumn();
  }

  /**
   * Sets the position in the source.
   * 
   * @param token
   * @param token
   */
  public void setLocation(Token token) {
    this.line = token.beginLine;
    this.column = token.beginColumn;
  }
}
