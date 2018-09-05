#version 120

struct LightStruct
{
  // The position of the light's center relative to the camera, in world coordinates
  vec3 position;
  // The radius of the light. 1 is slightly more than half a block
  float radius;
  // The color of the light, in RGBA
  vec4 color;
};

// The texture for the light
uniform sampler2D LightSampler;
// The depth texture
uniform sampler2D DepthSampler;

// The magic matrix to get world coordinates from pixel ones
uniform mat4 InverseTransformMatrix;
// The size of the viewport (typically, [0,0,1080,720])
uniform vec4 ViewPort;
uniform LightStruct[] u_light;
uniform int u_lightCount;

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
    vec4 texture = texture2D(LightSampler, texcoord);

    vec3 ndc = vPosition.xyz / vPosition.w; //perspective divide/normalize
    vec2 viewportCoord = ndc.xy * 0.5 + 0.5; //ndc is -1 to 1 in GL. scale for 0 to 1

    // Depth fading
    float sceneDepth = texture2D(DepthSampler, viewportCoord).x;
    vec3 pixelPosition = CalcEyeFromWindow(sceneDepth).xyz;

    vec4 color = vec4(0.);
    float alpha = 0.;
    for(int i = 0; i < u_lightCount; i++)
    {
      float relativeDepth = smoothstep(1 - u_light[i].radius, 1, 1 - distance(u_light[i].position, pixelPosition));

      color = u_light[i].color;
      alpha = max(alpha, relativeDepth);
    }
    texture *= u_light[0].color;
    texture.a *= alpha;

    gl_FragColor = texture;
}
