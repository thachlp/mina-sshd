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

package org.apache.sshd.common.io;

import java.util.Objects;

import org.apache.sshd.util.test.BaseTestSupport;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
@TestMethodOrder(MethodName.class)
public class BuiltinIoServiceFactoryFactoriesTest extends BaseTestSupport {
    public BuiltinIoServiceFactoryFactoriesTest() {
        super();
    }

    @Test
    void fromFactoryName() {
        for (String name : new String[] { null, "", getCurrentTestName() }) {
            assertNull(BuiltinIoServiceFactoryFactories.fromFactoryName(name), "Unexpected success for name='" + name + "'");
        }

        for (BuiltinIoServiceFactoryFactories expected : BuiltinIoServiceFactoryFactories.VALUES) {
            String name = expected.getName();
            assertSame(expected, BuiltinIoServiceFactoryFactories.fromFactoryName(name), name);
        }
    }

    @Test
    void fromFactoryClass() {
        IoServiceFactoryFactory ioServiceProvider = getIoServiceProvider();
        Class<?> providerClass = ioServiceProvider.getClass();
        String providerClassName = providerClass.getName();
        for (BuiltinIoServiceFactoryFactories expected : BuiltinIoServiceFactoryFactories.VALUES) {
            if (!expected.isSupported()) {
                outputDebugMessage("Skip unsupported: %s", expected);
                continue;
            }

            if (!Objects.equals(providerClassName, expected.getFactoryClassName())) {
                outputDebugMessage("Skip mismatched factory class name: %s", expected);
                continue;
            }

            outputDebugMessage("Testing: %s", expected);
            Class<?> clazz = expected.getFactoryClass();
            assertSame(expected, BuiltinIoServiceFactoryFactories.fromFactoryClass(clazz), clazz.getSimpleName());
        }
    }

    @Test
    void classNames() {
        IoServiceFactoryFactory ioServiceProvider = getIoServiceProvider();
        Class<?> providerClass = ioServiceProvider.getClass();
        String providerClassName = providerClass.getName();
        boolean found = false;
        for (BuiltinIoServiceFactoryFactories builtin : BuiltinIoServiceFactoryFactories.VALUES) {
            if (providerClassName.equals(builtin.getFactoryClassName())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "No BuiltinIoServiceFactoryFactories match for class name " + providerClassName);
    }
}
