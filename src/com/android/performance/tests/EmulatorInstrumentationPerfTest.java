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

import com.android.ddmlib.Log;
import com.android.tradefed.config.IConfiguration;
import com.android.tradefed.config.IConfigurationReceiver;
import com.android.tradefed.config.Option;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.invoker.TestInformation;
import com.android.tradefed.log.LogUtil;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.targetprep.BuildError;
import com.android.tradefed.targetprep.ITargetPreparer;
import com.android.tradefed.targetprep.TargetSetupError;
import com.android.tradefed.testtype.IDeviceTest;
import com.android.tradefed.testtype.IRemoteTest;

/**
 * A performance test that does repeated emulator launch + run instrumentation test.
 *
 * <p>Intended to be paired with a metrics collector like EmulatorMemoryCpuCollector to measure
 */
public class EmulatorInstrumentationPerfTest implements IRemoteTest, IConfigurationReceiver {
    @Option(name = "iterations", description = "number of launch + run test iterations to perform")
    private int mIterations = 1;

    private IConfiguration mConfig;

    @Override
    public void setConfiguration(IConfiguration configuration) {
        mConfig = configuration;
    }

    @Override
    public void run(TestInformation testInfo, ITestInvocationListener listener)
            throws DeviceNotAvailableException {
        for (int i = 1; i <= mIterations; i++) {
            LogUtil.CLog.logAndDisplay(
                    Log.LogLevel.INFO, "Performing %d iteration of %d", i, mIterations);

            try {
                // Pull the objects to perform the emulator launch and the actual instrumentation
                // out of the config
                // They are stored in config in order to support receiving Options.
                ITargetPreparer emulatorLaunchPreparer =
                        (ITargetPreparer)
                                mConfig.getConfigurationObject("emulator_launch_preparer");
                IRemoteTest instrumentationRemoteTest =
                        (IRemoteTest) mConfig.getConfigurationObject("delegate_test");
                emulatorLaunchPreparer.setUp(testInfo);
                ((IDeviceTest) instrumentationRemoteTest).setDevice(testInfo.getDevice());
                instrumentationRemoteTest.run(testInfo, listener);

                // don't kill the device on the last iteration, as tradefed will insist on doing
                // this
                if (i < mIterations) {
                    testInfo.getDevice().executeAdbCommand("emu", "kill");

                    testInfo.getDevice().waitForDeviceNotAvailable(10 * 1000);
                }
            } catch (TargetSetupError | BuildError e) {
                LogUtil.CLog.e(e);
            }
        }
    }
}
