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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@link JJMain} class.
 */
public abstract class JJMain {

  private static final Pattern GENERATED = Pattern.compile("@generated\\(([^\\)]+)\\)");

  /**
   * Constructs an instance of {@link JJMain}.
   */
  private JJMain() {}


  /**
   * This prints the banner line when the various tools are invoked. This takes as argument the
   * tool's full name and its version.
   */
  public static void bannerLine(String fullName, String ver) {
    System.out.print("Java Compiler Compiler Version " + JavaCC.VERSION.toString() + " (" + fullName);
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

  private static List<String> readToolNameList(String str) {
    Matcher matcher = JJMain.GENERATED.matcher(str);
    while (matcher.find()) {
      return Arrays.asList(matcher.group(1).split(","));
    }
    return Collections.emptyList();
  }

  /**
   * Returns true if tool name passed is one of the tool names returned by getToolNames(fileName).
   *
   * @throws IOException
   * @throws FileNotFoundException
   */
  static boolean isGeneratedBy(String toolName, String fileName) {
    try (InputStream stream = new FileInputStream(fileName)) {
      String data = new String(stream.readAllBytes());
      for (String element : JJMain.readToolNameList(data)) {
        if (toolName.equals(element)) {
          return true;
        }
      }
    } catch (IOException e) {}
    return false;
  }

  static final void writeGenerated(PrintWriter writer) {
    writer.println("/* @generated(JJTree) */");
  }
}
