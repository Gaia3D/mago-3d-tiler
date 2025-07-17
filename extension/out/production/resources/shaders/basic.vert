attribute vec3 aVertexPosition;
attribute vec4 aVertexColor;

uniform mat4 uProjectionMatrix;
uniform mat4 uModelRotationMatrix;
uniform mat4 uObjectRotationMatrix;

varying vec4 vColor;

vec4 getOrthoPosition() {
  vec4 transformedPosition = uObjectRotationMatrix * vec4(aVertexPosition, 1.0);
  vec4 orthoPosition = uModelRotationMatrix * transformedPosition;
  return orthoPosition;
}

void main(void) {
  vec4 orthoPosition = getOrthoPosition();
  vColor = aVertexColor;
  gl_Position = uProjectionMatrix * orthoPosition;
}