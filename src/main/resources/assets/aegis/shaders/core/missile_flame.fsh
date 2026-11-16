#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        v += amp * vnoise(p);
        p = p * 2.03 + vec2(17.1, 9.2);
        amp *= 0.5;
    }
    return v;
}

void main() {
    float night = vertexColor.r;
    float seed = vertexColor.g * 97.0;
    float mode = vertexColor.b;
    float power = vertexColor.a;
    float t = GameTime * 1200.0;

    vec4 outCol;

    if (mode < 0.5) {
        // Exhaust plume: u across, v along (0 at nozzle, 1 at the tip).
        float x = (texCoord0.x - 0.5) * 2.0;
        float v = texCoord0.y;

        float n = fbm(vec2(x * 2.2 + seed, v * 3.5 - t * 9.0));
        float n2 = fbm(vec2(x * 4.0 - seed, v * 7.0 - t * 16.0));
        float width = mix(0.60, 0.06, pow(v, 0.75));
        float wobble = (n - 0.5) * 0.45 * v;
        float d = abs(x + wobble) / max(width, 0.001);

        float body = smoothstep(1.0, 0.1, d);
        float tip = smoothstep(1.0, 0.45 + 0.35 * n2, v);
        float diamonds = 0.8 + 0.2 * sin(v * 42.0 - t * 34.0) * (1.0 - v);
        float heat = clamp(body * tip * diamonds * (0.65 + 0.7 * n2) * power * 1.35, 0.0, 1.0);

        vec3 hot = vec3(1.00, 0.97, 0.86);
        vec3 mid = vec3(1.00, 0.60, 0.12);
        vec3 cool = vec3(0.85, 0.14, 0.04);
        vec3 col = mix(cool, mid, smoothstep(0.08, 0.50, heat));
        col = mix(col, hot, smoothstep(0.55, 0.95, heat));

        // Night: hotter, slightly blue-white core and brighter overall. Day: richer orange, a little see-through.
        col = mix(col, col * vec3(0.82, 0.92, 1.18), night * 0.55 * smoothstep(0.6, 1.0, heat));
        col *= mix(0.92, 1.55, night);
        float alpha = heat * mix(0.78, 1.0, night);
        outCol = vec4(col, alpha);
    } else {
        // Glow halo around the nozzle. Faint in daylight, strong at night.
        vec2 p = (texCoord0 - 0.5) * 2.0;
        float r = length(p);
        float falloff = exp(-r * r * 3.4);
        float flicker = 0.86 + 0.09 * sin(t * 47.0 + seed) + 0.05 * sin(t * 83.0 + seed * 2.0);
        float strength = mix(0.10, 0.95, night) * power * flicker;
        vec3 col = mix(vec3(1.0, 0.45, 0.10), vec3(1.0, 0.82, 0.55), falloff * falloff);
        outCol = vec4(col, falloff * strength);
    }

    outCol *= ColorModulator;
    if (outCol.a < 0.004) {
        discard;
    }
    fragColor = outCol;
}
