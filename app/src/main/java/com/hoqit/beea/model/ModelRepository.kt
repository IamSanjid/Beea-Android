package com.hoqit.beea.model

import com.rementia.openwakeword.lib.model.WakeWordModel

object ModelRepository {
    fun getModels(): List<WakeWordModel> {
        return listOf(
            WakeWordModel(
                name = "Hey Beea",
                modelPath = "Hey_Beea_20260820_124841.onnx",
                threshold = 0.5f
            )
        )
    }
}