/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2011 Waleri Enns and CONTACT Software GmbH
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.cxx.checks;

import com.google.common.io.Files;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.api.utils.SonarException;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.cxx.visitors.CxxCharsetAwareVisitor;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

@Rule(
  key = "UseFileHeader",
  description = "Use the uniformed header for this project",
  priority = Priority.BLOCKER)

//similar Vera++ rule T013 "No copyright notice found"
public class FileHeaderCheck extends SquidCheck<Grammar> implements CxxCharsetAwareVisitor {

  private static final String DEFAULT_HEADER_FORMAT = "";

  @RuleProperty(
    key = "headerFormat",
    description = "TEXT",
    defaultValue = DEFAULT_HEADER_FORMAT)
  
  public String headerFormat = DEFAULT_HEADER_FORMAT;

  private Charset charset;
  private String[] expectedLines;

  @Override
  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  @Override
  public void init() {
    expectedLines = headerFormat.split("(?:\r)?\n|\r");
  }

  @Override
  public void visitFile(AstNode astNode) {
    List<String> lines;
    try {
      lines = Files.readLines(getContext().getFile(), charset);
    } catch (IOException e) {
      throw new SonarException(e);
    }

    if (!matches(expectedLines, lines)) {
      getContext().createFileViolation(this, "Add or update the header of this file.");
    }
  }

  private static boolean matches(String[] expectedLines, List<String> lines) {
    boolean result;

    if (expectedLines.length <= lines.size()) {
      result = true;

      Iterator<String> it = lines.iterator();
      for (int i = 0; i < expectedLines.length; i++) {
        String line = it.next();
        if (!line.equals(expectedLines[i])) {
          result = false;
          break;
        }
      }
    } else {
      result = false;
    }

    return result;
  }

}
