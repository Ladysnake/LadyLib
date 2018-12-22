package ladylib

import ladylib.client.shader.PostProcessShader
import ladylib.client.shader.ShaderUtil
import org.apiguardian.api.API
import org.apiguardian.api.API.Status.EXPERIMENTAL

@API(status = EXPERIMENTAL, since = "2.6.2")
inline fun PostProcessShader.setupDynamicUniformsKt(index: Int = 0, dynamicSetBlock: () -> Unit) {
    val sm = (this.shaderGroup ?: return).listShaders[index].shaderManager
    ShaderUtil.useShader(sm.program)
    dynamicSetBlock()
    ShaderUtil.revert()
}
