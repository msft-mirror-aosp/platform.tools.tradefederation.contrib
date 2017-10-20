/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tradefed.targetprep;

import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.config.Option;
import com.android.tradefed.config.OptionClass;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.util.IRunUtil;
import com.android.tradefed.util.RunUtil;

@OptionClass(alias = "restart-system-server")
public class RestartSystemServerTargetPreparer implements ITargetCleaner {
    @Option(name = "poll-interval-millis",
            description = "Time interval to poll if system server has restarted")
    private long mPollIntervalMillis = 3000L;
    @Option(name = "max-tries",
            description = "Max number of tries to poll")
    private int mMaxTries = 10;

    @Option(name = "restart-before")
    private boolean mRestartBefore = true;

    @Option(name = "restart-after")
    private boolean mRestartAfter = false;

    private IRunUtil mRunUtil;

    public RestartSystemServerTargetPreparer() {
        this(RunUtil.getDefault());
    }

    public RestartSystemServerTargetPreparer(IRunUtil runUtil) {
        this.mRunUtil = runUtil;
    }

    private boolean restartSystemServer(ITestDevice device) throws DeviceNotAvailableException {
        device.executeShellCommand("setprop sys.boot_completed 0");
        String pid = device.executeShellCommand("pidof system_server");
        device.executeShellCommand("kill " + pid);
        boolean success = false;
        for (int tries = 0; tries < mMaxTries; ++tries) {
            if (device.executeShellCommand("getprop sys.boot_completed").trim().equals("1")) {
                success = true;
                break;
            }
            mRunUtil.sleep(mPollIntervalMillis);
        }
        return success;
    }

    @Override
    public void setUp(ITestDevice device, IBuildInfo buildInfo)
            throws TargetSetupError, BuildError, DeviceNotAvailableException {
        if (!mRestartBefore) {
            return;
        }
        boolean success = restartSystemServer(device);
        if (!success) {
            throw new TargetSetupError("Gave up on waiting for system server to restart",
                    device.getDeviceDescriptor());
        }
    }

    @Override
    public void tearDown(ITestDevice device, IBuildInfo buildInfo, Throwable e)
            throws DeviceNotAvailableException {
        if (!mRestartAfter) {
            return;
        }
        boolean success = restartSystemServer(device);
        if (!success) {
            CLog.i("Failed to restart system server on tearDown.");
        }
    }
}
