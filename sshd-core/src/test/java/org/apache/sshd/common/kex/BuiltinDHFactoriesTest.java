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

package org.apache.sshd.common.kex;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.kex.BuiltinDHFactories.ParseResult;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.util.test.BaseTestSupport;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
@TestMethodOrder(MethodName.class)
@Tag("NoIoTestCase")
public class BuiltinDHFactoriesTest extends BaseTestSupport {
    public BuiltinDHFactoriesTest() {
        super();
    }

    @Test
    void fromName() {
        for (BuiltinDHFactories expected : BuiltinDHFactories.VALUES) {
            String name = expected.getName();
            BuiltinDHFactories actual = BuiltinDHFactories.fromFactoryName(name);
            assertSame(expected, actual, name);
        }
    }

    @Test
    void allConstantsCovered() throws Exception {
        Set<BuiltinDHFactories> avail = EnumSet.noneOf(BuiltinDHFactories.class);
        Field[] fields = BuiltinDHFactories.Constants.class.getFields();
        for (Field f : fields) {
            String name = (String) f.get(null);
            BuiltinDHFactories value = BuiltinDHFactories.fromFactoryName(name);
            assertNotNull(value, "No match found for " + name);
            assertTrue(avail.add(value), name + " re-specified");
        }

        assertEquals("Incomplete coverage", BuiltinDHFactories.VALUES, avail);
    }

    @Test
    void parseDHFactorysList() {
        List<String> builtin = NamedResource.getNameList(BuiltinDHFactories.VALUES);
        List<String> unknown
                = Arrays.asList(getClass().getPackage().getName(), getClass().getSimpleName(), getCurrentTestName());
        Random rnd = new Random();
        for (int index = 0; index < (builtin.size() + unknown.size()); index++) {
            Collections.shuffle(builtin, rnd);
            Collections.shuffle(unknown, rnd);

            List<String> weavedList = new ArrayList<>(builtin.size() + unknown.size());
            for (int bIndex = 0, uIndex = 0; (bIndex < builtin.size()) || (uIndex < unknown.size());) {
                boolean useBuiltin = false;
                if (bIndex < builtin.size()) {
                    useBuiltin = uIndex >= unknown.size() || rnd.nextBoolean();
                }

                if (useBuiltin) {
                    weavedList.add(builtin.get(bIndex));
                    bIndex++;
                } else if (uIndex < unknown.size()) {
                    weavedList.add(unknown.get(uIndex));
                    uIndex++;
                }
            }

            String fullList = GenericUtils.join(weavedList, ',');
            ParseResult result = BuiltinDHFactories.parseDHFactoriesList(fullList);
            List<String> parsed = NamedResource.getNameList(result.getParsedFactories());
            List<String> missing = result.getUnsupportedFactories();

            // makes sure not only that the contents are the same but also the order
            assertListEquals(fullList + "[parsed]", builtin, parsed);
            assertListEquals(fullList + "[unsupported]", unknown, missing);
        }
    }

    @Test
    void resolveFactoryOnBuiltinValues() {
        for (DHFactory expected : BuiltinDHFactories.VALUES) {
            String name = expected.getName();
            DHFactory actual = BuiltinDHFactories.resolveFactory(name);
            assertSame(expected, actual, name);
        }
    }

    @Test
    void notAllowedToRegisterBuiltinFactories() {
        for (DHFactory expected : BuiltinDHFactories.VALUES) {
            try {
                BuiltinDHFactories.registerExtension(expected);
                fail("Unexpected success for " + expected.getName());
            } catch (IllegalArgumentException e) {
                // expected - ignored
            }
        }
    }

    @Test
    void notAllowedToOverrideRegisteredFactories() {
        assertThrows(IllegalArgumentException.class, () -> {
            DHFactory expected = Mockito.mock(DHFactory.class);
            Mockito.when(expected.getName()).thenReturn(getCurrentTestName());

            String name = expected.getName();
            try {
                for (int index = 1; index <= Byte.SIZE; index++) {
                    BuiltinDHFactories.registerExtension(expected);
                    assertEquals(1, index, "Unexpected success at attempt #" + index);
                }
            } finally {
                BuiltinDHFactories.unregisterExtension(name);
            }
        });
    }

    @Test
    void resolveFactoryOnRegisteredExtension() {
        DHFactory expected = Mockito.mock(DHFactory.class);
        Mockito.when(expected.getName()).thenReturn(getCurrentTestName());

        String name = expected.getName();
        try {
            assertNull(BuiltinDHFactories.resolveFactory(name), "Extension already registered");
            BuiltinDHFactories.registerExtension(expected);

            DHFactory actual = BuiltinDHFactories.resolveFactory(name);
            assertSame(expected, actual, "Mismatched resolved instance");
        } finally {
            DHFactory actual = BuiltinDHFactories.unregisterExtension(name);
            assertSame(expected, actual, "Mismatched unregistered instance");
            assertNull(BuiltinDHFactories.resolveFactory(name), "Extension not un-registered");
        }
    }

    @Test
    void dhg() throws Exception {
        for (DHFactory expected : BuiltinDHFactories.VALUES) {
            if (!expected.isGroupExchange()) {
                if (expected.isSupported()) {
                    assertNotNull(expected.create(), expected + ": Null DH created");
                }
            }
        }
    }

    @Test
    void dhgRead() throws Exception {
        assertArrayEquals(DHGroupData.getP1(), DHGroupData.getOakleyGroupPrimeValue("group2.prime"), "P1");
        assertArrayEquals(DHGroupData.getP14(), DHGroupData.getOakleyGroupPrimeValue("group14.prime"), "P14");
    }
}
