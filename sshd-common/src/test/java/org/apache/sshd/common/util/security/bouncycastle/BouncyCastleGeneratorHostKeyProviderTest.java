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

package org.apache.sshd.common.util.security.bouncycastle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sshd.common.cipher.ECCurves;
import org.apache.sshd.common.config.keys.BuiltinIdentities;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.util.test.JUnitTestSupport;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
@TestMethodOrder(MethodName.class) // see https://github.com/junit-team/junit/wiki/Parameterized-tests
@Tag("NoIoTestCase")
public class BouncyCastleGeneratorHostKeyProviderTest extends JUnitTestSupport {
    private String keyType;
    private int keySize;

    public void initBouncyCastleGeneratorHostKeyProviderTest(String keyType, int keySize) {
        this.keyType = keyType;
        this.keySize = keySize;
    }

    public static List<Object[]> parameters() {
        if (!SecurityUtils.isBouncyCastleRegistered()) {
            return Collections.emptyList();
        }

        List<Object[]> params = new ArrayList<>();
        for (Integer ks : RSA_SIZES) {
            params.add(new Object[] { BuiltinIdentities.Constants.RSA, ks });
        }
        for (Integer ks : DSS_SIZES) {
            params.add(new Object[] { BuiltinIdentities.Constants.DSA, ks });
        }

        /*
         * TODO - causes an issue where BC cannot parse its own file if (SecurityUtils.isECCSupported()) { for (ECCurves
         * curve : ECCurves.VALUES) { params.add(new Object[]{BuiltinIdentities.Constants.ECDSA, curve.getKeySize()}); }
         * }
         */
        return params;
    }

    @MethodSource("parameters")
    @ParameterizedTest(name = "{0} / {1}")
    public void keyReadWrite(String keyType, int keySize) throws IOException, GeneralSecurityException {
        initBouncyCastleGeneratorHostKeyProviderTest(keyType, keySize);
        KeyPair expected;
        if (BuiltinIdentities.Constants.RSA.equalsIgnoreCase(keyType)) {
            expected = KeyUtils.generateKeyPair(KeyPairProvider.SSH_RSA, keySize);
        } else if (BuiltinIdentities.Constants.DSA.equalsIgnoreCase(keyType)) {
            expected = KeyUtils.generateKeyPair(KeyPairProvider.SSH_DSS, keySize);
        } else if (BuiltinIdentities.Constants.ECDSA.equalsIgnoreCase(keyType)) {
            ECCurves curve = ECCurves.fromCurveSize(keySize);
            assertNotNull(curve, "No curve for key size=" + keySize);
            expected = KeyUtils.generateKeyPair(curve.getKeyType(), curve.getKeySize());
        } else if (BuiltinIdentities.Constants.ED25519.equalsIgnoreCase(keyType)) {
            KeyPairGenerator g = SecurityUtils.getKeyPairGenerator(SecurityUtils.EDDSA);
            expected = g.generateKeyPair();
        } else {
            throw new InvalidKeyException("Unsupported key type: " + keyType);
        }

        PublicKey key = expected.getPublic();
        String keyAlgorithm = key.getAlgorithm();
        if (BuiltinIdentities.Constants.ECDSA.equalsIgnoreCase(keyAlgorithm)) {
            keyAlgorithm = KeyUtils.EC_ALGORITHM;
        } else if (BuiltinIdentities.Constants.ED25519.equalsIgnoreCase(keyAlgorithm)) {
            keyAlgorithm = SecurityUtils.EDDSA;
        }

        Path dir = getTempTargetFolder();
        dir = Files.createDirectories(dir.resolve(getClass().getSimpleName()));
        Path file = dir.resolve(keyType + "-" + keySize + ".pem");
        BouncyCastleGeneratorHostKeyProvider.writePEMKeyPair(expected, file);

        AbstractGeneratorHostKeyProvider provider = new BouncyCastleGeneratorHostKeyProvider(file);
        provider.setAlgorithm(keyAlgorithm);

        Iterable<KeyPair> keys = provider.loadKeys(null);
        KeyPair actual = null;
        for (KeyPair k : keys) {
            assertNull(actual, "Unexpected multiple keys loaded");
            actual = k;
        }

        assertKeyPairEquals(keyType + "/" + keySize, expected, actual);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + keyType + "/" + keySize + "]";
    }
}
