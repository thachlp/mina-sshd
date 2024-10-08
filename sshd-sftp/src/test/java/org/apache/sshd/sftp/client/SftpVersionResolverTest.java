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

package org.apache.sshd.sftp.client;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.sshd.common.NamedResource;
import org.apache.sshd.sftp.client.SftpVersionSelector.NamedVersionSelector;
import org.apache.sshd.util.test.JUnitTestSupport;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
@TestMethodOrder(MethodName.class)
@Tag("NoIoTestCase")
public class SftpVersionResolverTest extends JUnitTestSupport {
    private NamedVersionSelector expected;
    private NamedVersionSelector actual;

    public void initSftpVersionResolverTest(String selector, NamedVersionSelector expected) {
        this.expected = expected;
        this.actual = SftpVersionSelector.resolveVersionSelector(selector);
    }

    public static Collection<Object[]> parameters() {
        return new LinkedList<Object[]>() {
            // Not serializing it
            private static final long serialVersionUID = 1L;

            {
                addTestCase(null, SftpVersionSelector.CURRENT);
                addTestCase("", SftpVersionSelector.CURRENT);
                addTestCase(SftpVersionSelector.CURRENT);
                addTestCase(SftpVersionSelector.MINIMUM);
                addTestCase(SftpVersionSelector.MAXIMUM);
                addTestCase(SftpVersionSelector.fixedVersionSelector(3));
                addTestCase(SftpVersionSelector.preferredVersionSelector(3, 4, 5));
            }

            private void addTestCase(NamedVersionSelector expected) {
                addTestCase(expected.getName(), expected);
            }

            private void addTestCase(String selector, NamedVersionSelector expected) {
                add(new Object[] { selector, expected });
            }
        };
    }

    @MethodSource("parameters")
    @ParameterizedTest(name = "selector={0}")
    public void resolvedResult(String selector, NamedVersionSelector expected) {
        initSftpVersionResolverTest(selector, expected);
        assertEquals(expected, actual);
    }

    @MethodSource("parameters")
    @ParameterizedTest(name = "selector={0}")
    public void preDefinedSelectorResolution(String selector, NamedVersionSelector expected) {
        initSftpVersionResolverTest(selector, expected);
        Assumptions.assumeTrue((NamedResource.safeCompareByName(SftpVersionSelector.CURRENT, expected, false) == 0)
                || (NamedResource.safeCompareByName(SftpVersionSelector.MINIMUM, expected, false) == 0)
                || (NamedResource.safeCompareByName(SftpVersionSelector.MAXIMUM, expected, false) == 0),
                "Pre-defined selector ?");
        assertSame(expected, actual);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[expected=" + expected + ", actual=" + actual + "]";
    }
}
