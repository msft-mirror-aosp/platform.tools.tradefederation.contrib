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
public class FillStorageTargetPreparerTest {
    private FillStorageTargetPreparer mFillStorageTargetPreparer;
    private ITestDevice mMockDevice;
    private IBuildInfo mMockBuildInfo;

    @Before
    public void setUp() {
        mFillStorageTargetPreparer = new FillStorageTargetPreparer();
        mMockDevice = EasyMock.createMock(ITestDevice.class);
        mMockBuildInfo = EasyMock.createMock(IBuildInfo.class);
    }

    @Test
    public void testSetUpWriteFile() throws Exception {
        OptionSetter optionSetter = new OptionSetter(mFillStorageTargetPreparer);
        optionSetter.setOptionValue("free-bytes", "50");
        EasyMock.expect(mMockDevice.executeShellCommand(EasyMock.anyObject())).andReturn(
                "a\n0 0 0 75 0\n").once();
        EasyMock.expect(mMockDevice.executeShellCommand(EasyMock.anyObject())).andReturn(
                null).once();
        EasyMock.replay(mMockDevice, mMockBuildInfo);

        mFillStorageTargetPreparer.setUp(mMockDevice, mMockBuildInfo);
        EasyMock.verify(mMockBuildInfo, mMockDevice);
    }

    @Test
    public void testSetUpSkip() throws Exception {
        OptionSetter optionSetter = new OptionSetter(mFillStorageTargetPreparer);
        optionSetter.setOptionValue("free-bytes", "50000");
        EasyMock.expect(mMockDevice.executeShellCommand(EasyMock.anyObject())).andReturn(
                "a\n0 0 0 25 0\n").once();
        EasyMock.replay(mMockDevice, mMockBuildInfo);

        mFillStorageTargetPreparer.setUp(mMockDevice, mMockBuildInfo);
        EasyMock.verify(mMockBuildInfo, mMockDevice);
    }

    @Test
    public void testTearDown() throws Exception {
        EasyMock.expect(mMockDevice.executeShellCommand(EasyMock.anyObject())).andReturn(
                null).once();
        EasyMock.replay(mMockDevice, mMockBuildInfo);

        mFillStorageTargetPreparer.tearDown(mMockDevice, mMockBuildInfo, null);
        EasyMock.verify(mMockBuildInfo, mMockDevice);
    }
}
