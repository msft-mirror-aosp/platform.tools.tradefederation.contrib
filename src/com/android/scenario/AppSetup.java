/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.scenario;

import com.android.tradefed.config.OptionClass;
import com.android.tradefed.testtype.AndroidJUnitTest;

/** A test that runs app setup scenarios only. */
@OptionClass(alias = "app-setup")
public final class AppSetup extends AndroidJUnitTest {
    public AppSetup() {
        super();
        // Specifically target the app setup scenarios.
        setPackageName("android.platform.test.scenario");
        addIncludeAnnotation("android.platform.test.scenario.annotation.AppSetup");
    }
}
