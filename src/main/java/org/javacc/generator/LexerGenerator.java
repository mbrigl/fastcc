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

import org.fastcc.source.CppWriter;
import org.fastcc.source.SourceWriter;
import org.fastcc.utils.Encoding;
import org.javacc.JavaCCContext;
import org.javacc.JavaCCRequest;
import org.javacc.parser.Action;
import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.Nfa;
import org.javacc.parser.NfaState;
import org.javacc.parser.Options;
import org.javacc.parser.RChoice;
import org.javacc.parser.RStringLiteral;
import org.javacc.parser.RStringLiteral.KindInfo;
import org.javacc.parser.RegExprSpec;
import org.javacc.parser.RegularExpression;
import org.javacc.parser.TokenProduction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

/**
 * The {@link LexerGenerator} class.
 */
public abstract class LexerGenerator extends CodeGenerator {

  // Hashtable of vectors
  private Hashtable<String, List<TokenProduction>> allTpsForState  = new Hashtable<>();
  private int[]                                    kinds;
  private String                                   lexStateSuffix;
  private Hashtable<String, NfaState>              initStates      = new Hashtable<>();
  private int[]                                    maxLongsReqd;
  private boolean[]                                hasNfa;
  private NfaState                                 initialState;
  private RegularExpression                        curRE;

  protected int                                    maxOrdinal      = 1;
  protected String[]                               newLexState;
  protected boolean[]                              ignoreCase;
  protected Action[]                               actions;
  protected int                                    stateSetSize;
  protected int                                    totalNumStates;
  protected int                                    maxLexStates;
  protected NfaState[]                             singlesToSkip;
  protected long[]                                 toSkip;
  protected long[]                                 toSpecial;
  protected long[]                                 toMore;
  protected long[]                                 toToken;
  protected int                                    defaultLexState;
  protected RegularExpression[]                    rexprs;
  protected int[]                                  initMatch;
  protected int[]                                  canMatchAnyChar;
  protected boolean                                hasEmptyMatch;
  protected boolean[]                              canLoop;
  protected boolean                                hasLoop         = false;
  protected boolean[]                              canReachOnMore;
  protected boolean                                hasSkipActions  = false;
  protected boolean                                hasMoreActions  = false;
  protected boolean                                hasTokenActions = false;
  protected boolean                                hasSpecial      = false;
  protected boolean                                hasSkip         = false;
  protected boolean                                hasMore         = false;
  protected boolean                                keepLineCol;


  private final LexerData data;

