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
import com.android.tradefed.config.OptionSetter;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.util.IRunUtil;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit Tests for {@link RestartSystemServerTargetPreparer}.
 */
@RunWith(JUnit4.class)
public class RestartSystemServerTargetPreparerTest {

    private RestartSystemServerTargetPreparer mRestartSystemServerTargetPreparer;
    private ITestDevice mMockDevice;
    private IBuildInfo mMockBuildInfo;
    private IRunUtil mMockRunUtil;

    @Before
    public void setUp() {
        mMockDevice = EasyMock.createMock(ITestDevice.class);
        mMockBuildInfo = EasyMock.createMock(IBuildInfo.class);
        mMockRunUtil = EasyMock.createMock(IRunUtil.class);
        mRestartSystemServerTargetPreparer = new RestartSystemServerTargetPreparer(mMockRunUtil);
    }

    @Test
    public void testSetUp_bootCompleteImmediate() throws Exception {
        EasyMock.expect(mMockDevice.executeShellCommand("setprop sys.boot_completed 0")).andReturn(
                null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("pidof system_server")).andReturn(
                "123").once();
        EasyMock.expect(mMockDevice.executeShellCommand("kill 123")).andReturn(null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("getprop sys.boot_completed")).andReturn(
                "1").once();
        EasyMock.replay(mMockDevice, mMockBuildInfo);

        mRestartSystemServerTargetPreparer.setUp(mMockDevice, mMockBuildInfo);
        EasyMock.verify(mMockDevice, mMockBuildInfo);
    }

    @Test
    public void testSetUp_bootCompleteImmediate_space() throws Exception {
        EasyMock.expect(mMockDevice.executeShellCommand("setprop sys.boot_completed 0")).andReturn(
                null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("pidof system_server")).andReturn(
                "123").once();
        EasyMock.expect(mMockDevice.executeShellCommand("kill 123")).andReturn(null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("getprop sys.boot_completed")).andReturn(
                "1 ").once();
        EasyMock.replay(mMockDevice, mMockBuildInfo);

        mRestartSystemServerTargetPreparer.setUp(mMockDevice, mMockBuildInfo);
        EasyMock.verify(mMockDevice, mMockBuildInfo);
    }

    @Test
    public void testSetUp_bootCompleteAfterOneTry() throws Exception {
        EasyMock.expect(mMockDevice.executeShellCommand("setprop sys.boot_completed 0")).andReturn(
                null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("pidof system_server")).andReturn(
                "123").once();
        EasyMock.expect(mMockDevice.executeShellCommand("kill 123")).andReturn(null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("getprop sys.boot_completed")).andReturn(
                "0").once();
        mMockRunUtil.sleep(EasyMock.anyLong());
        EasyMock.expectLastCall().once();
        EasyMock.expect(mMockDevice.executeShellCommand("getprop sys.boot_completed")).andReturn(
                "1").once();
        EasyMock.replay(mMockDevice, mMockBuildInfo);

        mRestartSystemServerTargetPreparer.setUp(mMockDevice, mMockBuildInfo);
        EasyMock.verify(mMockDevice, mMockBuildInfo);
    }

    @Test(expected = TargetSetupError.class)
    public void testSetUp_giveUp() throws Exception {
        OptionSetter optionSetter = new OptionSetter(mRestartSystemServerTargetPreparer);
        optionSetter.setOptionValue("max-tries", "1");
        EasyMock.expect(mMockDevice.executeShellCommand("setprop sys.boot_completed 0")).andReturn(
                null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("pidof system_server")).andReturn(
                "123").once();
        EasyMock.expect(mMockDevice.executeShellCommand("kill 123")).andReturn(null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("getprop sys.boot_completed")).andReturn(
                "0").once();
        mMockRunUtil.sleep(EasyMock.anyLong());
        EasyMock.expectLastCall().once();
        EasyMock.expect(mMockDevice.getDeviceDescriptor()).andReturn(null);
        EasyMock.replay(mMockDevice, mMockBuildInfo);

        mRestartSystemServerTargetPreparer.setUp(mMockDevice, mMockBuildInfo);
        EasyMock.verify(mMockDevice, mMockBuildInfo);
    }

    @Test
    public void testTearDown_restart() throws Exception {
        OptionSetter optionSetter = new OptionSetter(mRestartSystemServerTargetPreparer);
        optionSetter.setOptionValue("restart-before", "false");
        optionSetter.setOptionValue("restart-after", "true");

        EasyMock.expect(mMockDevice.executeShellCommand("setprop sys.boot_completed 0")).andReturn(
                null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("pidof system_server")).andReturn(
                "123").once();
        EasyMock.expect(mMockDevice.executeShellCommand("kill 123")).andReturn(null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("getprop sys.boot_completed")).andReturn(
                "1").once();
        EasyMock.replay(mMockDevice, mMockBuildInfo);

        mRestartSystemServerTargetPreparer.tearDown(mMockDevice, mMockBuildInfo, null);
        EasyMock.verify(mMockDevice, mMockBuildInfo);
    }

    @Test
    public void testNone() throws Exception {
        OptionSetter optionSetter = new OptionSetter(mRestartSystemServerTargetPreparer);
        optionSetter.setOptionValue("restart-before", "false");
        optionSetter.setOptionValue("restart-after", "false");
        EasyMock.replay(mMockDevice, mMockBuildInfo);

        mRestartSystemServerTargetPreparer.setUp(mMockDevice, mMockBuildInfo);
        EasyMock.verify(mMockDevice, mMockBuildInfo);
    }
}
