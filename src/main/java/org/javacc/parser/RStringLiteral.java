// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

/* Copyright (c) 2006, Sun Microsystems, Inc.
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

package org.javacc.parser;

import org.javacc.generator.LexerData;

import java.util.HashSet;
import java.util.Set;


/**
 * Describes string literals.
 */

public class RStringLiteral extends RegularExpression {

  public final class KindInfo {

    public long[]              validKinds;
    public long[]              finalKinds;
    public int                 validKindCnt = 0;
    public int                 finalKindCnt = 0;
    private final Set<Integer> finalKindSet = new HashSet<>();
    private final Set<Integer> validKindSet = new HashSet<>();

    public KindInfo(int maxKind) {
      this.validKinds = new long[(maxKind / 64) + 1];
      this.finalKinds = new long[(maxKind / 64) + 1];
    }

    public void InsertValidKind(int kind) {
      this.validKinds[kind / 64] |= (1L << (kind % 64));
      this.validKindCnt++;
      this.validKindSet.add(kind);
    }

    public void InsertFinalKind(int kind) {
      this.finalKinds[kind / 64] |= (1L << (kind % 64));
      this.finalKindCnt++;
      this.finalKindSet.add(kind);
    }
  }

  /**
   * The string image of the literal.
   */
  public String image;

  public RStringLiteral() {}

  RStringLiteral(Token t, String image) {
    setLine(t.beginLine);
    setColumn(t.beginColumn);
    this.image = image;
  }

  @Override
  public Nfa GenerateNfa(LexerData data, boolean ignoreCase) {
    if (this.image.length() == 1) {
      RCharacterList temp = new RCharacterList(this.image.charAt(0));
      return temp.GenerateNfa(data, ignoreCase);
    }

    NfaState startState = new NfaState(data);
    NfaState theStartState = startState;
    NfaState finalState = null;

    if (this.image.length() == 0) {
      return new Nfa(theStartState, theStartState);
    }

    int i;

    for (i = 0; i < this.image.length(); i++) {
      finalState = new NfaState(data);
      startState.charMoves = new char[1];
      startState.AddChar(this.image.charAt(i));

      if (data.ignoreCase() || ignoreCase) {
        startState.AddChar(Character.toLowerCase(this.image.charAt(i)));
        startState.AddChar(Character.toUpperCase(this.image.charAt(i)));
      }

      startState.next = finalState;
      startState = finalState;
    }

    return new Nfa(theStartState, finalState);
  }

  @Override
  public StringBuilder dump(int indent, Set<? super Expansion> alreadyDumped) {
    return super.dump(indent, alreadyDumped).append(' ').append(this.image);
  }

  @Override
  public String toString() {
    return super.toString() + " - " + this.image;
  }
}
