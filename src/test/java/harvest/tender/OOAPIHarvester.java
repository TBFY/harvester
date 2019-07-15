package harvest.tender;

import com.google.common.base.Strings;
import es.upm.oeg.tbfy.harvester.data.Document;
import es.upm.oeg.tbfy.harvester.data.OCDS;
import es.upm.oeg.tbfy.harvester.data.TED;
import es.upm.oeg.tbfy.harvester.io.OpenOppsRestClient;
import es.upm.oeg.tbfy.harvester.io.SolrClient;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class OOAPIHarvester {

    private static final Logger LOG = LoggerFactory.getLogger(OOAPIHarvester.class);


    @Before
    public void setup() throws IOException {
        Properties properties = new Properties(System.getProperties());

        properties.load(new FileInputStream("src/test/resources/credentials.properties"));

        System.setProperties(properties);
    }


    @Test
    public void retrieveContracts() throws IOException {

        OpenOppsRestClient restClient = new OpenOppsRestClient(System.getProperty("openopps.user"),System.getProperty("openopps.pwd"));
//        SolrClient solrClient = new SolrClient(System.getProperty("solr.endpoint"));

        int page = 1;
        int size = 100;
        int count = 0;
        int maxDocs = 1000;


//        solrClient.open();

        LOG.info("Ready to harvest OpenOpps API");
        try{
            while(true){

                List<OCDS> ocdsList = restClient.getOCDS(page++, size);

                boolean finish = false;

                for(OCDS ocds : ocdsList){

                    // save to Solr
                    Document doc = ocds.toDocument();

                    Integer dsize = Strings.isNullOrEmpty(doc.getContent())? 0 : doc.getContent().length();
//                    solrClient.save(doc);

                    LOG.info("Saved " + ocds + "[" +dsize + "]");

                    if (maxDocs > 0 && ++count > maxDocs) {
                        finish = true;
                        break;
                    }
                }

                if (finish) break;

                if (ocdsList.isEmpty()) break;

                if (ocdsList.size() < size) break;

                break;

            }
        }finally {
//            solrClient.close();
        }
        LOG.info("saved " + count + " documents");

    }

    @Test
    public void retrieveTedArticles() throws IOException {

        OpenOppsRestClient restClient = new OpenOppsRestClient(System.getProperty("openopps.user"),System.getProperty("openopps.pwd"));
        SolrClient solrClient = new SolrClient("http://librairy.linkeddata.es/solr/documents");

        int maxDocs     = 2000;
        int minLength   = 1000;

        solrClient.open();

        try{

            for (Integer year : Arrays.asList(2010,2011,2012,2013,2014)){

                String gt = year + "-01-01";

                String lt = ++year + "-01-01";

                for(String lang : Arrays.asList("es")){
                    int pageNumber  = 1;
                    int pageSize    = 200;
                    int count       = 0;

                    LOG.info("Ready to harvest OpenOpps API in " + lang);
                    while(count<maxDocs){

                        List<TED> articles = restClient.getTED(pageNumber++, pageSize, Optional.of(lang), Optional.of(gt), Optional.of(lt));

                        boolean finish = false;

                        for(TED tedNew : articles){

                            // save to Solr
                            try{
                                Document doc = tedNew.toDocument();

                                Integer dsize = Strings.isNullOrEmpty(doc.getContent())? 0 : doc.getContent().length();
                                if (dsize < minLength) continue;
                                if (doc.getLabels() == null || doc.getLabels().isEmpty()) continue;

                                solrClient.save(doc);
                                LOG.info("Saved " + tedNew + "[" +dsize + "]");

                                if (maxDocs > 0 && ++count > maxDocs) {
                                    finish = true;
                                    break;
                                }
                            }catch (Exception e){
                                LOG.warn("Parsing error to Solr Document: " + e.getMessage());
                            }
                        }

                        if (finish){
                            LOG.info("finish true");
                            break;
                        }

                        if (articles.isEmpty()) {
                            LOG.info("articles empty");
                            break;
                        }

                        if (articles.size() < pageSize){
                            LOG.info("articles size");
                            break;
                        }


                    }
                    LOG.info("saved " + count + " documents in " + lang);
                }

            }


        }finally {
            solrClient.close();
        }

    }

}


