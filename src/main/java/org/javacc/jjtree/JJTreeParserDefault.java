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

package org.javacc.jjtree;

import java.io.Reader;

/**
 * The {@link JJTreeParserDefault} implements a parser for the .jjt files.
 */
public class JJTreeParserDefault extends JJTreeParser {

  public JJTreeParserDefault(Reader reader) {
    super(new JJTreeParserTokenManager(new JavaCharStream(new StreamProvider(reader))));
  }

  /**
   * Parses the {@link Reader} and creates the abstract syntax tree.
   */
  public final ASTGrammar parse() throws ParseException {
    javacc_input();
    return (ASTGrammar) jjtree.rootNode();
  }

  @Override
  protected final void jjtreeOpenNodeScope(Node n) {
    ((JJTreeNode) n).setFirstToken(getToken(1));
  }

  @Override
  protected final void jjtreeCloseNodeScope(Node n) {
    ((JJTreeNode) n).setLastToken(getToken(0));
  }
}
