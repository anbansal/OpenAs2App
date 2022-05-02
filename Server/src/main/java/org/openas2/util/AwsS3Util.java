package org.openas2.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class AwsS3Util {
    private static final Log logger = LogFactory.getLog(IOUtil.class.getSimpleName());
    private static final S3Client s3Client = S3Client.builder().build();
    private static final String bucketName = "myjavabucket-002";

    public static void createFolder(File dir) {
        String dirPath = dir.getPath();
        String[] parts = dirPath.split("data/");
        if(parts.length == 2){            
            dirPath = parts[1]+"/";
        }else{
            dirPath = dir.getName();
        }
        s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(dirPath)
                .build(),
                RequestBody.empty());
        logger.info(dirPath+" is created at AWS S3 bucket");
    }

    public static void uploadFile(File file) {
        
        String filePath = file.getPath();
        String[] parts = filePath.split("data/");
        if(parts.length == 2){            
            filePath = parts[1];
        }else{
            filePath = file.getName();
        }
        if(file.exists()){
            s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(filePath)
                .build(),
                RequestBody.fromFile(file));            
            logger.info(file+" is uploaded at AWS S3 bucket");
        }else{
            logger.info(file+" is not uploaded at AWS S3 bucket");
        }      
    }

    public static void cleanBucket() {
        ListObjectsRequest listRequest = ListObjectsRequest.builder()
                                            .bucket(bucketName).build();

        ListObjectsResponse listResponse = s3Client.listObjects(listRequest);
        List<S3Object> listObjects = listResponse.contents();
        if(listObjects.size() != 0){
            List<ObjectIdentifier> objectsToDelete = new ArrayList<ObjectIdentifier>();

            for (S3Object s3Object : listObjects) {
                objectsToDelete.add(ObjectIdentifier.builder().key(s3Object.key()).build());
            }

            DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                                                        .bucket(bucketName)
                                                        .delete(Delete.builder().objects(objectsToDelete).build())
                                                        .build();

            DeleteObjectsResponse deleteObjectsResponse = s3Client.deleteObjects(deleteObjectsRequest);

            logger.info("AWS S3 bucket "+bucketName +" is cleaned: "+ deleteObjectsResponse.hasDeleted());
        }
    }    
}


