#version 330
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec4 aColor;
layout (location = 2) in vec2 aTexCoord;
layout (location = 3) in vec3 aNormal;


uniform mat4 uProjectionMatrix;
uniform mat4 uModelViewMatrix;
uniform mat4 uObjectMatrix;

out vec4 vColor;
out vec2 vTexCoord;
out vec3 vNormal;
out vec3 vCamDir;

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

  // normal
  vec3 normalWC = mat3(uObjectMatrix) * aNormal;
  vec3 normalCC = normalize(mat3(uModelViewMatrix) * normalWC);
    vNormal = normalCC;

  // camera direction
  vCamDir = vec3(0,0,-1);
}