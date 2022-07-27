/*
 * Copyright (c) 2008, Paul Cager. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
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

package org.fastcc.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates boiler-plate files from templates.
 */
public class Template {

  private static final Pattern PARAMETERS =
      Pattern.compile("\\{\\{([^\\{\\}\\?\\:]+)(?:\\?([^:]*):([^\\}]*))?(?:\\:\\-([^\\}]+))?\\}\\}");


  private final byte[]              bytes;
  private final Map<String, Object> options;

  /**
   * @param bytes
   * @param options
   */
  private Template(byte[] bytes, Map<String, Object> options) {
    this.bytes = bytes;
    this.options = options;
  }

  /**
   * Validates the condition against the properties.
   *
   * @param condition
   */
  private final boolean validate(String condition) {
    if (condition.startsWith("!")) { // negative condition
      return !validate(condition.substring(1));
    }

    if (!this.options.containsKey(condition)) {
      return false;
    }

    Object value = this.options.get(condition);
    if (value == null) {
      return false;
    }
    if ((value instanceof String) && ((String) value).isEmpty()) {
      return false;
    }
    if ((value instanceof Number) && ((Number) value).intValue() == 0) {
      return false;
    }

    return (!(value instanceof Boolean) || ((Boolean) value));
  }

  /**
   * Gets the iterable for the option
   *
   * @param option
   */
  private final Iterable<Object> iterable(String option) {
    if (!this.options.containsKey(option)) {
      return Collections.emptySet();
    }

    Object value = this.options.get(option);
    List<Object> list = new ArrayList<>();
    if (value instanceof Integer) {
      for (int i = 0; i < ((Integer) value); i++) {
        list.add(i);
      }
    } else if (value instanceof Iterable) {
      for (Object v : (Iterable<?>) value) {
        list.add(v);
      }
    }
    return list;
  }

  /**
   * Use the template.
   *
   * @param writer
   */
  public void write(PrintWriter writer) throws IOException {
    List<String> lines = new ArrayList<String>();
    InputStream stream = new ByteArrayInputStream(this.bytes);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
      String line = null;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
    }
    write(writer, lines);
  }

  /**
   * Use the template.
   *
   * @param writer
   * @param lines
   */
  private void write(PrintWriter writer, List<String> lines) {
    int index = 0;
    Stack<Boolean> conditions = new Stack<>();
    while (index < lines.size()) {
      String line = lines.get(index++);
      String cmd = line.toLowerCase();
      if (cmd.startsWith("@if ")) {
        boolean condition = validate(line.substring(4).trim());
        conditions.push(condition && (conditions.isEmpty() || conditions.peek()));
      } else if (cmd.startsWith("@elfi")) {
        boolean condition = validate(line.substring(4).trim());
        conditions.push(condition && (conditions.isEmpty() || conditions.peek()));
      } else if (cmd.startsWith("@else")) {
        boolean condition = !conditions.pop();
        conditions.push(condition && (conditions.isEmpty() || conditions.peek()));
      } else if (cmd.startsWith("@fi")) {
        conditions.pop();
      } else if (cmd.startsWith("@foreach ")) {
        Iterable<Object> iterable = iterable(line.substring(9).trim());
        List<String> subList = new ArrayList<>();
        String subLine = lines.get(index++);
        while (!subLine.toLowerCase().startsWith("@end")) {
          subList.add(subLine);
          subLine = lines.get(index++);
        }
        for (Object value : iterable) {
          this.options.put("$", value);
          write(writer, subList);
        }
      } else if (conditions.isEmpty() || conditions.peek()) {
        int offset = 0;
        Matcher matcher = Template.PARAMETERS.matcher(line);
        while (matcher.find()) {
          writer.print(line.substring(offset, matcher.start()));
          if (matcher.group(2) != null) {
            boolean validate = validate(matcher.group(1));
            writer.print(matcher.group(validate ? 2 : 3));
          } else if (matcher.group(4) != null) {
            boolean validate = validate(matcher.group(1));
            writer.print(validate ? this.options.get(matcher.group(1)) : matcher.group(4));
          } else if (matcher.group(1).endsWith("()")) {
            String name = matcher.group(1);
            String funcName = name.substring(0, name.length() - 2);
            Object instance = this.options.get(funcName);
            if (instance instanceof Function) {
              Function<Object, String> func = (Function<Object, String>) instance;
              writer.print(func.apply(this.options.get("$")));
            } else if (instance instanceof BiConsumer) {
              BiConsumer<PrintWriter, Object> func = (BiConsumer<PrintWriter, Object>) instance;
              func.accept(writer, this.options.get("$"));
            }
          } else {
            writer.print(this.options.get(matcher.group(1)));
          }
          offset = matcher.end();
        }
        writer.print(line.substring(offset));
        writer.println();
      }
    }
  }

  /**
   * Creates a new {@link Template}.
   *
   * @param template
   * @param options
   */
  public static Template of(String template, Map<String, Object> options) throws IOException {
    InputStream stream = Template.class.getResourceAsStream(template);
    if (stream == null) {
      throw new IOException("Invalid template name: " + template);
    }
    return new Template(stream.readAllBytes(), options);
  }
}
