/**
 * Invite landing (story 14.2). No roomId / room key in the returned model.
 */
(function (root, factory) {
    var api = factory();
    root.VanishXJoin = api;
    if (typeof module === "object" && module.exports) {
        module.exports = api;
    }
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
    var PREFIX = "VANISHX_INVITE";
    var ACTION = "JOIN_ROOM";
    var PLAY_PACKAGE = "com.vault.vanishx";
    var STAGING_PACKAGE = "com.vault.vanishx.staging";
    var DISPLAY_PREFIX_LEN = 8;
    var DAY_MS = 24 * 60 * 60 * 1000;
    var PLAY_DELAY_MS = 1100;

    function queryValue(search, name) {
        var q = search.charAt(0) === "?" ? search.slice(1) : search;
        var parts = q.split("&");
        for (var i = 0; i < parts.length; i++) {
            var kv = parts[i].split("=");
            if (kv[0] === name && kv.length >= 2) {
                try {
                    return decodeURIComponent(kv.slice(1).join("="));
                } catch (e) {
                    return kv.slice(1).join("=");
                }
            }
        }
        return null;
    }

    function extractToken(pathname, search) {
        var path = (pathname || "/").replace(/\/+$/, "") || "/";
        if (path === "/join") {
            return queryValue(search || "", "token");
        }
        if (path.indexOf("/j/") === 0) {
            var rest = path.slice(3);
            var seg = rest.split("/")[0];
            if (!seg) return null;
            try {
                return decodeURIComponent(seg);
            } catch (e) {
                return seg;
            }
        }
        return null;
    }

    function base64UrlToUtf8(token) {
        var b64 = token.replace(/-/g, "+").replace(/_/g, "/");
        var pad = b64.length % 4;
        if (pad) b64 += "====".slice(0, 4 - pad);
        var bin;
        try {
            bin = atob(b64);
        } catch (e) {
            return null;
        }
        var bytes = new Uint8Array(bin.length);
        for (var i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
        try {
            return new TextDecoder("utf-8").decode(bytes);
        } catch (e) {
            return bin;
        }
    }

    function readExpiresAt(json) {
        var m = /"e"\s*:\s*(-?\d+)/.exec(json);
        if (!m) return null;
        return Number(m[1]);
    }

    function isV1InviteJson(json) {
        if (typeof json !== "string") return false;
        if (!/"v"\s*:\s*1\b/.test(json)) return false;
        if (!/"r"\s*:\s*"[^"]+"/.test(json)) return false;
        if (!/"k"\s*:\s*"[^"]+"/.test(json)) return false;
        return readExpiresAt(json) !== null;
    }

    function clipboardPayload(token, expMs) {
        return PREFIX + ":" + ACTION + ":" + token + ":" + expMs;
    }

    function classifyUa(ua) {
        var s = ua || "";
        if (/android/i.test(s)) return "android";
        if (/iPhone|iPad|iPod/i.test(s)) return "ios";
        if (/Macintosh/.test(s) && /Mobile/.test(s)) return "ios";
        return "desktop";
    }

    function isInAppBrowser(ua) {
        return /Zalo|FBAN|FBAV|FB_IAB|Instagram|Line\/|MicroMessenger|Bytedance|TikTok/i.test(ua || "");
    }

    function readJsonString(json, key) {
        var m = new RegExp('"' + key + '"\\s*:\\s*"((?:\\\\.|[^"\\\\])*)"').exec(json);
        return m ? m[1].replace(/\\(.)/g, "$1") : null;
    }

    function vanishxSchemeUrl(token) {
        var json = base64UrlToUtf8(token);
        if (!json || !isV1InviteJson(json)) return null;
        var roomId = readJsonString(json, "r");
        var roomKey = readJsonString(json, "k");
        var exp = readExpiresAt(json);
        if (!roomId || !roomKey) return null;
        var url = "vanishx://r/" + encodeURIComponent(roomId) + "?k=" + encodeURIComponent(roomKey);
        if (exp > 0) url += "&e=" + exp;
        return url;
    }

    function androidAppPackage(host) {
        var h = (host || "").split(":")[0].toLowerCase();
        if (h === "vanihx-staging.web.app" || h === "vanihx-staging.firebaseapp.com") {
            return STAGING_PACKAGE;
        }
        return PLAY_PACKAGE;
    }

    function shouldFallbackToPlay(host) {
        return androidAppPackage(host) === PLAY_PACKAGE;
    }

    /**
     * Chrome intent that opens the installed app without App Links verification.
     * Path/query only — no roomId or key.
     */
    function androidIntentUrl(canonical, host) {
        var url;
        try {
            url = new URL(canonical);
        } catch (e) {
            return null;
        }
        var pkg = androidAppPackage(host || url.host);
        return "intent://" + url.host + url.pathname + url.search +
            "#Intent;scheme=https;package=" + pkg + ";end";
    }

    function playHttpsUrl() {
        return "https://play.google.com/store/apps/details?id=" + PLAY_PACKAGE;
    }

    function displayShort(host, token) {
        return host + "/j/" + token.slice(0, DISPLAY_PREFIX_LEN);
    }

    function canonicalJoinUrl(origin, token) {
        return origin.replace(/\/+$/, "") + "/join?token=" + encodeURIComponent(token);
    }

    /**
     * @returns {{ ok: true, token: string, expMs: number } | { ok: false, reason: string }}
     */
    function resolveInvite(pathname, search, nowMs) {
        var token = extractToken(pathname, search);
        if (!token || token.length <= DISPLAY_PREFIX_LEN) {
            return { ok: false, reason: "missing" };
        }
        var json = base64UrlToUtf8(token);
        if (!json || !isV1InviteJson(json)) {
            return { ok: false, reason: "invalid" };
        }
        var roomExp = readExpiresAt(json);
        var cap = nowMs + DAY_MS;
        var expMs = roomExp > 0 ? Math.min(cap, roomExp) : cap;
        if (expMs <= nowMs) {
            return { ok: false, reason: "expired" };
        }
        return { ok: true, token: token, expMs: expMs };
    }

    return {
        PREFIX: PREFIX,
        PLAY_PACKAGE: PLAY_PACKAGE,
        STAGING_PACKAGE: STAGING_PACKAGE,
        PLAY_DELAY_MS: PLAY_DELAY_MS,
        DISPLAY_PREFIX_LEN: DISPLAY_PREFIX_LEN,
        extractToken: extractToken,
        clipboardPayload: clipboardPayload,
        classifyUa: classifyUa,
        isInAppBrowser: isInAppBrowser,
        vanishxSchemeUrl: vanishxSchemeUrl,
        androidAppPackage: androidAppPackage,
        shouldFallbackToPlay: shouldFallbackToPlay,
        androidIntentUrl: androidIntentUrl,
        playHttpsUrl: playHttpsUrl,
        displayShort: displayShort,
        canonicalJoinUrl: canonicalJoinUrl,
        resolveInvite: resolveInvite,
    };
});
