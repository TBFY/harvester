package es.upm.oeg.tbfy.harvester.io;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import es.upm.oeg.tbfy.harvester.utils.ParallelExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class AWSClient {

    private static final Logger LOG = LoggerFactory.getLogger(AWSClient.class);
//    private final AWSCredentials credentials;
    private final AmazonS3 s3client;


    public AWSClient(Regions region) {
//        this.credentials = new BasicAWSCredentials(
//                accessKey, secretKey
//        );
        this.s3client = AmazonS3ClientBuilder
                .standard()
                .withRegion(region)
                //.withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withCredentials(new SystemPropertiesCredentialsProvider())
                .build();


    }


    public List<String> listBuckets(){
        return s3client.listBuckets().stream().map(b -> b.getName()).collect(Collectors.toList());
    }

    public void handleObjects(String bucket, S3ObjectHandler handler){
        try{
            Integer size = 1000;
            ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucket).withMaxKeys(size);
            AtomicInteger counter = new AtomicInteger();
            ListObjectsV2Result result;
            ParallelExecutor executor = new ParallelExecutor();
            do{

                try{
                    result = s3client.listObjectsV2(req);

                    for(S3ObjectSummary summary: result.getObjectSummaries()){
                        executor.submit(() -> {
                            try{
                                handler.handle(bucket, summary.getKey());
                            }catch (Exception e){
                                LOG.error("Error handling object '" + summary.getKey()+"' from bucket: '" + bucket + "'",e);
                            }
                        });
                    }
                    String token = result.getNextContinuationToken();
                    LOG.debug(size*counter.incrementAndGet() + " objects handled");
                    req.setContinuationToken(token);
                    Thread.sleep(100);
                }catch (Exception e){
                    LOG.warn("error on request",e);
                    break;
                }
            }while(result.isTruncated());

            executor.awaitTermination(1l, TimeUnit.HOURS);

            LOG.info("Total requests: " + counter.get() + " of " + size + " each one");

        }catch (Exception e){
            LOG.warn("Object not available: " + e.getMessage());
        }
    }

    public void downloadObjects(String bucket, FileHandler handler){
        try{

            Path path = Paths.get("files");
            TransferManager xfer_mgr = TransferManagerBuilder.standard().withS3Client(s3client).build();
            LOG.info("downloading entire bucket: '" + bucket+"' ...");
            String key_prefix = "";
            MultipleFileDownload xfer = xfer_mgr.downloadDirectory(bucket, key_prefix, path.toFile());

            XferMgrProgress.showTransferProgress(xfer);
            XferMgrProgress.waitForCompletion(xfer);
            LOG.info("bucket: '" + bucket+"' downloaded");


            AtomicInteger counter = new AtomicInteger();
            Files.list(path).parallel().forEach(file -> {
                if (counter.incrementAndGet()%100 == 0) LOG.info(counter.get() + " files processed");
                handler.handle(file.toFile());
            });

            LOG.info(counter.get() + " total files processed");

        } catch (AmazonS3Exception e){
            LOG.warn("Object not available: " + e.getMessage());
        } catch (IOException e) {
            LOG.warn("File error: " + e.getMessage());
        } catch(Exception e){
            LOG.error("Unexpected error downloading files from bucket: " + bucket, e);
        }
    }


    public InputStream getContent(String bucket, String object){
        return getObject(bucket, object).getObjectContent();
    }

    public S3Object getObject(String bucket, String key){
        return s3client.getObject(new GetObjectRequest(bucket, key));
    }

    public void downloadObject(String bucket, String object, Path output) throws IOException {
        TransferManager xfer_mgr = TransferManagerBuilder.standard().withS3Client(s3client).build();
        try {
            Download xfer = xfer_mgr.download(bucket, object, output.toFile());
            XferMgrProgress.showTransferProgress(xfer);
            XferMgrProgress.waitForCompletion(xfer);
        } catch (AmazonServiceException e) {
            LOG.error("error downloading file",e);
        }
    }

    public interface S3ObjectHandler {
        boolean handle(String bucket, String object);
    }

    public interface FileHandler {
        boolean handle(File file);
    }
}
