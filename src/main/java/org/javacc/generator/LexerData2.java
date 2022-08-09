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

import org.javacc.parser.Action;
import org.javacc.parser.NfaState;
import org.javacc.parser.RegularExpression;
import org.javacc.parser.TokenProduction;

import java.util.Hashtable;
import java.util.List;

/**
 * The {@link LexerData2} provides the request data for the lexer generator.
 */
public class LexerData2 {

  // Hashtable of vectors
  Hashtable<String, List<TokenProduction>> allTpsForState  = new Hashtable<>();
  int[]                                    kinds;
  String                                   lexStateSuffix;
  Hashtable<String, NfaState>              initStates      = new Hashtable<>();
  int[]                                    maxLongsReqd;
  boolean[]                                hasNfa;
  NfaState                                 initialState;
  RegularExpression                        curRE;

  public int                               maxOrdinal      = 1;
  public String[]                          newLexState;
  public boolean[]                         ignoreCase;
  public Action[]                          actions;
  public int                               stateSetSize;
  public int                               totalNumStates;
  public int                               maxLexStates;
  public NfaState[]                        singlesToSkip;
  public long[]                            toSkip;
  public long[]                            toSpecial;
  public long[]                            toMore;
  public long[]                            toToken;
  public int                               defaultLexState;
  public RegularExpression[]               rexprs;
  public int[]                             initMatch;
  public int[]                             canMatchAnyChar;
  public boolean                           hasEmptyMatch;
  public boolean[]                         canLoop;
  public boolean                           hasLoop         = false;
  public boolean[]                         canReachOnMore;
  public boolean                           hasSkipActions  = false;
  public boolean                           hasMoreActions  = false;
  public boolean                           hasTokenActions = false;
  public boolean                           hasSpecial      = false;
  public boolean                           hasSkip         = false;
  public boolean                           hasMore         = false;
  public boolean                           keepLineCol;

  /**
   * Constructs an instance of {@link LexerData2}.
   */
  LexerData2() {
    this.actions = null;
    this.allTpsForState = new Hashtable<>();
    this.canLoop = null;
    this.canMatchAnyChar = null;
    this.canReachOnMore = null;
    this.curRE = null;
    this.defaultLexState = 0;
    this.hasEmptyMatch = false;
    this.hasLoop = false;
    this.hasMore = false;
    this.hasMoreActions = false;
    this.hasNfa = null;
    this.hasSkip = false;
    this.hasSkipActions = false;
    this.hasSpecial = false;
    this.hasTokenActions = false;
    this.ignoreCase = null;
    this.initMatch = null;
    this.initStates = new Hashtable<>();
    this.initialState = null;
    this.keepLineCol = false;
    this.kinds = null;
    this.lexStateSuffix = null;
    this.maxLexStates = 0;
    this.maxLongsReqd = null;
    this.maxOrdinal = 1;
    this.newLexState = null;
    this.rexprs = null;
    this.singlesToSkip = null;
    this.stateSetSize = 0;
    this.toMore = null;
    this.toSkip = null;
    this.toSpecial = null;
    this.toToken = null;
  }
}
