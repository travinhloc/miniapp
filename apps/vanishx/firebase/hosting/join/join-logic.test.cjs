const test = require("node:test");
const assert = require("node:assert/strict");
const join = require("./join-logic.js");

function tokenFor(payloadObj) {
    const json = JSON.stringify(payloadObj);
    return Buffer.from(json, "utf8")
        .toString("base64")
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=+$/, "");
}

const sample = tokenFor({ v: 1, r: "room-secret", k: "key-secret", e: 9_999_999_999_999 });

test("extracts token from /join query", () => {
    assert.equal(join.extractToken("/join", "?token=" + sample), sample);
});

test("extracts token from /j/ path", () => {
    assert.equal(join.extractToken("/j/" + sample, ""), sample);
});

test("rejects short display prefix", () => {
    const resolved = join.resolveInvite("/j/" + sample.slice(0, 8), "", Date.now());
    assert.equal(resolved.ok, false);
    assert.equal(resolved.reason, "missing");
});

test("missing token does not resolve", () => {
    const resolved = join.resolveInvite("/join", "", Date.now());
    assert.equal(resolved.ok, false);
    assert.equal(resolved.reason, "missing");
});

test("clipboard payload has no roomId or key", () => {
    const now = 1_700_000_000_000;
    const resolved = join.resolveInvite("/join", "?token=" + sample, now);
    assert.equal(resolved.ok, true);
    const payload = join.clipboardPayload(resolved.token, resolved.expMs);
    assert.match(payload, /^VANISHX_INVITE:JOIN_ROOM:/);
    assert.equal(payload.includes("room-secret"), false);
    assert.equal(payload.includes("key-secret"), false);
    assert.equal(JSON.stringify(resolved).includes("room-secret"), false);
    assert.equal(JSON.stringify(resolved).includes("key-secret"), false);
});

test("exp is min of now+24h and room e", () => {
    const now = 1_000_000_000_000;
    const roomE = now + 3_600_000;
    const tok = tokenFor({ v: 1, r: "r1", k: "k1", e: roomE });
    const resolved = join.resolveInvite("/join", "?token=" + tok, now);
    assert.equal(resolved.expMs, roomE);
});

test("e=0 uses now+24h", () => {
    const now = 1_000_000_000_000;
    const tok = tokenFor({ v: 1, r: "r1", k: "k1", e: 0 });
    const resolved = join.resolveInvite("/join", "?token=" + tok, now);
    assert.equal(resolved.expMs, now + 24 * 60 * 60 * 1000);
});

test("expired room is invalid", () => {
    const now = 2_000_000_000_000;
    const tok = tokenFor({ v: 1, r: "r1", k: "k1", e: now - 1 });
    const resolved = join.resolveInvite("/join", "?token=" + tok, now);
    assert.equal(resolved.ok, false);
    assert.equal(resolved.reason, "expired");
});

test("classify UA", () => {
    assert.equal(join.classifyUa("Mozilla/5.0 (Linux; Android 14)"), "android");
    assert.equal(join.classifyUa("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0)"), "ios");
    assert.equal(join.classifyUa("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"), "desktop");
});

test("Play urls use production applicationId", () => {
    assert.equal(join.playMarketUrl(), "market://details?id=com.vault.vanishx");
    assert.match(join.playHttpsUrl(), /play\.google\.com.*com\.vault\.vanishx/);
});
