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

import java.util.HashMap;
import java.util.Map;

@OptionClass(alias = "set-restore-sys-prop")
public class SetAndRestoreSystemPropertyTargetPreparer implements ITargetCleaner {

    @Option(name = "set-property",
            description = "Property to set then restore on cleanup")
    private Map<String, String> mProperties = new HashMap<>();

    private Map<String, String> mOldProperties = new HashMap<>();

    private void setProperty(ITestDevice device, String prop, String newValue)
            throws DeviceNotAvailableException {
        device.executeShellCommand(String.format("setprop \"%s\" \"%s\"", prop, newValue));
    }

    private String getProperty(ITestDevice device, String prop)
            throws DeviceNotAvailableException {
        return device.executeShellCommand(String.format("getprop \"%s\"", prop)).trim();
    }

    @Override
    public void setUp(ITestDevice device, IBuildInfo buildInfo)
            throws TargetSetupError, BuildError, DeviceNotAvailableException {
        for (String prop : mProperties.keySet()) {
            String oldValue = getProperty(device, prop);
            mOldProperties.put(prop, oldValue);
            String newValue = mProperties.get(prop);
            setProperty(device, prop, newValue);
            if (!getProperty(device, prop).equals(newValue)) {
                throw new TargetSetupError("Failed to set property " + prop,
                        device.getDeviceDescriptor());
            }
        }
    }

    @Override
    public void tearDown(ITestDevice device, IBuildInfo buildInfo, Throwable e)
            throws DeviceNotAvailableException {
        for (Map.Entry<String, String> entry : mOldProperties.entrySet()) {
            setProperty(device, entry.getKey(), entry.getValue());
        }
    }
}
