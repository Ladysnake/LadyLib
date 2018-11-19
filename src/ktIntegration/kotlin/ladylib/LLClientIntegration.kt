package ladylib

import ladylib.client.shader.PostProcessShader
import ladylib.client.shader.ShaderUtil
import ladylib.misc.PublicApi

@PublicApi
inline fun PostProcessShader.setupDynamicUniformsKt(index: Int = 0, dynamicSetBlock: () -> Unit) {
    val sm = (this.shaderGroup ?: return).listShaders[index].shaderManager
    ShaderUtil.useShader(sm.program)
    dynamicSetBlock()
    ShaderUtil.revert()
}
