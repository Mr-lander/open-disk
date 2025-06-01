// src/utils/jwt.js
export function decodeJWT (token) {
    if (!token) return {};
    const parts = token.split('.');
    if (parts.length < 2) return {};
    // Base64URL -> Base64
    const b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    // 补全 = 号
    const pad = b64.length % 4;
    const padded = pad ? b64 + '='.repeat(4 - pad) : b64;
    // atob 解码，再 JSON.parse
    try {
        const json = atob(padded);
        return JSON.parse(json);
    } catch {
        return {};
    }
}
