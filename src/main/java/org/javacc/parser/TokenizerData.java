package org.javacc.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// A simple class to hold the data generated by the tokenizer. This is passed to
// the code generators to produce code.
public class TokenizerData {

  // Name of the parser as specified in the PARSER_BEGIN/PARSER_END block.
  public String parserName;

  // Decls coming from TOKEN_MGR_DECLS
  public String decls;

  // A map of <LexState, first char> to a sequence of literals indexed by:
  //      ((int0LexicalState << 16 | (int)c)
  // The literals in the list are all guaranteed to start with the char and re
  // sorted by length so that the "longest-match" rule is done trivially by
  // just going through the sequence in the order.
  // Since they are all literals, there is no duplication (JavaCC checks that)
  // and hence if a longer match is matched, no need to check the shorter match.
  public Map<Integer, List<String>> literalSequence;

  // A map of list of kind values indexed by ((int0LexicalState << 16 | (int)c)
  // same key as before.
  public Map<Integer, List<Integer>> literalKinds;

  // The NFA start state for a given string literal match. We use this to start
  // the NFA if needed after a literal match is completed.
  public Map<Integer, Integer> kindToNfaStartState;

  // Class representing NFA state.
  public static class NfaState {
    // Index of the state.
    public int index;
    // Set of allowed characters.
    public Set<Character> characters;
    // Next state indices.
    public Set<Integer> nextStates;
    // Initial state needs to transition to multiple states so the NFA will try
    // all possibilities.
    // TODO(sreeni) : Try and get rid of it at some point.
    public Set<Integer> compositeStates;
    // match kind if any. Integer.MAX_VALUE if this is not a final state.
    public int kind;

    NfaState(int index, Set<Character> characters,
             Set<Integer> nextStates, Set<Integer> compositeStates, int kind) {
      this.index = index;
      this.characters = characters;
      this.nextStates = nextStates;
      this.kind = kind;
      this.compositeStates = compositeStates;
    }
  }

  // The main nfa.
  public final Map<Integer, NfaState> nfa = new HashMap<>();

  public enum MatchType {
    SKIP,
    SPECIAL_TOKEN,
    MORE,
    TOKEN,
  }

  // Match info.
  public static class MatchInfo {
    // String literal image in case this string literal token, null otherwise.
    public String image;
    // Kind index.
    public int kind;
    // Type of match.
    public MatchType matchType;
    // Any lexical state transition specified.
    public int newLexState;
    // Any lexical state transition specified.
    public String action;

    public MatchInfo(String image, int kind, MatchType matchType,
                     int newLexState, String action) {
      this.image = image;
      this.kind = kind;
      this.matchType = matchType;
      this.newLexState = newLexState;
      this.action = action;
    }
  }

  // On match info indexed by the match kind.
  public Map<Integer, MatchInfo> allMatches = new HashMap<>();

  // Initial nfa states indexed by lexical state.
  public Map<Integer, Integer> initialStates;

  // Kind of the wildcard match (~[]) indexed by lexical state.
  public Map<Integer, Integer> wildcardKind;

  // Name of lexical state - for debugging.
  public String[] lexStateNames;

  // DEFULAT lexical state index.
  public int defaultLexState;

  public void setParserName(String parserName) {
    this.parserName = parserName;
  }

  public void setDecls(String decls) {
    this.decls = decls;
  }

  public void setLiteralSequence(Map<Integer, List<String>> literalSequence) {
    this.literalSequence = literalSequence;
  }

  public void setLiteralKinds(Map<Integer, List<Integer>> literalKinds) {
    this.literalKinds = literalKinds;
  }

  public void setKindToNfaStartState(
      Map<Integer, Integer> kindToNfaStartState) {
    this.kindToNfaStartState = kindToNfaStartState;
  }

  public void addNfaState(int index, Set<Character> characters,
                          Set<Integer> nextStates,
                          Set<Integer> compositeStates, int kind) {
    NfaState nfaState =
        new NfaState(index, characters, nextStates, compositeStates, kind);
    nfa.put(index, nfaState);
  }

  public void setInitialStates(Map<Integer, Integer> initialStates) {
    this.initialStates = initialStates;
  }

  public void setWildcardKind(Map<Integer, Integer> wildcardKind) {
    this.wildcardKind = wildcardKind;
  }

  public void setLexStateNames(String[] lexStateNames) {
    this.lexStateNames = lexStateNames;
  }

  public void setDefaultLexState(int defaultLexState) {
    this.defaultLexState = defaultLexState;
  }

  public void updateMatchInfo(Map<Integer, String> actions,
                              int[] newLexStateIndices,
                              long[] toSkip, long[] toSpecial,
                              long[] toMore, long[] toToken) {
    for (int i = 0; i < newLexStateIndices.length; i++) {
      int vectorIndex = i >> 6;
      long bits = (1L << (i & 077));
      MatchType matchType = MatchType.TOKEN;
      if (toSkip.length > vectorIndex && (toSkip[vectorIndex] & bits) != 0L) {
        matchType = MatchType.SKIP;
      } else if (toSpecial.length > vectorIndex &&
                 (toSpecial[vectorIndex] & bits) != 0L) {
        matchType = MatchType.SPECIAL_TOKEN;
      } else if (toMore.length > vectorIndex &&
                 (toMore[vectorIndex] & bits) != 0L) {
        matchType = MatchType.MORE;
      } else {
        assert(toToken.length > vectorIndex &&
               (toToken[vectorIndex] & bits) != 0L);
        matchType = MatchType.TOKEN;
      }
      MatchInfo matchInfo =
          new MatchInfo(Options.getIgnoreCase()
                            ? null : RStringLiteral.allImages[i], i, matchType,
                        newLexStateIndices[i], actions.get(i));
      allMatches.put(i, matchInfo);
    }
  }
}
