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

package com.android.performance.tests;

import com.android.tradefed.config.IConfiguration;
import com.android.tradefed.config.IConfigurationReceiver;
import com.android.tradefed.config.Option;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.IManagedTestDevice;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.device.TestDeviceState;
import com.android.tradefed.invoker.TestInformation;
import com.android.tradefed.log.LogUtil;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.targetprep.BaseLocalEmulatorPreparer;
import com.android.tradefed.testtype.IDeviceTest;
import com.android.tradefed.testtype.IRemoteTest;
import com.android.tradefed.util.RunUtil;
import com.android.tradefed.util.proto.TfMetricProtoUtil;

import com.google.common.base.Preconditions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.Callable;

/** A performance test that does repeated emulator launches and measures timings. */
public class EmulatorStartupPerfTest implements IRemoteTest, IConfigurationReceiver {
    @Option(name = "iterations", description = "number of launch iterations to perform")
    private int mIterations = 1;

    @Option(
            name = "install-apk",
            description = "path to instrumentation apk to install",
            mandatory = true)
    private File mInstallApk;

    private IConfiguration mConfig;

    @Override
    public void setConfiguration(IConfiguration configuration) {
        mConfig = configuration;
    }

    public static class EmulatorLauncher extends BaseLocalEmulatorPreparer {

        /**
         * Launches emulator with args provided via Configuration.
         *
         * <p>We want to launch emulator directly rather than going through
         * DeviceManager#launchEmulator in order to measure timing accurately: DeviceManager
         * performs several time consuming steps before returning
         */
        public void launchEmulator(ITestDevice device) throws IOException {
            List<String> args = buildEmulatorLaunchArgs();

            args.add("-read-only");
            String port = device.getSerialNumber().replace("emulator-", "");
            args.add("-port");
            args.add(port);

            Process p = new ProcessBuilder().command(args).start();
            ((IManagedTestDevice) device).setEmulatorProcess(p);
        }
    }

    @Override
    public void run(TestInformation testInfo, ITestInvocationListener listener)
            throws DeviceNotAvailableException {
        Preconditions.checkArgument(testInfo.getDevice().getIDevice().isEmulator());
        Preconditions.checkArgument(
                testInfo.getDevice().getDeviceState() == TestDeviceState.NOT_AVAILABLE);
        Preconditions.checkArgument(
                mInstallApk.exists(), mInstallApk.getAbsolutePath() + " does not exist");

        Map<String, List<Long>> timingsMap = new HashMap<>();

        // Pull the objects to perform the emulator launch
        // They are stored in config in order to support receiving Options.
        EmulatorLauncher emulatorLauncher =
                (EmulatorLauncher) mConfig.getConfigurationObject("emulator_launcher");
        IRemoteTest delegateTest = (IRemoteTest) mConfig.getConfigurationObject("delegate_test");
        ((IDeviceTest) delegateTest).setDevice(testInfo.getDevice());

        for (int i = 1; i <= mIterations; i++) {
            LogUtil.CLog.i("Performing %d iteration of %d", i, mIterations);

            try {
                long startTimeMs = System.currentTimeMillis();
                captureTime(
                        timingsMap,
                        "online_time",
                        startTimeMs,
                        () -> {
                            emulatorLauncher.launchEmulator(testInfo.getDevice());
                            testInfo.getDevice().waitForDeviceOnline(1 * 60 * 1000);
                            return null;
                        });

                captureTime(
                        timingsMap,
                        "boot_time",
                        startTimeMs,
                        () -> {
                            waitForBootComplete(testInfo.getDevice(), startTimeMs + 3 * 60 * 1000);
                            return null;
                        });

                captureTime(
                        timingsMap,
                        "install_time",
                        startTimeMs,
                        () -> {
                            // direcly install package instead of using ITestDevice.installPackage
                            // to avoid
                            // overhead of the expensive aapt parsing checks it does
                            testInfo.getDevice()
                                    .executeAdbCommand("install", mInstallApk.getAbsolutePath());
                            return null;
                        });

                captureTime(
                        timingsMap,
                        "test_time",
                        startTimeMs,
                        () -> {
                            delegateTest.run(testInfo, listener);
                            return null;
                        });

                LogUtil.CLog.i("Metrics: %s", timingsMap);

                // let devicemanager kill emulator for last iteration
                if (i < mIterations) {
                    testInfo.getDevice().executeAdbCommand("emu", "kill");
                    testInfo.getDevice().waitForDeviceNotAvailable(10 * 1000);
                }

            } catch (Exception e) {
                LogUtil.CLog.e(e);
                listener.invocationFailed(e);
            }
        }

        reportMetrics(listener, timingsMap);
    }

    private void captureTime(
            Map<String, List<Long>> timingsMap, String key, long startTimeMs, Callable<Void> action)
            throws Exception {
        action.call();
        long measuredTime = System.currentTimeMillis() - startTimeMs;
        List<Long> timesList = timingsMap.computeIfAbsent(key, k -> new ArrayList<>());
        timesList.add(measuredTime);
    }

    private void reportMetrics(
            ITestInvocationListener listener, Map<String, List<Long>> timingsMap) {
        Map<String, String> metrics = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : timingsMap.entrySet()) {
            metrics.put(entry.getKey(), Long.toString(getMedian(entry.getValue())));
        }
        LogUtil.CLog.i("About to report metrics: %s", metrics);
        listener.testRunStarted("emulator_launch", 0);
        listener.testRunEnded(0, TfMetricProtoUtil.upgradeConvert(metrics));

    }

    private static long getMedian(List<Long> items) {
        Collections.sort(items);
        int medianEntry = items.size() / 2;
        return items.get(medianEntry);
    }

    private void waitForBootComplete(ITestDevice device, long quitAfterTime)
            throws DeviceNotAvailableException {
        // we don't want to use  waitForDeviceAvailable, as that has a 3 second sleep
        // so directly query for boot complete
        while (System.currentTimeMillis() < quitAfterTime) {
            String result = device.executeShellCommand("getprop dev.bootcomplete");
            if (result.trim().equals("1")) {
                return;
            }
            RunUtil.getDefault().sleep(50);
        }
    }
}
