precision highp float;

varying vec4 vColor;
void main(void) {
  gl_FragColor = vColor * 0.8;
}