package pt.isel.cn;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client {
    private static String svcIP =getIp();
    private static final String CLOUD_FUNCTION_URL =
            "https://europe-southwest1-cn2425-t1-g09.cloudfunctions.net/funcIpLookup?name=group-server";
    private static int svcPort = 9090;
    private static ManagedChannel channel;
    private static ImageGrpc.ImageBlockingStub blockingStub;
    private static ImageGrpc.ImageStub noBlockStub;

    private static final int CHUNK_SIZE = 64 * 1024; // 64 KB

    static void sendImageChunks(String imagePath, int chunkSize) {
        try {
            final String imageName = java.nio.file.Paths.get(imagePath).getFileName().toString();
            System.out.println(STR."Sending image: \{imageName}");
            StreamObserver<Chunk> requestObserver = noBlockStub.processImage(new StreamObserver<>() {
                @Override
                public void onNext(Identifier identifier) {
                    System.out.println("Received identifier: " + identifier.getFileID());
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("Error during image processing: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    System.out.println("Image chunks sent successfully.");
                }
            });

            byte[] buffer = new byte[chunkSize];
            try (FileInputStream fis = new FileInputStream(imagePath)) {
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    Chunk chunk = Chunk.newBuilder()
                            .setImageName(imageName)
                            .setContent(ByteString.copyFrom(buffer, 0, bytesRead))
                            .build();
                    requestObserver.onNext(chunk);
                }
            }

            requestObserver.onCompleted();
        } catch (Exception e) {
            System.err.println("Error while sending image chunks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length == 2) {
                svcIP = args[0];
                svcPort = Integer.parseInt(args[1]);
            }
            boolean connected = false;
            while (!connected) {
                while (svcIP == null) {
                    System.out.println("Failed to get IP from cloud function. Retrying...");
                    Thread.sleep(5000); // Wait before retrying
                    svcIP = getIp();
                }
                System.out.println("Connecting to " + svcIP + ":" + svcPort);
                try {
                    // Attempt to connect to the server
                    channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
                            .usePlaintext()
                            .build();
                    noBlockStub = ImageGrpc.newStub(channel);
                    blockingStub = ImageGrpc.newBlockingStub(channel);
                    connected = true;
                    System.out.println("Connected to the server at " + svcIP + ":" + svcPort);
                } catch (Exception e) {
                    System.err.println("Failed to connect to the server. Retrying...");
                    Thread.sleep(2000); // Wait before retrying
                }
            }
            Scanner scanner = new Scanner(System.in);
            boolean end = false;
            while (!end) {
                int option = menu(scanner);
                switch (option) {
                    case 1:
                        submitMonumentPhoto();
                        break;
                    case 2:
                        listMonumentsOfPhoto();
                        break;
                    case 3:
                        showStaticMap();
                        break;
                    case 4:
                        getMonumentsAboveConfidence();
                        break;
                    case 99:
                        end = true;
                        break;
                }
            }
            channel.shutdown();
        } catch (Exception ex) {
            System.out.println("Unhandled exception");
            ex.printStackTrace();
        }
    }

    private static int menu(Scanner scanner) {
        System.out.println();
        System.out.println("    MENU");
        System.out.println(" 1 - Submit a monument photo");
        System.out.println(" 2 - List all monuments of a photo");
        System.out.println(" 3 - Show static map of a geolocation");
        System.out.println(" 4 - Get photo names of monuments above a certain confidence");
        System.out.println("99 - Exit");
        System.out.println();
        System.out.print("Choose an option: ");
        return scanner.nextInt();
    }

    private static void submitMonumentPhoto() {
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter the pathname of the file to upload? ");
        String absFileName = scan.nextLine();

        sendImageChunks(absFileName, CHUNK_SIZE);
    }

    private static void listMonumentsOfPhoto() {
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter the file ID of the photo: ");
        Identifier fileID = toIdentifier(scan.nextLine());
        ExistingMonuments monuments = blockingStub.correspondingImage(fileID);
        System.out.println("Monuments in the photo:");
        for (Monument monument : monuments.getMonumentsList()) {
            System.out.println("Monument: " + "\n" + formatMonument(monument));
        }
    }

    private static void showStaticMap() {
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter the request identifier:");
        String fileID = scan.nextLine();

        try {
            MapImage map = blockingStub.getMapImage(toIdentifier(fileID));
            String file = String.format("map_%s.png", fileID);
            // save in results-maps folder
            String resultsDir = "./results-maps";
            Files.createDirectories(Paths.get(resultsDir));
            file = resultsDir + "/" + file;
            // save the image
            Files.write(Paths.get(file), map.getStaticMapImage().toByteArray());
            System.out.println("Map saved to " + file);
        } catch (Exception ex) {
            System.err.println("Error while getting the map - " + ex.getMessage());
        }
    }

    private static void getMonumentsAboveConfidence() {
        Scanner scan = new Scanner(System.in);
        System.out.print("Minimum confidence (0,0-0,9): ");
        float thr = scan.nextFloat();
        scan.nextLine();

        Confidence req = Confidence.newBuilder().setConfidence(thr).build();

        try {
            PhotosList res = blockingStub.getMonumentAboveConfidence(req);
            if (res.getPhotosCount() == 0) {
                System.out.println("No monuments meet that confidence.");
                return;
            }
            System.out.println("Monuments with confidence " + thr + ":");
            res.getPhotosList().forEach(m -> System.out.println(formatPhotoName(m)));
        } catch (Exception ex) {
            System.err.println("Error getting monuments " + ex.getMessage());
        }
    }

    private static String formatMonument(Monument m) {
        return """
                  - name : %s
                  - location : (%.6f, %.6f)
                  - confidence: %.3f
                """.formatted(
                m.getName(),
                m.getLocation().getLatitude(),
                m.getLocation().getLongitude(),
                m.getConfidence().getConfidence());
    }

    private static String formatPhotoName(PhotoName m) {
        return """
                  - name : %s
                  - location : (%.6f, %.6f)
                  - confidence: %.3f
                """.formatted(
                m.getName(),
                m.getLocation().getLatitude(),
                m.getLocation().getLongitude(),
                m.getConfidence().getConfidence());
    }

    private static Identifier toIdentifier(String fileID) {
        return Identifier.newBuilder()
                .setFileID(fileID)
                .build();
    }

    private static String getIp() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLOUD_FUNCTION_URL))
                    .GET()
                    .build();
            HttpResponse<String > response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 ) {
                IPList ipList = new Gson().fromJson(response.body(), IPList.class);
            if (ipList.ips.isEmpty()) return null;
            int randomIndex = (int) (Math.random() * ipList.ips.size());
            return ipList.ips.get(randomIndex);
            } else {
               return null;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error fetching IPs: " + e.getMessage());
            return null;
        }
    }

}

