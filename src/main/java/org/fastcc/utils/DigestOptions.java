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

package org.fastcc.utils;

import org.javacc.parser.Options;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;


/**
 * The {@link DigestOptions} class.
 */
public class DigestOptions extends HashMap<String, Object> {

  private static final long serialVersionUID = 1773784122156760640L;


  private final Map<String, Object> consumed = new HashMap<>();

  /**
   * Constructs an instance of {@link DigestOptions}.
   */
  private DigestOptions() {}

  final boolean hasConsumed() {
    return !this.consumed.isEmpty();
  }

  final Stream<Map.Entry<String, Object>> consumed() {
    return this.consumed.entrySet().stream();
  }

  /**
   * Returns the value to which the specified key is mapped, or {@code null} if this map contains no
   * mapping for the key.
   *
   * @param key
   */
  @Override
  public final Object get(Object key) {
    Object value = super.get(key);
    this.consumed.put((String) key, value);
    return value;
  }

  /**
   * Create an {@link DigestOptions} from {@link Options}.
   */
  public static DigestOptions get() {
    DigestOptions options = new DigestOptions();
    options.putAll(Options.getOptions());
    return options;
  }
}
