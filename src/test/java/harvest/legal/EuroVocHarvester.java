package harvest.legal;

import es.upm.oeg.tbfy.harvester.data.Document;
import es.upm.oeg.tbfy.harvester.io.SolrClient;
import es.upm.oeg.tbfy.harvester.utils.TextUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class EuroVocHarvester {

    private static final Logger LOG = LoggerFactory.getLogger(EuroVocHarvester.class);

    private static final String PATH              = "input/eurovoc";

    @Before
    public void setup() throws IOException {
        Properties properties = new Properties(System.getProperties());

        properties.load(new FileInputStream("src/test/resources/credentials.properties"));

        System.setProperties(properties);
    }


    @Test
    public void execute()  {

        try{


            SolrClient solrClient = new SolrClient(System.getProperty("solr.categories.endpoint"));

            solrClient.open();

            AtomicInteger counter = new AtomicInteger();

            Iterator<Path> fileIterator = Files.newDirectoryStream(Paths.get(PATH), path -> path.toFile().exists()).iterator();

            while(fileIterator.hasNext()){

                Path path = fileIterator.next();

                File file = path.toFile();

                try{
                    LOG.info("Reading file: " + file.getAbsolutePath());
                    org.jsoup.nodes.Document xml = Jsoup.parse(file, "utf-8");


                    Element header = xml.select("[LNG]").first();

                    String lang = header.attr("lng");

                    for(Element record : header.children()){
                        Map<String,Object> data = new HashMap<>();

                        String id   = record.select("descripteur_id").text();
                        String name = record.select("libelle").text();

                        data.put(lang+"_s",name);

                        solrClient.update(id,data);
                    }



                }catch (Exception e){
                    LOG.warn("Error parsing file: " + file.getAbsolutePath(), e);
                }


            }

            LOG.info(counter.get() + " objects saved");

            solrClient.close();

        }catch (Exception e){
            LOG.error("Error on test execution",e);
        }

    }
}
