package pt.isel.cn;

import com.google.api.core.ApiFuture;
import com.google.cloud.WriteChannel;
import com.google.cloud.firestore.*;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

public class Service extends ImageGrpc.ImageImplBase {

    final static int ZOOM = 15; // Streets
    final static String SIZE = "600x300";
    final static Firestore db = DatabaseUtil.getDB("lab04");

    public Service(int svcPort) {
        System.out.println("Service is available on port:" + svcPort);
    }


    @Override
    public void getMapImage(
            Identifier request,
            StreamObserver<MapImage> responseObserver
    ) {
        System.out.println("getMapImage called!");
        //get location from firestore
        try {
            // Get the document reference
            DocumentReference docRef = db.collection("monuments_results").document(request.getFileID());
            // Get the document snapshot
            DocumentSnapshot document = docRef.get().get();
            Result result = document.toObject(Result.class);
            if (result == null) {
                System.err.println("Result is null!");
                responseObserver.onError(new RuntimeException("Result is null"));
                return;
            }
            // Select the first monument
            MonumentDomain monument = result.monuments.getFirst();
            // Get the API key from environment variables
            String apiKey = System.getenv("GOOGLE_MAPS_API_KEY");
            if (apiKey == null) {
                System.err.println("API key not found in environment variables.");
                responseObserver.onError(new RuntimeException("API key not found"));
                return;
            }
            byte[] imageBytes = getStaticMapSaveImage(monument.location.latitude, monument.location.longitude, apiKey);
            ByteString res = ByteString.copyFrom(imageBytes);
            responseObserver.onNext(MapImage.newBuilder().setStaticMapImage(res).build());
            responseObserver.onCompleted();
            System.out.println("Image sent");
        } catch (Throwable e) {
            System.err.println("Failed to save image: " + e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void getMonumentAboveConfidence(
            Confidence confidence,
            StreamObserver<PhotosList> responseObserver
    ) {
        System.out.println("getMonumentAboveConfidence called!");
        try {
            float con = confidence.getConfidence();

            CollectionReference colRef = db.collection("monuments_results");
            ApiFuture<QuerySnapshot> querySnapshot = colRef.get();

            PhotosList.Builder responseBuilder = PhotosList.newBuilder();

            for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
                Result res = doc.toObject(Result.class);
                for (MonumentDomain monument : res.monuments) {
                    if (monument.confidence > con) {
                        PhotoName photoName = PhotoName.newBuilder()
                                .setName(monument.name)
                                .setConfidence(
                                        Confidence.newBuilder()
                                                .setConfidence(monument.confidence)
                                                .build())
                                .setLocation(
                                        Location.newBuilder()
                                                .setLatitude(monument.location.latitude)
                                                .setLongitude(monument.location.longitude)
                                )
                                .build();
                        responseBuilder.addPhotos(photoName);
                    }
                }
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Throwable e) {
            System.err.println("Failed to get monuments: " + e.getMessage());
            responseObserver.onError(e);
        }

    }

    private static byte[] getStaticMapSaveImage(Double latitude, Double longitude, String apiKey) {
        String mapUrl = "https://maps.googleapis.com/maps/api/staticmap?"
                + "center=" + latitude + "," + longitude
                + "&zoom=" + ZOOM
                + "&size=" + SIZE
                + "&key=" + apiKey;
        System.out.println(mapUrl);
        try {
            URL url = new URI(mapUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            InputStream in = conn.getInputStream();
            BufferedInputStream bufIn = new BufferedInputStream(in);
            byte[] buffer = bufIn.readAllBytes();
            in.close();
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StreamObserver<Chunk> processImage(StreamObserver<Identifier> responseObserver) {
        final UUID uuid = UUID.randomUUID();
        final String bucketName = "cn-2024-bucket-t1-09-eu";
        final String PROJECT_ID = "cn2425-t1-g09";
        final String TOPIC_ID = "cn-2024-topic-t1-09";
        final Storage storage = StorageOptions.getDefaultInstance().getService();
        ChunkWriter  chunkWriter = new ChunkWriter(storage, bucketName, uuid);

        return new StreamObserver<Chunk>() {

            @Override
            public void onNext(Chunk chunk) {
                try {
                    chunkWriter.write(chunk);
                } catch (Exception e) {
                    responseObserver.onError(Status.INTERNAL.withDescription("Error writing chunk to storage: " + e.getMessage()).asRuntimeException());
                }
            }

            @Override
            public void onError(Throwable t) {
                try {
                    chunkWriter.close();
                } catch (Exception e) {
                    System.err.println("Error closing chunk writer: " + e.getMessage());
                }
                responseObserver.onError(Status.INTERNAL.withDescription("Stream error: " + t.getMessage()).asRuntimeException());
            }

            @Override
            public void onCompleted() {
                try {
                    // Close the write channel to finalize the blob
                    chunkWriter.close();

                    Identifier identifier = Identifier.newBuilder()
                            .setFileID(String.valueOf(uuid))
                            .build();
                    responseObserver.onNext(identifier);
                    responseObserver.onCompleted();

                    // Initialize Google Cloud Pub/Sub client
                    Publisher publisher = Publisher.newBuilder(TopicName.of(PROJECT_ID, TOPIC_ID)).build();
                    TopicAdminClient topicAdmin = TopicAdminClient.create();
                    Topic res =
                            topicAdmin.getTopic("projects/%s/topics/%s".formatted(PROJECT_ID, TOPIC_ID));
                    if (res == null) System.out.println("Topic not found");
                    PubsubMessage message = PubsubMessage.newBuilder()
                            .putAttributes("bucket", bucketName)
                            .putAttributes("blob", chunkWriter.getBlobName())
                            .putAttributes("id", identifier.getFileID())
                            .build();

                    publisher.publish(message).get();
                    publisher.shutdown();
                } catch (Exception e) {
                    responseObserver.onError(Status.INTERNAL.withDescription("Error finalizing storage or publishing to Pub/Sub: " + e.getMessage()).asRuntimeException());
                }
            }
        };
    }


    @Override
    public void correspondingImage(Identifier request, StreamObserver<ExistingMonuments> responseObserver) {
        try {
            // Get the document reference and snapshot
            DocumentReference docRef = db.collection("monuments_results").document(request.getFileID());
            DocumentSnapshot document = docRef.get().get();

            // Build response
            ExistingMonuments.Builder responseBuilder = ExistingMonuments.newBuilder();
            if (document.exists()) {
                Result monuments = document.toObject(Result.class);
                if (monuments != null) {
                    for (MonumentDomain monument : monuments.monuments) {
                        // First map to domain classes
                        responseBuilder.addMonuments(
                                Monument.newBuilder()
                                        .setName(monument.name)
                                        .setLocation(Location.newBuilder()
                                                .setLatitude(monument.location.latitude)
                                                .setLongitude(monument.location.longitude)
                                                .build())
                                        .setConfidence(Confidence.newBuilder()
                                                .setConfidence(monument.confidence)
                                                .build())
                                        .build()
                        );
                    }
                }
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Error processing request: " + e.getMessage()).asRuntimeException());
        }
    }
}
