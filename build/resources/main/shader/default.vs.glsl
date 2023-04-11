attribute vec3 aVertexPosition;
attribute vec3 aVertexNormal;
attribute vec4 aVertexColor;
attribute vec2 aVertexTextureCoordinate;

uniform int uTextureType;
uniform mat4 uProjectionMatrix;
uniform mat4 uModelRotationMatrix;
uniform mat4 uObjectRotationMatrix;
uniform sampler2D uTexture;

varying vec4 vColor;
varying vec2 vTextureCoord;

vec4 getOrthoPosition() {
  vec4 transformedPosition = uObjectRotationMatrix * vec4(aVertexPosition, 1.0);
  vec4 orthoPosition = uModelRotationMatrix * transformedPosition;
  return orthoPosition;
}

void main(void) {
  vec4 orthoPosition = getOrthoPosition();
  vColor = aVertexColor;
  vTextureCoord = aVertexTextureCoordinate;
  gl_Position = uProjectionMatrix * orthoPosition;
}