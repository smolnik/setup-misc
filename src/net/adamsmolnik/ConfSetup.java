package net.adamsmolnik;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.AddUserToGroupRequest;
import com.amazonaws.services.identitymanagement.model.CreateLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.VersionListing;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class ConfSetup {

    private static final String XYZ = "studentXYZ";

    private static final AmazonS3Client s3 = new AmazonS3Client();

    public static void main(String[] args) throws Exception {
        // removeS3Policy();
    	createUsers();
        //addToGroup();
    	//s3Cleaning();
    }

    private static void s3Cleaning() {
        AmazonS3 s3 = new AmazonS3Client();
        s3.listBuckets().stream().filter(b -> b.getName().contains("student")).forEach(b -> {
            VersionListing vl = s3.listVersions(b.getName(), "");
            vl.getVersionSummaries().forEach(v -> {
                s3.deleteVersion(b.getName(), v.getKey(), v.getVersionId());
            });

            ObjectListing ol = s3.listObjects(b.getName());
            ol.getObjectSummaries().forEach(s -> {
                s3.deleteObject(b.getName(), s.getKey());
            });
            s3.deleteBucket(b.getName());
        });
    }

    private static void removeS3Policy() {
        AmazonS3 s3 = new AmazonS3Client();
        s3.deleteBucketPolicy("adams_personal");
    }

    
    private static void codesRefinement() throws IOException {
        String ss[] = new String(Files.readAllBytes(Paths.get("c:/putty/certs/students/codes.txt")), StandardCharsets.UTF_8).split(",");
        int i = 0;
        List<String> codes = new ArrayList<>();
        for (String s : ss) {
            ++i;
            if (i >= 41) {
                codes.add(s.trim());
            }
        }
        System.out.println(codes.size());
        Files.readAllLines(Paths.get("c:/putty/certs/students/codesRefined.txt")).forEach(codes::add);
        codes.forEach(System.out::println);
    }

    public static String fetch(int studentNumber) throws Exception {
        AmazonS3 s3 = new AmazonS3Client();
        String studentName = createStudentName(studentNumber);
        char[] cbuf = new char[100];
        try (InputStreamReader isr = new InputStreamReader(s3.getObject(studentName, "bonus/aws_coupon_for_" + studentName + ".txt").getObjectContent())) {
            isr.read(cbuf);
            return new String(cbuf).trim();
        }
    }

    private static void createUsers() throws Exception {
        AmazonIdentityManagement client = new AmazonIdentityManagementClient();
        for (int i = 24; i <= 24; i++) {
            String userName = createStudentName(i);
            client.createUser(new CreateUserRequest(userName));
            client.addUserToGroup(new AddUserToGroupRequest("codepot-student", userName));
            // client.addUserToGroup(new AddUserToGroupRequest("", userName));
            String pass = "***";
            client.createLoginProfile(new CreateLoginProfileRequest(userName, pass));
            // CreateAccessKeyResult result = client.createAccessKey(new
            // CreateAccessKeyRequest(userName));
            // AccessKey accessKey = result.getAccessKey();
        }

    }

    private static void addToGroup() throws Exception {
        AmazonIdentityManagement client = new AmazonIdentityManagementClient();
        for (int i = 1; i <= 1; i++) {
            String userName = createStudentName(i);
            client.addUserToGroup(new AddUserToGroupRequest("admin", userName));
        }

    }

    
    private static String createStudentName(int i) {
        return String.format("%03d", i);
    }

    private static void misc() throws Exception {
        final String studentNumber = "student098";

        String credentialsFileName = "credentials_" + studentNumber + ".csv";
        File credentials = new File("/putty/certs/students/" + credentialsFileName);
        s3.putObject(studentNumber, "keys/" + credentialsFileName, credentials);

        Path parentPath = Paths.get(Thread.currentThread().getContextClassLoader().getResource(".").toURI()).getParent();
        Path path = parentPath.resolve("resources");

        System.out.println(path);
        Files.walkFileTree(path, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                List<String> newLines = new ArrayList<>();
                Files.lines(file).forEach(s -> {
                    newLines.add(s.contains(XYZ) ? s.replaceAll(XYZ, studentNumber) : s);
                });
                Path newPath = path.getParent().resolve("temp").resolve(file.getFileName());
                Files.write(newPath, newLines, StandardCharsets.UTF_8);
                String fileName = file.getFileName().toString();
                String key = "conf";
                if ("global.properties".equals(fileName)) {
                } else {
                    key = key + "/services";
                }
                key = key + "/" + fileName;
                PutObjectResult resp = s3.putObject(studentNumber, key, newPath.toFile());
                System.out.println(resp);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void putCodes() throws IOException {
        AtomicInteger ai = new AtomicInteger();
        Files.readAllLines(Paths.get("/putty/certs/students/codesOk20141017.txt")).forEach(s -> {
            String studentName = createStudentName(ai.incrementAndGet());
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            ObjectMetadata om = new ObjectMetadata();
            om.setContentLength(bytes.length);
            PutObjectResult resp = s3.putObject(studentName, "bonus/aws_coupon_for_" + studentName + ".txt", new ByteArrayInputStream(bytes), om);
        });
        ;
    }

    private static void createSqs() {
        AmazonSQS sqs = new AmazonSQSClient();
        for (int i = 51; i <= 53; i++) {
            String name = createStudentName(i);
            CreateQueueRequest createQueueRequest = new CreateQueueRequest(getQueueName(name));
            sqs.createQueue(createQueueRequest).getQueueUrl();
        }
    }

    private static String getQueueName(String name) {
        return name + "-queue";
    }

    private static void sendMessages() {
        AmazonSQS sqs = new AmazonSQSClient();
        for (int i = 51; i <= 53; i++) {
            String name = createStudentName(i);
            sqs.sendMessage(new SendMessageRequest(getQueueName(name), "This is a message from " + name));
        }
    }

}
