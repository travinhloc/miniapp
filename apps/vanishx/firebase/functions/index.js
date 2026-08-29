const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
const { buildDataMessage } = require("./push-payload");

admin.initializeApp();

async function fanOut(roomId, type, senderPub) {
    const message = buildDataMessage({ roomId, type, senderPub });
    if (!message) return;
    await admin.messaging().send(message);
}

exports.onMailboxMessageCreated = functions.database
    .ref("/rooms/{roomId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
        const val = snap.val() || {};
        await fanOut(context.params.roomId, "message", val.senderPub);
    });

exports.onRoomSignalCreated = functions.database
    .ref("/rooms/{roomId}/signals/{signalId}")
    .onCreate(async (snap, context) => {
        const val = snap.val() || {};
        try {
            if (val.type === "ping") {
                await fanOut(context.params.roomId, "ping", val.fromPub);
            }
        } finally {
            await snap.ref.remove();
        }
    });