  /**
   * Constructs an instance of {@link CodeGenerator}.
   */
  protected LexerGenerator(SourceWriter source, JavaCCRequest request, JavaCCContext context) {
    super(source, context.getLanguage());
    this.data = new LexerData(request);
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

  protected final void writeTemplate(String name, Map<String, Object> additionalOptions) throws IOException {
    addOption("maxOrdinal", Integer.valueOf(this.maxOrdinal));
    addOption("maxLexStates", Integer.valueOf(this.maxLexStates));
    addOption("hasEmptyMatch", Boolean.valueOf(this.hasEmptyMatch));
    addOption("hasSkip", Boolean.valueOf(this.hasSkip));
    addOption("hasMore", Boolean.valueOf(this.hasMore));
    addOption("hasSpecial", Boolean.valueOf(this.hasSpecial));
    addOption("hasMoreActions", Boolean.valueOf(this.hasMoreActions));
    addOption("hasSkipActions", Boolean.valueOf(this.hasSkipActions));
    addOption("hasTokenActions", Boolean.valueOf(this.hasTokenActions));
    addOption("stateSetSize", this.stateSetSize);
    addOption("hasActions", this.hasMoreActions || this.hasSkipActions || this.hasTokenActions);
    addOption("tokMgrClassName", getTokenManager());
    int x = 0;
    for (int l : this.maxLongsReqd) {
      x = Math.max(x, l);
    }
    addOption("maxLongs", x);
    addOption("cu_name", getLexerData().request.getParserName());

    additionalOptions.entrySet().forEach(e -> addOption(e.getKey(), e.getValue()));

    getSource().writeTemplate(name);
  }

  protected final LexerData getLexerData() {
    return this.data;
  }

  protected final String getTokenManager() {
    return getLexerData().request.getParserName() + "TokenManager";
  }

  private void AddCharToSkip(char c, int kind) {
    this.singlesToSkip[getLexerData().getStateIndex()].AddChar(c);
    this.singlesToSkip[getLexerData().getStateIndex()].kind = kind;
  }

  // --------------------------------------- RString

  private void GenerateNfaStartStates(NfaState initialState) {
    boolean[] seen = new boolean[getLexerData().generatedStates()];
    Hashtable<String, String> stateSets = new Hashtable<>();
    String stateSetString = "";
    int i, j, kind, jjmatchedPos = 0;
    int maxKindsReqd = (getLexerData().maxStrKind / 64) + 1;
    long[] actives;
    List<NfaState> newStates = new ArrayList<>();
    List<NfaState> oldStates = null, jjtmpStates;

    getLexerData().statesForPos = new Hashtable[getLexerData().maxLen];
    getLexerData().intermediateKinds = new int[getLexerData().maxStrKind + 1][];
    getLexerData().intermediateMatchedPos = new int[getLexerData().maxStrKind + 1][];

    for (i = 0; i < getLexerData().maxStrKind; i++) {
      if (getLexerData().getState(i) != getLexerData().getStateIndex()) {
        continue;
      }

      String image = getLexerData().getImage(i);

      if ((image == null) || (image.length() < 1)) {
        continue;
      }

      try {
        if (((oldStates = (List<NfaState>) initialState.epsilonMoves.clone()) == null) || (oldStates.size() == 0)) {
          DumpNfaStartStatesCode(getLexerData().statesForPos);
          return;
        }
      } catch (Exception e) {
        JavaCCErrors.semantic_error("Error cloning state vector");
      }

      getLexerData().intermediateKinds[i] = new int[image.length()];
      getLexerData().intermediateMatchedPos[i] = new int[image.length()];
      jjmatchedPos = 0;
      kind = Integer.MAX_VALUE;

      for (j = 0; j < image.length(); j++) {
        if ((oldStates == null) || (oldStates.size() <= 0)) {
          // Here, j > 0
          kind = getLexerData().intermediateKinds[i][j] = getLexerData().intermediateKinds[i][j - 1];
          jjmatchedPos = getLexerData().intermediateMatchedPos[i][j] = getLexerData().intermediateMatchedPos[i][j - 1];
        } else {
          kind = NfaState.MoveFromSet(image.charAt(j), oldStates, newStates);
          oldStates.clear();

          if ((j == 0) && (kind != Integer.MAX_VALUE) && (this.canMatchAnyChar[getLexerData().getStateIndex()] != -1)
              && (kind > this.canMatchAnyChar[getLexerData().getStateIndex()])) {
            kind = this.canMatchAnyChar[getLexerData().getStateIndex()];
          }

          if (GetStrKind(image.substring(0, j + 1)) < kind) {
            getLexerData().intermediateKinds[i][j] = kind = Integer.MAX_VALUE;
            jjmatchedPos = 0;
          } else if (kind != Integer.MAX_VALUE) {
            getLexerData().intermediateKinds[i][j] = kind;
            jjmatchedPos = getLexerData().intermediateMatchedPos[i][j] = j;
          } else if (j == 0) {
            kind = getLexerData().intermediateKinds[i][j] = Integer.MAX_VALUE;
          } else {
            kind = getLexerData().intermediateKinds[i][j] = getLexerData().intermediateKinds[i][j - 1];
            jjmatchedPos =
                getLexerData().intermediateMatchedPos[i][j] = getLexerData().intermediateMatchedPos[i][j - 1];
          }

          stateSetString = GetStateSetString(newStates);
        }

        if ((kind == Integer.MAX_VALUE) && ((newStates == null) || (newStates.size() == 0))) {
          continue;
        }

        int p;
        if (stateSets.get(stateSetString) == null) {
          stateSets.put(stateSetString, stateSetString);
          for (p = 0; p < newStates.size(); p++) {
            if (seen[newStates.get(p).stateName]) {
              newStates.get(p).inNextOf++;
            } else {
              seen[newStates.get(p).stateName] = true;
            }
          }
        } else {
          for (p = 0; p < newStates.size(); p++) {
            seen[newStates.get(p).stateName] = true;
          }
        }

        jjtmpStates = oldStates;
        oldStates = newStates;
        (newStates = jjtmpStates).clear();

        if (getLexerData().statesForPos[j] == null) {
          getLexerData().statesForPos[j] = new Hashtable<>();
        }

        if ((actives =
            (getLexerData().statesForPos[j].get(kind + ", " + jjmatchedPos + ", " + stateSetString))) == null) {
          actives = new long[maxKindsReqd];
          getLexerData().statesForPos[j].put(kind + ", " + jjmatchedPos + ", " + stateSetString, actives);
        }

        actives[i / 64] |= 1L << (i % 64);
        // String name = NfaState.StoreStateSet(stateSetString);
      }
    }

    DumpNfaStartStatesCode(getLexerData().statesForPos);
  }

  private final void BuildLexStatesTable() {
    Iterator<TokenProduction> it = getLexerData().request.getTokenProductions().iterator();
    TokenProduction tp;
    int i;

    String[] tmpLexStateName = new String[getLexerData().request.getStateCount()];
    while (it.hasNext()) {
      tp = it.next();
      List<RegExprSpec> respecs = tp.respecs;
      List<TokenProduction> tps;

      for (i = 0; i < tp.lexStates.length; i++) {
        if ((tps = this.allTpsForState.get(tp.lexStates[i])) == null) {
          tmpLexStateName[this.maxLexStates++] = tp.lexStates[i];
          this.allTpsForState.put(tp.lexStates[i], tps = new ArrayList<>());
        }

        tps.add(tp);
      }

      if ((respecs == null) || (respecs.size() == 0)) {
        continue;
      }

      RegularExpression re;
      for (i = 0; i < respecs.size(); i++) {
        if (this.maxOrdinal <= (re = respecs.get(i).rexp).ordinal) {
          this.maxOrdinal = re.ordinal + 1;
        }
      }
    }

    this.kinds = new int[this.maxOrdinal];
    this.toSkip = new long[(this.maxOrdinal / 64) + 1];
    this.toSpecial = new long[(this.maxOrdinal / 64) + 1];
    this.toMore = new long[(this.maxOrdinal / 64) + 1];
    this.toToken = new long[(this.maxOrdinal / 64) + 1];
    this.toToken[0] = 1L;
    this.actions = new Action[this.maxOrdinal];
    this.actions[0] = getLexerData().request.getActionForEof();
    this.hasTokenActions = getLexerData().request.getActionForEof() != null;
    this.initStates = new Hashtable<>();
    this.canMatchAnyChar = new int[this.maxLexStates];
    this.canLoop = new boolean[this.maxLexStates];
    getLexerData().lexStateNames = new String[this.maxLexStates];
    this.singlesToSkip = new NfaState[this.maxLexStates];
    System.arraycopy(tmpLexStateName, 0, getLexerData().lexStateNames, 0, this.maxLexStates);

    for (i = 0; i < this.maxLexStates; i++) {
      this.canMatchAnyChar[i] = -1;
    }

    this.hasNfa = new boolean[this.maxLexStates];
    getLexerData().mixed = new boolean[this.maxLexStates];
    this.maxLongsReqd = new int[this.maxLexStates];
    this.initMatch = new int[this.maxLexStates];
    this.newLexState = new String[this.maxOrdinal];
    this.newLexState[0] = getLexerData().request.getNextStateForEof();
    this.hasEmptyMatch = false;
    getLexerData().lexStates = new int[this.maxOrdinal];
    this.ignoreCase = new boolean[this.maxOrdinal];
    this.rexprs = new RegularExpression[this.maxOrdinal];
    getLexerData().allImages = new String[this.maxOrdinal];
    this.canReachOnMore = new boolean[this.maxLexStates];
  }

  public final void start() throws IOException {
    if (JavaCCErrors.hasError()) {
      return;
    }

    this.keepLineCol = Options.getKeepLineColumn();
    List<RegularExpression> choices = new ArrayList<>();
    TokenProduction tp;
    int i, j;

    BuildLexStatesTable();
    PrintClassHead();

    Enumeration<String> e = this.allTpsForState.keys();

    boolean ignoring = false;

    while (e.hasMoreElements()) {
      getLexerData().reset();

      String key = e.nextElement();

      getLexerData().lexStateIndex = getLexerData().getStateIndex(key);
      this.lexStateSuffix = "_" + getLexerData().getStateIndex();
      List<TokenProduction> allTps = this.allTpsForState.get(key);
      this.initStates.put(key, this.initialState = new NfaState(getLexerData()));
      ignoring = false;

      this.singlesToSkip[getLexerData().getStateIndex()] = new NfaState(getLexerData());
      this.singlesToSkip[getLexerData().getStateIndex()].dummy = true;

      if (key.equals("DEFAULT")) {
        this.defaultLexState = getLexerData().getStateIndex();
      }

      for (i = 0; i < allTps.size(); i++) {
        tp = allTps.get(i);
        int kind = tp.kind;
        boolean ignore = tp.ignoreCase;
        List<RegExprSpec> rexps = tp.respecs;

        if (i == 0) {
          ignoring = ignore;
        }

        for (j = 0; j < rexps.size(); j++) {
          RegExprSpec respec = rexps.get(j);
          this.curRE = respec.rexp;

          this.rexprs[getLexerData().curKind = this.curRE.ordinal] = this.curRE;
          getLexerData().lexStates[this.curRE.ordinal] = getLexerData().getStateIndex();
          this.ignoreCase[this.curRE.ordinal] = ignore;

          if (this.curRE.private_rexp) {
            this.kinds[this.curRE.ordinal] = -1;
            continue;
          }

          if (!Options.getNoDfa() && (this.curRE instanceof RStringLiteral)
              && !((RStringLiteral) this.curRE).image.equals("")) {
            GenerateDfa(((RStringLiteral) this.curRE), this.curRE.ordinal);
            if ((i != 0) && !getLexerData().isMixedState() && (ignoring != ignore)) {
              getLexerData().mixed[getLexerData().getStateIndex()] = true;
            }
          } else if (this.curRE.CanMatchAnyChar()) {
            if ((this.canMatchAnyChar[getLexerData().getStateIndex()] == -1)
                || (this.canMatchAnyChar[getLexerData().getStateIndex()] > this.curRE.ordinal)) {
              this.canMatchAnyChar[getLexerData().getStateIndex()] = this.curRE.ordinal;
            }
          } else {
            Nfa temp;

            if (this.curRE instanceof RChoice) {
              choices.add(this.curRE);
            }

            temp = this.curRE.GenerateNfa(getLexerData(), ignore);
            temp.end.isFinal = true;
            temp.end.kind = this.curRE.ordinal;
            this.initialState.AddMove(temp.start);
          }

          if (this.kinds.length < this.curRE.ordinal) {
            int[] tmp = new int[this.curRE.ordinal + 1];

            System.arraycopy(this.kinds, 0, tmp, 0, this.kinds.length);
            this.kinds = tmp;
          }
          // System.out.println(" ordina : " + curRE.ordinal);

          this.kinds[this.curRE.ordinal] = kind;

          if ((respec.nextState != null)
              && !respec.nextState.equals(getLexerData().getStateName(getLexerData().getStateIndex()))) {
            this.newLexState[this.curRE.ordinal] = respec.nextState;
          }

          if ((respec.act != null) && (respec.act.getActionTokens() != null)
              && (respec.act.getActionTokens().size() > 0)) {
            this.actions[this.curRE.ordinal] = respec.act;
          }

          switch (kind) {
            case TokenProduction.SPECIAL:
              this.hasSkipActions |=
                  (this.actions[this.curRE.ordinal] != null) || (this.newLexState[this.curRE.ordinal] != null);
              this.hasSpecial = true;
              this.toSpecial[this.curRE.ordinal / 64] |= 1L << (this.curRE.ordinal % 64);
              this.toSkip[this.curRE.ordinal / 64] |= 1L << (this.curRE.ordinal % 64);
              break;
            case TokenProduction.SKIP:
              this.hasSkipActions |= (this.actions[this.curRE.ordinal] != null);
              this.hasSkip = true;
              this.toSkip[this.curRE.ordinal / 64] |= 1L << (this.curRE.ordinal % 64);
              break;
            case TokenProduction.MORE:
              this.hasMoreActions |= (this.actions[this.curRE.ordinal] != null);
              this.hasMore = true;
              this.toMore[this.curRE.ordinal / 64] |= 1L << (this.curRE.ordinal % 64);

              if (this.newLexState[this.curRE.ordinal] != null) {
                this.canReachOnMore[getLexerData().getStateIndex(this.newLexState[this.curRE.ordinal])] = true;
              } else {
                this.canReachOnMore[getLexerData().getStateIndex()] = true;
              }

              break;
            case TokenProduction.TOKEN:
              this.hasTokenActions |= (this.actions[this.curRE.ordinal] != null);
              this.toToken[this.curRE.ordinal / 64] |= 1L << (this.curRE.ordinal % 64);
              break;
          }
        }
      }

      // Generate a static block for initializing the nfa transitions
      NfaState.ComputeClosures(getLexerData());

      for (i = 0; i < this.initialState.epsilonMoves.size(); i++) {
        this.initialState.epsilonMoves.elementAt(i).GenerateCode();
      }

      this.hasNfa[getLexerData().getStateIndex()] = (getLexerData().generatedStates() != 0);
      if (this.hasNfa[getLexerData().getStateIndex()]) {
        this.initialState.GenerateCode();
        GenerateInitMoves(this.initialState);
      }

      if ((this.initialState.kind != Integer.MAX_VALUE) && (this.initialState.kind != 0)) {
        if (((this.toSkip[this.initialState.kind / 64] & (1L << this.initialState.kind)) != 0L)
            || ((this.toSpecial[this.initialState.kind / 64] & (1L << this.initialState.kind)) != 0L)) {
          this.hasSkipActions = true;
        } else if ((this.toMore[this.initialState.kind / 64] & (1L << this.initialState.kind)) != 0L) {
          this.hasMoreActions = true;
        } else {
          this.hasTokenActions = true;
        }

        if ((this.initMatch[getLexerData().getStateIndex()] == 0)
            || (this.initMatch[getLexerData().getStateIndex()] > this.initialState.kind)) {
          this.initMatch[getLexerData().getStateIndex()] = this.initialState.kind;
          this.hasEmptyMatch = true;
        }
      } else if (this.initMatch[getLexerData().getStateIndex()] == 0) {
        this.initMatch[getLexerData().getStateIndex()] = Integer.MAX_VALUE;
      }

      FillSubString();

      if (this.hasNfa[getLexerData().getStateIndex()] && !getLexerData().isMixedState()) {
        GenerateNfaStartStates(this.initialState);
      }

      DumpDfaCode();

      if (this.hasNfa[getLexerData().getStateIndex()]) {
        DumpMoveNfa();
      }

      this.totalNumStates += getLexerData().generatedStates();
      if (this.stateSetSize < getLexerData().generatedStates()) {
        this.stateSetSize = getLexerData().generatedStates();
      }
    }

    for (i = 0; i < choices.size(); i++) {
      ((RChoice) choices.get(i)).CheckUnmatchability(getLexerData());
    }

    dumpAll();
  }

  protected abstract void PrintClassHead();

  protected abstract void dumpAll() throws IOException;

  private void DumpNfaStartStatesCode(Hashtable<String, long[]>[] statesForPos) {
    if (getLexerData().maxStrKind == 0) { // No need to generate this function
      return;
    }

    int i, maxKindsReqd = (getLexerData().maxStrKind / 64) + 1;
    boolean condGenerated = false;
    int ind = 0;

    StringBuilder params = new StringBuilder();
    for (i = 0; i < (maxKindsReqd - 1); i++) {
      params.append("" + getLongType() + " active" + i + ", ");
    }
    params.append("" + getLongType() + " active" + i + ")");

    // TODO :: CBA -- Require Unification of output language specific processing into a single Enum
    // class
    if (isJavaLanguage()) {
      genCode("private final int jjStopStringLiteralDfa" + this.lexStateSuffix + "(int pos, " + params);
    } else if (isCppLanguage()) {
      generateMethodDefHeaderCpp(" int", "jjStopStringLiteralDfa" + this.lexStateSuffix + "(int pos, " + params);
    } else {
      throw new RuntimeException("Output language type not fully implemented : " + getLanguage());
    }

    genCodeLine("{");

    if (Options.getDebugTokenManager()) {
      // TODO :: CBA -- Require Unification of output language specific processing into a single
      // Enum class
      if (isJavaLanguage()) {
        genCodeLine("      debugStream.println(\"   No more string literal token matches are possible.\");");
      } else if (isCppLanguage()) {
        genCodeLine("      fprintf(debugStream, \"   No more string literal token matches are possible.\");");
      } else {
        throw new RuntimeException("Output language type not fully implemented : " + getLanguage());
      }
    }

    genCodeLine("   switch (pos)");
    genCodeLine("   {");

    for (i = 0; i < (getLexerData().maxLen - 1); i++) {
      if (statesForPos[i] == null) {
        continue;
      }

      genCodeLine("      case " + i + ":");

      Enumeration<String> e = statesForPos[i].keys();
      while (e.hasMoreElements()) {
        String stateSetString = e.nextElement();
        long[] actives = statesForPos[i].get(stateSetString);

        for (int j = 0; j < maxKindsReqd; j++) {
          if (actives[j] == 0L) {
            continue;
          }

          if (condGenerated) {
            genCode(" || ");
          } else {
            genCode("         if (");
          }

          condGenerated = true;

          genCode("(active" + j + " & 0x" + Long.toHexString(actives[j]) + "L) != 0L");
        }

        if (condGenerated) {
          genCodeLine(")");

          String kindStr = stateSetString.substring(0, ind = stateSetString.indexOf(", "));
          String afterKind = stateSetString.substring(ind + 2);
          int jjmatchedPos = Integer.parseInt(afterKind.substring(0, afterKind.indexOf(", ")));

          if (!kindStr.equals(String.valueOf(Integer.MAX_VALUE))) {
            genCodeLine("         {");
          }

          if (!kindStr.equals(String.valueOf(Integer.MAX_VALUE))) {
            if (i == 0) {
              genCodeLine("            jjmatchedKind = " + kindStr + ";");

              if (((this.initMatch[getLexerData().getStateIndex()] != 0)
                  && (this.initMatch[getLexerData().getStateIndex()] != Integer.MAX_VALUE))) {
                genCodeLine("            jjmatchedPos = 0;");
              }
            } else if (i == jjmatchedPos) {
              if (getLexerData().subStringAtPos[i]) {
                genCodeLine("            if (jjmatchedPos != " + i + ")");
                genCodeLine("            {");
                genCodeLine("               jjmatchedKind = " + kindStr + ";");
                genCodeLine("               jjmatchedPos = " + i + ";");
                genCodeLine("            }");
              } else {
                genCodeLine("            jjmatchedKind = " + kindStr + ";");
                genCodeLine("            jjmatchedPos = " + i + ";");
              }
            } else {
              if (jjmatchedPos > 0) {
                genCodeLine("            if (jjmatchedPos < " + jjmatchedPos + ")");
              } else {
                genCodeLine("            if (jjmatchedPos == 0)");
              }
              genCodeLine("            {");
              genCodeLine("               jjmatchedKind = " + kindStr + ";");
              genCodeLine("               jjmatchedPos = " + jjmatchedPos + ";");
              genCodeLine("            }");
            }
          }

          kindStr = stateSetString.substring(0, ind = stateSetString.indexOf(", "));
          afterKind = stateSetString.substring(ind + 2);
          stateSetString = afterKind.substring(afterKind.indexOf(", ") + 2);

          if (stateSetString.equals("null;")) {
            genCodeLine("            return -1;");
          } else {
            genCodeLine("            return " + AddCompositeStateSet(stateSetString, true) + ";");
          }

          if (!kindStr.equals(String.valueOf(Integer.MAX_VALUE))) {
            genCodeLine("         }");
          }
          condGenerated = false;
        }
      }

      genCodeLine("         return -1;");
    }

    genCodeLine("      default :");
    genCodeLine("         return -1;");
    genCodeLine("   }");
    genCodeLine("}");

    params.setLength(0);
    params.append("(int pos, ");
    for (i = 0; i < (maxKindsReqd - 1); i++) {
      params.append(getLongType() + " active" + i + ", ");
    }
    params.append(getLongType() + " active" + i + ")");

    if (isJavaLanguage()) {
      genCode("private final int jjStartNfa" + this.lexStateSuffix + params);
    } else {
      generateMethodDefHeaderCpp("int ", "jjStartNfa" + this.lexStateSuffix + params);
    }
    genCodeLine("{");

    if (getLexerData().isMixedState()) {
      if (getLexerData().generatedStates() != 0) {
        genCodeLine("   return jjMoveNfa" + this.lexStateSuffix + "(" + InitStateName() + ", pos + 1);");
      } else {
        genCodeLine("   return pos + 1;");
      }

      genCodeLine("}");
      return;
    }

    genCode(
        "   return jjMoveNfa" + this.lexStateSuffix + "(" + "jjStopStringLiteralDfa" + this.lexStateSuffix + "(pos, ");
    for (i = 0; i < (maxKindsReqd - 1); i++) {
      genCode("active" + i + ", ");
    }
    genCode("active" + i + ")");
    genCodeLine(", pos + 1);");
    genCodeLine("}");
  }


  private int GetStrKind(String str) {
    for (int i = 0; i < getLexerData().maxStrKind; i++) {
      if (getLexerData().getState(i) != getLexerData().getStateIndex()) {
        continue;
      }

      String image = getLexerData().allImages[i];
      if ((image != null) && image.equals(str)) {
        return i;
      }
    }

    return Integer.MAX_VALUE;
  }

  private String GetStateSetString(List<NfaState> states) {
    if ((states == null) || (states.size() == 0)) {
      return "null;";
    }

    int[] set = new int[states.size()];
    String retVal = "{ ";
    for (int i = 0; i < states.size();) {
      int k;
      retVal += (k = states.get(i).stateName) + ", ";
      set[i] = k;

      if ((i++ > 0) && ((i % 16) == 0)) {
        retVal += "\n";
      }
    }

    retVal += "};";
    getLexerData().setNextStates(retVal, set);
    return retVal;
  }

  private void DumpDfaCode() {
    Hashtable<String, ?> tab;
    String key;
    KindInfo info;
    int maxLongsReqd = (getLexerData().maxStrKind / 64) + 1;
    int i, j, k;
    boolean ifGenerated;
    this.maxLongsReqd[getLexerData().getStateIndex()] = maxLongsReqd;

    if (getLexerData().maxLen == 0) {
      // TODO :: CBA -- Require Unification of output language specific processing into a single
      // Enum class
      if (isJavaLanguage()) {
        genCodeLine("private int " + "jjMoveStringLiteralDfa0" + this.lexStateSuffix + "()");
      } else if (isCppLanguage()) {
        generateMethodDefHeaderCpp(" int ", "jjMoveStringLiteralDfa0" + this.lexStateSuffix + "()");
      } else {
        throw new RuntimeException("Output language type not fully implemented : " + getLanguage());
      }
      DumpNullStrLiterals();
      return;
    }

    if (!getLexerData().boilerPlateDumped) {
      DumpBoilerPlate();
      getLexerData().boilerPlateDumped = true;
    }

    boolean createStartNfa = false;
    for (i = 0; i < getLexerData().maxLen; i++) {
      boolean atLeastOne = false;
      boolean startNfaNeeded = false;
      tab = getLexerData().charPosKind.get(i);
      String[] keys = LexerGenerator.ReArrange(tab);

      StringBuilder params = new StringBuilder();
      params.append("(");
      if (i != 0) {
        if (i == 1) {
          for (j = 0; j < (maxLongsReqd - 1); j++) {
            if (i <= getLexerData().maxLenForActive[j]) {
              if (atLeastOne) {
                params.append(", ");
              } else {
                atLeastOne = true;
              }
              params.append(getLongType() + " active" + j);
            }
          }

          if (i <= getLexerData().maxLenForActive[j]) {
            if (atLeastOne) {
              params.append(", ");
            }
            params.append(getLongType() + " active" + j);
          }
        } else {
          for (j = 0; j < (maxLongsReqd - 1); j++) {
            if (i <= (getLexerData().maxLenForActive[j] + 1)) {
              if (atLeastOne) {
                params.append(", ");
              } else {
                atLeastOne = true;
              }
              params.append(getLongType() + " old" + j + ", " + getLongType() + " active" + j);
            }
          }

          if (i <= (getLexerData().maxLenForActive[j] + 1)) {
            if (atLeastOne) {
              params.append(", ");
            }
            params.append(getLongType() + " old" + j + ", " + getLongType() + " active" + j);
          }
        }
      }
      params.append(")");

      // TODO :: CBA -- Require Unification of output language specific processing into a single
      // Enum class
      if (isJavaLanguage()) {
        genCode("private int " + "jjMoveStringLiteralDfa" + i + this.lexStateSuffix + params);
      } else if (isCppLanguage()) {
        generateMethodDefHeaderCpp(" int ", "jjMoveStringLiteralDfa" + i + this.lexStateSuffix + params);
      } else {
        throw new RuntimeException("Output language type not fully implemented : " + getLanguage());
      }

      genCodeLine("{");

      if (i != 0) {
        if (i > 1) {
          atLeastOne = false;
          genCode("   if ((");

          for (j = 0; j < (maxLongsReqd - 1); j++) {
            if (i <= (getLexerData().maxLenForActive[j] + 1)) {
              if (atLeastOne) {
                genCode(" | ");
              } else {
                atLeastOne = true;
              }
              genCode("(active" + j + " &= old" + j + ")");
            }
          }

          if (i <= (getLexerData().maxLenForActive[j] + 1)) {
            if (atLeastOne) {
              genCode(" | ");
            }
            genCode("(active" + j + " &= old" + j + ")");
          }

          genCodeLine(") == 0L)");
          if (!getLexerData().isMixedState() && (getLexerData().generatedStates() != 0)) {
            genCode("      return jjStartNfa" + this.lexStateSuffix + "(" + (i - 2) + ", ");
            for (j = 0; j < (maxLongsReqd - 1); j++) {
              if (i <= (getLexerData().maxLenForActive[j] + 1)) {
                genCode("old" + j + ", ");
              } else {
                genCode("0L, ");
              }
            }
            if (i <= (getLexerData().maxLenForActive[j] + 1)) {
              genCodeLine("old" + j + ");");
            } else {
              genCodeLine("0L);");
            }
          } else if (getLexerData().generatedStates() != 0) {
            genCodeLine("      return jjMoveNfa" + this.lexStateSuffix + "(" + InitStateName() + ", " + (i - 1) + ");");
          } else {
            genCodeLine("      return " + i + ";");
          }
        }

        if ((i != 0) && Options.getDebugTokenManager()) {
          if (isJavaLanguage()) {
            genCodeLine(
                "   if (jjmatchedKind != 0 && jjmatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
            genCodeLine("      debugStream.println(\"   Currently matched the first \" + "
                + "(jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
            genCodeLine("   debugStream.println(\"   Possible string literal matches : { \"");
          } else {
            genCodeLine(
                "   if (jjmatchedKind != 0 && jjmatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
            genCodeLine(
                "      fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\", (jjmatchedPos + 1), addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
            genCodeLine("   fprintf(debugStream, \"   Possible string literal matches : { \");");
          }

          StringBuilder fmt = new StringBuilder();
          StringBuilder args = new StringBuilder();
          for (int vecs = 0; vecs < ((getLexerData().maxStrKind / 64) + 1); vecs++) {
            if (i <= getLexerData().maxLenForActive[vecs]) {
              if (isJavaLanguage()) {
                genCodeLine(" +");
                genCode("         jjKindsForBitVector(" + vecs + ", ");
                genCode("active" + vecs + ") ");
              } else {
                if (fmt.length() > 0) {
                  fmt.append(", ");
                  args.append(", ");
                }

                fmt.append("%s");
                args.append("         jjKindsForBitVector(" + vecs + ", ");
                args.append("active" + vecs + ")" + (isJavaLanguage() ? " " : ".c_str() "));
              }
            }
          }

          // TODO :: CBA -- Require Unification of output language specific processing into a single
          // Enum class
          if (isJavaLanguage()) {
            genCodeLine(" + \" } \");");
          } else if (isCppLanguage()) {
            fmt.append("}\\n");
            genCodeLine("    fprintf(debugStream, \"" + fmt + "\"," + args + ");");
          } else {
            throw new RuntimeException("Output language type not fully implemented : " + getLanguage());
          }
        }

        // TODO :: CBA -- Require Unification of output language specific processing into a single
        // Enum class
        if (isJavaLanguage()) {
          genCodeLine("   try { curChar = input_stream.readChar(); }");
          genCodeLine("   catch(java.io.IOException e) {");
        } else if (isCppLanguage()) {
          genCodeLine("   if (input_stream->endOfInput()) {");
        } else {
          throw new RuntimeException("Output language type not fully implemented : " + getLanguage());
        }

        if (!getLexerData().isMixedState() && (getLexerData().generatedStates() != 0)) {
          genCode("      jjStopStringLiteralDfa" + this.lexStateSuffix + "(" + (i - 1) + ", ");
          for (k = 0; k < (maxLongsReqd - 1); k++) {
            if (i <= getLexerData().maxLenForActive[k]) {
              genCode("active" + k + ", ");
            } else {
              genCode("0L, ");
            }
          }

          if (i <= getLexerData().maxLenForActive[k]) {
            genCodeLine("active" + k + ");");
          } else {
            genCodeLine("0L);");
          }


          if ((i != 0) && Options.getDebugTokenManager()) {
            if (isJavaLanguage()) {
              genCodeLine(
                  "      if (jjmatchedKind != 0 && jjmatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
              genCodeLine("         debugStream.println(\"   Currently matched the first \" + "
                  + "(jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
            } else {
              genCodeLine(
                  "      if (jjmatchedKind != 0 && jjmatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
              genCodeLine(
                  "      fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\", (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
            }
          }

          genCodeLine("      return " + i + ";");
        } else if (getLexerData().generatedStates() != 0) {
          genCodeLine("   return jjMoveNfa" + this.lexStateSuffix + "(" + InitStateName() + ", " + (i - 1) + ");");
        } else {
          genCodeLine("      return " + i + ";");
        }

        genCodeLine("   }");
      }


      // TODO :: CBA -- Require Unification of output language specific processing into a single
      // Enum class
      if ((i != 0) && isCppLanguage()) {
        genCodeLine("   curChar = input_stream->readChar();");
      }

      if ((i != 0) && Options.getDebugTokenManager()) {

        // TODO :: CBA -- Require Unification of output language specific processing into a single
        // Enum class
        if (isJavaLanguage()) {
          genCodeLine("   debugStream.println("
              + (this.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
              + "\"Current character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") "
              + "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
        } else if (isCppLanguage()) {
          genCodeLine("   fprintf(debugStream, " + "\"<%s>Current character : %c(%d) at line %d column %d\\n\","
              + "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, "
              + "input_stream->getEndLine(), input_stream->getEndColumn());");
        } else {
          throw new RuntimeException("Output language type not fully implemented : " + getLanguage());
        }
      }

      genCodeLine("   switch(curChar)");
      genCodeLine("   {");

      CaseLoop:
      for (String key2 : keys) {
        key = key2;
        info = (KindInfo) tab.get(key);
        ifGenerated = false;
        char c = key.charAt(0);

        if ((i == 0) && (c < 128) && (info.finalKindCnt != 0)
            && ((getLexerData().generatedStates() == 0) || !CanStartNfaUsingAscii(c))) {
          int kind;
          for (j = 0; j < maxLongsReqd; j++) {
            if (info.finalKinds[j] != 0L) {
              break;
            }
          }

          for (k = 0; k < 64; k++) {
            if (((info.finalKinds[j] & (1L << k)) != 0L) && !getLexerData().subString[kind = ((j * 64) + k)]) {
              if (((getLexerData().intermediateKinds != null)
                  && (getLexerData().intermediateKinds[((j * 64) + k)] != null)
                  && (getLexerData().intermediateKinds[((j * 64) + k)][i] < ((j * 64) + k))
                  && (getLexerData().intermediateMatchedPos != null)
                  && (getLexerData().intermediateMatchedPos[((j * 64) + k)][i] == i))
                  || ((this.canMatchAnyChar[getLexerData().getStateIndex()] >= 0)
                      && (this.canMatchAnyChar[getLexerData().getStateIndex()] < ((j * 64) + k)))) {
                break;
              } else if (((this.toSkip[kind / 64] & (1L << (kind % 64))) != 0L)
                  && ((this.toSpecial[kind / 64] & (1L << (kind % 64))) == 0L) && (this.actions[kind] == null)
                  && (this.newLexState[kind] == null)) {
                AddCharToSkip(c, kind);

                if (getLexerData().ignoreCase()) {
                  if (c != Character.toUpperCase(c)) {
                    AddCharToSkip(Character.toUpperCase(c), kind);
                  }

                  if (c != Character.toLowerCase(c)) {
                    AddCharToSkip(Character.toLowerCase(c), kind);
                  }
                }
                continue CaseLoop;
              }
            }
          }
        }

        // Since we know key is a single character ...
        if (getLexerData().ignoreCase()) {
          if (c != Character.toUpperCase(c)) {
            genCodeLine("      case " + (int) Character.toUpperCase(c) + ":");
          }

          if (c != Character.toLowerCase(c)) {
            genCodeLine("      case " + (int) Character.toLowerCase(c) + ":");
          }
        }

        genCodeLine("      case " + (int) c + ":");

        long matchedKind;
        String prefix = (i == 0) ? "         " : "            ";

        if (info.finalKindCnt != 0) {
          for (j = 0; j < maxLongsReqd; j++) {
            if ((matchedKind = info.finalKinds[j]) == 0L) {
              continue;
            }

            for (k = 0; k < 64; k++) {
              if ((matchedKind & (1L << k)) == 0L) {
                continue;
              }

              if (ifGenerated) {
                genCode("         else if ");
              } else if (i != 0) {
                genCode("         if ");
              }

              ifGenerated = true;

              int kindToPrint;
              if (i != 0) {
                genCodeLine("((active" + j + " & 0x" + Long.toHexString(1L << k) + "L) != 0L)");
              }

              if ((getLexerData().intermediateKinds != null)
                  && (getLexerData().intermediateKinds[((j * 64) + k)] != null)
                  && (getLexerData().intermediateKinds[((j * 64) + k)][i] < ((j * 64) + k))
                  && (getLexerData().intermediateMatchedPos != null)
                  && (getLexerData().intermediateMatchedPos[((j * 64) + k)][i] == i)) {
                JavaCCErrors.warning(" \"" + Encoding.escape(getLexerData().getImage((j * 64) + k))
                    + "\" cannot be matched as a string literal token " + "at line " + GetLine((j * 64) + k)
                    + ", column " + GetColumn((j * 64) + k) + ". It will be matched as "
                    + GetLabel(getLexerData().intermediateKinds[((j * 64) + k)][i]) + ".");
                kindToPrint = getLexerData().intermediateKinds[((j * 64) + k)][i];
              } else if ((i == 0) && (this.canMatchAnyChar[getLexerData().getStateIndex()] >= 0)
                  && (this.canMatchAnyChar[getLexerData().getStateIndex()] < ((j * 64) + k))) {
                JavaCCErrors.warning(" \"" + Encoding.escape(getLexerData().getImage((j * 64) + k))
                    + "\" cannot be matched as a string literal token " + "at line " + GetLine((j * 64) + k)
                    + ", column " + GetColumn((j * 64) + k) + ". It will be matched as "
                    + GetLabel(this.canMatchAnyChar[getLexerData().getStateIndex()]) + ".");
                kindToPrint = this.canMatchAnyChar[getLexerData().getStateIndex()];
              } else {
                kindToPrint = (j * 64) + k;
              }

              if (!getLexerData().subString[((j * 64) + k)]) {
                int stateSetName = GetStateSetForKind(i, (j * 64) + k);

                if (stateSetName != -1) {
                  createStartNfa = true;
                  genCodeLine(prefix + "return jjStartNfaWithStates" + this.lexStateSuffix + "(" + i + ", "
                      + kindToPrint + ", " + stateSetName + ");");
                } else {
                  genCodeLine(prefix + "return jjStopAtPos" + "(" + i + ", " + kindToPrint + ");");
                }
              } else if (((this.initMatch[getLexerData().getStateIndex()] != 0)
                  && (this.initMatch[getLexerData().getStateIndex()] != Integer.MAX_VALUE)) || (i != 0)) {
                genCodeLine("         {");
                genCodeLine(prefix + "jjmatchedKind = " + kindToPrint + ";");
                genCodeLine(prefix + "jjmatchedPos = " + i + ";");
                genCodeLine("         }");
              } else {
                genCodeLine(prefix + "jjmatchedKind = " + kindToPrint + ";");
              }
            }
          }
        }

        if (info.validKindCnt != 0) {
          atLeastOne = false;

          if (i == 0) {
            genCode("         return ");

            genCode("jjMoveStringLiteralDfa" + (i + 1) + this.lexStateSuffix + "(");
            for (j = 0; j < (maxLongsReqd - 1); j++) {
              if ((i + 1) <= getLexerData().maxLenForActive[j]) {
                if (atLeastOne) {
                  genCode(", ");
                } else {
                  atLeastOne = true;
                }

                genCode("0x" + Long.toHexString(info.validKinds[j]) + (isJavaLanguage() ? "L" : "L"));
              }
            }

            if ((i + 1) <= getLexerData().maxLenForActive[j]) {
              if (atLeastOne) {
                genCode(", ");
              }

              genCode("0x" + Long.toHexString(info.validKinds[j]) + (isJavaLanguage() ? "L" : "L"));
            }
            genCodeLine(");");
          } else {
            genCode("         return ");

            genCode("jjMoveStringLiteralDfa" + (i + 1) + this.lexStateSuffix + "(");

            for (j = 0; j < (maxLongsReqd - 1); j++) {
              if ((i + 1) <= (getLexerData().maxLenForActive[j] + 1)) {
                if (atLeastOne) {
                  genCode(", ");
                } else {
                  atLeastOne = true;
                }

                if (info.validKinds[j] != 0L) {
                  genCode(
                      "active" + j + ", 0x" + Long.toHexString(info.validKinds[j]) + (isJavaLanguage() ? "L" : "L"));
                } else {
                  genCode("active" + j + ", 0L");
                }
              }
            }

            if ((i + 1) <= (getLexerData().maxLenForActive[j] + 1)) {
              if (atLeastOne) {
                genCode(", ");
              }
              if (info.validKinds[j] != 0L) {
                genCode("active" + j + ", 0x" + Long.toHexString(info.validKinds[j]) + (isJavaLanguage() ? "L" : "L"));
              } else {
                genCode("active" + j + ", 0L");
              }
            }

            genCodeLine(");");
          }
        } else // A very special case.
        if ((i == 0) && getLexerData().isMixedState()) {

          if (getLexerData().generatedStates() != 0) {
            genCodeLine("         return jjMoveNfa" + this.lexStateSuffix + "(" + InitStateName() + ", 0);");
          } else {
            genCodeLine("         return 1;");
          }
        } else if (i != 0) // No more str literals to look for
        {
          genCodeLine("         break;");
          startNfaNeeded = true;
        }
      }

      /*
       * default means that the current character is not in any of the strings at this position.
       */
      genCodeLine("      default :");

      if (Options.getDebugTokenManager()) {
        if (isJavaLanguage()) {
          genCodeLine("      debugStream.println(\"   No string literal matches possible.\");");
        } else {
          genCodeLine("      fprintf(debugStream, \"   No string literal matches possible.\\n\");");
        }
      }

      if (getLexerData().generatedStates() != 0) {
        if (i == 0) {
          /*
           * This means no string literal is possible. Just move nfa with this guy and return.
           */
          genCodeLine("         return jjMoveNfa" + this.lexStateSuffix + "(" + InitStateName() + ", 0);");
        } else {
          genCodeLine("         break;");
          startNfaNeeded = true;
        }
      } else {
        genCodeLine("         return " + (i + 1) + ";");
      }


      genCodeLine("   }");

      if (i != 0) {
        if (startNfaNeeded) {
          if (!getLexerData().isMixedState() && (getLexerData().generatedStates() != 0)) {
            /*
             * Here, a string literal is successfully matched and no more string literals are
             * possible. So set the kind and state set upto and including this position for the
             * matched string.
             */

            genCode("   return jjStartNfa" + this.lexStateSuffix + "(" + (i - 1) + ", ");
            for (k = 0; k < (maxLongsReqd - 1); k++) {
              if (i <= getLexerData().maxLenForActive[k]) {
                genCode("active" + k + ", ");
              } else {
                genCode("0L, ");
              }
            }
            if (i <= getLexerData().maxLenForActive[k]) {
              genCodeLine("active" + k + ");");
            } else {
              genCodeLine("0L);");
            }
          } else if (getLexerData().generatedStates() != 0) {
            genCodeLine("   return jjMoveNfa" + this.lexStateSuffix + "(" + InitStateName() + ", " + i + ");");
          } else {
            genCodeLine("   return " + (i + 1) + ";");
          }
        }
      }

      genCodeLine("}");
    }

    if (!getLexerData().isMixedState() && (getLexerData().generatedStates() != 0) && createStartNfa) {
      DumpStartWithStates();
    }
  }


  private static String[] ReArrange(Hashtable<String, ?> tab) {
    String[] ret = new String[tab.size()];
    Enumeration<String> e = tab.keys();
    int cnt = 0;

    while (e.hasMoreElements()) {
      int i = 0, j;
      String s;
      char c = (s = e.nextElement()).charAt(0);

      while ((i < cnt) && (ret[i].charAt(0) < c)) {
        i++;
      }

      if (i < cnt) {
        for (j = cnt - 1; j >= i; j--) {
          ret[j + 1] = ret[j];
        }
      }

      ret[i] = s;
      cnt++;
    }

    return ret;
  }

  private void DumpNullStrLiterals() {
    genCodeLine("{");

    if (getLexerData().generatedStates() > 0) {
      genCodeLine("   return jjMoveNfa" + this.lexStateSuffix + "(" + InitStateName() + ", 0);");
    } else {
      genCodeLine("   return 1;");
    }

    genCodeLine("}");
  }

  private void DumpStartWithStates() {
    // TODO :: CBA -- Require Unification of output language specific processing into a single Enum
    // class
    if (isJavaLanguage()) {
      genCodeLine("private int " + "jjStartNfaWithStates" + this.lexStateSuffix + "(int pos, int kind, int state)");
    } else if (isCppLanguage()) {
      generateMethodDefHeaderCpp("int",
          "jjStartNfaWithStates" + this.lexStateSuffix + "(int pos, int kind, int state)");
    } else {
      throw new RuntimeException("Output language type not fully implemented : " + getLanguage());
    }
    genCodeLine("{");
    genCodeLine("   jjmatchedKind = kind;");
    genCodeLine("   jjmatchedPos = pos;");

    if (Options.getDebugTokenManager()) {
      if (isJavaLanguage()) {
        genCodeLine("   debugStream.println(\"   No more string literal token matches are possible.\");");
        genCodeLine("   debugStream.println(\"   Currently matched the first \" "
            + "+ (jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
      } else {
        genCodeLine("   fprintf(debugStream, \"   No more string literal token matches are possible.\");");
        genCodeLine(
            "   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
      }
    }

    // TODO :: CBA -- Require Unification of output language specific processing into a single Enum
    // class
    if (isJavaLanguage()) {
      genCodeLine("   try { curChar = input_stream.readChar(); }");
      genCodeLine("   catch(java.io.IOException e) { return pos + 1; }");
    } else if (isCppLanguage()) {
      genCodeLine("   if (input_stream->endOfInput()) { return pos + 1; }");
      genCodeLine("   curChar = input_stream->readUnicode(); // TOL: Support Unicode");
    } else {
      throw new RuntimeException("Output language type not fully implemented : " + getLanguage());
    }
    if (Options.getDebugTokenManager()) {
      if (isJavaLanguage()) {
        genCodeLine("   debugStream.println("
            + (this.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
            + "\"Current character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") "
            + "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
      } else if (isCppLanguage()) {
        genCodeLine("   fprintf(debugStream, " + "\"<%s>Current character : %c(%d) at line %d column %d\\n\","
            + "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, "
            + "input_stream->getEndLine(), input_stream->getEndColumn());");
      } else {
        throw new RuntimeException("Output language type not fully implemented : " + getLanguage());
      }
    }

    genCodeLine("   return jjMoveNfa" + this.lexStateSuffix + "(state, pos + 1);");
    genCodeLine("}");
  }

  private void DumpBoilerPlate() {
    // TODO :: CBA -- Require Unification of output language specific processing into a single Enum
    // class
    if (isJavaLanguage()) {
      genCodeLine("private int " + "jjStopAtPos(int pos, int kind)");
    } else if (isCppLanguage()) {
      generateMethodDefHeaderCpp(" int ", "jjStopAtPos(int pos, int kind)");
    } else {
      throw new RuntimeException("Output language type not fully implemented : " + getLanguage());
    }
    genCodeLine("{");
    genCodeLine("   jjmatchedKind = kind;");
    genCodeLine("   jjmatchedPos = pos;");

    if (Options.getDebugTokenManager()) {
      // TODO :: CBA -- Require Unification of output language specific processing into a single
      // Enum class
      if (isJavaLanguage()) {
        genCodeLine("   debugStream.println(\"   No more string literal token matches are possible.\");");
        genCodeLine("   debugStream.println(\"   Currently matched the first \" + (jjmatchedPos + 1) + "
            + "\" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
      } else if (isCppLanguage()) {
        genCodeLine("   fprintf(debugStream, \"   No more string literal token matches are possible.\");");
        genCodeLine(
            "   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
      } else {
        throw new RuntimeException("Output language type not fully implemented : " + getLanguage());
      }
    }

    genCodeLine("   return pos + 1;");
    genCodeLine("}");
  }


  private String GetLabel(int kind) {
    RegularExpression re = this.rexprs[kind];

    if (re instanceof RStringLiteral) {
      return " \"" + Encoding.escape(((RStringLiteral) re).image) + "\"";
    } else if (!re.label.equals("")) {
      return " <" + re.label + ">";
    } else {
      return " <token of kind " + kind + ">";
    }
  }

  private int GetLine(int kind) {
    return this.rexprs[kind].getLine();
  }

  private int GetColumn(int kind) {
    return this.rexprs[kind].getColumn();
  }

  private int GetStateSetForKind(int pos, int kind) {
    if (getLexerData().isMixedState() || (getLexerData().generatedStates() == 0)) {
      return -1;
    }

    Hashtable<String, long[]> allStateSets = getLexerData().statesForPos[pos];

    if (allStateSets == null) {
      return -1;
    }

    Enumeration<String> e = allStateSets.keys();

    while (e.hasMoreElements()) {
      String s = e.nextElement();
      long[] actives = allStateSets.get(s);

      s = s.substring(s.indexOf(", ") + 2);
      s = s.substring(s.indexOf(", ") + 2);

      if (s.equals("null;")) {
        continue;
      }

      if ((actives != null) && ((actives[kind / 64] & (1L << (kind % 64))) != 0L)) {
        return AddCompositeStateSet(s, true);
      }
    }

    return -1;
  }

  private boolean CanStartNfaUsingAscii(char c) {
    if (c >= 128) {
      throw new Error("JavaCC Bug: Please send mail to sankar@cs.stanford.edu");
    }

    String s = this.initialState.GetEpsilonMovesString();

    if ((s == null) || s.equals("null;")) {
      return false;
    }

    int[] states = getLexerData().getNextStates(s);

    for (int state : states) {
      NfaState tmp = getLexerData().getIndexedState(state);

      if ((tmp.asciiMoves[c / 64] & (1L << (c % 64))) != 0L) {
        return true;
      }
    }

    return false;
  }


  /**
   * Used for top level string literals.
   */
  private void GenerateDfa(RStringLiteral rstring, int kind) {
    String s;
    Hashtable<String, KindInfo> temp;
    KindInfo info;
    int len;

    if (getLexerData().maxStrKind <= rstring.ordinal) {
      getLexerData().maxStrKind = rstring.ordinal + 1;
    }

    if ((len = rstring.image.length()) > getLexerData().maxLen) {
      getLexerData().maxLen = len;
    }

    char c;
    for (int i = 0; i < len; i++) {
      if (getLexerData().ignoreCase()) {
        s = ("" + (c = rstring.image.charAt(i))).toLowerCase(Locale.ENGLISH);
      } else {
        s = "" + (c = rstring.image.charAt(i));
      }

      if (i >= getLexerData().charPosKind.size()) { // Kludge, but OK
        getLexerData().charPosKind.add(temp = new Hashtable<>());
      } else { // Kludge, but OK
        temp = getLexerData().charPosKind.get(i);
      }

      if ((info = temp.get(s)) == null) {
        temp.put(s, info = rstring.new KindInfo(this.maxOrdinal));
      }

      if ((i + 1) == len) {
        info.InsertFinalKind(rstring.ordinal);
      } else {
        info.InsertValidKind(rstring.ordinal);
      }

      if (!getLexerData().ignoreCase() && this.ignoreCase[rstring.ordinal] && (c != Character.toLowerCase(c))) {
        s = ("" + rstring.image.charAt(i)).toLowerCase(Locale.ENGLISH);

        if (i >= getLexerData().charPosKind.size()) { // Kludge, but OK
          getLexerData().charPosKind.add(temp = new Hashtable<>());
        } else { // Kludge, but OK
          temp = getLexerData().charPosKind.get(i);
        }

        if ((info = temp.get(s)) == null) {
          temp.put(s, info = rstring.new KindInfo(this.maxOrdinal));
        }

        if ((i + 1) == len) {
          info.InsertFinalKind(rstring.ordinal);
        } else {
          info.InsertValidKind(rstring.ordinal);
        }
      }

      if (!getLexerData().ignoreCase() && this.ignoreCase[rstring.ordinal] && (c != Character.toUpperCase(c))) {
        s = ("" + rstring.image.charAt(i)).toUpperCase();

        if (i >= getLexerData().charPosKind.size()) { // Kludge, but OK
          getLexerData().charPosKind.add(temp = new Hashtable<>());
        } else { // Kludge, but OK
          temp = getLexerData().charPosKind.get(i);
        }

        if ((info = temp.get(s)) == null) {
          temp.put(s, info = rstring.new KindInfo(this.maxOrdinal));
        }

        if ((i + 1) == len) {
          info.InsertFinalKind(rstring.ordinal);
        } else {
          info.InsertValidKind(rstring.ordinal);
        }
      }
    }

    getLexerData().maxLenForActive[rstring.ordinal / 64] =
        Math.max(getLexerData().maxLenForActive[rstring.ordinal / 64], len - 1);
    getLexerData().allImages[rstring.ordinal] = rstring.image;
  }

  // ////////////////////////// NFaState

  private void DumpMoveNfa() {
    // if (!boilerPlateDumped)
    // PrintBoilerPlate(codeGenerator);

    // boilerPlateDumped = true;
    int i;
    int[] kindsForStates = null;

    if (getLexerData().kinds == null) {
      getLexerData().kinds = new int[this.maxLexStates][];
      getLexerData().statesForState = new int[this.maxLexStates][][];
    }

    ReArrange();

    for (i = 0; i < getLexerData().getAllStateCount(); i++) {
      NfaState temp = getLexerData().getAllState(i);

      if ((temp.lexState != getLexerData().getStateIndex()) || !temp.HasTransitions() || temp.dummy
          || (temp.stateName == -1)) {
        continue;
      }

      if (kindsForStates == null) {
        kindsForStates = new int[getLexerData().generatedStates()];
        getLexerData().statesForState[getLexerData().getStateIndex()] =
            new int[Math.max(getLexerData().generatedStates(), getLexerData().dummyStateIndex + 1)][];
      }

      kindsForStates[temp.stateName] = temp.lookingFor;
      getLexerData().statesForState[getLexerData().getStateIndex()][temp.stateName] = temp.compositeStates;

      GenerateNonAsciiMoves(temp);
    }

    Enumeration<String> e = getLexerData().stateNameForComposite.keys();

    while (e.hasMoreElements()) {
      String s = e.nextElement();
      int state = getLexerData().stateNameForComposite.get(s).intValue();

      if (state >= getLexerData().generatedStates()) {
        getLexerData().statesForState[getLexerData().getStateIndex()][state] = getLexerData().getNextStates(s);
      }
    }

    if (getLexerData().stateSetsToFix.size() != 0) {
      FixStateSets();
    }

    getLexerData().kinds[getLexerData().getStateIndex()] = kindsForStates;

    if (isJavaLanguage()) {
      genCodeLine("private int " + "jjMoveNfa" + this.lexStateSuffix + "(int startState, int curPos)");
    } else {
      generateMethodDefHeaderCpp("int", "jjMoveNfa" + this.lexStateSuffix + "(int startState, int curPos)");
    }
    genCodeLine("{");
    if (getLexerData().generatedStates() == 0) {
      genCodeLine("   return curPos;");
      genCodeLine("}");
      return;
    }

    if (getLexerData().isMixedState()) {
      genCodeLine("   int strKind = jjmatchedKind;");
      genCodeLine("   int strPos = jjmatchedPos;");
      genCodeLine("   int seenUpto;");
      if (isJavaLanguage()) {
        genCodeLine("   input_stream.backup(seenUpto = curPos + 1);");
        genCodeLine("   try { curChar = input_stream.readChar(); }");
        genCodeLine("   catch(java.io.IOException e) { throw new Error(\"Internal Error\"); }");
      } else {
        genCodeLine("   input_stream->backup(seenUpto = curPos + 1);");
        genCodeLine("   assert(!input_stream->endOfInput());");
        genCodeLine("   curChar = input_stream->readUnicode(); // TOL: Support Unicode");
      }
      genCodeLine("   curPos = 0;");
    }

    genCodeLine("   int startsAt = 0;");
    genCodeLine("   jjnewStateCnt = " + getLexerData().generatedStates() + ";");
    genCodeLine("   int i = 1;");
    genCodeLine("   jjstateSet[0] = startState;");

    if (Options.getDebugTokenManager()) {
      if (isJavaLanguage()) {
        genCodeLine("      debugStream.println(\"   Starting NFA to match one of : \" + "
            + "jjKindsForStateVector(curLexState, jjstateSet, 0, 1));");
      } else {
        genCodeLine(
            "      fprintf(debugStream, \"   Starting NFA to match one of : %s\\n\", jjKindsForStateVector(curLexState, jjstateSet, 0, 1).c_str());");
      }
    }

    if (Options.getDebugTokenManager()) {
      if (isJavaLanguage()) {
        genCodeLine("      debugStream.println("
            + (this.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
            + "\"Current character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") "
            + "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
      } else {
        genCodeLine("   fprintf(debugStream, " + "\"<%s>Current character : %c(%d) at line %d column %d\\n\","
            + "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, "
            + "input_stream->getEndLine(), input_stream->getEndColumn());");
      }
    }

    genCodeLine("   int kind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
    genCodeLine("   for (;;)");
    genCodeLine("   {");
    genCodeLine("      if (++jjround == 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
    genCodeLine("         ReInitRounds();");
    genCodeLine("      if (curChar < 64)");
    genCodeLine("      {");

    DumpAsciiMoves(0);

    genCodeLine("      }");

    genCodeLine("      else if (curChar < 128)");

    genCodeLine("      {");

    DumpAsciiMoves(1);

    genCodeLine("      }");

    genCodeLine("      else");
    genCodeLine("      {");

    DumpCharAndRangeMoves();

    genCodeLine("      }");

    genCodeLine("      if (kind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
    genCodeLine("      {");
    genCodeLine("         jjmatchedKind = kind;");
    genCodeLine("         jjmatchedPos = curPos;");
    genCodeLine("         kind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
    genCodeLine("      }");
    genCodeLine("      ++curPos;");

    if (Options.getDebugTokenManager()) {
      if (isJavaLanguage()) {
        genCodeLine(
            "      if (jjmatchedKind != 0 && jjmatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
        genCodeLine("         debugStream.println("
            + "\"   Currently matched the first \" + (jjmatchedPos + 1) + \" characters as"
            + " a \" + tokenImage[jjmatchedKind] + \" token.\");");
      } else {
        genCodeLine(
            "      if (jjmatchedKind != 0 && jjmatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
        genCodeLine(
            "   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
      }
    }

    if (isJavaLanguage()) {
      genCodeLine("      if ((i = jjnewStateCnt) == (startsAt = " + getLexerData().generatedStates()
          + " - (jjnewStateCnt = startsAt)))");
    } else {
      genCodeLine("      if ((i = jjnewStateCnt), (jjnewStateCnt = startsAt), (i == (startsAt = "
          + getLexerData().generatedStates() + " - startsAt)))");
    }
    if (getLexerData().isMixedState()) {
      genCodeLine("         break;");
    } else {
      genCodeLine("         return curPos;");
    }

    if (Options.getDebugTokenManager()) {
      if (isJavaLanguage()) {
        genCodeLine("      debugStream.println(\"   Possible kinds of longer matches : \" + "
            + "jjKindsForStateVector(curLexState, jjstateSet, startsAt, i));");
      } else {
        genCodeLine(
            "      fprintf(debugStream, \"   Possible kinds of longer matches : %s\\n\", jjKindsForStateVector(curLexState, jjstateSet, startsAt, i).c_str());");
      }
    }

    if (isJavaLanguage()) {
      genCodeLine("      try { curChar = input_stream.readChar(); }");
    } else {
      if (getLexerData().isMixedState()) {
        genCodeLine("      if (input_stream->endOfInput()) { break; }");
      } else {
        genCodeLine("      if (input_stream->endOfInput()) { return curPos; }");
      }
      genCodeLine("      curChar = input_stream->readUnicode(); // TOL: Support Unicode");
    }

    if (getLexerData().isMixedState()) {
      if (isJavaLanguage()) {
        genCodeLine("      catch(java.io.IOException e) { break; }");
      }
    } else if (isJavaLanguage()) {
      genCodeLine("      catch(java.io.IOException e) { return curPos; }");
    }

    if (Options.getDebugTokenManager()) {
      if (isJavaLanguage()) {
        genCodeLine("      debugStream.println("
            + (this.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "")
            + "\"Current character : \" + TokenMgrException.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") "
            + "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
      } else {
        genCodeLine("   fprintf(debugStream, " + "\"<%s>Current character : %c(%d) at line %d column %d\\n\","
            + "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, "
            + "input_stream->getEndLine(), input_stream->getEndColumn());");
      }
    }

    genCodeLine("   }");

    if (getLexerData().isMixedState()) {
      genCodeLine("   if (jjmatchedPos > strPos)");
      genCodeLine("      return curPos;");
      genCodeLine("");
      if (isJavaLanguage()) {
        genCodeLine("   int toRet = Math.max(curPos, seenUpto);");
      } else {
        genCodeLine("   int toRet = MAX(curPos, seenUpto);");
      }
      genCodeLine("");
      genCodeLine("   if (curPos < toRet)");
      if (isJavaLanguage()) {
        genCodeLine("      for (i = toRet - Math.min(curPos, seenUpto); i-- > 0; )");
        genCodeLine("         try { curChar = input_stream.readChar(); }");
        genCodeLine("         catch(java.io.IOException e) { "
            + "throw new Error(\"Internal Error : Please send a bug report.\"); }");
      } else {
        genCodeLine("      for (i = toRet - MIN(curPos, seenUpto); i-- > 0; )");
        genCodeLine("        {  assert(!input_stream->endOfInput());");
        genCodeLine("           curChar = input_stream->readUnicode(); } // TOL: Support Unicode");
      }
      genCodeLine("");
      genCodeLine("   if (jjmatchedPos < strPos)");
      genCodeLine("   {");
      genCodeLine("      jjmatchedKind = strKind;");
      genCodeLine("      jjmatchedPos = strPos;");
      genCodeLine("   }");
      genCodeLine("   else if (jjmatchedPos == strPos && jjmatchedKind > strKind)");
      genCodeLine("      jjmatchedKind = strKind;");
      genCodeLine("");
      genCodeLine("   return toRet;");
    }

    genCodeLine("}");
    getLexerData().clearAllStates();
  }

  private void ReArrange() {
    List<NfaState> v = getLexerData().cloneAllStates();

    if (getLexerData().getAllStateCount() != getLexerData().generatedStates()) {
      throw new Error("What??");
    }

    for (int j = 0; j < v.size(); j++) {
      NfaState tmp = v.get(j);
      if ((tmp.stateName != -1) && !tmp.dummy) {
        getLexerData().setAllState(tmp.stateName, tmp);
      }
    }
  }

  private void FixStateSets() {
    Hashtable<String, int[]> fixedSets = new Hashtable<>();
    Enumeration<String> e = getLexerData().stateSetsToFix.keys();
    int[] tmp = new int[getLexerData().generatedStates()];
    int i;

    while (e.hasMoreElements()) {
      String s;
      int[] toFix = getLexerData().stateSetsToFix.get(s = e.nextElement());
      int cnt = 0;

      // System.out.print("Fixing : ");
      for (i = 0; i < toFix.length; i++) {
        // System.out.print(toFix[i] + ", ");
        if (toFix[i] != -1) {
          tmp[cnt++] = toFix[i];
        }
      }

      int[] fixed = new int[cnt];
      System.arraycopy(tmp, 0, fixed, 0, cnt);
      fixedSets.put(s, fixed);
      getLexerData().setNextStates(s, fixed);
      // System.out.println(" as " + GetStateSetString(fixed));
    }

    for (i = 0; i < getLexerData().getAllStateCount(); i++) {
      NfaState tmpState = getLexerData().getAllState(i);
      int[] newSet;

      if ((tmpState.next == null) || (tmpState.next.usefulEpsilonMoves == 0)) {
        continue;
      }

      /*
       * if (compositeStateTable.get(tmpState.next.epsilonMovesString) != null)
       * tmpState.next.usefulEpsilonMoves = 1; else
       */ if ((newSet = fixedSets.get(tmpState.next.epsilonMovesString)) != null) {
        tmpState.FixNextStates(newSet);
      }
    }
  }

  private void DumpAsciiMoves(int byteNum) {
    boolean[] dumped = new boolean[Math.max(getLexerData().generatedStates(), getLexerData().dummyStateIndex + 1)];
    Enumeration<String> e = getLexerData().compositeStateTable.keys();

    DumpHeadForCase(byteNum);

    while (e.hasMoreElements()) {
      DumpCompositeStatesAsciiMoves(e.nextElement(), byteNum, dumped);
    }

    for (NfaState element : getLexerData().getAllStates()) {
      NfaState temp = element;

      if (dumped[temp.stateName] || (temp.lexState != getLexerData().getStateIndex()) || !temp.HasTransitions()
          || temp.dummy || (temp.stateName == -1)) {
        continue;
      }

      String toPrint = "";

      if (temp.stateForCase != null) {
        if (temp.inNextOf == 1) {
          continue;
        }

        if (dumped[temp.stateForCase.stateName]) {
          continue;
        }

        toPrint = PrintNoBreak(temp.stateForCase, byteNum, dumped);

        if (temp.asciiMoves[byteNum] == 0L) {
          if (toPrint.equals("")) {
            genCodeLine("                  break;");
          }

          continue;
        }
      }

      if (temp.asciiMoves[byteNum] == 0L) {
        continue;
      }

      if (!toPrint.equals("")) {
        genCode(toPrint);
      }

      dumped[temp.stateName] = true;
      genCodeLine("               case " + temp.stateName + ":");
      DumpAsciiMove(temp, byteNum, dumped);
    }

    if ((byteNum != 0) && (byteNum != 1)) {
      genCodeLine("               default : if (i1 == 0 || l1 == 0 || i2 == 0 ||  l2 == 0) break; else break;");
    } else {
      genCodeLine("               default : break;");
    }

    genCodeLine("            }");
    genCodeLine("         } while(i != startsAt);");
  }


  private void DumpCharAndRangeMoves() {
    boolean[] dumped = new boolean[Math.max(getLexerData().generatedStates(), getLexerData().dummyStateIndex + 1)];
    Enumeration<String> e = getLexerData().compositeStateTable.keys();
    int i;

    DumpHeadForCase(-1);

    while (e.hasMoreElements()) {
      DumpCompositeStatesNonAsciiMoves(e.nextElement(), dumped);
    }

    for (i = 0; i < getLexerData().getAllStateCount(); i++) {
      NfaState temp = getLexerData().getAllState(i);

      if ((temp.stateName == -1) || dumped[temp.stateName] || (temp.lexState != getLexerData().getStateIndex())
          || !temp.HasTransitions() || temp.dummy) {
        continue;
      }

      String toPrint = "";

      if (temp.stateForCase != null) {
        if (temp.inNextOf == 1) {
          continue;
        }

        if (dumped[temp.stateForCase.stateName]) {
          continue;
        }

        toPrint = PrintNoBreak(temp.stateForCase, -1, dumped);

        if (temp.nonAsciiMethod == -1) {
          if (toPrint.equals("")) {
            genCodeLine("                  break;");
          }

          continue;
        }
      }

      if (temp.nonAsciiMethod == -1) {
        continue;
      }

      if (!toPrint.equals("")) {
        genCode(toPrint);
      }

      dumped[temp.stateName] = true;
      // System.out.println("case : " + temp.stateName);
      genCodeLine("               case " + temp.stateName + ":");
      DumpNonAsciiMove(temp, dumped);
    }


    genCodeLine("               default : if (i1 == 0 || l1 == 0 || i2 == 0 ||  l2 == 0) break; else break;");
    genCodeLine("            }");
    genCodeLine("         } while(i != startsAt);");
  }


  private void DumpHeadForCase(int byteNum) {
    if (byteNum == 0) {
      genCodeLine("         " + getLongType() + " l = 1L << curChar;");
      if (!isJavaLanguage()) {
        genCodeLine("         (void)l;");
      }
    } else if (byteNum == 1) {
      genCodeLine("         " + getLongType() + " l = 1L << (curChar & 077);");
      if (!isJavaLanguage()) {
        genCodeLine("         (void)l;");
      }
    } else {
      genCodeLine("         int hiByte = (curChar >> 8);");
      genCodeLine("         int i1 = hiByte >> 6;");
      genCodeLine("         " + getLongType() + " l1 = 1L << (hiByte & 077);");
      genCodeLine("         int i2 = (curChar & 0xff) >> 6;");
      genCodeLine("         " + getLongType() + " l2 = 1L << (curChar & 077);");
    }

    // genCodeLine(" MatchLoop: do");
    genCodeLine("         do");
    genCodeLine("         {");

    genCodeLine("            switch(jjstateSet[--i])");
    genCodeLine("            {");
  }


  private void DumpCompositeStatesAsciiMoves(String key, int byteNum, boolean[] dumped) {
    int i;

    int[] nameSet = getLexerData().getNextStates(key);

    if ((nameSet.length == 1) || dumped[StateNameForComposite(key)]) {
      return;
    }

    NfaState toBePrinted = null;
    int neededStates = 0;
    NfaState tmp;
    NfaState stateForCase = null;
    String toPrint = "";
    boolean stateBlock = (getLexerData().stateBlockTable.get(key) != null);

    for (i = 0; i < nameSet.length; i++) {
      tmp = getLexerData().getAllState(nameSet[i]);

      if (tmp.asciiMoves[byteNum] != 0L) {
        if (neededStates++ == 1) {
          break;
        } else {
          toBePrinted = tmp;
        }
      } else {
        dumped[tmp.stateName] = true;
      }

      if (tmp.stateForCase != null) {
        if (stateForCase != null) {
          throw new Error("JavaCC Bug: Please send mail to sankar@cs.stanford.edu : ");
        }

        stateForCase = tmp.stateForCase;
      }
    }

    if (stateForCase != null) {
      toPrint = PrintNoBreak(stateForCase, byteNum, dumped);
    }

    if (neededStates == 0) {
      if ((stateForCase != null) && toPrint.equals("")) {
        genCodeLine("                  break;");
      }
      return;
    }

    if (neededStates == 1) {
      // if (byteNum == 1)
      // System.out.println(toBePrinted.stateName + " is the only state for "
      // + key + " ; and key is : " + StateNameForComposite(key));

      if (!toPrint.equals("")) {
        genCode(toPrint);
      }

      genCodeLine("               case " + StateNameForComposite(key) + ":");

      if (!dumped[toBePrinted.stateName] && !stateBlock && (toBePrinted.inNextOf > 1)) {
        genCodeLine("               case " + toBePrinted.stateName + ":");
      }

      dumped[toBePrinted.stateName] = true;
      DumpAsciiMove(toBePrinted, byteNum, dumped);
      return;
    }

    List<List<NfaState>> partition = PartitionStatesSetForAscii(nameSet, byteNum);

    if (!toPrint.equals("")) {
      genCode(toPrint);
    }

    int keyState = StateNameForComposite(key);
    genCodeLine("               case " + keyState + ":");
    if (keyState < getLexerData().generatedStates()) {
      dumped[keyState] = true;
    }

    for (i = 0; i < partition.size(); i++) {
      List<NfaState> subSet = partition.get(i);

      for (int j = 0; j < subSet.size(); j++) {
        tmp = subSet.get(j);

        if (stateBlock) {
          dumped[tmp.stateName] = true;
        }
        DumpAsciiMoveForCompositeState(tmp, byteNum, j != 0);
      }
    }

    if (stateBlock) {
      genCodeLine("                  break;");
    } else {
      genCodeLine("                  break;");
    }
  }


  private int StateNameForComposite(String stateSetString) {
    return getLexerData().stateNameForComposite.get(stateSetString).intValue();
  }

  private Vector<List<NfaState>> PartitionStatesSetForAscii(int[] states, int byteNum) {
    int[] cardinalities = new int[states.length];
    Vector<NfaState> original = new Vector<>();
    Vector<List<NfaState>> partition = new Vector<>();
    NfaState tmp;

    original.setSize(states.length);
    int cnt = 0;
    for (int i = 0; i < states.length; i++) {
      tmp = getLexerData().getAllState(states[i]);

      if (tmp.asciiMoves[byteNum] != 0L) {
        int j;
        int p = LexerGenerator.NumberOfBitsSet(tmp.asciiMoves[byteNum]);

        for (j = 0; j < i; j++) {
          if (cardinalities[j] <= p) {
            break;
          }
        }

        for (int k = i; k > j; k--) {
          cardinalities[k] = cardinalities[k - 1];
        }

        cardinalities[j] = p;

        original.insertElementAt(tmp, j);
        cnt++;
      }
    }

    original.setSize(cnt);

    while (original.size() > 0) {
      tmp = original.get(0);
      original.removeElement(tmp);

      long bitVec = tmp.asciiMoves[byteNum];
      List<NfaState> subSet = new ArrayList<>();
      subSet.add(tmp);

      for (int j = 0; j < original.size(); j++) {
        NfaState tmp1 = original.get(j);

        if ((tmp1.asciiMoves[byteNum] & bitVec) == 0L) {
          bitVec |= tmp1.asciiMoves[byteNum];
          subSet.add(tmp1);
          original.removeElementAt(j--);
        }
      }

      partition.add(subSet);
    }

    return partition;
  }

  private static int NumberOfBitsSet(long l) {
    int ret = 0;
    for (int i = 0; i < 63; i++) {
      if (((l >> i) & 1L) != 0L) {
        ret++;
      }
    }

    return ret;
  }

  private void DumpCompositeStatesNonAsciiMoves(String key, boolean[] dumped) {
    int i;
    int[] nameSet = getLexerData().getNextStates(key);

    if ((nameSet.length == 1) || dumped[StateNameForComposite(key)]) {
      return;
    }

    NfaState toBePrinted = null;
    int neededStates = 0;
    NfaState tmp;
    NfaState stateForCase = null;
    String toPrint = "";
    boolean stateBlock = (getLexerData().stateBlockTable.get(key) != null);

    for (i = 0; i < nameSet.length; i++) {
      tmp = getLexerData().getAllState(nameSet[i]);

      if (tmp.nonAsciiMethod != -1) {
        if (neededStates++ == 1) {
          break;
        } else {
          toBePrinted = tmp;
        }
      } else {
        dumped[tmp.stateName] = true;
      }

      if (tmp.stateForCase != null) {
        if (stateForCase != null) {
          throw new Error("JavaCC Bug: Please send mail to sankar@cs.stanford.edu : ");
        }

        stateForCase = tmp.stateForCase;
      }
    }

    if (stateForCase != null) {
      toPrint = PrintNoBreak(stateForCase, -1, dumped);
    }

    if (neededStates == 0) {
      if ((stateForCase != null) && toPrint.equals("")) {
        genCodeLine("                  break;");
      }

      return;
    }

    if (neededStates == 1) {
      if (!toPrint.equals("")) {
        genCode(toPrint);
      }

      genCodeLine("               case " + StateNameForComposite(key) + ":");

      if (!dumped[toBePrinted.stateName] && !stateBlock && (toBePrinted.inNextOf > 1)) {
        genCodeLine("               case " + toBePrinted.stateName + ":");
      }

      dumped[toBePrinted.stateName] = true;
      DumpNonAsciiMove(toBePrinted, dumped);
      return;
    }

    if (!toPrint.equals("")) {
      genCode(toPrint);
    }

    int keyState = StateNameForComposite(key);
    genCodeLine("               case " + keyState + ":");
    if (keyState < getLexerData().generatedStates()) {
      dumped[keyState] = true;
    }

    for (i = 0; i < nameSet.length; i++) {
      tmp = getLexerData().getAllState(nameSet[i]);

      if (tmp.nonAsciiMethod != -1) {
        if (stateBlock) {
          dumped[tmp.stateName] = true;
        }
        DumpNonAsciiMoveForCompositeState(tmp);
      }
    }

    if (stateBlock) {
      genCodeLine("                  break;");
    } else {
      genCodeLine("                  break;");
    }
  }


  private void GenerateNonAsciiMoves(NfaState state) {
    int i = 0, j = 0;
    char hiByte;
    int cnt = 0;
    long[][] loBytes = new long[256][4];

    if (((state.charMoves == null) || (state.charMoves[0] == 0))
        && ((state.rangeMoves == null) || (state.rangeMoves[0] == 0))) {
      return;
    }

    if (state.charMoves != null) {
      for (i = 0; i < state.charMoves.length; i++) {
        if (state.charMoves[i] == 0) {
          break;
        }

        hiByte = (char) (state.charMoves[i] >> 8);
        loBytes[hiByte][(state.charMoves[i] & 0xff) / 64] |= (1L << ((state.charMoves[i] & 0xff) % 64));
      }
    }

    if (state.rangeMoves != null) {
      for (i = 0; i < state.rangeMoves.length; i += 2) {
        if (state.rangeMoves[i] == 0) {
          break;
        }

        char c, r;

        r = (char) (state.rangeMoves[i + 1] & 0xff);
        hiByte = (char) (state.rangeMoves[i] >> 8);

        if (hiByte == (char) (state.rangeMoves[i + 1] >> 8)) {
          for (c = (char) (state.rangeMoves[i] & 0xff); c <= r; c++) {
            loBytes[hiByte][c / 64] |= (1L << (c % 64));
          }

          continue;
        }

        for (c = (char) (state.rangeMoves[i] & 0xff); c <= 0xff; c++) {
          loBytes[hiByte][c / 64] |= (1L << (c % 64));
        }

        while (++hiByte < (char) (state.rangeMoves[i + 1] >> 8)) {
          loBytes[hiByte][0] |= 0xffffffffffffffffL;
          loBytes[hiByte][1] |= 0xffffffffffffffffL;
          loBytes[hiByte][2] |= 0xffffffffffffffffL;
          loBytes[hiByte][3] |= 0xffffffffffffffffL;
        }

        for (c = 0; c <= r; c++) {
          loBytes[hiByte][c / 64] |= (1L << (c % 64));
        }
      }
    }

    long[] common = null;
    boolean[] done = new boolean[256];

    for (i = 0; i <= 255; i++) {
      if (done[i]
          || (done[i] = (loBytes[i][0] == 0) && (loBytes[i][1] == 0) && (loBytes[i][2] == 0) && (loBytes[i][3] == 0))) {
        continue;
      }

      for (j = i + 1; j < 256; j++) {
        if (done[j]) {
          continue;
        }

        if ((loBytes[i][0] == loBytes[j][0]) && (loBytes[i][1] == loBytes[j][1]) && (loBytes[i][2] == loBytes[j][2])
            && (loBytes[i][3] == loBytes[j][3])) {
          done[j] = true;
          if (common == null) {
            done[i] = true;
            common = new long[4];
            common[i / 64] |= (1L << (i % 64));
          }

          common[j / 64] |= (1L << (j % 64));
        }
      }

      if (common != null) {
        Integer ind;
        String tmp;

        tmp = "{\n   0x" + Long.toHexString(common[0]) + "L, " + "0x" + Long.toHexString(common[1]) + "L, " + "0x"
            + Long.toHexString(common[2]) + "L, " + "0x" + Long.toHexString(common[3]) + "L\n};";
        if ((ind = getLexerData().lohiByteTab.get(tmp)) == null) {
          getLexerData().allBitVectors.add(tmp);

          if (!NfaState.AllBitsSet(tmp)) {
            if (isJavaLanguage()) {
              genCodeLine("static final long[] jjbitVec" + getLexerData().lohiByteCnt + " = " + tmp);
            } else {
              ((CppWriter) getSource()).switchToStatics();
              genCodeLine("static const unsigned long long jjbitVec" + getLexerData().lohiByteCnt + "[] = " + tmp);
            }
          }
          getLexerData().lohiByteTab.put(tmp, ind = Integer.valueOf(getLexerData().lohiByteCnt++));
        }

        getLexerData().tmpIndices[cnt++] = ind.intValue();

        tmp = "{\n   0x" + Long.toHexString(loBytes[i][0]) + "L, " + "0x" + Long.toHexString(loBytes[i][1]) + "L, "
            + "0x" + Long.toHexString(loBytes[i][2]) + "L, " + "0x" + Long.toHexString(loBytes[i][3]) + "L\n};";
        if ((ind = getLexerData().lohiByteTab.get(tmp)) == null) {
          getLexerData().allBitVectors.add(tmp);

          if (!NfaState.AllBitsSet(tmp)) {
            if (isJavaLanguage()) {
              genCodeLine("static final long[] jjbitVec" + getLexerData().lohiByteCnt + " = " + tmp);
            } else {
              ((CppWriter) getSource()).switchToStatics();
              genCodeLine("static const unsigned long long jjbitVec" + getLexerData().lohiByteCnt + "[] = " + tmp);
              ((CppWriter) getSource()).switchToImpl();
            }
          }
          getLexerData().lohiByteTab.put(tmp, ind = Integer.valueOf(getLexerData().lohiByteCnt++));
        }

        getLexerData().tmpIndices[cnt++] = ind.intValue();

        common = null;
      }
    }

    state.nonAsciiMoveIndices = new int[cnt];
    System.arraycopy(getLexerData().tmpIndices, 0, state.nonAsciiMoveIndices, 0, cnt);

    for (i = 0; i < 256; i++) {
      if (done[i]) {
        loBytes[i] = null;
      } else {
        // System.out.print(i + ", ");
        String tmp;
        Integer ind;

        tmp = "{\n   0x" + Long.toHexString(loBytes[i][0]) + "L, " + "0x" + Long.toHexString(loBytes[i][1]) + "L, "
            + "0x" + Long.toHexString(loBytes[i][2]) + "L, " + "0x" + Long.toHexString(loBytes[i][3]) + "L\n};";

        if ((ind = getLexerData().lohiByteTab.get(tmp)) == null) {
          getLexerData().allBitVectors.add(tmp);

          if (!NfaState.AllBitsSet(tmp)) {
            if (isJavaLanguage()) {
              genCodeLine("static final long[] jjbitVec" + getLexerData().lohiByteCnt + " = " + tmp);
            } else {
              ((CppWriter) getSource()).switchToStatics();
              genCodeLine("static const unsigned long long jjbitVec" + getLexerData().lohiByteCnt + "[] = " + tmp);
            }
          }
          getLexerData().lohiByteTab.put(tmp, ind = Integer.valueOf(getLexerData().lohiByteCnt++));
        }

        if (state.loByteVec == null) {
          state.loByteVec = new Vector<>();
        }

        state.loByteVec.add(Integer.valueOf(i));
        state.loByteVec.add(ind);
      }
    }
    // System.out.println("");
    UpdateDuplicateNonAsciiMoves(state);
  }

  private void DumpAsciiMove(NfaState state, int byteNum, boolean dumped[]) {
    boolean nextIntersects = state.selfLoop() && state.isComposite;
    boolean onlyState = true;

    for (NfaState element : getLexerData().getAllStates()) {
      NfaState temp1 = element;

      if ((state == temp1) || (temp1.stateName == -1) || temp1.dummy || (state.stateName == temp1.stateName)
          || (temp1.asciiMoves[byteNum] == 0L)) {
        continue;
      }

      if (onlyState && ((state.asciiMoves[byteNum] & temp1.asciiMoves[byteNum]) != 0L)) {
        onlyState = false;
      }

      if (!nextIntersects
          && NfaState.Intersect(getLexerData(), temp1.next.epsilonMovesString, state.next.epsilonMovesString)) {
        nextIntersects = true;
      }

      if (!dumped[temp1.stateName] && !temp1.isComposite && (state.asciiMoves[byteNum] == temp1.asciiMoves[byteNum])
          && (state.kindToPrint == temp1.kindToPrint)
          && ((state.next.epsilonMovesString == temp1.next.epsilonMovesString)
              || ((state.next.epsilonMovesString != null) && (temp1.next.epsilonMovesString != null)
                  && state.next.epsilonMovesString.equals(temp1.next.epsilonMovesString)))) {
        dumped[temp1.stateName] = true;
        genCodeLine("               case " + temp1.stateName + ":");
      }
    }

    // if (onlyState)
    // nextIntersects = false;

    int oneBit = NfaState.OnlyOneBitSet(state.asciiMoves[byteNum]);
    if (state.asciiMoves[byteNum] != 0xffffffffffffffffL) {
      if (((state.next == null) || (state.next.usefulEpsilonMoves == 0)) && (state.kindToPrint != Integer.MAX_VALUE)) {
        String kindCheck = "";

        if (!onlyState) {
          kindCheck = " && kind > " + state.kindToPrint;
        }

        if (oneBit != -1) {
          genCodeLine("                  if (curChar == " + ((64 * byteNum) + oneBit) + kindCheck + ")");
        } else {
          genCodeLine("                  if ((0x" + Long.toHexString(state.asciiMoves[byteNum]) + "L & l) != 0L"
              + kindCheck + ")");
        }

        genCodeLine("                     kind = " + state.kindToPrint + ";");

        if (onlyState) {
          genCodeLine("                  break;");
        } else {
          genCodeLine("                  break;");
        }

        return;
      }
    }

    String prefix = "";
    if (state.kindToPrint != Integer.MAX_VALUE) {

      if (oneBit != -1) {
        genCodeLine("                  if (curChar != " + ((64 * byteNum) + oneBit) + ")");
        genCodeLine("                     break;");
      } else if (state.asciiMoves[byteNum] != 0xffffffffffffffffL) {
        genCodeLine("                  if ((0x" + Long.toHexString(state.asciiMoves[byteNum]) + "L & l) == 0L)");
        genCodeLine("                     break;");
      }

      if (onlyState) {
        genCodeLine("                  kind = " + state.kindToPrint + ";");
      } else {
        genCodeLine("                  if (kind > " + state.kindToPrint + ")");
        genCodeLine("                     kind = " + state.kindToPrint + ";");
      }
    } else if (oneBit != -1) {
      genCodeLine("                  if (curChar == " + ((64 * byteNum) + oneBit) + ")");
      prefix = "   ";
    } else if (state.asciiMoves[byteNum] != 0xffffffffffffffffL) {
      genCodeLine("                  if ((0x" + Long.toHexString(state.asciiMoves[byteNum]) + "L & l) != 0L)");
      prefix = "   ";
    }

    if ((state.next != null) && (state.next.usefulEpsilonMoves > 0)) {
      int[] stateNames = getLexerData().getNextStates(state.next.epsilonMovesString);
      if (state.next.usefulEpsilonMoves == 1) {
        int name = stateNames[0];
        if (nextIntersects) {
          genCodeLine(prefix + "                  { jjCheckNAdd(" + name + "); }");
        } else {
          genCodeLine(prefix + "                  jjstateSet[jjnewStateCnt++] = " + name + ";");
        }
      } else if ((state.next.usefulEpsilonMoves == 2) && nextIntersects) {
        genCodeLine(
            prefix + "                  { jjCheckNAddTwoStates(" + stateNames[0] + ", " + stateNames[1] + "); }");
      } else {
        int[] indices = NfaState.GetStateSetIndicesForUse(getLexerData(), state.next.epsilonMovesString);
        boolean notTwo = ((indices[0] + 1) != indices[1]);

        if (nextIntersects) {
          genCode(prefix + "                  { jjCheckNAddStates(" + indices[0]);
          if (notTwo) {
            getLexerData().jjCheckNAddStatesDualNeeded = true;
            genCode(", " + indices[1]);
          } else {
            getLexerData().jjCheckNAddStatesUnaryNeeded = true;
          }
          genCodeLine("); }");
        } else {
          genCodeLine(prefix + "                  { jjAddStates(" + indices[0] + ", " + indices[1] + "); }");
        }
      }
    }

    if (onlyState) {
      genCodeLine("                  break;");
    } else {
      genCodeLine("                  break;");
    }
  }


  private String PrintNoBreak(NfaState state, int byteNum, boolean[] dumped) {
    if (state.inNextOf != 1) {
      throw new Error("JavaCC Bug: Please send mail to sankar@cs.stanford.edu");
    }

    dumped[state.stateName] = true;

    if (byteNum >= 0) {
      if (state.asciiMoves[byteNum] != 0L) {
        genCodeLine("               case " + state.stateName + ":");
        DumpAsciiMoveForCompositeState(state, byteNum, false);
        return "";
      }
    } else if (state.nonAsciiMethod != -1) {
      genCodeLine("               case " + state.stateName + ":");
      DumpNonAsciiMoveForCompositeState(state);
      return "";
    }

    return ("               case " + state.stateName + ":\n");
  }


  private void DumpAsciiMoveForCompositeState(NfaState state, int byteNum, boolean elseNeeded) {
    boolean nextIntersects = state.selfLoop();

    for (NfaState temp1 : getLexerData().getAllStates()) {
      if ((state == temp1) || (temp1.stateName == -1) || temp1.dummy || (state.stateName == temp1.stateName)
          || (temp1.asciiMoves[byteNum] == 0L)) {
        continue;
      }

      if (!nextIntersects
          && NfaState.Intersect(getLexerData(), temp1.next.epsilonMovesString, state.next.epsilonMovesString)) {
        nextIntersects = true;
        break;
      }
    }

    // System.out.println(stateName + " \'s nextIntersects : " + nextIntersects);
    String prefix = "";
    if (state.asciiMoves[byteNum] != 0xffffffffffffffffL) {
      int oneBit = NfaState.OnlyOneBitSet(state.asciiMoves[byteNum]);

      if (oneBit != -1) {
        genCodeLine(
            "                  " + (elseNeeded ? "else " : "") + "if (curChar == " + ((64 * byteNum) + oneBit) + ")");
      } else {
        genCodeLine("                  " + (elseNeeded ? "else " : "") + "if ((0x"
            + Long.toHexString(state.asciiMoves[byteNum]) + "L & l) != 0L)");
      }
      prefix = "   ";
    }

    if (state.kindToPrint != Integer.MAX_VALUE) {
      if (state.asciiMoves[byteNum] != 0xffffffffffffffffL) {
        genCodeLine("                  {");
      }

      genCodeLine(prefix + "                  if (kind > " + state.kindToPrint + ")");
      genCodeLine(prefix + "                     kind = " + state.kindToPrint + ";");
    }

    if ((state.next != null) && (state.next.usefulEpsilonMoves > 0)) {
      int[] stateNames = getLexerData().getNextStates(state.next.epsilonMovesString);
      if (state.next.usefulEpsilonMoves == 1) {
        int name = stateNames[0];

        if (nextIntersects) {
          genCodeLine(prefix + "                  { jjCheckNAdd(" + name + "); }");
        } else {
          genCodeLine(prefix + "                  jjstateSet[jjnewStateCnt++] = " + name + ";");
        }
      } else if ((state.next.usefulEpsilonMoves == 2) && nextIntersects) {
        genCodeLine(
            prefix + "                  { jjCheckNAddTwoStates(" + stateNames[0] + ", " + stateNames[1] + "); }");
      } else {
        int[] indices = NfaState.GetStateSetIndicesForUse(getLexerData(), state.next.epsilonMovesString);
        boolean notTwo = ((indices[0] + 1) != indices[1]);

        if (nextIntersects) {
          genCode(prefix + "                  { jjCheckNAddStates(" + indices[0]);
          if (notTwo) {
            getLexerData().jjCheckNAddStatesDualNeeded = true;
            genCode(", " + indices[1]);
          } else {
            getLexerData().jjCheckNAddStatesUnaryNeeded = true;
          }
          genCodeLine("); }");
        } else {
          genCodeLine(prefix + "                  { jjAddStates(" + indices[0] + ", " + indices[1] + "); }");
        }
      }
    }

    if ((state.asciiMoves[byteNum] != 0xffffffffffffffffL) && (state.kindToPrint != Integer.MAX_VALUE)) {
      genCodeLine("                  }");
    }
  }

  private final void DumpNonAsciiMoveForCompositeState(NfaState state) {
    boolean nextIntersects = state.selfLoop();
    for (NfaState temp1 : getLexerData().getAllStates()) {
      if ((state == temp1) || (temp1.stateName == -1) || temp1.dummy || (state.stateName == temp1.stateName)
          || (temp1.nonAsciiMethod == -1)) {
        continue;
      }

      if (!nextIntersects
          && NfaState.Intersect(getLexerData(), temp1.next.epsilonMovesString, state.next.epsilonMovesString)) {
        nextIntersects = true;
        break;
      }
    }

    genCodeLine("                  if (jjCanMove_" + state.nonAsciiMethod + "(hiByte, i1, i2, l1, l2))");

    if (state.kindToPrint != Integer.MAX_VALUE) {
      genCodeLine("                  {");
      genCodeLine("                     if (kind > " + state.kindToPrint + ")");
      genCodeLine("                        kind = " + state.kindToPrint + ";");
    }

    if ((state.next != null) && (state.next.usefulEpsilonMoves > 0)) {
      int[] stateNames = getLexerData().getNextStates(state.next.epsilonMovesString);
      if (state.next.usefulEpsilonMoves == 1) {
        int name = stateNames[0];
        if (nextIntersects) {
          genCodeLine("                     { jjCheckNAdd(" + name + "); }");
        } else {
          genCodeLine("                     jjstateSet[jjnewStateCnt++] = " + name + ";");
        }
      } else if ((state.next.usefulEpsilonMoves == 2) && nextIntersects) {
        genCodeLine("                     { jjCheckNAddTwoStates(" + stateNames[0] + ", " + stateNames[1] + "); }");
      } else {
        int[] indices = NfaState.GetStateSetIndicesForUse(getLexerData(), state.next.epsilonMovesString);
        boolean notTwo = ((indices[0] + 1) != indices[1]);

        if (nextIntersects) {
          genCode("                     { jjCheckNAddStates(" + indices[0]);
          if (notTwo) {
            getLexerData().jjCheckNAddStatesDualNeeded = true;
            genCode(", " + indices[1]);
          } else {
            getLexerData().jjCheckNAddStatesUnaryNeeded = true;
          }
          genCodeLine("); }");
        } else {
          genCodeLine("                     { jjAddStates(" + indices[0] + ", " + indices[1] + "); }");
        }
      }
    }

    if (state.kindToPrint != Integer.MAX_VALUE) {
      genCodeLine("                  }");
    }
  }

  private final void DumpNonAsciiMove(NfaState state, boolean dumped[]) {
    boolean nextIntersects = state.selfLoop() && state.isComposite;

    for (NfaState element : getLexerData().getAllStates()) {
      NfaState temp1 = element;

      if ((state == temp1) || (temp1.stateName == -1) || temp1.dummy || (state.stateName == temp1.stateName)
          || (temp1.nonAsciiMethod == -1)) {
        continue;
      }

      if (!nextIntersects
          && NfaState.Intersect(getLexerData(), temp1.next.epsilonMovesString, state.next.epsilonMovesString)) {
        nextIntersects = true;
      }

      if (!dumped[temp1.stateName] && !temp1.isComposite && (state.nonAsciiMethod == temp1.nonAsciiMethod)
          && (state.kindToPrint == temp1.kindToPrint)
          && ((state.next.epsilonMovesString == temp1.next.epsilonMovesString)
              || ((state.next.epsilonMovesString != null) && (temp1.next.epsilonMovesString != null)
                  && state.next.epsilonMovesString.equals(temp1.next.epsilonMovesString)))) {
        dumped[temp1.stateName] = true;
        genCodeLine("               case " + temp1.stateName + ":");
      }
    }

    if ((state.next == null) || (state.next.usefulEpsilonMoves <= 0)) {
      String kindCheck = " && kind > " + state.kindToPrint;


      genCodeLine(
          "                  if (jjCanMove_" + state.nonAsciiMethod + "(hiByte, i1, i2, l1, l2)" + kindCheck + ")");
      genCodeLine("                     kind = " + state.kindToPrint + ";");
      genCodeLine("                  break;");
      return;
    }

    String prefix = "   ";
    if (state.kindToPrint != Integer.MAX_VALUE) {
      genCodeLine("                  if (!jjCanMove_" + state.nonAsciiMethod + "(hiByte, i1, i2, l1, l2))");
      genCodeLine("                     break;");

      genCodeLine("                  if (kind > " + state.kindToPrint + ")");
      genCodeLine("                     kind = " + state.kindToPrint + ";");
      prefix = "";
    } else {
      genCodeLine("                  if (jjCanMove_" + state.nonAsciiMethod + "(hiByte, i1, i2, l1, l2))");
    }

    if ((state.next != null) && (state.next.usefulEpsilonMoves > 0)) {
      int[] stateNames = getLexerData().getNextStates(state.next.epsilonMovesString);
      if (state.next.usefulEpsilonMoves == 1) {
        int name = stateNames[0];
        if (nextIntersects) {
          genCodeLine(prefix + "                  { jjCheckNAdd(" + name + "); }");
        } else {
          genCodeLine(prefix + "                  jjstateSet[jjnewStateCnt++] = " + name + ";");
        }
      } else if ((state.next.usefulEpsilonMoves == 2) && nextIntersects) {
        genCodeLine(
            prefix + "                  { jjCheckNAddTwoStates(" + stateNames[0] + ", " + stateNames[1] + "); }");
      } else {
        int[] indices = NfaState.GetStateSetIndicesForUse(getLexerData(), state.next.epsilonMovesString);
        boolean notTwo = ((indices[0] + 1) != indices[1]);

        if (nextIntersects) {
          genCode(prefix + "                  { jjCheckNAddStates(" + indices[0]);
          if (notTwo) {
            getLexerData().jjCheckNAddStatesDualNeeded = true;
            genCode(", " + indices[1]);
          } else {
            getLexerData().jjCheckNAddStatesUnaryNeeded = true;
          }
          genCodeLine("); }");
        } else {
          genCodeLine(prefix + "                  { jjAddStates(" + indices[0] + ", " + indices[1] + "); }");
        }
      }
    }

    genCodeLine("                  break;");
  }


  private void UpdateDuplicateNonAsciiMoves(NfaState state) {
    for (int i = 0; i < getLexerData().nonAsciiTableForMethod.size(); i++) {
      NfaState tmp = getLexerData().nonAsciiTableForMethod.get(i);
      if (NfaState.EqualLoByteVectors(state.loByteVec, tmp.loByteVec)
          && NfaState.EqualNonAsciiMoveIndices(state.nonAsciiMoveIndices, tmp.nonAsciiMoveIndices)) {
        state.nonAsciiMethod = i;
        return;
      }
    }

    state.nonAsciiMethod = getLexerData().nonAsciiTableForMethod.size();
    getLexerData().nonAsciiTableForMethod.add(state);
  }

  private int InitStateName() {
    String s = this.initialState.GetEpsilonMovesString();

    if (this.initialState.usefulEpsilonMoves != 0) {
      return StateNameForComposite(s);
    }
    return -1;
  }

  public void DumpNonAsciiMoveMethods() {
    if (getLexerData().nonAsciiTableForMethod.size() <= 0) {
      return;
    }

    for (NfaState tmp : getLexerData().nonAsciiTableForMethod) {
      DumpNonAsciiMoveMethod(tmp);
    }
  }

  private void DumpNonAsciiMoveMethod(NfaState state) {
    int j;
    if (isJavaLanguage()) {
      genCodeLine("private static final boolean jjCanMove_" + state.nonAsciiMethod
          + "(int hiByte, int i1, int i2, long l1, long l2)");
    } else {
      generateMethodDefHeaderCpp("bool", "jjCanMove_" + state.nonAsciiMethod
          + "(int hiByte, int i1, int i2, unsigned long long l1, unsigned long long l2)");
    }
    genCodeLine("{");
    genCodeLine("   switch(hiByte)");
    genCodeLine("   {");

    if ((state.loByteVec != null) && (state.loByteVec.size() > 0)) {
      for (j = 0; j < state.loByteVec.size(); j += 2) {
        genCodeLine("      case " + state.loByteVec.get(j).intValue() + ":");
        if (!NfaState.AllBitsSet(getLexerData().allBitVectors.get(state.loByteVec.get(j + 1).intValue()))) {
          genCodeLine("         return ((jjbitVec" + state.loByteVec.get(j + 1).intValue() + "[i2" + "] & l2) != 0L);");
        } else {
          genCodeLine("            return true;");
        }
      }
    }

    genCodeLine("      default :");

    if ((state.nonAsciiMoveIndices != null) && ((j = state.nonAsciiMoveIndices.length) > 0)) {
      do {
        if (!NfaState.AllBitsSet(getLexerData().allBitVectors.get(state.nonAsciiMoveIndices[j - 2]))) {
          genCodeLine("         if ((jjbitVec" + state.nonAsciiMoveIndices[j - 2] + "[i1] & l1) != 0L)");
        }
        if (!NfaState.AllBitsSet(getLexerData().allBitVectors.get(state.nonAsciiMoveIndices[j - 1]))) {
          genCodeLine("            if ((jjbitVec" + state.nonAsciiMoveIndices[j - 1] + "[i2] & l2) == 0L)");
          genCodeLine("               return false;");
          genCodeLine("            else");
        }
        genCodeLine("            return true;");
      } while ((j -= 2) > 0);
    }

    genCodeLine("         return false;");
    genCodeLine("   }");
    genCodeLine("}");
  }


  public void generateMethodDefHeaderCpp(String qualifiedModsAndRetType, String nameAndParams) {
    ((CppWriter) getSource()).switchToHeader();
    genCodeLine(qualifiedModsAndRetType + " " + nameAndParams + ";");

    String modsAndRetType = null;
    int i = qualifiedModsAndRetType.lastIndexOf(':');
    if (i >= 0) {
      modsAndRetType = qualifiedModsAndRetType.substring(i + 1);
    }

    if (modsAndRetType != null) {
      i = modsAndRetType.lastIndexOf("virtual");
      if (i >= 0) {
        modsAndRetType = modsAndRetType.substring(i + "virtual".length());
      }
    }
    i = qualifiedModsAndRetType.lastIndexOf("virtual");
    if (i >= 0) {
      qualifiedModsAndRetType = qualifiedModsAndRetType.substring(i + "virtual".length());
    }
    ((CppWriter) getSource()).switchToImpl();
    genCode("\n" + qualifiedModsAndRetType + " " + getTokenManager() + "::" + nameAndParams);
  }


  /**
   * Returns true if s1 starts with s2 (ignoring case for each character).
   */
  static private boolean StartsWithIgnoreCase(String s1, String s2) {
    if (s1.length() < s2.length()) {
      return false;
    }

    for (int i = 0; i < s2.length(); i++) {
      char c1 = s1.charAt(i), c2 = s2.charAt(i);

      if ((c1 != c2) && (Character.toLowerCase(c2) != c1) && (Character.toUpperCase(c2) != c1)) {
        return false;
      }
    }

    return true;
  }

  private final void FillSubString() {
    String image;
    getLexerData().subString = new boolean[getLexerData().maxStrKind + 1];
    getLexerData().subStringAtPos = new boolean[getLexerData().maxLen];

    for (int i = 0; i < getLexerData().maxStrKind; i++) {
      getLexerData().subString[i] = false;

      if (((image = getLexerData().getImage(i)) == null)
          || (getLexerData().getState(i) != getLexerData().getStateIndex())) {
        continue;
      }

      if (getLexerData().isMixedState()) {
        // We will not optimize for mixed case
        getLexerData().subString[i] = true;
        getLexerData().subStringAtPos[image.length() - 1] = true;
        continue;
      }

      for (int j = 0; j < getLexerData().maxStrKind; j++) {
        if ((j != i) && (getLexerData().getState(j) == getLexerData().getStateIndex())
            && ((getLexerData().getImage(j)) != null)) {
          if (getLexerData().getImage(j).indexOf(image) == 0) {
            getLexerData().subString[i] = true;
            getLexerData().subStringAtPos[image.length() - 1] = true;
            break;
          } else if (getLexerData().ignoreCase()
              && LexerGenerator.StartsWithIgnoreCase(getLexerData().getImage(j), image)) {
            getLexerData().subString[i] = true;
            getLexerData().subStringAtPos[image.length() - 1] = true;
            break;
          }
        }
      }
    }
  }


  private int GenerateInitMoves(NfaState state) {
    state.GetEpsilonMovesString();

    if (state.epsilonMovesString == null) {
      state.epsilonMovesString = "null;";
    }

    return AddCompositeStateSet(state.epsilonMovesString, true);
  }

  private int AddCompositeStateSet(String stateSetString, boolean starts) {
    Integer stateNameToReturn;

    if ((stateNameToReturn = getLexerData().stateNameForComposite.get(stateSetString)) != null) {
      return stateNameToReturn.intValue();
    }

    int toRet = 0;
    int[] nameSet = getLexerData().getNextStates(stateSetString);

    if (!starts) {
      getLexerData().stateBlockTable.put(stateSetString, stateSetString);
    }

    if (nameSet == null) {
      throw new Error("JavaCC Bug: Please file a bug at: https://github.com/javacc/javacc/issues");
    }

    if (nameSet.length == 1) {
      stateNameToReturn = Integer.valueOf(nameSet[0]);
      getLexerData().stateNameForComposite.put(stateSetString, stateNameToReturn);
      return nameSet[0];
    }

    for (int element : nameSet) {
      if (element == -1) {
        continue;
      }

      NfaState st = getLexerData().getIndexedState(element);
      st.isComposite = true;
      st.compositeStates = nameSet;
    }

    while ((toRet < nameSet.length) && (starts && (getLexerData().getIndexedState(nameSet[toRet]).inNextOf > 1))) {
      toRet++;
    }

    Enumeration<String> e = getLexerData().compositeStateTable.keys();
    String s;
    while (e.hasMoreElements()) {
      s = e.nextElement();
      if (!s.equals(stateSetString) && NfaState.Intersect(getLexerData(), stateSetString, s)) {
        int[] other = getLexerData().compositeStateTable.get(s);

        while ((toRet < nameSet.length) && ((starts && (getLexerData().getIndexedState(nameSet[toRet]).inNextOf > 1))
            || (NfaState.ElemOccurs(nameSet[toRet], other) >= 0))) {
          toRet++;
        }
      }
    }

    int tmp;

    if (toRet >= nameSet.length) {
      if (getLexerData().dummyStateIndex == -1) {
        tmp = getLexerData().dummyStateIndex = getLexerData().generatedStates();
      } else {
        tmp = ++getLexerData().dummyStateIndex;
      }
    } else {
      tmp = nameSet[toRet];
    }

    stateNameToReturn = Integer.valueOf(tmp);
    getLexerData().stateNameForComposite.put(stateSetString, stateNameToReturn);
    getLexerData().compositeStateTable.put(stateSetString, nameSet);

    return tmp;
  }
}
