#version 130

uniform sampler2D texture;

// The amount of saturation in the resulting image
uniform float saturation;

varying vec2 texcoord;

void main()
{
    vec4 texel = texture2D(texture, texcoord);
    // cut the corners
    float alpha = 1.- ((1.-sign(mod(round(texcoord.x * 15.), 15.))) * (1.-sign(mod(round(texcoord.y * 15.), 15.))));
    // change the saturation and apply calculated alpha
    gl_FragColor = vec4(mix(vec3(dot(texel.rgb, vec3(0.2125, 0.7154, 0.0721))), texel.rgb, saturation), min(alpha, texel.a));
}
