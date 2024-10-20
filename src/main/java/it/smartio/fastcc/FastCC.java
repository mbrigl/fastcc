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

package it.smartio.fastcc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import it.smartio.fastcc.utils.Version;

/**
 * This package contains data created as a result of parsing and semanticizing a JavaCC input file.
 * This data is what is used by the back-ends of JavaCC as well as any other back-end of JavaCC
 * related tools such as JJTree.
 */
public interface FastCC {

  String PARSER_NAME                = "PARSER_NAME";

  String JJTREE_MULTI               = "MULTI";
  String JJTREE_NODE_TYPE           = "NODE_TYPE";
  String JJTREE_NODE_PREFIX         = "NODE_PREFIX";
  String JJTREE_NODE_PACKAGE        = "NODE_PACKAGE";
  String JJTREE_NODE_EXTENDS        = "NODE_EXTENDS";
  String JJTREE_NODE_CLASS          = "NODE_CLASS";
  String JJTREE_NODE_DEFAULT_VOID   = "NODE_DEFAULT_VOID";
  String JJTREE_NODE_SCOPE_HOOK     = "NODE_SCOPE_HOOK";
  String JJTREE_OUTPUT_FILE         = "OUTPUT_FILE";
  String JJTREE_TRACK_TOKENS        = "TRACK_TOKENS";
  String JJTREE_NODE_FACTORY        = "NODE_FACTORY";
  String JJTREE_BUILD_NODE_FILES    = "BUILD_NODE_FILES";

  String JJTREE_VISITOR             = "VISITOR";
  String JJTREE_VISITOR_EXCEPTION   = "VISITOR_EXCEPTION";
  String JJTREE_VISITOR_DATA_TYPE   = "VISITOR_DATA_TYPE";
  String JJTREE_VISITOR_RETURN_TYPE = "VISITOR_RETURN_TYPE";
  String JJTREE_VISITOR_RETURN_VOID = "VISITOR_RETURN_TYPE_VOID";


  String JJPARSER_LEGACY                  = "LEGACY";
  String JJPARSER_NO_DFA                  = "NO_DFA";
  String JJPARSER_LOOKAHEAD               = "LOOKAHEAD";
  String JJPARSER_IGNORE_CASE             = "IGNORE_CASE";
  String JJPARSER_ERROR_REPORTING         = "ERROR_REPORTING";
  String JJPARSER_DEBUG_TOKEN_MANAGER     = "DEBUG_TOKEN_MANAGER";
  String JJPARSER_DEBUG_LOOKAHEAD         = "DEBUG_LOOKAHEAD";
  String JJPARSER_DEBUG_PARSER            = "DEBUG_PARSER";
  String JJPARSER_OTHER_AMBIGUITY_CHECK   = "OTHER_AMBIGUITY_CHECK";
  String JJPARSER_CHOICE_AMBIGUITY_CHECK  = "CHOICE_AMBIGUITY_CHECK";
  String JJPARSER_CACHE_TOKENS            = "CACHE_TOKENS";
  String JJPARSER_FORCE_LA_CHECK          = "FORCE_LA_CHECK";
  String JJPARSER_SANITY_CHECK            = "SANITY_CHECK";
  String JJPARSER_OUTPUT_DIRECTORY        = "OUTPUT_DIRECTORY";
  String JJPARSER_CODEGENERATOR           = "CODE_GENERATOR";
  String JJPARSER_KEEP_LINE_COLUMN        = "KEEP_LINE_COLUMN";
  String JJPARSER_DEPTH_LIMIT             = "DEPTH_LIMIT";

  String JJPARSER_JAVA_PACKAGE            = "PACKAGE";
  String JJPARSER_JAVA_IMPORTS            = "IMPORTS";
  String JJPARSER_JAVA_EXTENDS            = "EXTENDS";
  String JJPARSER_JAVA_LEXER              = "LEXER";

  String JJPARSER_CPP_NAMESPACE           = "NAMESPACE";
  String JJPARSER_CPP_STOP_ON_FIRST_ERROR = "STOP_ON_FIRST_ERROR";
  String JJPARSER_CPP_STACK_LIMIT         = "STACK_LIMIT";


  Version VERSION = FastCC.load();

  static Version load() {
    String major = "??";
    String minor = "??";
    String patch = "??";

    try (InputStream is = FastCC.class.getResourceAsStream("/version.properties")) {
      Properties properties = new Properties();
      properties.load(is);
      major = properties.getProperty("version.major", major);
      minor = properties.getProperty("version.minor", minor);
      patch = properties.getProperty("version.patch", patch);
    } catch (IOException e) {
      System.err.println("Could not read version.properties: " + e);
    }
    return Version.of(Integer.parseInt(major), Integer.parseInt(minor), patch.equals("") ? 0 : Integer.parseInt(patch));
  }
}
