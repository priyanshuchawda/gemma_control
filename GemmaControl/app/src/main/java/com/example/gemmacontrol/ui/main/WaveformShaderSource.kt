package com.example.gemmacontrol.ui.main

/**
 * AGSL shader source ported from Google AI Edge Gallery's AudioAnimation.kt.
 * Colors are tuned to GemmaControl's Material 3 palette.
 *
 * Gallery source:
 * gallery/Android/src/.../ui/common/AudioAnimation.kt
 */
internal const val WAVEFORM_SHADER_SOURCE = """
uniform float2 iResolution;
uniform vec4 bgColor;
uniform float iTime;
uniform float amplitude;
uniform float pOffset;

vec3 mix4(vec3 c1, vec3 c2, vec3 c3, vec3 c4, vec2 uv) {
  float t1 = sin(iTime / 1.6);
  float t2 = sin(iTime / 1.8);
  return mix(
    mix(c1, c2, smoothstep(0.0 + t1*0.1, 0.24 + t1*0.1, uv.y)),
    mix(c3, c4, smoothstep(-0.16 - t2*0.1, 0.24 - t2*0.1, uv.y)),
    smoothstep(0.0, 0.7 + t1*0.1, uv.x));
}

float hash(float i) {
  return -1. + 2. * fract(sin(i * 127.1) * 43758.1453123);
}

float perlin1d(float d) {
  float i = floor(d);
  float f = d - i;
  float y = f*f*f*(6.*f*f - 15.*f + 10.);
  float r = mix(hash(i)*f, hash(i+1.)*(f-1.), y);
  return r * 0.5 + 0.5;
}

half4 main(float2 fragCoord) {
  float2 uv = fragCoord / iResolution.xy;
  uv.y = 1.0 - uv.y;

  if (amplitude == 0.) {
    uv.y += sin(uv.x * 4.0 + -iTime * 1.2) * 0.036;
  } else {
    uv.y -= perlin1d(pOffset + uv.x * 3.) * amplitude / 2.0;
  }

  // GemmaControl brand colors: deep indigo / violet / teal / soft blue
  vec3 col = mix4(
    vec3(0.42, 0.35, 0.95),  // indigo
    vec3(0.65, 0.42, 0.95),  // violet
    vec3(0.27, 0.75, 0.78),  // teal
    vec3(0.38, 0.58, 0.93),  // soft blue
    uv);

  float fade = smoothstep(0.24, 0.34, uv.y);
  vec4 final = mix(vec4(col, 1.0), bgColor, fade);
  return vec4(half3(final.xyz) * (1.0 + amplitude * 0.2), final.a);
}
"""
