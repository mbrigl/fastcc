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

package org.javacc.semantic;

import org.javacc.parser.Action;
import org.javacc.parser.Choice;
import org.javacc.parser.Expansion;
import org.javacc.parser.Lookahead;
import org.javacc.parser.NonTerminal;
import org.javacc.parser.NormalProduction;
import org.javacc.parser.OneOrMore;
import org.javacc.parser.ParseException;
import org.javacc.parser.RChoice;
import org.javacc.parser.REndOfFile;
import org.javacc.parser.RJustName;
import org.javacc.parser.ROneOrMore;
import org.javacc.parser.RRepetitionRange;
import org.javacc.parser.RSequence;
import org.javacc.parser.RStringLiteral;
import org.javacc.parser.RZeroOrMore;
import org.javacc.parser.RZeroOrOne;
import org.javacc.parser.RegExprSpec;
import org.javacc.parser.RegularExpression;
import org.javacc.parser.Sequence;
import org.javacc.parser.TokenProduction;
import org.javacc.parser.TryBlock;
import org.javacc.parser.ZeroOrMore;
import org.javacc.parser.ZeroOrOne;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Semanticize {

  private final SemanticRequest request;
  private final SemanticContext context;

  private long                  generationIndex;
  private int                   laLimit;
  private boolean               considerSemanticLA;


  private ArrayList<MatchInfo>                  sizeLimitedMatches;
  private final List<List<RegExprSpec>>         removeList;
  private final List<Object>                    itemList;

  private RegularExpression                     other;
  // The string in which the following methods store information.
  private String                                loopString;

  /**
   * A mapping of ordinal values (represented as objects of type "Integer") to the corresponding
   * RegularExpression's.
   */
  private final Map<Integer, RegularExpression> rexps_of_tokens    = new HashMap<>();
  /**
   * This is a symbol table that contains all named tokens (those that are defined with a label).
   * The index to the table is the image of the label and the contents of the table are of type
   * "RegularExpression".
   */
  private final Map<String, RegularExpression>  named_tokens_table = new HashMap<>();


  /**
   * Constructs an instance of {@link Semanticize}.
   *
   * @param request
   * @param context
   */
  private Semanticize(SemanticRequest request, SemanticContext context) {
    this.request = request;
    this.context = context;

    this.generationIndex = 1;
    this.laLimit = 0;

    this.considerSemanticLA = false;
    this.sizeLimitedMatches = null;

    this.removeList = new ArrayList<>();
    this.itemList = new ArrayList<>();
    this.other = null;
    this.loopString = null;
  }

  private final SemanticContext getContext() {
    return this.context;
  }

  final long nextGenerationIndex() {
    return this.generationIndex++;
  }

  final int laLimit() {
    return this.laLimit;
  }

  final boolean considerSemanticLA() {
    return this.considerSemanticLA;
  }

  final void setLaLimit(int limit) {
    this.laLimit = limit;
  }

  final void setConsiderSemanticLA(boolean considerSemanticLA) {
    this.considerSemanticLA = considerSemanticLA;
  }

  final void initSizeLimitedMatches() {
    this.sizeLimitedMatches = new ArrayList<>();
  }

  final List<MatchInfo> getSizeLimitedMatches() {
    return this.sizeLimitedMatches;
  }

  final RegularExpression getRegularExpression(int index) {
    return this.rexps_of_tokens.get(index);
  }

  public static void semanticize(SemanticRequest request, SemanticContext context) throws ParseException {
    if (context.hasErrors()) {
      throw new ParseException();
    }

    if ((context.getLookahead() > 1) && !context.isForceLaCheck() && context.isSanityCheck()) {
      context.onWarning("Lookahead adequacy checking not being performed since option LOOKAHEAD "
          + "is more than 1.  Set option FORCE_LA_CHECK to true to force checking.");
    }


    Semanticize semanticize = new Semanticize(request, context);

    /*
     * The following walks the entire parse tree to convert all LOOKAHEAD's that are not at choice
     * points (but at beginning of sequences) and converts them to trivial choices. This way, their
     * semantic lookahead specification can be evaluated during other lookahead evaluations.
     */
    for (NormalProduction bnfproduction : request.getNormalProductions()) {
      ExpansionTreeWalker.postOrderWalk(bnfproduction.getExpansion(), semanticize.new LookaheadFixer());
    }

    /*
     * The following loop populates "production_table"
     */
    for (NormalProduction p : request.getNormalProductions()) {
      if (request.setProductionTable(p) != null) {
        context.onSemanticError(p, p.getLhs() + " occurs on the left hand side of more than one production.");
      }
    }

    /*
     * The following walks the entire parse tree to make sure that all non-terminals on RHS's are
     * defined on the LHS.
     */
    for (NormalProduction bnfproduction : request.getNormalProductions()) {
      ExpansionTreeWalker.preOrderWalk((bnfproduction).getExpansion(), semanticize.new ProductionDefinedChecker());
    }


    /*
     * The following loop ensures that all target lexical states are defined. Also piggybacking on
     * this loop is the detection of <EOF> and <name> in token productions. After reporting an
     * error, these entries are removed. Also checked are definitions on inline private regular
     * expressions. This loop works slightly differently when is set to true. In this case, <name>
     * occurrences are OK, while regular expression specs generate a warning.
     */
    for (TokenProduction tokenProduction : request.getTokenProductions()) {
      TokenProduction tp = (tokenProduction);
      List<RegExprSpec> respecs = tp.respecs;
      for (RegExprSpec respec : respecs) {
        RegExprSpec res = (respec);
        if (res.nextState != null) {
          if (request.getStateIndex(res.nextState) == null) {
            context.onSemanticError(res.nsTok, "Lexical state \"" + res.nextState + "\" has not been defined.");
          }
        }
        if (res.rexp instanceof REndOfFile) {
          // context.onSemanticError(res.rexp, "Badly placed <EOF>.");
          if (tp.lexStates != null) {
            context.onSemanticError(res.rexp,
                "EOF action/state change must be specified for all states, " + "i.e., <*>TOKEN:.");
          }
          if (tp.kind != TokenProduction.TOKEN) {
            context.onSemanticError(res.rexp,
                "EOF action/state change can be specified only in a " + "TOKEN specification.");
          }
          if ((request.getNextStateForEof() != null) || (request.getActionForEof() != null)) {
            context.onSemanticError(res.rexp, "Duplicate action/state change specification for <EOF>.");
          }
          request.setActionForEof(res.act);
          request.setNextStateForEof(res.nextState);
          semanticize.prepareToRemove(respecs, res);
        } else if (tp.isExplicit && (res.rexp instanceof RJustName)) {
          context.onWarning(res.rexp, "Ignoring free-standing regular expression reference.  "
              + "If you really want this, you must give it a different label as <NEWLABEL:<" + res.rexp.label + ">>.");
          semanticize.prepareToRemove(respecs, res);
        } else if (!tp.isExplicit && res.rexp.private_rexp) {
          context.onSemanticError(res.rexp,
              "Private (#) regular expression cannot be defined within " + "grammar productions.");
        }
      }
    }

    semanticize.removePreparedItems();

    /*
     * The following loop inserts all names of regular expressions into "named_tokens_table" and
     * "ordered_named_tokens". Duplications are flagged as errors.
     */
    for (TokenProduction tokenProduction : request.getTokenProductions()) {
      TokenProduction tp = (tokenProduction);
      List<RegExprSpec> respecs = tp.respecs;
      for (RegExprSpec respec : respecs) {
        RegExprSpec res = (respec);
        if (!(res.rexp instanceof RJustName) && !res.rexp.label.equals("")) {
          String s = res.rexp.label;
          Object obj = semanticize.named_tokens_table.put(s, res.rexp);
          if (obj != null) {
            context.onSemanticError(res.rexp, "Multiply defined lexical token name \"" + s + "\".");
          } else {
            request.addOrderedNamedToken(res.rexp);
          }
          if (request.getStateIndex(s) != null) {
            context.onSemanticError(res.rexp,
                "Lexical token name \"" + s + "\" is the same as " + "that of a lexical state.");
          }
        }
      }
    }

    /*
     * The following code merges multiple uses of the same string in the same lexical state and
     * produces error messages when there are multiple explicit occurrences (outside the BNF) of the
     * string in the same lexical state, or when within BNF occurrences of a string are duplicates
     * of those that occur as non-TOKEN's (SKIP, MORE, SPECIAL_TOKEN) or private regular
     * expressions. While doing this, this code also numbers all regular expressions (by setting
     * their ordinal values), and populates the table "names_of_tokens".
     */

    request.setTokenCount();
    for (TokenProduction tokenProduction : request.getTokenProductions()) {
      TokenProduction tp = (tokenProduction);
      List<RegExprSpec> respecs = tp.respecs;
      if (tp.lexStates == null) {
        tp.lexStates = new String[request.getStateNames().size()];
        int i = 0;
        for (String stateName : request.getStateNames()) {
          tp.lexStates[i++] = stateName;
        }
      }
      Hashtable<String, Hashtable<String, RegularExpression>> table[] = new Hashtable[tp.lexStates.length];
      for (int i = 0; i < tp.lexStates.length; i++) {
        table[i] = request.getSimpleTokenTable(tp.lexStates[i]);
      }
      for (RegExprSpec respec : respecs) {
        RegExprSpec res = (respec);
        if (res.rexp instanceof RStringLiteral) {
          RStringLiteral sl = (RStringLiteral) res.rexp;
          // This loop performs the checks and actions with respect to each lexical state.
          for (int i = 0; i < table.length; i++) {
            // Get table of all case variants of "sl.image" into table2.
            Hashtable<String, RegularExpression> table2 = table[i].get(sl.image.toUpperCase());
            if (table2 == null) {
              // There are no case variants of "sl.image" earlier than the current one.
              // So go ahead and insert this item.
              if (sl.ordinal == 0) {
                sl.ordinal = request.addTokenCount();
              }
              table2 = new Hashtable<>();
              table2.put(sl.image, sl);
              table[i].put(sl.image.toUpperCase(), table2);
            } else if (semanticize.hasIgnoreCase(table2, sl.image)) { // hasIgnoreCase
                                                                      // sets
              // "other"
              // if it is found.
              // Since IGNORE_CASE version exists, current one is useless and bad.
              if (!sl.tpContext.isExplicit) {
                // inline BNF string is used earlier with an IGNORE_CASE.
                context.onSemanticError(sl,
                    "String \"" + sl.image + "\" can never be matched "
                        + "due to presence of more general (IGNORE_CASE) regular expression " + "at line "
                        + semanticize.other.getLine() + ", column " + semanticize.other.getColumn() + ".");
              } else {
                // give the standard error message.
                context.onSemanticError(sl,
                    "Duplicate definition of string token \"" + sl.image + "\" " + "can never be matched.");
              }
            } else if (sl.tpContext.ignoreCase) {
              // This has to be explicit. A warning needs to be given with respect
              // to all previous strings.
              String pos = "";
              int count = 0;
              for (Enumeration<RegularExpression> enum2 = table2.elements(); enum2.hasMoreElements();) {
                RegularExpression rexp = (enum2.nextElement());
                if (count != 0) {
                  pos += ",";
                }
                pos += " line " + rexp.getLine();
                count++;
              }
              if (count == 1) {
                context.onWarning(sl, "String with IGNORE_CASE is partially superseded by string at" + pos + ".");
              } else {
                context.onWarning(sl, "String with IGNORE_CASE is partially superseded by strings at" + pos + ".");
              }
              // This entry is legitimate. So insert it.
              if (sl.ordinal == 0) {
                sl.ordinal = request.addTokenCount();
              }
              table2.put(sl.image, sl);
              // The above "put" may override an existing entry (that is not IGNORE_CASE) and that's
              // the desired behavior.
            } else {
              // The rest of the cases do not involve IGNORE_CASE.
              RegularExpression re = table2.get(sl.image);
              if (re == null) {
                if (sl.ordinal == 0) {
                  sl.ordinal = request.addTokenCount();
                }
                table2.put(sl.image, sl);
              } else if (tp.isExplicit) {
                // This is an error even if the first occurrence was implicit.
                if (tp.lexStates[i].equals("DEFAULT")) {
                  context.onSemanticError(sl, "Duplicate definition of string token \"" + sl.image + "\".");
                } else {
                  context.onSemanticError(sl, "Duplicate definition of string token \"" + sl.image
                      + "\" in lexical state \"" + tp.lexStates[i] + "\".");
                }
              } else if (re.tpContext.kind != TokenProduction.TOKEN) {
                context.onSemanticError(sl, "String token \"" + sl.image + "\" has been defined as a \""
                    + TokenProduction.kindImage[re.tpContext.kind] + "\" token.");
              } else if (re.private_rexp) {
                context.onSemanticError(sl,
                    "String token \"" + sl.image + "\" has been defined as a private regular expression.");
              } else {
                // This is now a legitimate reference to an existing RStringLiteral.
                // So we assign it a number and take it out of "rexprlist".
                // Therefore, if all is OK (no errors), then there will be only unequal
                // string literals in each lexical state. Note that the only way
                // this can be legal is if this is a string declared inline within the
                // BNF. Hence, it belongs to only one lexical state - namely "DEFAULT".
                sl.ordinal = re.ordinal;
                semanticize.prepareToRemove(respecs, res);
              }
            }
          }
        } else if (!(res.rexp instanceof RJustName)) {
          res.rexp.ordinal = request.addTokenCount();
        }
        if (!(res.rexp instanceof RJustName) && !res.rexp.label.equals("")) {
          request.setNamesOfToken(res.rexp);
        }
        if (!(res.rexp instanceof RJustName)) {
          semanticize.rexps_of_tokens.put(Integer.valueOf(res.rexp.ordinal), res.rexp);
        }
      }
    }

    semanticize.removePreparedItems();

    /*
     * The following code performs a tree walk on all regular expressions attaching links to
     * "RJustName"s. Error messages are given if undeclared names are used, or if "RJustNames" refer
     * to private regular expressions or to regular expressions of any kind other than TOKEN. In
     * addition, this loop also removes top level "RJustName"s from "rexprlist". This code is not
     * executed if Options.getUserTokenManager() is set to true. Instead the following block of code
     * is executed.
     */

    FixRJustNames frjn = semanticize.new FixRJustNames();
    for (TokenProduction tokenProduction : request.getTokenProductions()) {
      TokenProduction tp = (tokenProduction);
      List<RegExprSpec> respecs = tp.respecs;
      for (RegExprSpec respec : respecs) {
        RegExprSpec res = (respec);
        frjn.root = res.rexp;
        ExpansionTreeWalker.preOrderWalk(res.rexp, frjn);
        if (res.rexp instanceof RJustName) {
          semanticize.prepareToRemove(respecs, res);
        }
      }
    }

    semanticize.removePreparedItems();
    semanticize.removePreparedItems();

    if (context.hasErrors()) {
      throw new ParseException();
    }

    // The following code sets the value of the "emptyPossible" field of NormalProduction
    // nodes. This field is initialized to false, and then the entire list of
    // productions is processed. This is repeated as long as at least one item
    // got updated from false to true in the pass.
    boolean emptyUpdate = true;
    while (emptyUpdate) {
      emptyUpdate = false;
      for (NormalProduction prod : request.getNormalProductions()) {
        if (Semanticize.emptyExpansionExists(prod.getExpansion())) {
          if (!prod.isEmptyPossible()) {
            emptyUpdate = prod.setEmptyPossible(true);
          }
        }
      }
    }

    if (context.isSanityCheck() && !context.hasErrors()) {

      // The following code checks that all ZeroOrMore, ZeroOrOne, and OneOrMore nodes
      // do not contain expansions that can expand to the empty token list.
      for (NormalProduction bnfproduction : request.getNormalProductions()) {
        ExpansionTreeWalker.preOrderWalk(bnfproduction.getExpansion(), semanticize.new EmptyChecker());
      }

      // The following code goes through the productions and adds pointers to other
      // productions that it can expand to without consuming any tokens. Once this is
      // done, a left-recursion check can be performed.
      for (NormalProduction prod : request.getNormalProductions()) {
        semanticize.addLeftMost(prod, prod.getExpansion());
      }

      // Now the following loop calls a recursive walk routine that searches for
      // actual left recursions. The way the algorithm is coded, once a node has
      // been determined to participate in a left recursive loop, it is not tried
      // in any other loop.
      for (NormalProduction prod : request.getNormalProductions()) {
        if (prod.getWalkStatus() == 0) {
          semanticize.prodWalk(prod);
        }
      }

      for (TokenProduction tokenProduction : request.getTokenProductions()) {
        TokenProduction tp = (tokenProduction);
        List<RegExprSpec> respecs = tp.respecs;
        for (RegExprSpec respec : respecs) {
          RegExprSpec res = (respec);
          RegularExpression rexp = res.rexp;
          if (rexp.walkStatus == 0) {
            rexp.walkStatus = -1;
            if (semanticize.rexpWalk(rexp)) {
              semanticize.loopString = "..." + rexp.label + "... --> " + semanticize.loopString;
              context.onSemanticError(rexp, "Loop in regular expression detected: \"" + semanticize.loopString + "\"");
            }
            rexp.walkStatus = 1;
          }
        }
      }

      /*
       * The following code performs the lookahead ambiguity checking.
       */
      if (!context.hasErrors()) {
        for (NormalProduction bnfproduction : request.getNormalProductions()) {
          ExpansionTreeWalker.preOrderWalk((bnfproduction).getExpansion(),
              semanticize.new LookaheadChecker(semanticize));
        }
      }

    } // matches "if (Options.getSanityCheck()) {"

    if (context.hasErrors()) {
      throw new ParseException();
    }

  }

  // returns true if "exp" can expand to the empty string, returns false otherwise.
  public static boolean emptyExpansionExists(Expansion exp) {
    if (exp instanceof NonTerminal) {
      return ((NonTerminal) exp).getProd().isEmptyPossible();
    } else if (exp instanceof Action) {
      return true;
    } else if (exp instanceof RegularExpression) {
      return false;
    } else if (exp instanceof OneOrMore) {
      return Semanticize.emptyExpansionExists(((OneOrMore) exp).expansion);
    } else if ((exp instanceof ZeroOrMore) || (exp instanceof ZeroOrOne)) {
      return true;
    } else if (exp instanceof Lookahead) {
      return true;
    } else if (exp instanceof Choice) {
      for (Object object : ((Choice) exp).getChoices()) {
        if (Semanticize.emptyExpansionExists((Expansion) object)) {
          return true;
        }
      }
      return false;
    } else if (exp instanceof Sequence) {
      for (Object object : ((Sequence) exp).units) {
        if (!Semanticize.emptyExpansionExists((Expansion) object)) {
          return false;
        }
      }
      return true;
    } else if (exp instanceof TryBlock) {
      return Semanticize.emptyExpansionExists(((TryBlock) exp).exp);
    } else {
      return false; // This should be dead code.
    }
  }

  // Checks to see if the "str" is superseded by another equal (except case) string
  // in table.
  private boolean hasIgnoreCase(Hashtable<String, RegularExpression> table, String str) {
    RegularExpression rexp;
    rexp = (table.get(str));
    if ((rexp != null) && !rexp.tpContext.ignoreCase) {
      return false;
    }
    for (Enumeration<RegularExpression> enumeration = table.elements(); enumeration.hasMoreElements();) {
      rexp = (enumeration.nextElement());
      if (rexp.tpContext.ignoreCase) {
        this.other = rexp;
        return true;
      }
    }
    return false;
  }

  // Updates prod.leftExpansions based on a walk of exp.
  private void addLeftMost(NormalProduction prod, Expansion exp) {
    if (exp instanceof NonTerminal) {
      for (int i = 0; i < prod.leIndex; i++) {
        if (prod.getLeftExpansions()[i] == ((NonTerminal) exp).getProd()) {
          return;
        }
      }
      if (prod.leIndex == prod.getLeftExpansions().length) {
        NormalProduction[] newle = new NormalProduction[prod.leIndex * 2];
        System.arraycopy(prod.getLeftExpansions(), 0, newle, 0, prod.leIndex);
        prod.setLeftExpansions(newle);
      }
      prod.getLeftExpansions()[prod.leIndex++] = ((NonTerminal) exp).getProd();
    } else if (exp instanceof OneOrMore) {
      addLeftMost(prod, ((OneOrMore) exp).expansion);
    } else if (exp instanceof ZeroOrMore) {
      addLeftMost(prod, ((ZeroOrMore) exp).expansion);
    } else if (exp instanceof ZeroOrOne) {
      addLeftMost(prod, ((ZeroOrOne) exp).expansion);
    } else if (exp instanceof Choice) {
      for (Object object : ((Choice) exp).getChoices()) {
        addLeftMost(prod, (Expansion) object);
      }
    } else if (exp instanceof Sequence) {
      for (Object object : ((Sequence) exp).units) {
        Expansion e = (Expansion) object;
        addLeftMost(prod, e);
        if (!Semanticize.emptyExpansionExists(e)) {
          break;
        }
      }
    } else if (exp instanceof TryBlock) {
      addLeftMost(prod, ((TryBlock) exp).exp);
    }
  }


  // Returns true to indicate an unraveling of a detected left recursion loop,
  // and returns false otherwise.
  private boolean prodWalk(NormalProduction prod) {
    prod.setWalkStatus(-1);
    for (int i = 0; i < prod.leIndex; i++) {
      if (prod.getLeftExpansions()[i].getWalkStatus() == -1) {
        prod.getLeftExpansions()[i].setWalkStatus(-2);
        this.loopString = prod.getLhs() + "... --> " + prod.getLeftExpansions()[i].getLhs() + "...";
        if (prod.getWalkStatus() == -2) {
          prod.setWalkStatus(1);
          context.onSemanticError(prod, "Left recursion detected: \"" + this.loopString + "\"");
          return false;
        } else {
          prod.setWalkStatus(1);
          return true;
        }
      } else if (prod.getLeftExpansions()[i].getWalkStatus() == 0) {
        if (prodWalk(prod.getLeftExpansions()[i])) {
          this.loopString = prod.getLhs() + "... --> " + this.loopString;
          if (prod.getWalkStatus() == -2) {
            prod.setWalkStatus(1);
            context.onSemanticError(prod, "Left recursion detected: \"" + this.loopString + "\"");
            return false;
          } else {
            prod.setWalkStatus(1);
            return true;
          }
        }
      }
    }
    prod.setWalkStatus(1);
    return false;
  }

  // Returns true to indicate an unraveling of a detected loop,
  // and returns false otherwise.
  private boolean rexpWalk(RegularExpression rexp) {
    if (rexp instanceof RJustName) {
      RJustName jn = (RJustName) rexp;
      if (jn.regexpr.walkStatus == -1) {
        jn.regexpr.walkStatus = -2;
        this.loopString = "..." + jn.regexpr.label + "...";
        // Note: Only the regexpr's of RJustName nodes and the top leve
        // regexpr's can have labels. Hence it is only in these cases that
        // the labels are checked for to be added to the loopString.
        return true;
      } else if (jn.regexpr.walkStatus == 0) {
        jn.regexpr.walkStatus = -1;
        if (rexpWalk(jn.regexpr)) {
          this.loopString = "..." + jn.regexpr.label + "... --> " + this.loopString;
          if (jn.regexpr.walkStatus == -2) {
            jn.regexpr.walkStatus = 1;
            context.onSemanticError(jn.regexpr, "Loop in regular expression detected: \"" + this.loopString + "\"");
            return false;
          } else {
            jn.regexpr.walkStatus = 1;
            return true;
          }
        } else {
          jn.regexpr.walkStatus = 1;
          return false;
        }
      }
    } else if (rexp instanceof RChoice) {
      for (Object object : ((RChoice) rexp).getChoices()) {
        if (rexpWalk((RegularExpression) object)) {
          return true;
        }
      }
      return false;
    } else if (rexp instanceof RSequence) {
      for (Object object : ((RSequence) rexp).units) {
        if (rexpWalk((RegularExpression) object)) {
          return true;
        }
      }
      return false;
    } else if (rexp instanceof ROneOrMore) {
      return rexpWalk(((ROneOrMore) rexp).regexpr);
    } else if (rexp instanceof RZeroOrMore) {
      return rexpWalk(((RZeroOrMore) rexp).regexpr);
    } else if (rexp instanceof RZeroOrOne) {
      return rexpWalk(((RZeroOrOne) rexp).regexpr);
    } else if (rexp instanceof RRepetitionRange) {
      return rexpWalk(((RRepetitionRange) rexp).regexpr);
    }
    return false;
  }

  private void prepareToRemove(List<RegExprSpec> vec, Object item) {
    this.removeList.add(vec);
    this.itemList.add(item);
  }

  private void removePreparedItems() {
    for (int i = 0; i < this.removeList.size(); i++) {
      List<RegExprSpec> list = this.removeList.get(i);
      list.remove(this.itemList.get(i));
    }
    this.removeList.clear();
    this.itemList.clear();
  }

  /**
   * Objects of this class are created from class Semanticize to work on references to regular
   * expressions from RJustName's.
   */
  private class FixRJustNames implements TreeWalker {

    private RegularExpression root;

    @Override
    public boolean goDeeper(Expansion e) {
      return true;
    }

    @Override
    public void action(Expansion e) {
      if (e instanceof RJustName) {
        RJustName jn = (RJustName) e;
        RegularExpression rexp = Semanticize.this.named_tokens_table.get(jn.label);
        if (rexp == null) {
          getContext().onSemanticError(e, "Undefined lexical token name \"" + jn.label + "\".");
        } else if ((jn == this.root) && !jn.tpContext.isExplicit && rexp.private_rexp) {
          getContext().onSemanticError(e,
              "Token name \"" + jn.label + "\" refers to a private " + "(with a #) regular expression.");
        } else if ((jn == this.root) && !jn.tpContext.isExplicit && (rexp.tpContext.kind != TokenProduction.TOKEN)) {
          getContext().onSemanticError(e, "Token name \"" + jn.label + "\" refers to a non-token "
              + "(SKIP, MORE, IGNORE_IN_BNF) regular expression.");
        } else {
          jn.ordinal = rexp.ordinal;
          jn.regexpr = rexp;
        }
      }
    }

  }

  private class LookaheadFixer implements TreeWalker {

    @Override
    public boolean goDeeper(Expansion e) {
      if (e instanceof RegularExpression) {
        return false;
      } else {
        return true;
      }
    }

    @Override
    public void action(Expansion e) {
      if (e instanceof Sequence) {
        if ((e.parent instanceof Choice) || (e.parent instanceof ZeroOrMore) || (e.parent instanceof OneOrMore)
            || (e.parent instanceof ZeroOrOne)) {
          return;
        }
        Sequence seq = (Sequence) e;
        Lookahead la = (Lookahead) (seq.units.get(0));
        if (!la.isExplicit()) {
          return;
        }
        // Create a singleton choice with an empty action.
        Choice ch = new Choice();
        ch.setLine(la.getLine());
        ch.setColumn(la.getColumn());
        ch.parent = seq;
        Sequence seq1 = new Sequence();
        seq1.setLine(la.getLine());
        seq1.setColumn(la.getColumn());
        seq1.parent = ch;
        seq1.units.add(la);
        la.parent = seq1;
        Action act = new Action();
        act.setLine(la.getLine());
        act.setColumn(la.getColumn());
        act.parent = seq1;
        seq1.units.add(act);
        ch.getChoices().add(seq1);
        if (la.getAmount() != 0) {
          if (la.getActionTokens().size() != 0) {
            context.onWarning(la, "Encountered LOOKAHEAD(...) at a non-choice location.  "
                + "Only semantic lookahead will be considered here.");
          } else {
            context.onWarning(la, "Encountered LOOKAHEAD(...) at a non-choice location.  This will be ignored.");
          }
        }
        // Now we have moved the lookahead into the singleton choice. Now create
        // a new dummy lookahead node to replace this one at its original location.
        Lookahead la1 = new Lookahead();
        la1.setExplicit(false);
        la1.setLine(la.getLine());
        la1.setColumn(la.getColumn());
        la1.parent = seq;
        // Now set the la_expansion field of la and la1 with a dummy expansion (we use EOF).
        la.setLaExpansion(new REndOfFile());
        la1.setLaExpansion(new REndOfFile());
        seq.units.set(0, la1);
        seq.units.add(1, ch);
      }
    }

  }

  private class ProductionDefinedChecker implements TreeWalker {

    @Override
    public boolean goDeeper(Expansion e) {
      if (e instanceof RegularExpression) {
        return false;
      } else {
        return true;
      }
    }

    @Override
    public void action(Expansion e) {
      if (e instanceof NonTerminal) {
        NonTerminal nt = (NonTerminal) e;
        if ((nt.setProd(request.getProductionTable(nt.getName()))) == null) {
          getContext().onSemanticError(e, "Non-terminal " + nt.getName() + " has not been defined.");
        } else {
          nt.getProd().getParents().add(nt);
        }
      }
    }

  }

  private class EmptyChecker implements TreeWalker {

    @Override
    public boolean goDeeper(Expansion e) {
      if (e instanceof RegularExpression) {
        return false;
      } else {
        return true;
      }
    }

    @Override
    public void action(Expansion e) {
      if (e instanceof OneOrMore) {
        if (Semanticize.emptyExpansionExists(((OneOrMore) e).expansion)) {
          getContext().onSemanticError(e, "Expansion within \"(...)+\" can be matched by empty string.");
        }
      } else if (e instanceof ZeroOrMore) {
        if (Semanticize.emptyExpansionExists(((ZeroOrMore) e).expansion)) {
          context.onSemanticError(e, "Expansion within \"(...)*\" can be matched by empty string.");
        }
      } else if (e instanceof ZeroOrOne) {
        if (Semanticize.emptyExpansionExists(((ZeroOrOne) e).expansion)) {
          getContext().onSemanticError(e, "Expansion within \"(...)?\" can be matched by empty string.");
        }
      }
    }

  }

  private class LookaheadChecker implements TreeWalker {

    private final Semanticize data;

    /**
     * Constructs an instance of {@link LookaheadChecker}.
     *
     * @param data
     */
    private LookaheadChecker(Semanticize data) {
      this.data = data;
    }

    @Override
    public boolean goDeeper(Expansion e) {
      if (e instanceof RegularExpression) {
        return false;
      } else if (e instanceof Lookahead) {
        return false;
      } else {
        return true;
      }
    }

    @Override
    public void action(Expansion e) {
      if (e instanceof Choice) {
        if ((getContext().getLookahead() == 1) || getContext().isForceLaCheck()) {
          LookaheadCalc.choiceCalc((Choice) e, this.data, getContext());
        }
      } else if (e instanceof OneOrMore) {
        OneOrMore exp = (OneOrMore) e;
        if (getContext().isForceLaCheck() || (implicitLA(exp.expansion) && (getContext().getLookahead() == 1))) {
          LookaheadCalc.ebnfCalc(exp, exp.expansion, this.data, getContext());
        }
      } else if (e instanceof ZeroOrMore) {
        ZeroOrMore exp = (ZeroOrMore) e;
        if (getContext().isForceLaCheck() || (implicitLA(exp.expansion) && (getContext().getLookahead() == 1))) {
          LookaheadCalc.ebnfCalc(exp, exp.expansion, this.data, getContext());
        }
      } else if (e instanceof ZeroOrOne) {
        ZeroOrOne exp = (ZeroOrOne) e;
        if (getContext().isForceLaCheck() || (implicitLA(exp.expansion) && (getContext().getLookahead() == 1))) {
          LookaheadCalc.ebnfCalc(exp, exp.expansion, this.data, getContext());
        }
      }
    }

    private boolean implicitLA(Expansion exp) {
      if (!(exp instanceof Sequence)) {
        return true;
      }
      Sequence seq = (Sequence) exp;
      Object obj = seq.units.get(0);
      if (!(obj instanceof Lookahead)) {
        return true;
      }
      Lookahead la = (Lookahead) obj;
      return !la.isExplicit();
    }
  }
}
