#version 120

uniform mat4 ViewMatrix;
uniform mat4 ModelMatrix;
uniform mat4 ProjectionMatrix;

uniform vec4 ViewPort;
uniform vec3 PlayerPosition;

varying vec4 vPosition;
varying vec2 texcoord;

void main(void) {
  gl_Position = ProjectionMatrix * ViewMatrix * ModelMatrix * gl_Vertex;
  vPosition = gl_Position;
  texcoord = gl_MultiTexCoord0.xy;
}
