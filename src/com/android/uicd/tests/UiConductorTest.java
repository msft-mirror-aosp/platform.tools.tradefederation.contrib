/*
 * Copyright (C) 2019 The Android Open Source Project
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

import com.android.tradefed.config.Option;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.invoker.TestInformation;
import com.android.tradefed.invoker.logger.CurrentInvocation;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.FileInputStreamSource;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.result.InputStreamSource;
import com.android.tradefed.result.LogDataType;
import com.android.tradefed.result.TestDescription;
import com.android.tradefed.testtype.IRemoteTest;
import com.android.tradefed.util.CommandResult;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.IRunUtil;
import com.android.tradefed.util.MultiMap;
import com.android.tradefed.util.RunUtil;

import com.google.common.annotations.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The class enables user to run their pre-recorded UICD tests on tradefed. Go to
 * https://github.com/google/android-uiconductor/releases/tag/v0.1.1 to download the uicd_cli.tar.gz
 * and extract the jar and apks required for the tests. Please look at the sample xmls in
 * res/config/uicd to configure your tests.
 */
public class UiConductorTest implements IRemoteTest {

    static final String MODULE_NAME = UiConductorTest.class.getSimpleName();
    static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30L);

    static final String INPUT_OPTION = "--input";
    static final String OUTPUT_OPTION = "--output";
    static final String DEVICES_OPTION = "--devices";
    static final String MODE_OPTION = "--mode";
    static final String GLOBAL_VARIABLE_OPTION = "--global_variable";

    @Option(
            name = "uicd-cli-jar",
            description = "UICD CLI jar to use when running tests",
            mandatory = true)
    private File mCliJar;

    @Option(
            name = "commandline-action-executable",
            description = "Additional binaries needed by command line actions. Can be repeated.")
    private Collection<File> mBinaries = new ArrayList<>();

    @Option(
            name = "global-variables",
            description = "Global variable (uicd_key1=value1,uicd_key2=value2)")
    private MultiMap<String, String> mGlobalVariables = new MultiMap<>();

    @Option(name = "play-mode", description = "Play mode (SINGLE|MULTIDEVICE|PLAYALL)")
    private String mPlayMode = "SINGLE";

    // Same key can have multiple test files because global-variables can be referenced using the
    // that particular key and shared across different tests.
    // Refer res/config/uicd/uiconductor-globalvariable-sample.xml for more information.
    @Option(
            name = "uicd-test",
            description = "JSON test file or directory of JSON test files to run. Can be repeated.",
            mandatory = true)
    private MultiMap<String, File> mTests = new MultiMap<>();

    @Option(name = "test-timeout", description = "Timeout for each test case")
    private Duration mTestTimeout = DEFAULT_TIMEOUT;

    private static final String BINARY_RELATIVE_PATH = "binary";

    private static final String OUTPUT_RELATIVE_PATH = "output";

    private static final String TESTS_RELATIVE_PATH = "tests";

    private static final String RESULTS_RELATIVE_PATH = "result";

    private static final String CHILDRENRESULT_ATTRIBUTE = "childrenResult";
    private static final String PLAYSTATUS_ATTRIBUTE = "playStatus";
    private static final String VALIDATIONDETAILS_ATTRIBUTE = "validationDetails";

    private static final String EXECUTABLE = "u+x";

    private IRunUtil mRunUtil;
    private Path mWorkDir;
    private List<ITestDevice> mDevices;

    @Override
    public void run(TestInformation testInfo, ITestInvocationListener listener)
            throws DeviceNotAvailableException {
        mWorkDir = createWorkDir();
        mDevices = testInfo.getDevices();
        CLog.i("Starting the UIConductor tests:\n");
        String jarFileDir = mWorkDir.resolve(BINARY_RELATIVE_PATH).toString();
        String testFilesDir = mWorkDir.resolve(TESTS_RELATIVE_PATH).toString();
        String binaryFilesDir = mWorkDir.toString();
        File jarFile;
        MultiMap<String, File> copiedTestFileMap = new MultiMap<>();
        if (mCliJar == null || !mCliJar.exists()) {
            CLog.e("Unable to fetch provided binary.\n");
            return;
        }
        try {
            jarFile = copyFile(mCliJar.getAbsolutePath(), jarFileDir);
            FileUtil.chmod(jarFile, EXECUTABLE);

            for (Map.Entry<String, File> testFileOrDirEntry : mTests.entries()) {
                copiedTestFileMap.putAll(
                        copyFile(
                                testFileOrDirEntry.getKey(),
                                testFileOrDirEntry.getValue().getAbsolutePath(),
                                testFilesDir));
            }

            for (File binaryFile : mBinaries) {
                File binary = copyFile(binaryFile.getAbsolutePath(), binaryFilesDir);
                FileUtil.chmod(binary, EXECUTABLE);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        mRunUtil = createRunUtil();
        mRunUtil.setWorkingDir(mWorkDir.toFile());
        long runStartTime = System.currentTimeMillis();
        listener.testRunStarted(MODULE_NAME, copiedTestFileMap.values().size());
        for (Map.Entry<String, File> testFileEntry : copiedTestFileMap.entries()) {
            runTest(
                    listener,
                    jarFile,
                    testFileEntry.getKey(),
                    testFileEntry.getValue().getName());
        }

        listener.testRunEnded(
                System.currentTimeMillis() - runStartTime, new HashMap<String, String>());
        CLog.i("Finishing the ui conductor tests\n");
    }

    /** @return {@link IRunUtil} instance to use */
    @VisibleForTesting
    IRunUtil createRunUtil() {
        return new RunUtil();
    }

    /** @return working directory to use */
    @VisibleForTesting
    Path createWorkDir() {
        try {
            return FileUtil.createTempDir(MODULE_NAME, CurrentInvocation.getWorkFolder()).toPath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void runTest(
            ITestInvocationListener listener, File jarFile, String key, String testFileName) {
        TestDescription testDesc = new TestDescription(MODULE_NAME, testFileName);
        listener.testStarted(testDesc, System.currentTimeMillis());

        String testId = UUID.randomUUID().toString();
        String[] command = buildCommand(jarFile, testFileName, testId, key);
        CommandResult commandResult = mRunUtil.runTimedCmd(mTestTimeout.toMillis(), command);
        logInfo(testId, "STD", commandResult.getStdout());
        logInfo(testId, "ERR", commandResult.getStderr());

        File resultsFile =
                mWorkDir.resolve(OUTPUT_RELATIVE_PATH)
                        .resolve(testId)
                        .resolve(RESULTS_RELATIVE_PATH)
                        .resolve("action_execution_result")
                        .toFile();

        if (resultsFile.exists()) {
            try {
                String content = FileUtil.readStringFromFile(resultsFile);
                JSONObject result = new JSONObject(content);
                List<String> errors = new ArrayList<>();
                errors = parseResult(errors, result);
                if (!errors.isEmpty()) {
                    listener.testFailed(testDesc, errors.get(0));
                    CLog.i("Test %s failed due to following errors: \n", testDesc.getTestName());
                    for (String error : errors) {
                        CLog.i(error + "\n");
                    }
                }
            } catch (IOException | JSONException e) {
                CLog.e(e);
            }
            String testResultFileName = testFileName + "_action_execution_result";
            try (InputStreamSource iSSource = new FileInputStreamSource(resultsFile)) {
                listener.testLog(testResultFileName, LogDataType.TEXT, iSSource);
            }
        }
        listener.testEnded(testDesc, System.currentTimeMillis(), new HashMap<String, String>());
    }

    private void logInfo(String testId, String cmdOutputType, String content) {
        CLog.i(
                "==========================="
                        + cmdOutputType
                        + " logs for "
                        + testId
                        + " starts===========================\n");
        CLog.i(content);
        CLog.i(
                "==========================="
                        + cmdOutputType
                        + " logs for "
                        + testId
                        + " ends===========================\n");
    }

    private List<String> parseResult(List<String> errors, JSONObject result) throws JSONException {

        if (result != null) {
            if (result.has(CHILDRENRESULT_ATTRIBUTE)) {
                JSONArray childResults = result.getJSONArray(CHILDRENRESULT_ATTRIBUTE);
                for (int i = 0; i < childResults.length(); i++) {
                    errors = parseResult(errors, childResults.getJSONObject(i));
                }
            }

            if (result.has(PLAYSTATUS_ATTRIBUTE)
                    && result.getString(PLAYSTATUS_ATTRIBUTE).equalsIgnoreCase("FAIL")) {
                if (result.has(VALIDATIONDETAILS_ATTRIBUTE)) {
                    errors.add(result.getString(VALIDATIONDETAILS_ATTRIBUTE));
                }
            }
        }
        return errors;
    }

    private File copyFile(String srcFilePath, String destDirPath) throws IOException {
        File srcFile = new File(srcFilePath);
        File destDir = new File(destDirPath);
        if (srcFile.isDirectory()) {
            for (File file : srcFile.listFiles()) {
                copyFile(file.getAbsolutePath(), Paths.get(destDirPath, file.getName()).toString());
            }
        }
        if (!destDir.isDirectory() && !destDir.mkdirs()) {
            throw new IOException(
                    String.format("Could not create directory %s", destDir.getAbsolutePath()));
        }
        File destFile = new File(Paths.get(destDir.toString(), srcFile.getName()).toString());
        FileUtil.copyFile(srcFile, destFile);
        return destFile;
    }

    // copy file to destDirPath while maintaining a map of key that refers to that src file
    private MultiMap<String, File> copyFile(String key, String srcFilePath, String destDirPath)
            throws IOException {
        MultiMap<String, File> copiedTestFileMap = new MultiMap<>();
        File srcFile = new File(srcFilePath);
        File destDir = new File(destDirPath);
        if (srcFile.isDirectory()) {
            for (File file : srcFile.listFiles()) {
                copiedTestFileMap.putAll(
                        copyFile(
                                key,
                                file.getAbsolutePath(),
                                Paths.get(destDirPath, file.getName()).toString()));
            }
        }
        if (!destDir.isDirectory() && !destDir.mkdirs()) {
            throw new IOException(
                    String.format("Could not create directory %s", destDir.getAbsolutePath()));
        }
        if (srcFile.isFile()) {
            File destFile = new File(Paths.get(destDir.toString(), srcFile.getName()).toString());
            FileUtil.copyFile(srcFile, destFile);
            copiedTestFileMap.put(key, destFile);
        }
        return copiedTestFileMap;
    }

    private String getTestFilesArgsForUicdBin(String testFilesDir, String filename) {
        return (!testFilesDir.isEmpty() && !filename.isEmpty())
                ? Paths.get(testFilesDir, filename).toString()
                : "";
    }

    private String getOutFilesArgsForUicdBin(String outFilesDir) {
        return !outFilesDir.isEmpty() ? outFilesDir : "";
    }

    private String getPlaymodeArgForUicdBin() {
        return !mPlayMode.isEmpty() ? mPlayMode : "";
    }

    private String getDevIdsArgsForUicdBin() {
        List<String> devIds = new ArrayList<>();
        for (ITestDevice device : mDevices) {
            devIds.add(device.getSerialNumber());
        }
        return String.join(",", devIds);
    }

    private String[] buildCommand(File jarFile, String testFileName, String testId, String key) {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(jarFile.getAbsolutePath());
        if (!getTestFilesArgsForUicdBin(TESTS_RELATIVE_PATH, testFileName).isEmpty()) {
            command.add(INPUT_OPTION);
            command.add(getTestFilesArgsForUicdBin(TESTS_RELATIVE_PATH, testFileName));
        }
        if (!getOutFilesArgsForUicdBin(OUTPUT_RELATIVE_PATH + "/" + testId).isEmpty()) {
            command.add(OUTPUT_OPTION);
            command.add(getOutFilesArgsForUicdBin(OUTPUT_RELATIVE_PATH + "/" + testId));
        }
        if (!getPlaymodeArgForUicdBin().isEmpty()) {
            command.add(MODE_OPTION);
            command.add(getPlaymodeArgForUicdBin());
        }
        if (!getDevIdsArgsForUicdBin().isEmpty()) {
            command.add(DEVICES_OPTION);
            command.add(getDevIdsArgsForUicdBin());
        }
        if (mGlobalVariables.containsKey(key)) {
            command.add(GLOBAL_VARIABLE_OPTION);
            command.add(String.join(",", mGlobalVariables.get(key)));
        }
        return command.toArray(new String[] {});
    }
}
