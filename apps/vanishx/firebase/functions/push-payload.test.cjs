const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const { buildDataMessage } = require("./push-payload");

describe("buildDataMessage", () => {
    it("sends data-only without ciphertext or system notification", () => {
        const msg = buildDataMessage({
            roomId: "r1",
            type: "message",
            senderPub: "pub",
        });
        assert.equal(msg.topic, "vx_room_r1");
        assert.deepEqual(msg.data, {
            v: "1",
            type: "message",
            roomId: "r1",
            senderPub: "pub",
        });
        assert.equal(msg.notification, undefined);
        assert.equal(Object.prototype.hasOwnProperty.call(msg.data, "ciphertext"), false);
    });

    it("builds ping with collapse key", () => {
        const msg = buildDataMessage({
            roomId: "r1",
            type: "ping",
            senderPub: "from",
        });
        assert.equal(msg.android.collapseKey, "r1_ping");
        assert.equal(msg.data.type, "ping");
    });

    it("rejects missing sender or unknown type", () => {
        assert.equal(
            buildDataMessage({ roomId: "r1", type: "message", senderPub: "" }),
            null,
        );
        assert.equal(
            buildDataMessage({ roomId: "r1", type: "joined", senderPub: "pub" }),
            null,
        );
    });
});
