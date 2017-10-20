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

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit Tests for {@link FillStorageTargetPreparer}.
 */
@RunWith(JUnit4.class)
public class SetAndRestoreSystemPropertyTargetPreparerTest {
    private SetAndRestoreSystemPropertyTargetPreparer mSetAndRestoreSystemPropertyTargetPreparer;
    private ITestDevice mMockDevice;
    private IBuildInfo mMockBuildInfo;

    @Before
    public void setUp() {
        mSetAndRestoreSystemPropertyTargetPreparer = new SetAndRestoreSystemPropertyTargetPreparer();
        mMockDevice = EasyMock.createMock(ITestDevice.class);
        mMockBuildInfo = EasyMock.createMock(IBuildInfo.class);
    }

    @Test(expected = TargetSetupError.class)
    public void testOneProp_fail() throws Exception {
        OptionSetter optionSetter = new OptionSetter(mSetAndRestoreSystemPropertyTargetPreparer);
        optionSetter.setOptionValue("set-property", "a", "new");
        EasyMock.expect(mMockDevice.executeShellCommand("getprop \"a\"")).andReturn("old").once();
        EasyMock.expect(mMockDevice.executeShellCommand("setprop \"a\" \"new\"")).andReturn(null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("getprop \"a\"")).andReturn("old").once();
        EasyMock.expect(mMockDevice.getDeviceDescriptor()).andReturn(null).once();
        EasyMock.replay(mMockDevice, mMockBuildInfo);

        mSetAndRestoreSystemPropertyTargetPreparer.setUp(mMockDevice, mMockBuildInfo);
        EasyMock.verify(mMockBuildInfo, mMockDevice);
    }

    @Test
    public void testZeroProps() throws Exception {
        EasyMock.replay(mMockDevice, mMockBuildInfo);

        mSetAndRestoreSystemPropertyTargetPreparer.setUp(mMockDevice, mMockBuildInfo);
        mSetAndRestoreSystemPropertyTargetPreparer.tearDown(mMockDevice, mMockBuildInfo, null);
        EasyMock.verify(mMockBuildInfo, mMockDevice);
    }

    @Test
    public void testOneProp() throws Exception {
        OptionSetter optionSetter = new OptionSetter(mSetAndRestoreSystemPropertyTargetPreparer);
        optionSetter.setOptionValue("set-property", "a", "new");
        EasyMock.expect(mMockDevice.executeShellCommand("getprop \"a\"")).andReturn("old").once();
        EasyMock.expect(mMockDevice.executeShellCommand("setprop \"a\" \"new\"")).andReturn(null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("getprop \"a\"")).andReturn("new").once();
        EasyMock.expect(mMockDevice.executeShellCommand("setprop \"a\" \"old\"")).andReturn(null).once();
        EasyMock.replay(mMockDevice, mMockBuildInfo);

        mSetAndRestoreSystemPropertyTargetPreparer.setUp(mMockDevice, mMockBuildInfo);
        mSetAndRestoreSystemPropertyTargetPreparer.tearDown(mMockDevice, mMockBuildInfo, null);
        EasyMock.verify(mMockBuildInfo, mMockDevice);
    }

    @Test
    public void testTwoProps() throws Exception {
        OptionSetter optionSetter = new OptionSetter(mSetAndRestoreSystemPropertyTargetPreparer);
        optionSetter.setOptionValue("set-property", "a", "new");
        optionSetter.setOptionValue("set-property", "b", "newb");
        EasyMock.expect(mMockDevice.executeShellCommand("getprop \"a\"")).andReturn("old").once();
        EasyMock.expect(mMockDevice.executeShellCommand("setprop \"a\" \"new\"")).andReturn(null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("getprop \"a\"")).andReturn("new").once();
        EasyMock.expect(mMockDevice.executeShellCommand("getprop \"b\"")).andReturn("oldb").once();
        EasyMock.expect(mMockDevice.executeShellCommand("setprop \"b\" \"newb\"")).andReturn(null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("getprop \"b\"")).andReturn("newb").once();
        EasyMock.expect(mMockDevice.executeShellCommand("setprop \"a\" \"old\"")).andReturn(null).once();
        EasyMock.expect(mMockDevice.executeShellCommand("setprop \"b\" \"oldb\"")).andReturn(null).once();
        EasyMock.replay(mMockDevice, mMockBuildInfo);

        mSetAndRestoreSystemPropertyTargetPreparer.setUp(mMockDevice, mMockBuildInfo);
        mSetAndRestoreSystemPropertyTargetPreparer.tearDown(mMockDevice, mMockBuildInfo, null);
        EasyMock.verify(mMockBuildInfo, mMockDevice);
    }

}
