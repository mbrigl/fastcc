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

package org.javacc.jjdoc;

import org.javacc.parser.Expansion;
import org.javacc.parser.NonTerminal;
import org.javacc.parser.NormalProduction;
import org.javacc.parser.RegExprSpec;
import org.javacc.parser.RegularExpression;
import org.javacc.parser.TokenProduction;

/**
 * Output BNF in HTML 3.2 format.
 */
class XTextGenerator extends TextGenerator {

  public XTextGenerator() {}

  @Override
  public void handleTokenProduction(TokenProduction tp) {

    StringBuilder sb = new StringBuilder();

    for (RegExprSpec res : tp.respecs) {
      String regularExpressionText = JJDoc.emitRE(res.rexp);
      sb.append(regularExpressionText);

      if (res.nsTok != null) {
        sb.append(" : " + res.nsTok.image);
      }

      sb.append("\n");
      // if (it2.hasNext()) {
      // sb.append("| ");
      // }
    }

    // text(sb.toString());
  }

  private void println(String s) {
    print(s + "\n");
  }

  @Override
  public void text(String s) {
    print(s);
  }

  @Override
  public void print(String s) {
    this.ostr.print(s);
  }


  @Override
  public void documentStart() {
    this.ostr = create_output_stream();
    println("grammar " + JJDocGlobals.input_file + " with org.eclipse.xtext.common.Terminals");
    println("import \"http://www.eclipse.org/emf/2002/Ecore\" as ecore");
    println("");
  }

  @Override
  public void documentEnd() {
    this.ostr.close();
  }

  /**
   * Prints out comments, used for tokens and non-terminals. {@inheritDoc}
   *
   * @see org.javacc.jjdoc.TextGenerator#specialTokens(java.lang.String)
   */
  @Override
  public void specialTokens(String s) {
    print(s);
  }


  @Override
  public void nonterminalsStart() {}

  @Override
  public void nonterminalsEnd() {}

  @Override
  public void tokensStart() {}

  @Override
  public void tokensEnd() {}

  @Override
  public void productionStart(NormalProduction np) {}

  @Override
  public void productionEnd(NormalProduction np) {}

  @Override
  public void expansionStart(Expansion e, boolean first) {}

  @Override
  public void expansionEnd(Expansion e, boolean first) {
    println(";");
  }

  @Override
  public void nonTerminalStart(NonTerminal nt) {
    print("terminal ");
  }

  @Override
  public void nonTerminalEnd(NonTerminal nt) {
    print(";");
  }

  @Override
  public void reStart(RegularExpression r) {}

  @Override
  public void reEnd(RegularExpression r) {}
}
