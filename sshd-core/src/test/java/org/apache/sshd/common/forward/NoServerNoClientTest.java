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
package org.apache.sshd.common.forward;

import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Port forwarding tests - Control, direct connect. No SSH
 */
@TestMethodOrder(MethodName.class)
public class NoServerNoClientTest extends AbstractServerCloseTestSupport {
    public NoServerNoClientTest() {
        super();
    }

    @Override
    protected SshdSocketAddress startRemotePF() throws Exception {
        return new SshdSocketAddress(TEST_LOCALHOST, testServerPort);
    }

    @Override
    protected SshdSocketAddress startLocalPF() throws Exception {
        return new SshdSocketAddress(TEST_LOCALHOST, testServerPort);
    }

    @Override
    protected void stopRemotePF(SshdSocketAddress bound) throws Exception {
        // Nothing to do
    }

    @Override
    protected void stopLocalPF(SshdSocketAddress bound) throws Exception {
        // Nothing to do
    }
}
