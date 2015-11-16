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

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.util.Collection;
import java.util.Set;

import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;

/**
 * Handles the input while taking into account the {@link TtyOptions} for
 * handling CR / LF
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class TtyFilterInputStream extends FilterInputStream {
    private final Set<TtyOptions> ttyOptions;
    private Buffer buffer = new ByteArrayBuffer(32);
    private int lastChar = -1;

    public TtyFilterInputStream(InputStream in, Collection<TtyOptions> ttyOptions) {
        super(in);
        // we create a copy of the options so as to avoid concurrent modifications
        this.ttyOptions = GenericUtils.of(ttyOptions);

        if (this.ttyOptions.contains(TtyOptions.LfOnlyInput) && this.ttyOptions.contains(TtyOptions.CrLfInput)) {
            throw new IllegalArgumentException("Ambiguous TTY options: " + ttyOptions);
        }
    }

    public synchronized void write(int c) {
        buffer.putByte((byte) c);
    }

    public synchronized void write(byte[] buf, int off, int len) {
        buffer.putBytes(buf, off, len);
    }

    @Override
    public int available() throws IOException {
        return super.available() + buffer.available();
    }

    @Override
    public synchronized int read() throws IOException {
        int c = readRawInput();
        if (c == -1) {
            return c;
        }

        if ((c == '\r') && ttyOptions.contains(TtyOptions.LfOnlyInput)) {
            c = readRawInput();
            if (c == -1) {
                throw new EOFException("Premature EOF while waiting for LF after CR");
            }

            if (c != '\n') {
                throw new StreamCorruptedException("CR not followed by LF");
            }
        }

        if ((c == '\n') && ttyOptions.contains(TtyOptions.CrLfInput)) {
            if (lastChar != '\r') {
                c = '\r';
                Buffer buf = new ByteArrayBuffer();
                buf.putByte((byte) '\n');
                buf.putBuffer(buffer);
                buffer = buf;
            }
        }

        lastChar = c;
        return c;
    }

    protected int readRawInput() throws IOException {
        // see if have any pending data
        if (buffer.available() > 0) {
            int c = buffer.getUByte();
            buffer.compact();
            return c;
        }

        return super.read();
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (buffer.available() == 0) {
            int nb = super.read(b, off, len);
            if (nb == -1) {
                return -1;
            }
            buffer.putRawBytes(b, off, nb);
        }

        int nb = 0;
        while ((nb < len) && (buffer.available() > 0)) {
            b[off + nb++] = (byte) read();
        }

        return nb;
    }
}