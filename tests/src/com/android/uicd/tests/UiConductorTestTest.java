/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.uicd.tests;

import static com.android.uicd.tests.UiConductorTest.DEVICES_OPTION;
import static com.android.uicd.tests.UiConductorTest.GLOBAL_VARIABLE_OPTION;
import static com.android.uicd.tests.UiConductorTest.INPUT_OPTION;
import static com.android.uicd.tests.UiConductorTest.MODE_OPTION;
import static com.android.uicd.tests.UiConductorTest.MODULE_NAME;
import static com.android.uicd.tests.UiConductorTest.DEFAULT_TIMEOUT;
import static com.android.uicd.tests.UiConductorTest.OUTPUT_OPTION;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.android.tradefed.config.OptionSetter;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.invoker.TestInformation;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.result.TestDescription;
import com.android.tradefed.util.CommandResult;
import com.android.tradefed.util.CommandStatus;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.IRunUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Unit tests for {@link UiConductorTest}. */
@RunWith(JUnit4.class)
public class UiConductorTestTest {

    private static final String DEVICE_SERIAL = "SERIAL";
    private static final String TEST_KEY = "TEST";

    @Rule public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock private TestInformation mTestInfo;
    @Mock private ITestInvocationListener mListener;
    @Mock private IRunUtil mRunUtil;
    @Mock private ITestDevice mDevice;

    private UiConductorTest mTest;
    private OptionSetter mOptionSetter;
    private Path mInputDir;
    private Path mWorkDir;
    private Path mCliJar;
    private Path mTestFile;

    @Before
    public void setUp() throws Exception {
        // Create temporary directories
        mInputDir = Files.createTempDirectory(this.getClass().getSimpleName());
        mWorkDir = Files.createTempDirectory(this.getClass().getSimpleName());

        // Initialize test and test information
        mTest = new UiConductorTest() {
            @Override
            IRunUtil createRunUtil() {
                return mRunUtil;
            }

            @Override
            Path createWorkDir() {
                return mWorkDir;
            }
        };
        mOptionSetter = new OptionSetter(mTest);
        when(mTestInfo.getDevices()).thenReturn(List.of(mDevice));
        when(mDevice.getSerialNumber()).thenReturn(DEVICE_SERIAL);

        // Create minimum set of files
        mCliJar = Files.createFile(mInputDir.resolve("cli.jar"));
        mOptionSetter.setOptionValue("uicd-cli-jar", mCliJar.toString());
        mTestFile = Files.createFile(mInputDir.resolve("test.json"));
        mOptionSetter.setOptionValue("uicd-test", TEST_KEY, mTestFile.toString());

        // Default to successful execution
        CommandResult result = new CommandResult(CommandStatus.SUCCESS);
        when(mRunUtil.runTimedCmd(anyLong(), any())).thenReturn(result);
    }

    @After
    public void tearDown() {
        FileUtil.recursiveDelete(mInputDir.toFile());
        FileUtil.recursiveDelete(mWorkDir.toFile());
    }

    @Test
    public void testRun() throws Exception {
        mTest.run(mTestInfo, mListener);
        // CLI was launched once with right arguments
        verify(mRunUtil).runTimedCmd(eq(DEFAULT_TIMEOUT.toMillis()),
                eq("java"), eq("-jar"), anyString(),
                eq(INPUT_OPTION), anyString(),
                eq(OUTPUT_OPTION), anyString(),
                eq(MODE_OPTION), eq("SINGLE"),
                eq(DEVICES_OPTION), eq(DEVICE_SERIAL));
        // A single test was run with no failures
        TestDescription test = new TestDescription(MODULE_NAME, "test.json");
        verify(mListener).testStarted(eq(test), anyLong());
        verify(mListener, never()).testFailed(any(), anyString());
        verify(mListener).testEnded(eq(test), anyLong(), anyMap());
    }

    @Ignore // TODO(b/173457826): Fix file management
    @Test(expected = IllegalArgumentException.class)
    public void testRun_cliNotFound() throws Exception {
        mCliJar.toFile().delete();
        mTest.run(mTestInfo, mListener);
    }

    @Ignore // TODO(b/173457810): Fix command error handling
    @Test
    public void testRun_timeout() throws Exception {
        CommandResult result = new CommandResult(CommandStatus.TIMED_OUT);
        when(mRunUtil.runTimedCmd(anyLong(), any())).thenReturn(result);
        mTest.run(mTestInfo, mListener);
        verify(mListener).testFailed(any(), anyString());
    }

    @Ignore // TODO(b/173457810): Fix command error handling
    @Test
    public void testRun_error() throws Exception {
        CommandResult result = new CommandResult(CommandStatus.EXCEPTION);
        when(mRunUtil.runTimedCmd(anyLong(), any())).thenReturn(result);
        mTest.run(mTestInfo, mListener);
        verify(mListener).testFailed(any(), anyString());
    }

    @Test
    public void testRun_failure() throws Exception {
        // TODO(b/173457810): Verify test result parsing after fixing file management
    }

    @Test
    public void testRun_testResultNotFound() throws Exception {
        // TODO(b/173457810): Verify test result parsing after fixing file management
    }

    @Test
    public void testRun_invalidTestResult() throws Exception {
        // TODO(b/173457810): Verify test result parsing after fixing file management
    }

    @Ignore // TODO(b/173457826): Fix file management
    @Test(expected = IllegalArgumentException.class)
    public void testRun_testNotFound() throws Exception {
        mTestFile.toFile().delete();
        mTest.run(mTestInfo, mListener);
    }

    @Test
    public void testRun_testDirectory() throws Exception {
        // TODO(b/173457826): Verify test directory handling after fixing file management
    }

    @Test
    public void testRun_binaryFiles() throws Exception {
        Path binary = Files.createFile(mInputDir.resolve("binary.sh"));
        mOptionSetter.setOptionValue("commandline-action-executable", binary.toString());
        mTest.run(mTestInfo, mListener);
        // Additional binary was copied into the working directory and set to executable
        File binaryFile = mWorkDir.resolve("binary.sh").toFile();
        assertTrue(binaryFile.exists());
        assertTrue(binaryFile.canExecute());
    }

    @Test
    public void testRun_playMode() throws Exception {
        mOptionSetter.setOptionValue("play-mode", "PLAYALL");
        mTest.run(mTestInfo, mListener);
        // Play mode was modified
        verify(mRunUtil).runTimedCmd(eq(DEFAULT_TIMEOUT.toMillis()),
                eq("java"), eq("-jar"), anyString(),
                eq(INPUT_OPTION), anyString(),
                eq(OUTPUT_OPTION), anyString(),
                eq(MODE_OPTION), eq("PLAYALL"),
                eq(DEVICES_OPTION), eq(DEVICE_SERIAL));
    }

    @Test
    public void testRun_globalVariables() throws Exception {
        mOptionSetter.setOptionValue("global-variables", TEST_KEY, "key1=value1");
        mOptionSetter.setOptionValue("global-variables", TEST_KEY, "key2=value2");
        mTest.run(mTestInfo, mListener);
        // Global variables were concatenated and added
        verify(mRunUtil).runTimedCmd(eq(DEFAULT_TIMEOUT.toMillis()),
                eq("java"), eq("-jar"), anyString(),
                eq(INPUT_OPTION), anyString(),
                eq(OUTPUT_OPTION), anyString(),
                eq(MODE_OPTION), eq("SINGLE"),
                eq(DEVICES_OPTION), eq(DEVICE_SERIAL),
                eq(GLOBAL_VARIABLE_OPTION), eq("key1=value1,key2=value2"));
    }
}
