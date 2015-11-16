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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.apache.sshd.common.util.OsUtils;

/**
 * Options controlling the I/O streams behavior regarding CR/LF and echoing
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public enum TtyOptions {
    /**
     * Echo the output
     */
    Echo,
    /**
     * Convert CRLF to LF if not already such when writing to output stream
     */
    LfOnlyOutput,
    /**
     * Convert LF to CRLF if not already such when writing to output stream
     */
    CrLfOutput,
    /**
     * Convert LF to CRLF if not already such when reading from input stream
     */
    CrLfInput,
    /**
     * Convert CRLF to LF if not already such when reading from input stream
     */
    LfOnlyInput;

    public static final Set<TtyOptions> LINUX_OPTIONS =
            Collections.unmodifiableSet(EnumSet.of(TtyOptions.LfOnlyOutput, TtyOptions.LfOnlyInput));

    /*
     * NOTE !!! the assumption is that a Windows server is writing to a Linux
     * client (the most likely) thus it expects CRLF on input, but outputs LF only
     */
    public static final Set<TtyOptions> WINDOWS_OPTIONS =
            Collections.unmodifiableSet(EnumSet.of(TtyOptions.Echo, TtyOptions.LfOnlyOutput, TtyOptions.CrLfInput));

    public static Set<TtyOptions> resolveDefaultTtyOptions() {
        return resolveTtyOptions(OsUtils.isWin32());
    }

    public static Set<TtyOptions> resolveTtyOptions(boolean isWin32) {
        if (isWin32) {
            return WINDOWS_OPTIONS;
        } else {
            return LINUX_OPTIONS;
        }
    }
}