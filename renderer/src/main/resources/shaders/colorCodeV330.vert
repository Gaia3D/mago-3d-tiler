#version 330
layout (location = 0) in vec3 aPosition;

uniform mat4 uProjectionMatrix;
uniform mat4 uModelViewMatrix;
uniform mat4 uObjectMatrix;
uniform int uColorCode;

out vec4 vColor;

vec4 decodeColor4(int colorCode) {
  int r = (colorCode >> 24) & 0xFF;
  int g = (colorCode >> 16) & 0xFF;
  int b = (colorCode >> 8) & 0xFF;
  int a = colorCode & 0xFF;
  return vec4(r / 255.0, g / 255.0, b / 255.0, a / 255.0);
}

vec4 getOrthoPosition() {
  vec4 transformedPosition = uObjectMatrix * vec4(aPosition, 1.0);
  vec4 orthoPosition = uModelViewMatrix * transformedPosition;
  return orthoPosition;
}

void main(void) {
  vec4 orthoPosition = getOrthoPosition();
  vColor = decodeColor4(uColorCode);
  if(uColorCode == -1)
  {
    vColor = vec4(1.0, 1.0, 1.0, 1.0);
  }
  gl_Position = uProjectionMatrix * orthoPosition;

}