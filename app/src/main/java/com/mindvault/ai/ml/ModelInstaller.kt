package com.mindvault.ai.ml

import android.content.Context
import java.io.File

object ModelInstaller {
    fun ensureModel(context: Context, assetName: String, subdir: String = "models"): File {
        val outDir = File(context.filesDir, subdir)
        if (!outDir.exists()) outDir.mkdirs()
        val out = File(outDir, assetName)
        if (out.exists() && out.length() > 0L) return out

        context.assets.open(assetName).use { ins ->
            out.outputStream().use { outs ->
                ins.copyTo(outs)
            }
        }
        return out
    }
}
