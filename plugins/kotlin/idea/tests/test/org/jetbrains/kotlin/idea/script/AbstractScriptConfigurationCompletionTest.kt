// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.script

import org.jetbrains.kotlin.idea.base.plugin.isK2Plugin
import org.jetbrains.kotlin.idea.completion.test.testCompletion
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

abstract class AbstractScriptConfigurationCompletionTest : AbstractScriptConfigurationTest() {
    fun doTest(unused: String) {
        configureScriptFile(testDataFile())
        testCompletion(
          file.text,
          JvmPlatforms.unspecifiedJvmPlatform,
          additionalValidDirectives = switches,
          complete = { completionType, count ->
                setType(completionType)
                complete(count)
                myItems
            },
          isK2Plugin = isK2Plugin()
        )
    }
}