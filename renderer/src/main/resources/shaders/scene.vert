attribute vec3 aVertexPosition;
attribute vec4 aVertexColor;

uniform mat4 uProjectionMatrix;
uniform mat4 uModelViewMatrix;
uniform mat4 uObjectMatrix;

varying vec4 vColor;

vec4 getOrthoPosition() {
  vec4 transformedPosition = uObjectMatrix * vec4(aVertexPosition, 1.0);
  vec4 orthoPosition = uModelViewMatrix * transformedPosition;
  return orthoPosition;
}

void main(void) {
  vec4 orthoPosition = getOrthoPosition();
  vColor = aVertexColor;
  gl_Position = uProjectionMatrix * orthoPosition;
}