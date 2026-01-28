package com.github.xepozz.infection

import com.intellij.openapi.util.io.FileUtil
import java.io.File


fun String.tryRelativeTo(basePath: String): String = FileUtil
    .getRelativePath(basePath, this, File.separatorChar)
    ?: this
