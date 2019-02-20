package harvest.tender;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Strings;
import es.upm.oeg.tbfy.harvester.data.OCDS;
import es.upm.oeg.tbfy.harvester.io.AWSClient;
import es.upm.oeg.tbfy.harvester.io.LanguageDetector;
import es.upm.oeg.tbfy.harvester.io.SolrClient;
import es.upm.oeg.tbfy.harvester.io.FileReader;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class OOS3Harvester {

    private static final Logger LOG = LoggerFactory.getLogger(OOS3Harvester.class);

    private static final String BUCKET              = "scraper-documents";

    private static final List<String> VALID_FORMATS = Arrays.asList("pdf","doc","docx","xlsx","xls","pptx","ppt","rtf");

    @Before
    public void setup() throws IOException {
        Properties properties = new Properties(System.getProperties());

        properties.load(new FileInputStream("src/test/resources/credentials.properties"));

        System.setProperties(properties);
    }


    @Test
    public void download()  {

        try{

            AWSClient awsClient = new AWSClient(Regions.US_EAST_1);

            SolrClient solrClient = new SolrClient(System.getProperty("solr.endpoint"));

            solrClient.open();

            AtomicInteger counter = new AtomicInteger();

            AWSClient.S3ObjectHandler solrHandler = (bucket,object) -> {

                S3Object s3object = null;
                InputStream inputStream = null;
                try{

                    counter.incrementAndGet();
                    OCDS ocds = new OCDS();
                    ocds.setId(object);
                    ocds.setSource("oo-s3");

//                    File file = new File(object);
                    String name = StringUtils.substringAfterLast(object,"/");
                    ocds.setTitle(name);

                    String fileExtension = StringUtils.substringAfterLast(name, ".");
                    String format = Strings.isNullOrEmpty(fileExtension)? "unknown" : fileExtension.toLowerCase();
                    ocds.setFormat(format);

                    String description = "";

                    s3object = awsClient.getObject(bucket, object);


                    if (VALID_FORMATS.contains(format)){
                        inputStream = s3object.getObjectContent();

                        description = FileReader.getTextChunks(inputStream, name);
                    }


                    String language = "unknown";

                    if (!Strings.isNullOrEmpty(description)) {
                        language = LanguageDetector.identifyLanguage(description);
                    }
                    ocds.setLanguage(language);

                    ocds.setDescription(description);

                    solrClient.save(ocds.toDocument());
                    return true;
                }catch (Exception e){
                    LOG.warn("Error getting file: " + object, e);
                    return false;
                } finally{
                    if (s3object != null) try {
                        s3object.close();
                    } catch (Exception e) {
                        LOG.warn("Error closing object", e);
                    }
                    if (inputStream != null) try {
                        inputStream.close();
                    } catch (IOException e) {
                        LOG.warn("Error closing stream", e);
                    }
                }

            };


            awsClient.handleObjects(BUCKET, solrHandler);

            LOG.info(counter.get() + " objects requested");

            solrClient.close();

        }catch (Exception e){
            LOG.error("Error on test execution",e);
        }

    }


}
