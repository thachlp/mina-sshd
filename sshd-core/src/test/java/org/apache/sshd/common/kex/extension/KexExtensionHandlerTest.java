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

package org.apache.sshd.common.kex.extension;

import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.sshd.common.kex.extension.parser.DelayCompression;
import org.apache.sshd.common.kex.extension.parser.DelayedCompressionAlgorithms;
import org.apache.sshd.common.kex.extension.parser.Elevation;
import org.apache.sshd.common.kex.extension.parser.NoFlowControl;
import org.apache.sshd.common.kex.extension.parser.ServerSignatureAlgorithms;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;
import org.apache.sshd.util.test.JUnitTestSupport;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
@TestMethodOrder(MethodName.class)
@Tag("NoIoTestCase")
public class KexExtensionHandlerTest extends JUnitTestSupport {
    public KexExtensionHandlerTest() {
        super();
    }

    @Test
    void encodeDecodeExtensionMessage() throws Exception {
        List<Map.Entry<String, ?>> expected = Arrays.asList(
                new SimpleImmutableEntry<>(
                        DelayCompression.NAME,
                        new DelayedCompressionAlgorithms()
                                .withClient2Server(
                                        Arrays.asList(getClass().getSimpleName(), getCurrentTestName()))
                                .withServer2Client(
                                        Arrays.asList(getClass().getPackage().getName(), getCurrentTestName()))),
                new SimpleImmutableEntry<>(
                        ServerSignatureAlgorithms.NAME,
                        Arrays.asList(getClass().getPackage().getName(), getClass().getSimpleName(), getCurrentTestName())),
                new SimpleImmutableEntry<>(NoFlowControl.NAME, getCurrentTestName()),
                new SimpleImmutableEntry<>(Elevation.NAME, getCurrentTestName()));
        Buffer buffer = new ByteArrayBuffer();
        KexExtensions.putExtensions(expected, buffer);

        List<Map.Entry<String, ?>> actual = new ArrayList<>(expected.size());
        KexExtensionHandler handler = new KexExtensionHandler() {
            @Override
            public boolean handleKexExtensionRequest(Session session, int index, int count, String name, byte[] data)
                    throws IOException {
                KexExtensionParser<?> parser = KexExtensions.getRegisteredExtensionParser(name);
                assertNotNull(parser, "No parser found for extension=" + name);

                Object value = parser.parseExtension(data);
                assertNotNull(value, "No value extracted for extension=" + name);
                actual.add(new SimpleImmutableEntry<>(name, value));
                return true;
            }
        };
        Session session = Mockito.mock(Session.class);
        handler.handleKexExtensionsMessage(session, buffer);

        assertEquals(expected.size(), actual.size(), "Mismatched recovered extensions count");
        for (int index = 0; index < actual.size(); index++) {
            Map.Entry<String, ?> expEntry = expected.get(index);
            String name = expEntry.getKey();
            Map.Entry<String, ?> actEntry = actual.get(index);
            assertEquals(name, actEntry.getKey(), "Mismatched extension name at index=" + index);

            Object expValue = expEntry.getValue();
            Object actValue = actEntry.getValue();
            if (expValue instanceof List<?>) {
                assertListEquals(name, (List<?>) expValue, (List<?>) actValue);
            } else {
                assertEquals(expValue, actValue, "Mismatched values for extension=" + name);
            }
        }
    }
}
