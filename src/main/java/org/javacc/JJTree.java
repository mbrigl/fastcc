// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

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

package org.javacc;

import org.javacc.generator.ParserEngine;
import org.javacc.jjtree.ASTGrammar;
import org.javacc.jjtree.JJTreeGlobals;
import org.javacc.jjtree.JJTreeOptions;
import org.javacc.jjtree.JJTreeParserDefault;
import org.javacc.parser.Options;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.ParseException;

public class JJTree {

  private static void help_message() {
    System.out.println("Usage:");
    System.out.println("    jjtree option-settings inputfile");
    System.out.println("");
    System.out.println("\"option-settings\" is a sequence of settings separated by spaces.");
    System.out.println("Each option setting must be of one of the following forms:");
    System.out.println("");
    System.out.println("    -optionname=value (e.g., -STATIC=false)");
    System.out.println("    -optionname:value (e.g., -STATIC:false)");
    System.out.println("    -optionname       (equivalent to -optionname=true.  e.g., -STATIC)");
    System.out.println("    -NOoptionname     (equivalent to -optionname=false. e.g., -NOSTATIC)");
    System.out.println("");
    System.out.println("Option settings are not case-sensitive, so one can say \"-nOsTaTiC\" instead");
    System.out.println("of \"-NOSTATIC\".  Option values must be appropriate for the corresponding");
    System.out.println("option, and must be either an integer or a string value.");
    System.out.println("");

    System.out.println("The boolean valued options are:");
    System.out.println("");
    System.out.println("    STATIC                   (default true)");
    System.out.println("    MULTI                    (default false)");
    System.out.println("    NODE_DEFAULT_VOID        (default false)");
    System.out.println("    NODE_SCOPE_HOOK          (default false)");
    System.out.println("    TRACK_TOKENS             (default false)");
    System.out.println("    VISITOR                  (default false)");
    System.out.println("");
    System.out.println("The string valued options are:");
    System.out.println("");
    System.out.println("    JDK_VERSION              (default \"1.5\")");
    System.out.println("    NODE_CLASS               (default \"\")");
    System.out.println("    NODE_PREFIX              (default \"AST\")");
    System.out.println("    NODE_PACKAGE             (default \"\")");
    System.out.println("    NODE_EXTENDS             (default \"\")");
    System.out.println("    NODE_FACTORY             (default \"\")");
    System.out.println("    OUTPUT_FILE              (default remove input file suffix, add .jj)");
    System.out.println("    OUTPUT_DIRECTORY         (default \"\")");
    System.out.println("    VISITOR_DATA_TYPE        (default \"\")");
    System.out.println("    VISITOR_RETURN_TYPE      (default \"Object\")");
    System.out.println("    VISITOR_EXCEPTION        (default \"\")");
    System.out.println("");
    System.out.println("JJTree also accepts JavaCC options, which it inserts into the generated file.");
    System.out.println("");

    System.out.println("EXAMPLES:");
    System.out.println("    jjtree -STATIC=false mygrammar.jjt");
    System.out.println("");
    System.out.println("ABOUT JJTree:");
    System.out.println("    JJTree is a preprocessor for JavaCC that inserts actions into a");
    System.out.println("    JavaCC grammar to build parse trees for the input.");
    System.out.println("");
    System.out.println("    For more information, see the online JJTree documentation at ");
    System.out.println("    https://javacc.dev.java.net/doc/JJTree.html ");
    System.out.println("");
  }

  /**
   * A main program that exercises the parser.
   */
  public static void main(String args[]) {
    JJMain.bannerLine("Tree Builder", "");

    JavaCCContext context = new JavaCCContext();

    JJTreeOptions.init();
    JJTreeGlobals.initialize();

    if (args.length == 0) {
      System.out.println("");
      JJTree.help_message();
      System.exit(1);
    } else {
      System.out.println("(type \"jjtree\" with no arguments for help)");
    }
    String fn = args[args.length - 1];

    if (Options.isOption(fn)) {
      System.out.println("Last argument \"" + fn + "\" is not a filename");
      System.exit(1);
    }
    for (int arg = 0; arg < (args.length - 1); arg++) {
      if (!Options.isOption(args[arg])) {
        System.out.println("Argument \"" + args[arg] + "\" must be an option setting.");
        System.exit(1);
      }
      Options.setCmdLineOption(args[arg]);
    }

    JJTreeOptions.validate();

    JJMain.createOutputDir(Options.getOutputDirectory());
    File file = new File(Options.getOutputDirectory(), JJTree.create_output_file_name(fn));

    try {
      if (JJMain.isGeneratedBy("JJTree", fn)) {
        throw new IOException(fn + " was generated by jjtree.  Cannot run jjtree again.");
      }

      System.out.println("Reading from file " + fn + " . . .");

      try (Reader reader = new InputStreamReader(new FileInputStream(fn), Options.getGrammarEncoding())) {
        JJTreeGlobals.toolList = JJMain.getToolNames(fn);
        JJTreeGlobals.toolList.add("JJTree");

        JJTreeParserDefault parser = new JJTreeParserDefault(reader);
        ASTGrammar root = parser.parse();
        if (Boolean.getBoolean("jjtree-dump")) {
          root.dump(" ");
        }

        System.out.println("opt:" + context.getLanguage());

        ParserEngine engine = ParserEngine.create(context);

        try (PrintWriter writer = new PrintWriter(file)) {
          engine.generateJJTree(root, writer);
        } catch (IOException ioe) {
          System.out.println("Error setting input: " + ioe.getMessage());
          System.exit(1);
        }

        System.out.println("Annotated grammar generated successfully in " + file.toString());

      } catch (ParseException pe) {
        System.out.println("Error parsing input: " + pe.toString());
        System.exit(1);
      } catch (Exception e) {
        System.out.println("Error parsing input: " + e.toString());
        e.printStackTrace(System.out);
        System.exit(1);
      }

    } catch (IOException ioe) {
      System.out.println("Error setting input: " + ioe.getMessage());
      System.exit(1);
    }
  }

  private static String create_output_file_name(String i) {
    String o = JJTreeOptions.getOutputFile();

    if (o.equals("")) {
      int s = i.lastIndexOf(File.separatorChar);
      if (s >= 0) {
        i = i.substring(s + 1);
      }

      int di = i.lastIndexOf('.');
      if (di == -1) {
        o = i + ".jj";
      } else {
        String suffix = i.substring(di);
        if (suffix.equals(".jj")) {
          o = i + ".jj";
        } else {
          o = i.substring(0, di) + ".jj";
        }
      }
    }

    return o;
  }
}
