package harvest.legal;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import es.upm.oeg.tbfy.harvester.io.SolrClient;
import es.upm.oeg.tbfy.harvester.utils.ParallelExecutor;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.After;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class EuroVocHarvester {

    private static final Logger LOG = LoggerFactory.getLogger(EuroVocHarvester.class);

    private static final String PATH              = "input/eurovoc/concepts";

    private SolrClient solrClient;
    private ConcurrentHashMap cache;

    @Before
    public void setup() throws IOException {
        Properties properties = new Properties(System.getProperties());

        properties.load(new FileInputStream("src/test/resources/credentials.properties"));

        System.setProperties(properties);

        this.solrClient = new SolrClient("http://librairy.linkeddata.es/data/eurovoc");

        solrClient.open();

        this.cache = new ConcurrentHashMap<>();

    }

    @After
    public void shutdown(){
        solrClient.close();
    }


    @Test
    public void execute()  {

        try{

            AtomicInteger counter = new AtomicInteger();

            Iterator<Path> fileIterator = Files.newDirectoryStream(Paths.get(PATH), path -> path.toFile().exists()).iterator();

            while(fileIterator.hasNext()){

                Path path = fileIterator.next();

                File file = path.toFile();

                try{
                    LOG.info("Reading file: " + file.getAbsolutePath());
                    org.jsoup.nodes.Document xml = Jsoup.parse(file, "utf-8");

                    String lang = xml.select("eurovoc").attr("language_selected");

                    Elements records = xml.select("record");


                    for(Element record : records){
                        Map<String,Object> data = new HashMap<>();

                        Boolean deprecated = Boolean.valueOf(record.attr("deprecated"));

                        if (deprecated) continue;

                        String id               = record.attr("id");

                        String thesaurusId      = record.attr("thesaurus_id");
                        data.put("thesaurus_s",thesaurusId);

                        Element label = record.select("label").first();
                        Element labelContent = label.children().first();
                        data.put(lang+"_s",labelContent.text());

                        Elements broaderConcepts = record.select("broader");
                        if (!broaderConcepts.isEmpty()){
                            data.put("broader_t",broaderConcepts.stream().map(e -> e.text()).collect(Collectors.joining(" ")));
                        }

                        Elements relatedConcepts = record.select("related");
                        if (!relatedConcepts.isEmpty()){
                            data.put("related_t",relatedConcepts.stream().map(e -> e.text()).collect(Collectors.joining(" ")));
                        }

                        solrClient.update(id,data);
                    }

                }catch (Exception e){
                    LOG.warn("Error parsing file: " + file.getAbsolutePath(), e);
                }
            }

            LOG.info(counter.get() + " objects saved");

            LOG.info("adding root concepts..");
            addRootConcept();

        }catch (Exception e){
            LOG.error("Error on test execution",e);
        }
    }

    @Test
    public void addRootConcept(){

        try{

            AtomicInteger counter = new AtomicInteger();

            LOG.info("Reading categories ");

            SolrClient.SolrIterator iterator = solrClient.query("*:*", Arrays.asList("broader_t", "related_t"), -1);

            Optional<Map<String,Object>> document;

            Map<String,List<String>> record = new ConcurrentHashMap<>();

            ParallelExecutor executor = new ParallelExecutor();
            while((document = iterator.next()).isPresent()){
                final Map<String,Object> data = document.get();
                executor.submit(() -> {
                    try{
                        String id = (String) data.get("id");
                        LOG.info("[" + counter.incrementAndGet() + "] -> " + id);
                        List<String> rootConcept = getRootConcept(record, id);
                        record.put(id, rootConcept);
                        solrClient.update(id, ImmutableMap.of("root_t", rootConcept.stream().collect(Collectors.joining(" "))));
                    }catch (Exception e){
                        LOG.error("Unexpected error", e);
                    }
                });
            }
            executor.awaitTermination(1, TimeUnit.HOURS);

            LOG.info("Total Concepts: " + record.size());
            Set<String> rootConcepts = new TreeSet<>();
            record.entrySet().forEach(e -> rootConcepts.addAll(e.getValue()));
            LOG.info("Root Concepts: " + rootConcepts.size());

        }catch (Exception e){
            LOG.error("Error on test execution",e);
        }

    }

    public List<String> getRootConcept(Map<String,List<String>> record, String concept)  {
        Map<String, Object> broader = null;
        try {
            if (record.containsKey(concept)) return record.get(concept);
            broader = solrClient.get(concept, Arrays.asList("broader_t"));
            if ((broader.isEmpty()) || (!broader.containsKey("broader_t")) || Strings.isNullOrEmpty((String) broader.get("broader_t"))) return Arrays.asList(concept);
            List<String> broaderList = Arrays.asList(((String) broader.get("broader_t")).split(" "));
            return broaderList.stream().flatMap(b -> getRootConcept(record, b).stream()).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Test
    public void retrieveLabelsFromRDF() throws IOException {

        String file = "input/eurovoc/eurovoc_in_skos_core_concepts.rdf";
        org.jsoup.nodes.Document xml = Jsoup.parse(new File(file), "utf-8");


        Elements records = xml.select("rdf|Description");

        for (Element element : records){

            Attributes attr = element.attributes();
            String about = attr.get("rdf:about");
            String id = StringUtils.substringAfterLast(about,"/");

            Elements labels = element.select("prefLabel");
            Map<String,Object> data = new HashMap<>();

            for(Element label: labels){

                String lang = label.attributes().get("xml:lang");
                String txt  = label.text();
                data.put(lang+"_s",txt);
            }

            solrClient.update(id, data);
            LOG.info("Updated " + id);
        }



    }
}
