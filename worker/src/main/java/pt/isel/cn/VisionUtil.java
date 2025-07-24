package pt.isel.cn;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class VisionUtil {

    private VisionUtil() {
    }

    /**
     * Detects landmarks in a photo and converts the Vision response to proto Monument objects.
     *
     * @param imageBytes the raw JPEG/PNG bytes of the picture
     * @throws IOException if Vision RPC fails
     */
    public static List<MonumentDomain> detectLandmarks(byte[] imageBytes) throws IOException {
        Image img = Image.newBuilder()
                .setContent(ByteString.copyFrom(imageBytes))
                .build();

        Feature feat = Feature.newBuilder()
                .setType(Feature.Type.LANDMARK_DETECTION)
                .build();

        AnnotateImageRequest req = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();

        // call vision (batch style, even if single)
        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse resp =
                    vision.batchAnnotateImages(List.of(req));
            List<AnnotateImageResponse> responses = resp.getResponsesList();
            List<MonumentDomain> result = new ArrayList<>();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                    throw new IOException("Vision API error: " + res.getError().getMessage());
                }
                // convert to proto Monument list
                for (EntityAnnotation ann : res.getLandmarkAnnotationsList()) {
                    LocationInfo info = ann.getLocationsList().listIterator().next();
                    LocationDomain location = new LocationDomain(
                            info.getLatLng().getLatitude(),
                            info.getLatLng().getLongitude()
                    );
                    result.add(
                            new MonumentDomain(
                                    ann.getScore(),
                                    ann.getDescription(),
                                    location
                            )
                    );
                }
            }
            return result;
        }
    }
}
