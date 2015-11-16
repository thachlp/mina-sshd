/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sshd.server.shell;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Set;

import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.ValidateUtils;

/**
 * Handles the output stream while taking care of the {@link TtyOptions} for CR / LF
 * and ECHO settings. <B>Note:</B> does not close the echo stream when filter stream is closed
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class TtyFilterOutputStream extends FilterOutputStream {
    private final Set<TtyOptions> ttyOptions;
    private TtyFilterInputStream echo;
    private int lastChar = -1;

    public TtyFilterOutputStream(OutputStream out, TtyFilterInputStream echo, Collection<TtyOptions> ttyOptions) {
        super(out);
        // we create a copy of the options so as to avoid concurrent modifications
        this.ttyOptions = GenericUtils.of(ttyOptions);
        if (this.ttyOptions.contains(TtyOptions.LfOnlyOutput) && this.ttyOptions.contains(TtyOptions.CrLfOutput)) {
            throw new IllegalArgumentException("Ambiguous TTY options: " + this.ttyOptions);
        }

        this.echo = this.ttyOptions.contains(TtyOptions.Echo) ? ValidateUtils.checkNotNull(echo, "No echo stream") : echo;
    }

    @Override
    public void write(int c) throws IOException {
        if ((c == '\r') && ttyOptions.contains(TtyOptions.LfOnlyOutput)) {
            lastChar = c;
            return;
        }

        if ((c == '\n') && ttyOptions.contains(TtyOptions.CrLfOutput) && (lastChar != '\r')) {
            writeRawOutput('\r');
        }

        writeRawOutput(c);
    }

    protected void writeRawOutput(int c) throws IOException {
        lastChar = c;
        super.write(c);
        if (ttyOptions.contains(TtyOptions.Echo)) {
            echo.write(c);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int curPos = off, l = 0; l < len; curPos++, l++) {
            write(b[curPos]);
        }
    }
}