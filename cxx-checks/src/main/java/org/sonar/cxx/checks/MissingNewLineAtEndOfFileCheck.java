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

import com.google.common.io.Closeables;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.api.utils.SonarException;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import com.sonar.sslr.api.Grammar;

import java.io.IOException;
import java.io.RandomAccessFile;

@Rule(key = "NewLineAtEOF",
      description = "Use always a empty line as the last line",
      priority = Priority.MINOR)

public class MissingNewLineAtEndOfFileCheck extends SquidCheck<Grammar> {

  @Override
  public void visitFile(AstNode astNode) {
    RandomAccessFile randomAccessFile = null;
    try {
      randomAccessFile = new RandomAccessFile(getContext().getFile(), "r");
      if (!endsWithNewline(randomAccessFile)) {
        getContext().createFileViolation(this, "Add a new line at the end of this file.");
      }
    } catch (IOException e) {
      throw new SonarException(e);
    } finally {
      Closeables.closeQuietly(randomAccessFile);
    }
  }

  private boolean endsWithNewline(RandomAccessFile randomAccessFile) throws IOException {
    if (randomAccessFile.length() < 1) {
      return false;
    }
    randomAccessFile.seek(randomAccessFile.length() - 1);
    byte[] chars = new byte[1];
    if (randomAccessFile.read(chars) < 1) {
      return false;
    }
    String ch = new String(chars);
    return "\n".equals(ch) || "\r".equals(ch);
  }

}
