const functions = require("firebase-functions");
const admin = require("firebase-admin");

exports.resetDailyUsageAndYield = functions.pubsub
  .schedule("0 0 * * *")
  .timeZone("Asia/Manila")
  .onRun(async (context) => {
    try {
      const firestore = admin.firestore();
      const usersSnapshot = await firestore.collection("usageData").get();

      const resetPromises = usersSnapshot.docs.map((doc) =>
        doc.ref.update({
          Text_TodayUsageValue: 0,
          Text_TodayYieldValue: 0,
        })
      );

      await Promise.all(resetPromises);
      console.log("Successfully reset daily usage and yield values for all users.");
    } catch (error) {
      console.error("Error resetting values:", error);
    }
  });
