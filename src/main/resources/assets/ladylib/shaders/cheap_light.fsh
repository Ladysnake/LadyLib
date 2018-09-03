#version 120

// The texture for the light
uniform sampler2D LightSampler;
// The depth texture
uniform sampler2D DepthSampler;

// The magic matrix to get world coordinates from pixel ones
uniform mat4 InverseTransformMatrix;
// The size of the viewport (typically, [0,0,1080,720])
uniform vec4 ViewPort;
// The position of the light's center relative to the camera, in world coordinates
uniform vec3 LightPosition;
// The radius of the light. 1 is slightly more than half a block
uniform float LightRadius = 1;
// The color of the light, in RGBA
uniform vec4 LightColor = vec4(1.);

varying vec4 vPosition;
varying vec2 texcoord;

vec4 CalcEyeFromWindow(in float depth)
{
  // derived from https://www.khronos.org/opengl/wiki/Compute_eye_space_from_window_space
  // ndc = Normalized Device Coordinates
  vec3 ndcPos;
  ndcPos.xy = ((2.0 * gl_FragCoord.xy) - (2.0 * ViewPort.xy)) / (ViewPort.zw) - 1;
  ndcPos.z = (2.0 * depth - gl_DepthRange.near - gl_DepthRange.far) / (gl_DepthRange.far - gl_DepthRange.near);
  vec4 clipPos = vec4(ndcPos, 1.);
  vec4 homogeneous = InverseTransformMatrix * clipPos;
  vec4 eyePos = vec4(homogeneous.xyz / homogeneous.w, homogeneous.w);
  return eyePos;
}

void main()
{
    vec4 texture = LightColor * texture2D(LightSampler, texcoord);

    vec3 ndc = vPosition.xyz / vPosition.w; //perspective divide/normalize
    vec2 viewportCoord = ndc.xy * 0.5 + 0.5; //ndc is -1 to 1 in GL. scale for 0 to 1

    // Depth fading
    float sceneDepth = texture2D(DepthSampler, viewportCoord).x;
    vec3 pixelPosition = CalcEyeFromWindow(sceneDepth).xyz;
    float relativeDepth = smoothstep(1 - LightRadius, 1, 1 - distance(LightPosition, pixelPosition));

    texture.a *= relativeDepth;

    gl_FragColor = texture;
}
