package harvest.tender;

import es.upm.oeg.tbfy.harvester.data.Document;
import es.upm.oeg.tbfy.harvester.io.SolrClient;
import org.apache.commons.lang.StringUtils;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 *
 *   Tenders Electronic Daily (TED) Dataset
 *    - public procurement notices from the EU and beyond
 *    - https://ted.europa.eu/
 *    - Available in xml format in 22 languages
 *    - Access to last daily editions of procurement notices as bulk download in XML format
 *    - FTP access login and password: "guest" for both fields.
 *    - ftp://guest:guest@ted.europa.eu/daily-packages/
 *
 */

public class TEDHarvester {

    private static final Logger LOG = LoggerFactory.getLogger(TEDHarvester.class);

    private static final String PATH              = "input/ted";

    private static final List<String> LANGUAGES   = Arrays.asList("en","es","fr","de","pt","it");

    @Before
    public void setup() throws IOException {
        Properties properties = new Properties(System.getProperties());

        properties.load(new FileInputStream("src/test/resources/credentials.properties"));

        System.setProperties(properties);
    }


    @Test
    public void execute()  {

        try{


            SolrClient solrClient = new SolrClient(System.getProperty("solr.endpoint"));

            solrClient.open();

            SimpleDateFormat TEDDATEFORMAT = new SimpleDateFormat("yyyyMMdd");
            SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

            AtomicInteger counter = new AtomicInteger();

            Iterator<Path> dirIterator = Files.newDirectoryStream(Paths.get(PATH), path -> path.toFile().isDirectory()).iterator();

            while(dirIterator.hasNext()){

                Path dir = dirIterator.next();

                Iterator<Path> fileIterator = Files.newDirectoryStream(Paths.get(PATH, dir.toFile().getName()), path -> path.toFile().isFile()).iterator();

                while(fileIterator.hasNext()){

                    Path path = fileIterator.next();

                    File file = path.toFile();

                    try{
                        org.jsoup.nodes.Document xml = Jsoup.parse(file, "utf-8");

                        for(String lang: LANGUAGES){

                            Document document = new Document();

                            document.setId(xml.select("RECEPTION_ID").text()+"-"+lang);
                            document.setLanguage(lang);
                            document.setSource("ted");
                            document.setFormat("xml");

                            Elements cpvList = xml.select("ORIGINAL_CPV");
                            List<String> labels = new ArrayList<>();
                            for(Element cpv : cpvList){
                                labels.add(cpv.attr("CODE"));
                            }
                            document.setLabels(labels);


                            Date date = TEDDATEFORMAT.parse(xml.select("DATE_PUB").text());

                            document.setDate(ISO8601DATEFORMAT.format(date));

                            document.setName(xml.select("ML_TI_DOC[LG="+lang+"]").select("P").text());

                            Elements body = xml.select("F02_2014[LG="+lang.toUpperCase()+"]").select("object_contract");

                            Elements paragraphs = body.select("P");
                            StringBuilder text = new StringBuilder();
                            Map<String,Integer> memory = new HashMap<>();
                            for(Element paragraph: paragraphs){
                                String txt = paragraph.text();
                                if (memory.containsKey(txt.replace(" ","_"))) continue;
                                text.append(paragraph.text()).append("\n");
                                memory.put(paragraph.text().replace(" ","_"),1);
                            }

                            document.setContent(text.toString());

                            solrClient.save(document);

                            LOG.info("saved " + document + "-" + counter.incrementAndGet());

                        }
                    }catch (Exception e){
                        LOG.warn("Error parsing file: " + file.getAbsolutePath(), e);
                    }


                }


            }


            LOG.info(counter.get() + " objects saved");

            solrClient.close();

        }catch (Exception e){
            LOG.error("Error on test execution",e);
        }

    }


}
