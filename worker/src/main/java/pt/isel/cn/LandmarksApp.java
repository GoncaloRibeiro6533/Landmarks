package pt.isel.cn;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import java.util.List;

public class LandmarksApp {

    public static void main(String[] args) {

        String projectId = System.getenv("PROJECT_ID");
        Firestore db = DatabaseUtil.getDB("lab04");
        Storage storage = StorageOptions.getDefaultInstance().getService();
        ProjectSubscriptionName subName =
                ProjectSubscriptionName.of(projectId, "subscription-1");

        Subscriber subscriber = Subscriber
                .newBuilder(subName, (PubsubMessage msg, AckReplyConsumer consumer) -> {
                    try {
                        String bucket = msg.getAttributesOrThrow("bucket");
                        String blob = msg.getAttributesOrThrow("blob");
                        String id = msg.getAttributesOrThrow("id");

                        Blob imgBlob = storage.get(bucket, blob);
                        byte[] imgBytes = imgBlob.getContent();

                        List<MonumentDomain> monuments = VisionUtil.detectLandmarks(imgBytes);

                        db.collection("monuments_results")
                                .document(id)
                                .set(new Result(monuments));

                        consumer.ack();
                    } catch (Exception e) {
                        consumer.nack();
                    }
                })
                .build();

        subscriber.startAsync().awaitRunning();
        subscriber.awaitTerminated();      // blocks always
    }
}
