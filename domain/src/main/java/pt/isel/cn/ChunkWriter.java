package pt.isel.cn;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import servicestubs.Chunk;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class ChunkWriter {

    private final Storage storage;           // Google Cloud Storage client
    private final String bucketName;        // destination bucket
    private final UUID uploadId;          // UUID chosen by caller (one per upload)

    private String imageName;           // name provided in first chunk
    private String blobName;            // full object name in GCS
    private WriteChannel writeChannel;       // lazily opened when first chunk arrives

    public ChunkWriter(Storage storage, String bucketName, UUID uploadId) {
        this.storage = storage;
        this.bucketName = bucketName;
        this.uploadId = uploadId;
    }

    /**
     * It initializes a WriteChannel on the first chunk and appends subsequent chunks to it.
     * The blob name is constructed using the image name and a UUID provided by the caller.
     */
    public void write(Chunk chunk) throws Exception {

        // first chunk must contain the image name. channel is opened lazily
        if (writeChannel == null) {
            imageName = stripExtension(chunk.getImageName());
            String ext = extensionOf(chunk.getImageName());
            blobName = "%s:%s.%s".formatted(imageName, uploadId, ext);

            BlobInfo info = BlobInfo.newBuilder(BlobId.of(bucketName, blobName)).build();
            writeChannel = storage.writer(info);
        }

        // append current chunk
        writeChannel.write(ByteBuffer.wrap(chunk.getContent().toByteArray()));
    }

    public void close() throws Exception {
        if (writeChannel != null) writeChannel.close();
    }

    public String getBlobName() {
        return blobName;
    }

    public String getImageName() {
        return imageName;
    }

    private static String extensionOf(String file) {
        int dot = file.lastIndexOf('.');
        return dot == -1 ? "" : file.substring(dot + 1);
    }

    private static String stripExtension(String file) {
        int dot = file.lastIndexOf('.');
        return dot == -1 ? file : file.substring(0, dot);
    }
}
