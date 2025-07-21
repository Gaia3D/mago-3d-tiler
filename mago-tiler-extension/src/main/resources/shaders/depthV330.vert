#version 330
layout (location = 0) in vec3 aPosition;

uniform mat4 uProjectionMatrix;
uniform mat4 uModelViewMatrix;
uniform mat4 uObjectMatrix;

out float vDepth;

vec4 getOrthoPosition() {
  vec4 transformedPosition = uObjectMatrix * vec4(aPosition, 1.0);
  vec4 orthoPosition = uModelViewMatrix * transformedPosition;
  return orthoPosition;
}

void main(void) {
  vec4 orthoPosition = getOrthoPosition();
  gl_Position = uProjectionMatrix * orthoPosition;
  vDepth = gl_Position.z * 0.5 + 0.5;
}