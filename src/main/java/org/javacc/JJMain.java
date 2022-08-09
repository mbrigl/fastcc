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

import org.javacc.parser.JavaCCErrors;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@link JJMain} class.
 */
public abstract class JJMain {

  /**
   * Constructs an instance of {@link JJMain}.
   */
  private JJMain() {}


  /**
   * This prints the banner line when the various tools are invoked. This takes as argument the
   * tool's full name and its version.
   */
  public static void bannerLine(String fullName, String ver) {
    System.out.print("Java Compiler Compiler Version " + JavaCCVersion.VERSION.toString() + " (" + fullName);
    if (!ver.equals("")) {
      System.out.print(" Version " + ver);
    }
    System.out.println(")");
  }

  static void createOutputDir(File outputDir) {
    if (!outputDir.exists()) {
      JavaCCErrors.warning("Output directory \"" + outputDir + "\" does not exist. Creating the directory.");

      if (!outputDir.mkdirs()) {
        JavaCCErrors.semantic_error("Cannot create the output directory : " + outputDir);
        return;
      }
    }

    if (!outputDir.isDirectory()) {
      JavaCCErrors.semantic_error("\"" + outputDir + " is not a valid output directory.");
      return;
    }

    if (!outputDir.canWrite()) {
      JavaCCErrors.semantic_error("Cannot write to the output output directory : \"" + outputDir + "\"");
    }
  }

  /**
   * Returns a List of names of the tools that have been used to generate the given file.
   */
  static List<String> getToolNames(String fileName) {
    char[] buf = new char[256];
    int read, total = 0;

    try (FileReader stream = new FileReader(fileName)) {
      for (;;) {
        if ((read = stream.read(buf, total, buf.length - total)) != -1) {
          if ((total += read) == buf.length) {
            break;
          }
        } else {
          break;
        }
      }

      return JJMain.makeToolNameList(new String(buf, 0, total));
    } catch (IOException e) {
      if (total > 0) {
        return JJMain.makeToolNameList(new String(buf, 0, total));
      }
    }

    return new ArrayList<>();
  }

  private static List<String> makeToolNameList(String str) {
    List<String> retVal = new ArrayList<>();

    int limit1 = str.indexOf('\n');
    if (limit1 == -1) {
      limit1 = 1000;
    }
    int limit2 = str.indexOf('\r');
    if (limit2 == -1) {
      limit2 = 1000;
    }
    int limit = (limit1 < limit2) ? limit1 : limit2;

    String tmp;
    if (limit == 1000) {
      tmp = str;
    } else {
      tmp = str.substring(0, limit);
    }

    if (tmp.indexOf(':') == -1) {
      return retVal;
    }

    tmp = tmp.substring(tmp.indexOf(':') + 1);

    if (tmp.indexOf(':') == -1) {
      return retVal;
    }

    tmp = tmp.substring(0, tmp.indexOf(':'));

    int i = 0, j = 0;

    while ((j < tmp.length()) && ((i = tmp.indexOf('&', j)) != -1)) {
      retVal.add(tmp.substring(j, i));
      j = i + 1;
    }

    if (j < tmp.length()) {
      retVal.add(tmp.substring(j));
    }

    return retVal;
  }

  /**
   * Returns true if tool name passed is one of the tool names returned by getToolNames(fileName).
   */
  static boolean isGeneratedBy(String toolName, String fileName) {
    for (String element : JJMain.getToolNames(fileName)) {
      if (toolName.equals(element)) {
        return true;
      }
    }
    return false;
  }
}
