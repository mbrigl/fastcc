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

package org.javacc;

import org.javacc.generator.JavaCCToken;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.Options;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link JavaCCContext} class.
 */
public class JavaCCContext {


  // Set to true if this file has been processed by JJTree.
  boolean      jjtreeGenerated;
  // The list of tools that have participated in generating the input grammar file.
  List<String> toolNames;


  /**
   * Constructs an instance of {@link JavaCCContext}.
   */
  public JavaCCContext() {
    JavaCCToken.reset();
    JavaCCErrors.reInit();
    Options.init();
  }

  public final boolean isGenerated() {
    return jjtreeGenerated;
  }

  public final List<String> getToolNames() {
    return new ArrayList<>(toolNames);
  }


  public final JavaCCLanguage getLanguage() {
    String language = Options.getOutputLanguage();
    if (language.equalsIgnoreCase("java"))
      return JavaCCLanguage.Java;
    if (language.equalsIgnoreCase("c++") || language.equalsIgnoreCase("cpp"))
      return JavaCCLanguage.Cpp;
    return JavaCCLanguage.Cpp;
  }
}
