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

package org.apache.sshd.sftp.client.extensions.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.sshd.common.digest.BuiltinDigests;
import org.apache.sshd.common.digest.Digest;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.buffer.BufferUtils;
import org.apache.sshd.common.util.io.IoUtils;
import org.apache.sshd.sftp.client.AbstractSftpClientTestSupport;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.CloseableHandle;
import org.apache.sshd.sftp.client.extensions.MD5FileExtension;
import org.apache.sshd.sftp.client.extensions.MD5HandleExtension;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.apache.sshd.util.test.CommonTestSupportUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
@TestMethodOrder(MethodName.class) // see https://github.com/junit-team/junit/wiki/Parameterized-tests
public class AbstractMD5HashExtensionTest extends AbstractSftpClientTestSupport {
    private static final List<Integer> DATA_SIZES = Collections.unmodifiableList(
            Arrays.asList(
                    (int) Byte.MAX_VALUE,
                    SftpConstants.MD5_QUICK_HASH_SIZE,
                    IoUtils.DEFAULT_COPY_SIZE,
                    Byte.SIZE * IoUtils.DEFAULT_COPY_SIZE));

    private int size;

    public void initAbstractMD5HashExtensionTest(int size) throws IOException {
        this.size = size;
    }

    public static Collection<Object[]> parameters() {
        return parameterize(DATA_SIZES);
    }

    @BeforeAll
    static void checkMD5Supported() {
        Assumptions.assumeTrue(BuiltinDigests.md5.isSupported(), "MD5 not supported");
    }

    @BeforeEach
    void setUp() throws Exception {
        setupServer();
    }

    @MethodSource("parameters")
    @ParameterizedTest(name = "dataSize={0}")
    public void md5HashExtension(int size) throws Exception {
        initAbstractMD5HashExtensionTest(size);
        testMD5HashExtension(size);
    }

    private void testMD5HashExtension(int dataSize) throws Exception {
        byte[] seed = (getClass().getName() + "#" + getCurrentTestName() + "-" + dataSize + IoUtils.EOL)
                .getBytes(StandardCharsets.UTF_8);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(dataSize + seed.length)) {
            while (baos.size() < dataSize) {
                baos.write(seed);
            }

            testMD5HashExtension(baos.toByteArray());
        }
    }

    @SuppressWarnings("checkstyle:nestedtrydepth")
    private void testMD5HashExtension(byte[] data) throws Exception {
        Digest digest = BuiltinDigests.md5.create();
        digest.init();
        digest.update(data);

        byte[] expectedHash = digest.digest();
        byte[] quickHash = expectedHash;
        if (data.length > SftpConstants.MD5_QUICK_HASH_SIZE) {
            byte[] quickData = new byte[SftpConstants.MD5_QUICK_HASH_SIZE];
            System.arraycopy(data, 0, quickData, 0, quickData.length);
            digest = BuiltinDigests.md5.create();
            digest.init();
            digest.update(quickData);
            quickHash = digest.digest();
        }

        Path targetPath = detectTargetFolder();
        Path lclSftp
                = CommonTestSupportUtils.resolve(targetPath, SftpConstants.SFTP_SUBSYSTEM_NAME, getClass().getSimpleName());
        Path srcFile = assertHierarchyTargetFolderExists(lclSftp).resolve("data-" + data.length + ".txt");
        Files.write(srcFile, data, IoUtils.EMPTY_OPEN_OPTIONS);

        Path parentPath = targetPath.getParent();
        String srcPath = CommonTestSupportUtils.resolveRelativeRemotePath(parentPath, srcFile);
        String srcFolder = CommonTestSupportUtils.resolveRelativeRemotePath(parentPath, srcFile.getParent());
        try (SftpClient sftp = createSingleSessionClient()) {
            MD5FileExtension file = assertExtensionCreated(sftp, MD5FileExtension.class);
            try {
                byte[] actual = file.getHash(srcFolder, 0L, 0L, quickHash);
                fail("Unexpected file success on folder=" + srcFolder + ": " + BufferUtils.toHex(':', actual));
            } catch (IOException e) { // expected - not allowed to hash a folder
                assertTrue(e instanceof SftpException, "Not an SftpException for file hash on " + srcFolder);
            }

            MD5HandleExtension hndl = assertExtensionCreated(sftp, MD5HandleExtension.class);
            try (CloseableHandle dirHandle = sftp.openDir(srcFolder)) {
                try {
                    byte[] actual = hndl.getHash(dirHandle, 0L, 0L, quickHash);
                    fail("Unexpected handle success on folder=" + srcFolder + ": " + BufferUtils.toHex(':', actual));
                } catch (IOException e) { // expected - not allowed to hash a folder
                    assertTrue(e instanceof SftpException, "Not an SftpException for handle hash on " + srcFolder);
                }
            }

            try (CloseableHandle fileHandle = sftp.open(srcPath, SftpClient.OpenMode.Read)) {
                for (byte[] qh : new byte[][] { GenericUtils.EMPTY_BYTE_ARRAY, quickHash }) {
                    for (boolean useFile : new boolean[] { true, false }) {
                        byte[] actualHash
                                = useFile ? file.getHash(srcPath, 0L, 0L, qh) : hndl.getHash(fileHandle, 0L, 0L, qh);
                        String type = useFile ? file.getClass().getSimpleName() : hndl.getClass().getSimpleName();
                        if (!Arrays.equals(expectedHash, actualHash)) {
                            fail("Mismatched hash for quick=" + BufferUtils.toHex(':', qh)
                                 + " using " + type + " on " + srcFile
                                 + ": expected=" + BufferUtils.toHex(':', expectedHash)
                                 + ", actual=" + BufferUtils.toHex(':', actualHash));
                        }
                    }
                }
            }
        }
    }
}
