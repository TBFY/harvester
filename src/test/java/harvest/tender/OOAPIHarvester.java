package harvest.tender;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.upm.oeg.tbfy.harvester.data.OCDS;
import es.upm.oeg.tbfy.harvester.io.ApiRestClient;
import es.upm.oeg.tbfy.harvester.io.SolrClient;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class OOAPIHarvester {

    private static final Logger LOG = LoggerFactory.getLogger(OOAPIHarvester.class);

    private static final String OPENOPPS_ENDPOINT   = "https://openopps.com/api/public";

    private static final Integer MAX                = 1000;

    @Before
    public void setup() throws IOException {
        Properties properties = new Properties(System.getProperties());

        properties.load(new FileInputStream("src/test/resources/credentials.properties"));

        System.setProperties(properties);
    }


    @Test
    public void execute() throws IOException {

        ApiRestClient restClient = new ApiRestClient(OPENOPPS_ENDPOINT, System.getProperty("token"));
        SolrClient solrClient = new SolrClient(System.getProperty("solr.endpoint"));

        int page = 1;
        int size = 10;
        int count = 0;

        ObjectMapper jsonMapper = new ObjectMapper();
        solrClient.open();

        LOG.info("Ready to harvest OpenOpps API");
        try{
            while(true){

                List<OCDS> ocdsList = restClient.getOCDS(page++, size);

                jsonMapper.writeValueAsString(ocdsList.get(0));

                boolean finish = false;

                for(OCDS ocds : ocdsList){

                    // save to Solr
                    solrClient.save(ocds.toDocument());

                    LOG.info("Saved " + ocds);

                    if (MAX > 0 && ++count > MAX) {
                        finish = true;
                        break;
                    }
                }

                if (finish) break;

                if (ocdsList.isEmpty()) break;

                if (ocdsList.size() < size) break;

            }
        }finally {
            solrClient.close();
        }

    }

}


