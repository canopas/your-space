const {onDocumentDeleted} = require("firebase-functions/v2/firestore");
const firebase_tools = require('firebase-tools');
const {setGlobalOptions} = require("firebase-functions/v2");
setGlobalOptions({maxInstances: 5});

exports.deleteuser = onDocumentDeleted("users/{userId}", async event => {
    const snap =  event.data;
    var userId = snap.data().id;

    try {
        await firebase_tools.firestore
            .delete(`users/${userId}/user_locations`, {
                project: process.env.GCLOUD_PROJECT,
                recursive: true,
                yes: true,
                force: true
            });

        await firebase_tools.firestore
           .delete(`users/${userId}/user_sessions`, {
               project: process.env.GCLOUD_PROJECT,
               recursive: true,
               yes: true,
               force: true
           });

        console.log('User collections deleted successfully.', userId);
    } catch (error) {
        console.error('Error deleting user locations:', error);
        throw new Error('Failed to delete user locations');
    }
});

exports.deleteMessages = onDocumentDeleted("space_thread/{threadId}", async event => {
    const snap =  event.data;
    var threadId = snap.data().id;

    try {
        await firebase_tools.firestore
            .delete(`space_thread/${threadId}/thread_messages`, {
                project: process.env.GCLOUD_PROJECT,
                recursive: true,
                yes: true,
                force: true
            });

       console.log('Thread messages deleted successfully.', userId);
    } catch (error) {
        console.error('Error deleting thread messages:', error);
        throw new Error('Failed to delete thread');
    }
});


