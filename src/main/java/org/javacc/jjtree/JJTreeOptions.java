// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

/* Copyright (c) 2005-2006, Kees Jan Koster kjkoster@kjkoster.org
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.javacc.jjtree;

import org.javacc.JavaCC;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.Options;

/**
 * The JJTree-specific options.
 *
 * @author Kees Jan Koster &lt;kjkoster@kjkoster.org&gt;
 */
public class JJTreeOptions extends Options {

  /**
   * Limit subclassing to derived classes.
   */
  protected JJTreeOptions() {}

  /**
   * Initialize the JJTree-specific options.
   */
  public static void init() {
    Options.init();
    Options.optionValues.put(JavaCC.JJTREE_MULTI, Boolean.FALSE);
    Options.optionValues.put(JavaCC.JJTREE_NODE_DEFAULT_VOID, Boolean.FALSE);
    Options.optionValues.put(JavaCC.JJTREE_NODE_SCOPE_HOOK, Boolean.FALSE);
    Options.optionValues.put(JavaCC.JJTREE_BUILD_NODE_FILES, Boolean.TRUE);
    Options.optionValues.put(JavaCC.JJTREE_VISITOR, Boolean.FALSE);
    Options.optionValues.put(JavaCC.JJTREE_TRACK_TOKENS, Boolean.FALSE);
    Options.optionValues.put(JavaCC.JJTREE_NODE_PREFIX, "AST");
    Options.optionValues.put(JavaCC.JJTREE_NODE_PACKAGE, "");
    Options.optionValues.put(JavaCC.JJTREE_NODE_EXTENDS, "");
    Options.optionValues.put(JavaCC.JJTREE_NODE_CLASS, "");
    Options.optionValues.put(JavaCC.JJTREE_NODE_FACTORY, "");
    Options.optionValues.put(JavaCC.JJTREE_OUTPUT_FILE, "");
    Options.optionValues.put(JavaCC.JJTREE_VISITOR_DATA_TYPE, "");
    Options.optionValues.put(JavaCC.JJTREE_VISITOR_RETURN_TYPE, "Object");
    Options.optionValues.put(JavaCC.JJTREE_VISITOR_EXCEPTION, "");

    // Also appears to be a duplicate
    Options.optionValues.put(JavaCC.JJPARSER_CPP_NAMESPACE, "");
  }

  /**
   * Check options for consistency
   */
  public static void validate() {
    if (!JJTreeOptions.getVisitor()) {
      if (JJTreeOptions.getVisitorDataType().length() > 0) {
        JavaCCErrors.warning("VISITOR_DATA_TYPE option will be ignored since VISITOR is false");
      }
      if ((JJTreeOptions.getVisitorReturnType().length() > 0)
          && !JJTreeOptions.getVisitorReturnType().equals("Object")) {
        JavaCCErrors.warning("VISITOR_RETURN_TYPE option will be ignored since VISITOR is false");
      }
      if (JJTreeOptions.getVisitorException().length() > 0) {
        JavaCCErrors.warning("VISITOR_EXCEPTION option will be ignored since VISITOR is false");
      }
    }
  }

  /**
   * Find the multi value.
   */
  public static boolean getMulti() {
    return Options.booleanValue(JavaCC.JJTREE_MULTI);
  }

  /**
   * Find the node default void value.
   */
  static boolean getNodeDefaultVoid() {
    return Options.booleanValue(JavaCC.JJTREE_NODE_DEFAULT_VOID);
  }

  /**
   * Find the node scope hook value.
   */
  public static boolean getNodeScopeHook() {
    return Options.booleanValue(JavaCC.JJTREE_NODE_SCOPE_HOOK);
  }

  /**
   * Find the node factory value.
   */
  public static String getNodeFactory() {
    return Options.stringValue(JavaCC.JJTREE_NODE_FACTORY);
  }

  /**
   * Find the build node files value.
   */
  public static boolean getBuildNodeFiles() {
    return Options.booleanValue(JavaCC.JJTREE_BUILD_NODE_FILES);
  }

  /**
   * Find the visitor value.
   */
  public static boolean getVisitor() {
    return Options.booleanValue(JavaCC.JJTREE_VISITOR);
  }

  /**
   * Find the trackTokens value.
   */
  public static boolean getTrackTokens() {
    return Options.booleanValue(JavaCC.JJTREE_TRACK_TOKENS);
  }

  /**
   * Find the node prefix value.
   */
  public static String getNodePrefix() {
    return Options.stringValue(JavaCC.JJTREE_NODE_PREFIX);
  }


  /**
   * Find the node class name.
   */
  public static String getNodeClass() {
    return Options.stringValue(JavaCC.JJTREE_NODE_CLASS);
  }

  /**
   * Find the node package value.
   */
  static String getNodePackage() {
    return Options.stringValue(JavaCC.JJTREE_NODE_PACKAGE);
  }

  /**
   * Find the output file value.
   */
  public static String getOutputFile() {
    return Options.stringValue(JavaCC.JJTREE_OUTPUT_FILE);
  }

  /**
   * Find the visitor exception value
   */
  public static String getVisitorException() {
    return Options.stringValue(JavaCC.JJTREE_VISITOR_EXCEPTION);
  }

  /**
   * Find the visitor data type value
   */
  public static String getVisitorDataType() {
    return Options.stringValue(JavaCC.JJTREE_VISITOR_DATA_TYPE);
  }

  /**
   * Find the visitor return type value
   */
  public static String getVisitorReturnType() {
    return Options.stringValue(JavaCC.JJTREE_VISITOR_RETURN_TYPE);
  }
}
