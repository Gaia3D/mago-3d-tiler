attribute vec3 aPosition;
attribute vec4 aColor;
attribute vec2 aTexCoord;

uniform mat4 uProjectionMatrix;
uniform mat4 uModelViewMatrix;
uniform mat4 uObjectMatrix;

varying vec4 vColor;
varying vec2 vTexCoord;

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