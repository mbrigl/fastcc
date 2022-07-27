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

package org.fastcc.utils;

import org.javacc.parser.Options;

/**
 * The {@link Encoding} class.
 */
public abstract class Encoding {

  /**
   * Constructs an instance of {@link Encoding}.
   */
  private Encoding() {}

  public static String escape(String str) {
    String retval = "";
    char ch;
    for (int i = 0; i < str.length(); i++) {
      ch = str.charAt(i);
      if (ch == '\b') {
        retval += "\\b";
      } else if (ch == '\t') {
        retval += "\\t";
      } else if (ch == '\n') {
        retval += "\\n";
      } else if (ch == '\f') {
        retval += "\\f";
      } else if (ch == '\r') {
        retval += "\\r";
      } else if (ch == '\"') {
        retval += "\\\"";
      } else if (ch == '\'') {
        retval += "\\\'";
      } else if (ch == '\\') {
        retval += "\\\\";
      } else if ((ch < 0x20) || (ch > 0x7e)) {
        String s = "0000" + Integer.toString(ch, 16);
        retval += "\\u" + s.substring(s.length() - 4, s.length());
      } else {
        retval += ch;
      }
    }
    return retval;
  }

  public static String escapeUnicode(String str) {
    if (Options.getOutputLanguage().equalsIgnoreCase(Options.OUTPUT_LANGUAGE__JAVA)) {
      StringBuilder builder = new StringBuilder(str.length());
      char ch;
      for (int i = 0; i < str.length(); i++) {
        ch = str.charAt(i);
        if (((ch < 0x20) || (ch > 0x7e)) && (ch != '\t') && (ch != '\n') && (ch != '\r') && (ch != '\f')) {
          String s = "0000" + Integer.toString(ch, 16);
          builder.append("\\u" + s.substring(s.length() - 4, s.length()));
        } else {
          builder.append(ch);
        }
      }
      return builder.toString();
    } else if (Options.getOutputLanguage().equalsIgnoreCase(Options.OUTPUT_LANGUAGE__CPP)) {
      return str;
    } else {
      // TODO :: CBA -- Require Unification of output language specific processing into a single
      // Enum class
      throw new RuntimeException("Unhandled Output Language : " + Options.getOutputLanguage());
    }
  }
}
