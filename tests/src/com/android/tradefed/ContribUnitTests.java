/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.tradefed;

import com.android.tradefed.targetprep.FillStorageTargetPreparerTest;
import com.android.tradefed.targetprep.RebootTargetPreparerTest;
import com.android.tradefed.targetprep.RestartSystemServerTargetPreparerTest;
import com.android.tradefed.targetprep.RootTargetPreparerTest;
import com.android.tradefed.targetprep.SaveAndRestoreTimeTargetPreparerTest;
import com.android.tradefed.targetprep.SetAndRestoreSystemPropertyTargetPreparerTest;
import com.android.tradefed.targetprep.WriteAndRestoreFileTargetPreparerTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * A test suite for all Trade Federation unit tests running under Junit4.
 *
 * <p>All tests listed here should be self-contained, and should not require any external
 * dependencies.
 */
@RunWith(Suite.class)
@SuiteClasses({
    // targetprep
    FillStorageTargetPreparerTest.class,
    RebootTargetPreparerTest.class,
    RestartSystemServerTargetPreparerTest.class,
    RootTargetPreparerTest.class,
    SaveAndRestoreTimeTargetPreparerTest.class,
    SetAndRestoreSystemPropertyTargetPreparerTest.class,
    WriteAndRestoreFileTargetPreparerTest.class,
})
public class ContribUnitTests {
    // empty of purpose
}
