/**
 * Data-only FCM for VanishX (Epic 15). Never attach ciphertext or notification body.
 */
function buildDataMessage({ roomId, type, senderPub }) {
    if (!roomId || typeof roomId !== "string") return null;
    if (type !== "message" && type !== "ping") return null;
    if (!senderPub || typeof senderPub !== "string") return null;
    return {
        topic: `vx_room_${roomId}`,
        data: {
            v: "1",
            type,
            roomId,
            senderPub,
        },
        android: {
            priority: "high",
            collapseKey: `${roomId}_${type}`,
        },
    };
}

module.exports = { buildDataMessage };
