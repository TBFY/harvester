package harvest.legal;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import es.upm.oeg.tbfy.harvester.data.Document;
import es.upm.oeg.tbfy.harvester.io.SolrClient;
import es.upm.oeg.tbfy.harvester.utils.ParallelExecutor;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

    String categoriesCollection = "http://librairy.linkeddata.es/solr/eurovoc";

    private static final List<String> exclude     = Arrays.asList();

    private static Map<String,Boolean> excludedThesaurus = new HashMap<>();

    @Before
    public void setup() throws IOException {
        Properties properties = new Properties(System.getProperties());

        properties.load(new FileInputStream("src/test/resources/credentials.properties"));

        System.setProperties(properties);
    }


    @Test
    public void index()  {

        try{

            SolrClient solrClient = new SolrClient(System.getProperty("solr.endpoint"));

            solrClient.open();

            AtomicInteger counter = new AtomicInteger();

            Iterator<Path> langIterator = Files.newDirectoryStream(Paths.get(PATH), path -> path.toFile().isDirectory()).iterator();

            while(langIterator.hasNext()){
                String lang = langIterator.next().toFile().getName();

                if (exclude.contains(lang)) continue;

                Iterator<Path> yearIterator = Files.newDirectoryStream(Paths.get(PATH, lang), path -> path.toFile().isDirectory()).iterator();

                while(yearIterator.hasNext()){

                    String year = yearIterator.next().toFile().getName();
                    Iterator<Path> iterator = Files.newDirectoryStream(Paths.get(PATH, lang, year), path -> path.toFile().isFile()).iterator();
                    while(iterator.hasNext()){

//                        if (counter.get() >= 1000) break;

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

                            String labelTxt = xml.select("textClass").text();

                            // avoid non-label documents
                            if (Strings.isNullOrEmpty(labelTxt)) continue;

                            List<String> docLabels = Arrays.asList(labelTxt.trim().split(" "));
                            document.setLabels(docLabels);
                            String title = xml.select("title").last().text();
                            document.setName(TextUtils.unescapePercentageCoding(title));
                            document.setDate(xml.select("date").text());

                            Elements paragraphs = xml.select("[type=body]").select("p");
                            StringBuilder text = new StringBuilder();
                            for(Element paragraph : paragraphs){
                                String textValue = paragraph.text();
                                String pText = TextUtils.unescapePercentageCoding(textValue).replaceAll(";",".");
                                text.append(pText);
                                text.append((!pText.endsWith(".") && !pText.endsWith(",") && !pText.endsWith(":") && !pText.endsWith(";"))? ". " : " ");

                            }
                            document.setContent(text.toString());

                            solrClient.save(document);

                            LOG.debug("saved " + document + "-" + counter.incrementAndGet());

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

    @Test
    public void addRootLabels(){

        List<String> GEO_THESAURUS = Arrays.asList( "0436", "0811", "2406", "7206", "7211", "7216", "7221", "7226", "7231", "7236", "7241", "7606", "7611", "7616", "7621", "7626");
        GEO_THESAURUS.forEach(i -> excludedThesaurus.put(i,false));


        String query = "source_s:jrc AND labels_t:[* TO *] AND lang_s:it";

        SolrClient solrDocClient = new SolrClient(System.getProperty("solr.endpoint"));
        SolrClient solrCatClient = new SolrClient(categoriesCollection);

        solrDocClient.open();
        solrCatClient.open();

        AtomicInteger counter = new AtomicInteger();

        try{
            SolrClient.SolrIterator iterator = solrDocClient.query(query, Arrays.asList("id", "labels_t"), -1);
            Optional<Map<String,Object>> document = null;
            Map<String,List<String>> concepts = new ConcurrentHashMap<>();
            ParallelExecutor executor = new ParallelExecutor();
            while((document = iterator.next()).isPresent()){

                final Map<String, Object> data = document.get();
                executor.submit(() -> {
                    try{
                        LOG.info("["+counter.incrementAndGet()+"]");
                        if (data.isEmpty() || !data.containsKey("labels_t")) return;

                        String id = (String) data.get("id");

                        List<String> labels = Arrays.asList(((String) data.get("labels_t")).split(" "));

                        String rootConcepts = labels.stream().flatMap(l -> getRootConcept(solrCatClient,concepts,l).stream()).distinct().collect(Collectors.joining(" "));
                        solrDocClient.update(id, ImmutableMap.of("root-labels_t",rootConcepts));

                    }catch (Exception e){
                        LOG.error("Unexpected error",e);
                    }

                });
            }
            executor.awaitTermination(1, TimeUnit.HOURS);


        }catch(Exception e ){
            LOG.error("Unexpected error",e);
        }

        LOG.info("Total Docs: " + counter.get());
        solrCatClient.close();
        solrDocClient.close();

    }

    private List<String> getRootConcept(SolrClient solrClient, Map<String, List<String>> concepts, String label){
        try {
            if (concepts.containsKey(label)) return concepts.get(label);
            Map<String, Object> result = solrClient.get(label, Arrays.asList("root_t","thesaurus_s"));
            if (result.isEmpty() || !result.containsKey("root_t")) return Collections.emptyList();

//            String thesaurusId = (String) result.get("thesaurus_s");
//            if (excludedThesaurus.containsKey(thesaurusId)){
//                concepts.put(label,Collections.emptyList());
//                return concepts.get(label);
//            }

            List<String> labelConcepts = Arrays.asList(((String) result.get("root_t")).split(" "));
            concepts.put(label,labelConcepts);
            return labelConcepts;
        } catch (Exception e) {
            LOG.error("Unexpected error",e);
            return Collections.emptyList();
        }
    }


    private List<String> getHierarchy(SolrClient solrClient, Map<String, Object> cache, String concept){
        try {
            if (!cache.containsKey(concept)){
                Map<String, Object> result = solrClient.get(concept, Arrays.asList("broader_t"));
                String val = result.get("broader_t") == null?"" : (String) result.get("broader_t");
                cache.put(concept, val);
            }
            if (!cache.containsKey(concept) || cache.get(concept) == null || Strings.isNullOrEmpty((String) cache.get(concept))) return new ArrayList<String>();
            List<String> parents = new ArrayList<>();
            parents.addAll(Arrays.asList(((String) cache.get(concept)).split(" ")));
            int limit = parents.size();
            for(int i=0;i<limit;i++){
                String parent = parents.get(i);
                if (parent.equalsIgnoreCase(concept)) continue;
                List<String> hp = getHierarchy(solrClient, cache, parent);
                parents.addAll(hp);
            }
            return parents;

        } catch (Exception e) {
            LOG.warn("Unexpected error",e);
            return new ArrayList<>();
        }
    }

}
