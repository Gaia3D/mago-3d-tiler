precision highp float;

uniform int uTextureType;
uniform sampler2D uTexture;

varying vec4 vColor;
varying vec2 vTextureCoord;
void main(void) {

  if (uTextureType == 1) {
    gl_FragColor = texture2D(uTexture, vTextureCoord.xy);
  } else {
    gl_FragColor = vColor;
  }
}