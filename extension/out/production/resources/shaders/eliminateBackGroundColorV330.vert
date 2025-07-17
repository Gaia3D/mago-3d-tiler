#version 330
layout (location = 0) in vec2 aPosition;
layout (location = 1) in vec2 aTexCoord;

out vec2 vTexCoord;

void main(void) {
  vTexCoord = vec2(1.0 - aTexCoord.x, 1.0 - aTexCoord.y);
  gl_Position = vec4(1.0 - 2.0 * aPosition, 0, 1);
}