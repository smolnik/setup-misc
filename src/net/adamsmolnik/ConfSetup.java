package net.adamsmolnik;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;

public class ConfSetup {

    private static final String XYZ = "studentXYZ";

    private static final AmazonS3Client s3 = new AmazonS3Client();

    public static void main(String[] args) throws Exception {
        String content = new String (Files.readAllBytes(Paths.get("/putty/certs/students/codes.txt")), StandardCharsets.UTF_8);
        String[] ss = content.split(",");
        int k = 0;
        for (int i = 1; i <= 60; i++) {
            String code = ss[i-1].trim();
            if(!code.isEmpty() && i <= 60){
                String bucket = "student0" + (i<10? "0" : "") + i; 
                byte [] bytes = code.getBytes(StandardCharsets.UTF_8);
                ObjectMetadata om = new ObjectMetadata();
                om.setContentLength(bytes.length);
                PutObjectResult resp = s3.putObject(bucket, "bonus/aws_coupon_for_" + bucket + ".txt", new ByteArrayInputStream(bytes), om);
            }
        }
        if(1 == 1){
            return;
        }
        
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
}
