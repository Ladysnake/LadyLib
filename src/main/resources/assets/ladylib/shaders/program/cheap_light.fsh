#version 120

struct LightStruct
{
  // The position of the light's center relative to the camera, in world coordinates
  vec3 position;
  // The radius of the light. 1 is slightly more than half a block
  float radius;
  vec4 color;
};

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform float zNear;
uniform float zFar;
uniform float aspect;

// The magic matrix to get world coordinates from pixel ones
uniform mat4 InverseTransformMatrix;
// The size of the viewport (typically, [0,0,1080,720])
uniform ivec4 ViewPort;
uniform LightStruct Lights[100];
uniform int LightCount;

varying vec2 texCoord;
varying vec4 vPosition;

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

float distSq(vec3 a, vec3 b) {
  return pow((a.x-b.x),2)+pow((a.y-b.y),2)+pow((a.z-b.z),2);
}

void compute_color(vec3 position, out vec4 lcolor, out float intens) {
  lcolor = vec4(0,0,0,1.0f);
	float sumR = 0;
	float sumG = 0;
	float sumB = 0;
	float count = 0;
	float maxIntens = 0;
	float totalIntens = 0;
	for (int i = 0; i < LightCount; i ++){
		if (distSq(Lights[i].position,position) <= pow(Lights[i].radius,2)){
			float faceexposure = 1.0f;
			float intensity = pow(max(0,1.0f-distance(Lights[i].position,position)/(Lights[i].radius)),2) * 1.0f * Lights[i].color.a * ((max(0,faceexposure)+0.5f)/1.5f);
			sumR += (intensity)*Lights[i].color.r;
			sumG += (intensity)*Lights[i].color.g;
			sumB += (intensity)*Lights[i].color.b;
			totalIntens += intensity;
			maxIntens = max(maxIntens,intensity);
		}
	}
	lcolor = vec4(max(sumR*1.5f / totalIntens,0.0f), max(sumG*1.5f / totalIntens, 0.0f), max(sumB*1.5f / totalIntens, 0.0f), 1.0f);
  intens = min(1.0, maxIntens);
}

void main()
{
    vec4 baseColor= texture2D(DiffuseSampler, texCoord);

    vec3 ndc = vPosition.xyz / vPosition.w; //perspective divide/normalize
    vec2 viewportCoord = ndc.xy * 0.5 + 0.5; //ndc is -1 to 1 in GL. scale for 0 to 1

    // Depth fading
    float sceneDepth = texture2D(DepthSampler, viewportCoord).x;
    vec3 pixelPosition = CalcEyeFromWindow(sceneDepth).xyz;

    vec4 lcolor;
    float intens;
    compute_color(pixelPosition, lcolor, intens);
    vec4 color = vec4(1 - (1 - baseColor.rgb) * (1 - lcolor.rgb * intens), baseColor.a);
    // color.rgb = vec3(distance(pixelPosition.xyz, vec3(0,0,0)));
    gl_FragColor = color;
}
