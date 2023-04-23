precision highp float;

uniform int uTextureType;
uniform sampler2D uTexture;

varying vec4 vColor;
varying vec2 vTextureCoord;
varying vec3 vNormal;
void main(void) {
  vec3 cameraDir = vec3(0.0, 0.0, -1.0);
  float dotProduct = abs(dot(vNormal, cameraDir));
  vec4 finalColor = vec4(0.0, 0.0, 0.0, 1.0);

  if (uTextureType == 1) {
    finalColor = texture2D(uTexture, vTextureCoord.xy);
  } else {
    finalColor = vColor;
  }
  if (dotProduct < 0.5) {
    dotProduct = 0.5;
  }
  gl_FragColor = vec4(finalColor.xyz * dotProduct, 1.0);
}