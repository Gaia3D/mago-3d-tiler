#version 330
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec4 aColor;
layout (location = 2) in vec2 aTexCoord;


uniform mat4 uProjectionMatrix;
uniform mat4 uModelViewMatrix;
uniform mat4 uObjectMatrix;

out vec4 vColor;
out vec2 vTexCoord;

vec4 getOrthoPosition() {
  vec4 transformedPosition = uObjectMatrix * vec4(aPosition, 1.0);
  vec4 orthoPosition = uModelViewMatrix * transformedPosition;
  return orthoPosition;
}

void main(void) {
  vec4 orthoPosition = getOrthoPosition();
  vColor = aColor;
  gl_Position = uProjectionMatrix * orthoPosition;
    vTexCoord = aTexCoord;
}