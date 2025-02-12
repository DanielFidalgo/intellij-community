// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInsight.inline.completion

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.BLOCK_COMMENT
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import org.jetbrains.annotations.ApiStatus
import java.awt.Color
import java.awt.Font

@ApiStatus.Experimental
object InlineFontUtils {
  fun font(editor: Editor): Font {
    return editor.colorsScheme.getFont(EditorFontType.ITALIC)
  }

  fun color(editor: Editor): Color {
    return editor.colorsScheme.getAttributes(BLOCK_COMMENT).foregroundColor
  }
}
