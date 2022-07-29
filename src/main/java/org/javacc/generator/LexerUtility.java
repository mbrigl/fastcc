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

package org.javacc.generator;

import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.RChoice;
import org.javacc.parser.RegularExpression;

/**
 * The {@link LexerUtility} class.
 */
abstract class LexerUtility {

  static void CheckUnmatchability(RChoice choice, LexerData data) {
    for (Object element : choice.getChoices()) {
      RegularExpression curRE = (RegularExpression) element;
      if (!curRE.private_rexp && (// curRE instanceof RJustName &&
      curRE.ordinal > 0) && (curRE.ordinal < choice.ordinal)
          && (data.getState(curRE.ordinal) == data.getState(choice.ordinal))) {
        if (choice.label != null) {
          JavaCCErrors.warning(choice,
              "Regular Expression choice : " + curRE.label + " can never be matched as : " + choice.label);
        } else {
          JavaCCErrors.warning(choice, "Regular Expression choice : " + curRE.label
              + " can never be matched as token of kind : " + choice.ordinal);
        }
      }
    }
  }
}
