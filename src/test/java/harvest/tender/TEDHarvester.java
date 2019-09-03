package harvest.tender;

import com.google.common.base.Strings;
import es.upm.oeg.tbfy.harvester.data.Document;
import es.upm.oeg.tbfy.harvester.io.SolrClient;
import es.upm.oeg.tbfy.harvester.utils.ParallelExecutor;
import es.upm.oeg.tbfy.harvester.utils.TextUtils;
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
import java.util.concurrent.TimeUnit;
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

    private static final List<String> LANGUAGES   = Arrays.asList("en","es","fr","de");

    private static final Integer  MIN_LENGTH      = 1000;

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

            ParallelExecutor executor = new ParallelExecutor();

            Iterator<Path> dirIterator = Files.newDirectoryStream(Paths.get(PATH), path -> path.toFile().isDirectory()).iterator();

            while(dirIterator.hasNext()){

                Path dir = dirIterator.next();

                Iterator<Path> fileIterator = Files.newDirectoryStream(Paths.get(PATH, dir.toFile().getName()), path -> path.toFile().isFile()).iterator();

                while(fileIterator.hasNext()){

                    Path path = fileIterator.next();

                    File file = path.toFile();

                    try{
                        final org.jsoup.nodes.Document xml = Jsoup.parse(file, "utf-8");

                        executor.submit(() -> {
                            try{
                                String receptionId = xml.select("RECEPTION_ID").text();

                                String dateVal = xml.select("DATE_PUB").text();
                                Optional<Date> date = Optional.empty();
                                if (!Strings.isNullOrEmpty(dateVal)) date = Optional.of(TEDDATEFORMAT.parse(dateVal));

                                Elements forms = xml.select("FORM_SECTION").first().children();

                                for(Element form: forms){

                                    Document document = new Document();

                                    String lang = form.attr("LG").toLowerCase();

                                    if (!LANGUAGES.contains(lang)) continue;

                                    document.setId(receptionId+"-"+lang);
                                    document.setLanguage(lang);
                                    document.setSource("ted");
                                    document.setFormat("xml");

                                    Elements cpvList = form.select("CPV_CODE");
                                    List<String> labels = new ArrayList<>();
                                    for(Element cpv : cpvList){
                                        labels.add(cpv.attr("CODE"));
                                    }
                                    document.setLabels(labels);

                                    if (date.isPresent()) document.setDate(ISO8601DATEFORMAT.format(date.get()));

                                    String title = form.select("TITLE").select("P").text();
                                    if (Strings.isNullOrEmpty(title)){
                                        String t = TextUtils.unescapePercentageCoding(form.select("title").text());
                                        title = t.contains(">")? StringUtils.substringsBetween(t,">","<")[0] : t;
                                    }
                                    document.setName(title);
                                    document.setContent(form.select("SHORT_DESCR").select("P").text());

                                    String content = document.getContent();

                                    if (Strings.isNullOrEmpty(content) || content.length() < MIN_LENGTH) return;

                                    solrClient.save(document);

                                    LOG.info("saved " + document + "-" + counter.incrementAndGet());

                                }
                            }catch (Exception e){
                                LOG.error("Unexpected error",e);
                            }
                        });
                    }catch (Exception e){
                        LOG.warn("Error parsing file: " + file.getAbsolutePath(), e);
                    }
                }
            }

            executor.awaitTermination(1l, TimeUnit.HOURS);

            LOG.info(counter.get() + " objects saved");

            solrClient.close();

        }catch (Exception e){
            LOG.error("Error on test execution",e);
        }

    }


}
