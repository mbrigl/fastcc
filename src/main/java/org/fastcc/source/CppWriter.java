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

package org.fastcc.source;

import org.fastcc.utils.DigestOptions;
import org.fastcc.utils.DigestWriter;
import org.javacc.JavaCC;
import org.javacc.JavaCCVersion;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.Options;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

/**
 * The {@link CppWriter} class.
 */
public class CppWriter extends SourceWriter {

  private final StringWriter writer;
  private final StringWriter header    = new StringWriter();
  private final StringWriter statistic = new StringWriter();


  /**
   * Constructs an instance of {@link CppWriter}.
   *
   * @param name
   */
  public CppWriter(String name) {
    super(name);
    this.writer = (StringWriter) out;
    header.append("#ifndef JAVACC_" + name.replace('.', '_').toUpperCase() + "_H\n");
    header.append("#define JAVACC_" + name.replace('.', '_').toUpperCase() + "_H\n");
  }

  public final void switchToImpl() {
    this.out = this.writer;
  }

  public final void switchToHeader() {
    this.out = this.header;
  }

  public final void switchToStatics() {
    this.out = this.statistic;
  }

  @Override
  public final void saveOutput(File path) {
    // dump the statics into the main file with the code.
    StringBuffer buffer = new StringBuffer();
    buffer.append(statistic.toString());
    buffer.append(writer.toString());

    // Finally enclose the whole thing in the namespace, if specified.
    if (Options.stringValue(JavaCC.JJPARSER_CPP_NAMESPACE).length() > 0) {
      buffer.append("}\n");
      header.append("}\n");
    }
    header.append("#endif\n");

    File file = new File(Options.getOutputDirectory(), getName() + ".h");
    saveOutput(file, header.getBuffer(), getOptions());

    file = new File(Options.getOutputDirectory(), getName() + ".cc");
    saveOutput(file, buffer, getOptions());
  }

  private void saveOutput(File file, StringBuffer buffer, DigestOptions options) {
    CppWriter.fixupLongLiterals(buffer);

    try (DigestWriter writer = DigestWriter.create(file, JavaCCVersion.VERSION, options)) {
      writer.print(buffer.toString());
    } catch (IOException ioe) {
      JavaCCErrors.fatal("Could not create output file: " + file);
    }
  }


  private static boolean isHexDigit(char c) {
    return ((c >= '0') && (c <= '9')) || ((c >= 'a') && (c <= 'f')) || ((c >= 'A') && (c <= 'F'));
  }

  // HACK
  private static void fixupLongLiterals(StringBuffer buffer) {
    for (int i = 0; i < (buffer.length() - 1); i++) {
      char c1 = buffer.charAt(i);
      char c2 = buffer.charAt(i + 1);
      if (Character.isDigit(c1) || ((c1 == '0') && (c2 == 'x'))) {
        i += c1 == '0' ? 2 : 1;
        while (CppWriter.isHexDigit(buffer.charAt(i))) {
          i++;
        }
        if (buffer.charAt(i) == 'L') {
          buffer.insert(i, "UL");
        }
        i++;
      }
    }
  }
}
