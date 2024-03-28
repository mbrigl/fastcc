/*
 * Copyright (c) 2001-2021 Territorium Online Srl / TOL GmbH. All Rights Reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code as defined in and that are
 * subject to the Territorium Online License Version 1.0. You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at http://www.tol.info/license/
 * and read it before using this file.
 *
 * The Original Code and all software distributed under the License are distributed on an 'AS IS'
 * basis, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, AND TERRITORIUM ONLINE HEREBY
 * DISCLAIMS ALL SUCH WARRANTIES, INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT. Please see the License for
 * the specific language governing rights and limitations under the License.
 */

package it.smartio.fastcc.jjtree;

/**
 * The {@link AbstractJJTreeParser} class.
 */
abstract class AbstractJJTreeParser {

  protected final void setParserName() {
    JJTreeGlobals.parserName = getToken(0).image;
  }

  protected final void addProduction(ASTProduction prod) {
    JJTreeGlobals.productions.put(prod.name, prod);
  }

  protected final void normalize() {
    getOptions().normalize();
  }

  protected final void setInputFileOption(Token o, Token v, Object value) {
    getOptions().setInputFileOption(o, v, o.image, value);
  }

  protected abstract Token getNextToken();

  protected abstract Token getToken(int index);

  protected void jjtreeOpenNodeScope(Node n) {}

  protected void jjtreeCloseNodeScope(Node n) {}

  protected JJTreeOptions getOptions() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if the next token is not in the FOLLOW list of "expansion". It is used to decide
   * when the end of an "expansion" has been reached.
   */
  protected boolean notTailOfExpansionUnit() {
    Token t;
    t = getToken(1);
    if ((t.kind == JJTreeParserConstants.BIT_OR) || (t.kind == JJTreeParserConstants.COMMA)
        || (t.kind == JJTreeParserConstants.RPAREN) || (t.kind == JJTreeParserConstants.RBRACE)
        || (t.kind == JJTreeParserConstants.RBRACKET)) {
      return false;
    }
    return true;
  }
}
