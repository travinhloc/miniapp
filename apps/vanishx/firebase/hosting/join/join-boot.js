(function () {
    var J = window.VanishXJoin;
    if (!J) return;

    function $(id) {
        return document.getElementById(id);
    }

    function show(id) {
        ["panel-android", "panel-ios", "panel-desktop", "panel-bad"].forEach(function (p) {
            $(p).classList.toggle("on", p === id);
        });
    }

    function copyText(text) {
        if (navigator.clipboard && navigator.clipboard.writeText) {
            return navigator.clipboard.writeText(text).catch(function () {
                return copyFallback(text);
            });
        }
        return Promise.resolve(copyFallback(text));
    }

    function copyFallback(text) {
        var area = document.createElement("textarea");
        area.value = text;
        area.setAttribute("readonly", "");
        area.style.position = "fixed";
        area.style.left = "-9999px";
        document.body.appendChild(area);
        area.select();
        var ok = false;
        try {
            ok = document.execCommand("copy");
        } catch (e) {
            ok = false;
        }
        document.body.removeChild(area);
        return ok;
    }

    function openPlay() {
        window.location.href = J.playMarketUrl();
        setTimeout(function () {
            window.location.href = J.playHttpsUrl();
        }, 400);
    }

    function renderQr(url) {
        var host = $("qr");
        if (!host || typeof qrcode !== "function") return;
        var qr = qrcode(0, "M");
        qr.addData(url);
        qr.make();
        host.innerHTML = qr.createSvgTag(4, 0);
    }

    var resolved = J.resolveInvite(
        window.location.pathname,
        window.location.search,
        Date.now()
    );
    var ua = J.classifyUa(navigator.userAgent);

    if (!resolved.ok) {
        if (resolved.reason === "expired") {
            $("bad-lead").textContent =
                "Lời mời đã hết hạn. Nhờ người gửi QR / link mới.";
        }
        show("panel-bad");
        return;
    }

    var payload = J.clipboardPayload(resolved.token, resolved.expMs);
    var canonical = J.canonicalJoinUrl(window.location.origin, resolved.token);

    if (ua === "android") {
        show("panel-android");
        copyText(payload);
        $("btn-play").addEventListener("click", function (ev) {
            ev.preventDefault();
            openPlay();
        });
        setTimeout(openPlay, J.PLAY_DELAY_MS);
        return;
    }

    if (ua === "ios") {
        show("panel-ios");
        copyText(payload);
        return;
    }

    show("panel-desktop");
    $("short").textContent = J.displayShort(window.location.host, resolved.token);
    renderQr(canonical);
    $("btn-copy").addEventListener("click", function () {
        copyText(canonical).then(function () {
            $("btn-copy").textContent = "Đã chép link";
        });
    });
})();
