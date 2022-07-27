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

import org.javacc.JavaCCRequest;
import org.javacc.parser.NfaState;
import org.javacc.parser.RStringLiteral.KindInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

/**
 * The {@link LexerData} provides the request data for the lexer generator.
 */
public class LexerData {

  public final JavaCCRequest request;


  int[]     lexStates;
  String[]  lexStateNames;
  int       lexStateIndex;

  boolean[] mixed;
  int       curKind;


  int                        lohiByteCnt;
  List<NfaState>             nonAsciiTableForMethod;
  Hashtable<String, Integer> lohiByteTab;
  List<String>               allBitVectors;
  int[]                      tmpIndices;            // 2 * 256
  public int[][]             kinds;
  public int[][][]           statesForState;


  public boolean                        jjCheckNAddStatesUnaryNeeded;
  public boolean                        jjCheckNAddStatesDualNeeded;
  public int                            lastIndex;
  public final Hashtable<String, int[]> tableToDump;
  public final List<int[]>              orderedStateSet;

  boolean                               boilerPlateDumped;


  // RString
  int      maxLen;
  int      maxStrKind;
  String[] allImages;
  boolean  subString[];
  boolean  subStringAtPos[];


  int[]   maxLenForActive;
  int[][] intermediateKinds;
  int[][] intermediateMatchedPos;


  Hashtable<String, long[]>[]       statesForPos;
  List<Hashtable<String, KindInfo>> charPosKind;

  // NfaState
  private int                  generatedStates;
  private final List<NfaState> indexedAllStates;

  private int                  idCnt;
  private List<NfaState>       allStates;

  int                          dummyStateIndex;
  public boolean               done;
  public boolean               mark[];


  private final Hashtable<String, int[]> allNextStates;
  final Hashtable<String, Integer>       stateNameForComposite;
  final Hashtable<String, int[]>         compositeStateTable;
  final Hashtable<String, String>        stateBlockTable;
  final Hashtable<String, int[]>         stateSetsToFix;
  public Hashtable<String, NfaState>     equivStatesTable;

  /**
   * Constructs an instance of {@link LexerData}.
   *
   * @param request
   */
  LexerData(JavaCCRequest request) {
    this.request = request;

    this.lexStates = null;
    this.lexStateNames = null;
    this.lexStateIndex = 0;

    this.mixed = null;
    this.curKind = 0;
    this.lohiByteCnt = 0;
    this.nonAsciiTableForMethod = new ArrayList<>();
    this.lohiByteTab = new Hashtable<>();
    this.allBitVectors = new ArrayList<>();

    this.kinds = null;
    this.tmpIndices = new int[512];
    this.statesForState = null;

    this.tableToDump = new Hashtable<>();
    this.orderedStateSet = new ArrayList<>();
    this.lastIndex = 0;
    this.jjCheckNAddStatesUnaryNeeded = false;
    this.jjCheckNAddStatesDualNeeded = false;
    this.boilerPlateDumped = false;

    // RString
    this.maxLen = 0;
    this.maxStrKind = 0;
    this.allImages = null;
    this.subString = null;
    this.subStringAtPos = null;
    this.charPosKind = new ArrayList<>();
    this.maxLenForActive = new int[100]; // 6400 tokens
    this.intermediateKinds = null;
    this.intermediateMatchedPos = null;
    this.statesForPos = null;

    // NfaState
    this.generatedStates = 0;
    this.indexedAllStates = new ArrayList<>();
    this.idCnt = 0;
    this.allStates = new ArrayList<>();
    this.dummyStateIndex = -1;
    this.done = false;
    this.mark = null;
    this.allNextStates = new Hashtable<>();
    this.stateNameForComposite = new Hashtable<>();
    this.compositeStateTable = new Hashtable<>();
    this.stateBlockTable = new Hashtable<>();
    this.stateSetsToFix = new Hashtable<>();
    this.equivStatesTable = new Hashtable<>();
  }

  public final boolean ignoreCase() {
    return request.ignoreCase();
  }

  public final int getStateCount() {
    return this.lexStateNames.length;
  }

  public final int getStateIndex() {
    return this.lexStateIndex;
  }

  public final int getState(int index) {
    return this.lexStates[index];
  }

  public final String getStateName(int index) {
    return this.lexStateNames[index];
  }

  public final boolean isMixedState() {
    return mixed[lexStateIndex];
  }

  public final int getCurrentKind() {
    return curKind;
  }

  public final int getImageCount() {
    return this.allImages == null ? -1 : this.allImages.length;
  }

  public final String getImage(int index) {
    return this.allImages[index];
  }

  public final void setImage(int index, String image) {
    this.allImages[index] = image;
  }

  public final int getStateIndex(String name) {
    for (int i = 0; i < this.lexStateNames.length; i++) {
      if ((this.lexStateNames[i] != null) && this.lexStateNames[i].equals(name)) {
        return i;
      }
    }
    throw new Error(); // Should never come here
  }

  final int generatedStates() {
    return this.generatedStates;
  }

  public final NfaState getIndexedState(int index) {
    return this.indexedAllStates.get(index);
  }

  public final int addIndexedState(NfaState state) {
    this.indexedAllStates.add(state);
    return generatedStates++;
  }

  public final int getAllStateCount() {
    return this.allStates.size();
  }

  public final NfaState getAllState(int index) {
    return this.allStates.get(index);
  }

  final void setAllState(int index, NfaState state) {
    this.allStates.set(index, state);
  }

  public final Iterable<NfaState> getAllStates() {
    return this.allStates;
  }

  public final int addAllState(NfaState state) {
    this.allStates.add(state);
    return this.idCnt++;
  }

  final List<NfaState> cloneAllStates() {
    List<NfaState> v = this.allStates;
    this.allStates = new ArrayList<>(Collections.nCopies(generatedStates(), null));
    return v;
  }


  final void clearAllStates() {
    this.allStates.clear();
  }

  public final int[] getNextStates(String name) {
    return this.allNextStates.get(name);
  }

  public final void setNextStates(String name, int[] states) {
    this.allNextStates.put(name, states);
  }

  /**
   * Reset the {@link LexerData} for another cycle.
   */
  final void reset() {
    // RString
    this.maxStrKind = 0;
    this.maxLen = 0;
    this.subString = null;
    this.subStringAtPos = null;

    this.charPosKind = new ArrayList<>();
    this.maxLenForActive = new int[100]; // 6400 tokens
    this.intermediateKinds = null;
    this.intermediateMatchedPos = null;
    this.statesForPos = null;

    // NfaState
    this.generatedStates = 0;
    this.idCnt = 0;
    this.dummyStateIndex = -1;
    this.done = false;
    this.mark = null;

    this.allStates.clear();
    this.indexedAllStates.clear();
    this.equivStatesTable.clear();
    this.allNextStates.clear();
    this.compositeStateTable.clear();
    this.stateBlockTable.clear();
    this.stateNameForComposite.clear();
    this.stateSetsToFix.clear();
  }
}
