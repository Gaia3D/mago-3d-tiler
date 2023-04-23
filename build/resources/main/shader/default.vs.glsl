attribute vec3 aVertexPosition;
attribute vec3 aVertexNormal;
attribute vec4 aVertexColor;
attribute vec2 aVertexTextureCoordinate;

uniform int uTextureType;
uniform mat4 uProjectionMatrix;
uniform mat4 uModelRotationMatrix;
uniform mat4 uObjectTransformMatrix;
uniform sampler2D uTexture;

varying vec4 vColor;
varying vec2 vTextureCoord;
varying vec3 vNormal;

vec4 getOrthoPosition() {
  vec4 transformedPosition = uObjectTransformMatrix * vec4(aVertexPosition, 1.0);
  vec4 orthoPosition = uModelRotationMatrix * transformedPosition;
  return orthoPosition;
}

void main(void) {

  mat3 objectTransformMatrix = mat3(uObjectTransformMatrix);
  vec3 rotatedNormal = objectTransformMatrix * aVertexNormal;
  rotatedNormal = normalize(rotatedNormal);

  mat3 modelViewRotationMatrix = mat3(uModelRotationMatrix);
  rotatedNormal = modelViewRotationMatrix * rotatedNormal;
  rotatedNormal = normalize(rotatedNormal);

  vec4 orthoPosition = getOrthoPosition();
  vColor = aVertexColor;
  vNormal = rotatedNormal;
  vTextureCoord = vec2(aVertexTextureCoordinate.x, aVertexTextureCoordinate.y);
  gl_Position = uProjectionMatrix * orthoPosition;
}