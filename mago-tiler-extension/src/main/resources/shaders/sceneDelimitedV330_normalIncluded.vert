#version 330
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec4 aColor;
layout (location = 2) in vec2 aTexCoord;
layout (location = 3) in vec3 aNormal;


uniform mat4 uProjectionMatrix;
uniform mat4 uModelViewMatrix;
uniform mat4 uObjectMatrix;
uniform vec3 uCameraDirection;
uniform vec3 uCameraUp;

out vec4 vColor;
out vec2 vTexCoord;
out vec3 vNormal;
out vec3 vTangent;
out vec3 vBitangent;
out vec3 vCamDir;
out vec3 vCamUp;

out vec3 fragLocalPos;

vec4 getOrthoPosition() {
    vec4 transformedPosition = uObjectMatrix * vec4(aPosition, 1.0);
    vec4 orthoPosition = uModelViewMatrix * transformedPosition;
    return orthoPosition;
}

void main(void) {
    fragLocalPos = aPosition;
    vec4 orthoPosition = getOrthoPosition();
    vColor = aColor;
    gl_Position = uProjectionMatrix * orthoPosition;
    vTexCoord = aTexCoord;

    // normal
    vec3 normalWC = mat3(uObjectMatrix) * aNormal;
    mat4 normalMatrix = transpose(inverse(uModelViewMatrix));
    mat3 normalMatrix3 = mat3(normalMatrix);
    vec3 normalCC = normalize(normalMatrix3 * normalWC);
    vNormal = normalCC;

    vec3 up = abs(vNormal.z) < 0.999 ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 1.0, 0.0);
    vec3 tangent = normalize(cross(up, vNormal));
    vec3 bitangent = normalize(cross(vNormal, tangent));

    vTangent  = tangent;
    vBitangent = bitangent;

    // camera direction
    vCamDir = uCameraDirection;
    vCamUp = uCameraUp;
}