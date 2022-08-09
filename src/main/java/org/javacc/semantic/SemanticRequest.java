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

package org.javacc.semantic;

import org.javacc.parser.Action;
import org.javacc.parser.NormalProduction;
import org.javacc.parser.RegularExpression;
import org.javacc.parser.TokenProduction;

import java.util.Hashtable;
import java.util.Set;

/**
 * The {@link SemanticRequest} class.
 */
public interface SemanticRequest {

  void setTokenCount();

  int addTokenCount();

  Set<String> getStateNames();

  String getStateName(int index);

  Integer getStateIndex(String name);

  Action getActionForEof();

  void setActionForEof(Action action);

  String getNextStateForEof();

  void setNextStateForEof(String state);

  Iterable<TokenProduction> getTokenProductions();

  Iterable<NormalProduction> getNormalProductions();

  NormalProduction getProductionTable(String name);

  NormalProduction setProductionTable(NormalProduction production);

  void addOrderedNamedToken(RegularExpression token);

  Hashtable<String, Hashtable<String, RegularExpression>> getSimpleTokenTable(String stateName);

  void setNamesOfToken(RegularExpression expression);
}
