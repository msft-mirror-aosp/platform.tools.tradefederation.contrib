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

@OptionClass(alias = "fill-storage")
public class FillStorageTargetPreparer implements ITargetCleaner {

    private static final String FILL_STORAGE_FILENAME = "/data/bigfile";

    @Option(name = "free-bytes",
            description = "Number of bytes that should be left free on the device.")
    private long mFreeBytesRequested;

    private long getDataFreeSpace(ITestDevice device) throws DeviceNotAvailableException {
        String output = device.executeShellCommand("df /data");
        String[] lines = output.split("\n");
        String[] splitLines = lines[1].split("\\s+");
        return Long.parseLong(splitLines[3]) * 1024L;
    }

    @Override
    public void setUp(ITestDevice device, IBuildInfo buildInfo)
            throws TargetSetupError, BuildError, DeviceNotAvailableException {
        long freeSpace = getDataFreeSpace(device);
        if (freeSpace > mFreeBytesRequested) {
            long bytesToWrite = freeSpace - mFreeBytesRequested;
            device.executeShellCommand(
                    String.format("fallocate -l %d %s", bytesToWrite, FILL_STORAGE_FILENAME));
            CLog.i("Wrote %d bytes to %s", bytesToWrite, FILL_STORAGE_FILENAME);
        } else {
            CLog.i("Not enough free space (%d bytes requested free, %d bytes actually free)",
                    mFreeBytesRequested, freeSpace);
        }
    }

    @Override
    public void tearDown(ITestDevice device, IBuildInfo buildInfo, Throwable e)
            throws DeviceNotAvailableException {
        device.executeShellCommand("rm -f " + FILL_STORAGE_FILENAME);
    }
}
