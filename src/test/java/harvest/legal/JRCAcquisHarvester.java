package harvest.legal;

import es.upm.oeg.tbfy.harvester.data.Document;
import es.upm.oeg.tbfy.harvester.io.SolrClient;
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
 *
 *   JRC-Acquis Dataset
 *    - Collection of legislative texts from the European Union generated between years 1958 and 2006
 *    - https://ec.europa.eu/jrc/en/language-technologies/jrc-acquis
 *    - Available in xml format in 22 languages
 *    - Documents additionally are accompanied by information on the manually assigned Eurovoc subject domain classes so that the JRC-Acquis can also be used to train automatic multi-label classification software
 *    - Download site (by language)
 *    - https://wt-public.emm4u.eu/Acquis/JRC-Acquis.3.0/corpus/
 *
 */

public class JRCAcquisHarvester {

    private static final Logger LOG = LoggerFactory.getLogger(JRCAcquisHarvester.class);

    private static final String PATH              = "input/jrc";

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

            AtomicInteger counter = new AtomicInteger();

            Iterator<Path> langIterator = Files.newDirectoryStream(Paths.get(PATH), path -> path.toFile().isDirectory()).iterator();

            while(langIterator.hasNext()){
                String lang = langIterator.next().toFile().getName();

                Iterator<Path> yearIterator = Files.newDirectoryStream(Paths.get(PATH, lang), path -> path.toFile().isDirectory()).iterator();

                while(yearIterator.hasNext()){

                    String year = yearIterator.next().toFile().getName();
                    Iterator<Path> iterator = Files.newDirectoryStream(Paths.get(PATH, lang, year), path -> path.toFile().isFile()).iterator();
                    while(iterator.hasNext()){

                        Path path = iterator.next();

                        File file = path.toFile();

                        try{
                            org.jsoup.nodes.Document xml = Jsoup.parse(file, "utf-8");

                            Document document = new Document();
                            Element header = xml.getElementsByAttribute("id").first();
                            document.setId(header.attr("id"));
                            document.setLanguage(header.attr("lang"));
                            document.setSource("jrc");
                            document.setFormat("xml");
                            document.setLabels(Arrays.asList(xml.select("textClass").text().split(" ")));
                            document.setName(xml.select("title").last().text());
                            document.setDate(xml.select("date").text());

                            Elements paragraphs = xml.select("p");
                            StringBuilder text = new StringBuilder();
                            for(Element paragraph : paragraphs){
                                text.append(paragraph.text()).append("\n");
                            }
                            document.setContent(text.toString());

                            solrClient.save(document);

                            LOG.info("saved " + document + "-" + counter.incrementAndGet());

                        }catch (Exception e){
                            LOG.warn("Error parsing file: " + file.getAbsolutePath(), e);
                        }

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
