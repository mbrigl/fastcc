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

package org.javacc.generator.java;

import org.fastcc.utils.DigestOptions;
import org.fastcc.utils.DigestWriter;
import org.fastcc.utils.Template;
import org.javacc.JavaCCRequest;
import org.javacc.JavaCCVersion;
import org.javacc.generator.JavaCCTokenInsertion;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.Options;

import java.io.File;
import java.io.IOException;

/**
 * Generate CharStream, TokenManager and Exceptions.
 */
class JavaFiles {


  interface JavaResourceTemplateLocations {

    String getTokenManagerTemplateResourceUrl();

    String getTokenTemplateResourceUrl();

    String getTokenMgrErrorTemplateResourceUrl();

    String getJavaCharStreamTemplateResourceUrl();

    String getCharStreamTemplateResourceUrl();

    String getParseExceptionTemplateResourceUrl();
  }


  private static class JavaModernResourceTemplateLocationImpl implements JavaResourceTemplateLocations {

    @Override
    public String getTokenMgrErrorTemplateResourceUrl() {
      // Same as Java
      return "/templates/TokenMgrError.template";
    }

    @Override
    public String getCharStreamTemplateResourceUrl() {
      // Same as Java
      return "/templates/CharStream.template";
    }

    @Override
    public String getTokenManagerTemplateResourceUrl() {
      // Same as Java
      return "/templates/TokenManager.template";
    }

    @Override
    public String getTokenTemplateResourceUrl() {
      // Same as Java
      return "/templates/Token.template";
    }


    @Override
    public String getJavaCharStreamTemplateResourceUrl() {
      return "/templates/JavaCharStream.template";
    }


    @Override
    public String getParseExceptionTemplateResourceUrl() {
      return "/templates/ParseException.template";
    }
  }

  static final JavaResourceTemplateLocations RESOURCES_JAVA_MODERN = new JavaModernResourceTemplateLocationImpl();


  static void gen_JavaCharStream(JavaCCRequest request, JavaResourceTemplateLocations locations) {
    DigestOptions options = DigestOptions.get();

    final File file = new File(Options.getOutputDirectory(), "JavaCharStream.java");
    try (DigestWriter ostr = DigestWriter.create(file, JavaCCVersion.VERSION, options)) {
      JavaCCTokenInsertion.print(ostr, request);
      Template.of(locations.getJavaCharStreamTemplateResourceUrl(), ostr.options()).write(ostr);
    } catch (IOException e) {
      System.err.println("Failed to create JavaCharStream " + e);
      JavaCCErrors.semantic_error("Could not open file JavaCharStream.java for writing.");
      throw new Error();
    }
  }


  static void gen_JavaModernFiles(JavaCCRequest request) {
    JavaFiles.genMiscFile(request, "Provider.java", "/templates/Provider.template");
    JavaFiles.genMiscFile(request, "StringProvider.java", "/templates/StringProvider.template");
    JavaFiles.genMiscFile(request, "StreamProvider.java", "/templates/StreamProvider.template");
  }

  private static void genMiscFile(JavaCCRequest request, String fileName, String templatePath) throws Error {
    File file = new File(Options.getOutputDirectory(), fileName);

    try (DigestWriter ostr = DigestWriter.create(file, JavaCCVersion.VERSION, DigestOptions.get())) {
      JavaCCTokenInsertion.print(ostr, request);
      Template.of(templatePath, ostr.options()).write(ostr);
    } catch (IOException e) {
      System.err.println("Failed to create " + fileName + " " + e);
      JavaCCErrors.semantic_error("Could not open file " + fileName + " for writing.");
      throw new Error();
    }
  }


  static void gen_ParseException(JavaCCRequest request, JavaResourceTemplateLocations locations) {
    File file = new File(Options.getOutputDirectory(), "ParseException.java");
    try (DigestWriter ostr = DigestWriter.create(file, JavaCCVersion.VERSION, DigestOptions.get())) {
      JavaCCTokenInsertion.print(ostr, request);
      Template.of(locations.getParseExceptionTemplateResourceUrl(), ostr.options()).write(ostr);
    } catch (IOException e) {
      System.err.println("Failed to create ParseException " + e);
      JavaCCErrors.semantic_error("Could not open file ParseException.java for writing.");
      throw new Error();
    }
  }


  static void gen_TokenMgrError(JavaCCRequest request, JavaResourceTemplateLocations locations) {
    String filename = "TokenMgrException.java";
    File file = new File(Options.getOutputDirectory(), filename);
    try (DigestWriter ostr = DigestWriter.create(file, JavaCCVersion.VERSION, DigestOptions.get())) {
      JavaCCTokenInsertion.print(ostr, request);
      Template.of(locations.getTokenMgrErrorTemplateResourceUrl(), ostr.options()).write(ostr);
    } catch (IOException e) {
      System.err.println("Failed to create " + filename + " " + e);
      JavaCCErrors.semantic_error("Could not open file " + filename + " for writing.");
      throw new Error();
    }
  }


  static void gen_Token(JavaCCRequest request, JavaResourceTemplateLocations locations) {
    final File file = new File(Options.getOutputDirectory(), "Token.java");
    try (DigestWriter ostr = DigestWriter.create(file, JavaCCVersion.VERSION, DigestOptions.get())) {
      JavaCCTokenInsertion.print(ostr, request);
      Template.of(locations.getTokenTemplateResourceUrl(), ostr.options()).write(ostr);
    } catch (IOException e) {
      System.err.println("Failed to create Token " + e);
      JavaCCErrors.semantic_error("Could not open file Token.java for writing.");
      throw new Error();
    }
  }
}
