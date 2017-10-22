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

import java.util.concurrent.TimeUnit;

/**
 * Unit Tests for {@link FillStorageTargetPreparer}.
 */
@RunWith(JUnit4.class)
public class SaveAndRestoreTimeTargetPreparerTest {
    private SaveAndRestoreTimeTargetPreparer mSaveAndRestoreTimeTargetPreparer;
    private SaveAndRestoreTimeTargetPreparer.ITimeGetter mMockTimeGetter;
    private ITestDevice mMockDevice;
    private IBuildInfo mMockBuildInfo;

    @Before
    public void setUp() {
        mMockDevice = EasyMock.createMock(ITestDevice.class);
        mMockBuildInfo = EasyMock.createMock(IBuildInfo.class);
        mMockTimeGetter = EasyMock.createMock(SaveAndRestoreTimeTargetPreparer.ITimeGetter.class);
        mSaveAndRestoreTimeTargetPreparer = new SaveAndRestoreTimeTargetPreparer(mMockTimeGetter);
    }

    @Test
    public void testSaveTime() throws Exception {
        EasyMock.expect(mMockTimeGetter.getTime()).andReturn(TimeUnit.MILLISECONDS.toNanos(2)).once();
        EasyMock.expect(mMockDevice.executeShellCommand(EasyMock.anyObject())).andReturn(
                "123\n").once();
        EasyMock.expect(mMockTimeGetter.getTime()).andReturn(TimeUnit.MILLISECONDS.toNanos(7)).once();
        EasyMock.expect(mMockDevice.executeShellCommand("date @128")).andReturn(
                null).once();
        EasyMock.replay(mMockDevice, mMockBuildInfo, mMockTimeGetter);

        mSaveAndRestoreTimeTargetPreparer.setUp(mMockDevice, mMockBuildInfo);
        mSaveAndRestoreTimeTargetPreparer.tearDown(mMockDevice, mMockBuildInfo, null);
        EasyMock.verify(mMockBuildInfo, mMockDevice);
    }

    @Test
    public void testSaveTime_setTimeAtBeginning() throws Exception {
        OptionSetter optionSetter = new OptionSetter(mSaveAndRestoreTimeTargetPreparer);
        optionSetter.setOptionValue("set-time", "true");
        optionSetter.setOptionValue("time", "555");
        EasyMock.expect(mMockTimeGetter.getTime()).andReturn(TimeUnit.MILLISECONDS.toNanos(2)).once();
        EasyMock.expect(mMockDevice.executeShellCommand(EasyMock.anyObject())).andReturn(
                "123\n").once();
        EasyMock.expect(mMockDevice.executeShellCommand("date @555")).andReturn(null).once();
        EasyMock.expect(mMockTimeGetter.getTime()).andReturn(TimeUnit.MILLISECONDS.toNanos(7)).once();
        EasyMock.expect(mMockDevice.executeShellCommand("date @128")).andReturn(null).once();
        EasyMock.replay(mMockDevice, mMockBuildInfo, mMockTimeGetter);

        mSaveAndRestoreTimeTargetPreparer.setUp(mMockDevice, mMockBuildInfo);
        mSaveAndRestoreTimeTargetPreparer.tearDown(mMockDevice, mMockBuildInfo, null);
        EasyMock.verify(mMockBuildInfo, mMockDevice);
    }
}
