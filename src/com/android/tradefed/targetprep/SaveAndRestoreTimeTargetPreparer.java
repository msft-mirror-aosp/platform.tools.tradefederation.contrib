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

import java.util.concurrent.TimeUnit;

// Remembers the time at the beginning and resets it (plus time elapsed) on cleanup.
// Can also optionally set the time at the beginning.
@OptionClass(alias = "save-restore-time")
public class SaveAndRestoreTimeTargetPreparer implements ITargetCleaner {

    @Option(name = "set-time",
            description = "Whether or not to set the time")
    private boolean mSetTime = false;

    @Option(name = "time",
            description = "Time to set")
    private long mTimeToSet;

    public interface ITimeGetter {
        long getTime();
    }

    public static class RealTimeGetter implements ITimeGetter {
        @Override
        public long getTime() {
            return System.nanoTime();
        }
    }

    private long mStartNanoTime, mDeviceStartTimeMillis;
    private ITimeGetter mTimeGetter;

    public SaveAndRestoreTimeTargetPreparer(ITimeGetter timeGetter) {
        this.mTimeGetter = timeGetter;
    }

    public SaveAndRestoreTimeTargetPreparer() {
        this(new RealTimeGetter());
    }

    private long getDeviceTime(ITestDevice device) throws DeviceNotAvailableException {
        return Long.parseLong(device.executeShellCommand("date +\"%s\"").trim());
    }

    private void setDeviceTime(ITestDevice device, long time) throws DeviceNotAvailableException {
        device.executeShellCommand("date @" + time);
    }

    @Override
    public void setUp(ITestDevice device, IBuildInfo buildInfo)
            throws TargetSetupError, BuildError, DeviceNotAvailableException {
        mStartNanoTime = mTimeGetter.getTime();
        mDeviceStartTimeMillis = getDeviceTime(device);
        if (mSetTime) {
            setDeviceTime(device, mTimeToSet);
        }
    }

    @Override
    public void tearDown(ITestDevice device, IBuildInfo buildInfo, Throwable e)
            throws DeviceNotAvailableException {
        long elapsedNanos = mTimeGetter.getTime() - mStartNanoTime;
        long newTime = mDeviceStartTimeMillis + TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        setDeviceTime(device, newTime);
    }
}
