package es.upm.oeg.tbfy.harvester.io;

import com.google.common.base.Strings;
import es.upm.oeg.tbfy.harvester.data.Document;
import es.upm.oeg.tbfy.harvester.data.OCDS;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.awt.SystemColor.window;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class SolrClient {

    private static final Logger LOG = LoggerFactory.getLogger(SolrClient.class);
    private HttpSolrClient solrClient;
    private AtomicInteger counter;
    private final String endpoint;


    public SolrClient(String endpoint) {
        this.endpoint = endpoint;
    }

    public void open(){
        this.solrClient = new HttpSolrClient.Builder(endpoint).build();
        this.counter = new AtomicInteger();
    }

    public Boolean update(String id, Map<String, Object> data) {
        Boolean saved = false;
        try{
            SolrInputDocument sd = new SolrInputDocument();
            sd.addField("id",id.replaceAll(" ",""));

            for(Map.Entry<String,Object> entry : data.entrySet()){
                String fieldName = entry.getKey();
                Object td = entry.getValue();
                Map<String,Object> updatedField = new HashMap<>();
                updatedField.put("set", td);
                sd.addField(fieldName, updatedField);
            }

            solrClient.add(sd);

            LOG.info("[" + counter.incrementAndGet() + "] Document '" + id + "' saved");

            if (counter.get() % 100 == 0){
                LOG.info("Committing partial annotations["+ this.counter.get() +"]");
                solrClient.commit();
            }

            saved = true;
        }catch (Exception e){
            LOG.error("Unexpected error annotating doc: " + id, e);
        }
        return saved;

    }

    public Boolean save(String id, Map<String, Object> data) {
        Boolean saved = false;
        try{
            SolrInputDocument sd = new SolrInputDocument();
            sd.addField("id",id.replaceAll(" ",""));

            for(String fieldName : data.keySet()){
                sd.addField(fieldName, data.get(fieldName));
            }

            solrClient.add(sd);

            LOG.info("[" + counter.incrementAndGet() + "] Document '" + id + "' saved");

            if (counter.get() % 100 == 0){
                LOG.info("Committing partial annotations["+ this.counter.get() +"]");
                solrClient.commit();
            }

            saved = true;
        }catch (Exception e){
            LOG.error("Unexpected error annotating doc: " + id, e);
        }
        return saved;

    }

    public boolean save(Document doc){

        try {
            SolrInputDocument document = new SolrInputDocument();
            document.addField("id",doc.getId());
            document.addField("name_s",doc.getName());
            document.addField("txt_t",doc.getContent());
            document.addField("size_i", Strings.isNullOrEmpty(doc.getContent())? 0 : doc.getContent().length());
            document.addField("labels_t",doc.getLabels().stream().map(r -> r.trim().replace(" ","_")).collect(Collectors.joining(" ")));
            document.addField("format_s",doc.getFormat());
            document.addField("lang_s",doc.getLanguage());
            document.addField("source_s",doc.getSource());
            document.addField("date_dt",doc.getDate());

            solrClient.add(document);

            if (counter.incrementAndGet() % 100 == 0) {
                LOG.info(counter.get() + " documents saved");
                solrClient.commit();
            }

        } catch (Exception e) {
            LOG.error("Unexpected error", e);
            return false;
        }

        return true;
    }
    
    public SolrIterator query(String query, List<String> fields, Integer maxSize) throws IOException {
        return new SolrIterator(query, fields, maxSize);
    }

    public Map<String,Object> get(String id, List<String> fields) throws IOException, SolrServerException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.addField("id");
        fields.forEach(f -> solrQuery.addField(f));
        solrQuery.setQuery("id:"+id);
        QueryResponse rsp = solrClient.query(solrQuery);
        SolrDocumentList resultList = rsp.getResults();
        Map<String,Object> data = new HashMap<>();
        if (resultList.isEmpty()) return data;
        SolrDocument result = resultList.get(0);
        fields.forEach(f -> data.put(f, result.getFieldValue(f)));
        return data;
    }

    public void close(){
        try {
            solrClient.commit();
        } catch (Exception e) {
            LOG.error("Unexpected error",e);
        }
    }


    public class SolrIterator{

        private final Integer window = 500;
        private final SolrQuery solrQuery;
        private final Integer maxSize;
        private final List<String> fields;
        private String nextCursorMark;
        private String cursorMark;
        private SolrDocumentList solrDocList;
        private AtomicInteger index;

        public SolrIterator(String query, List<String> fields, Integer maxSize) throws IOException {
            this.maxSize = maxSize;
            this.fields = fields;
            this.solrQuery = new SolrQuery();
            solrQuery.setRows(window);
            solrQuery.addField("id");
            fields.forEach(f -> solrQuery.addField(f));
            solrQuery.setQuery(query);
            solrQuery.addSort("id", SolrQuery.ORDER.asc);
            this.nextCursorMark = CursorMarkParams.CURSOR_MARK_START;
            query();
        }

        private void query() throws IOException {
            try{
                this.cursorMark = nextCursorMark;
                solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
                QueryResponse rsp = solrClient.query(solrQuery);
                this.nextCursorMark = rsp.getNextCursorMark();
                this.solrDocList = rsp.getResults();
                this.index = new AtomicInteger();
            }catch (Exception e){
                throw new IOException(e);
            }
        }

        public Optional<Map<String,Object>> next(){
            try{
                if (index.get() >= solrDocList.size()) {
                    if (index.get() < window){
                        return Optional.empty();
                    }
                    query();
                }

                if (cursorMark.equals(nextCursorMark)) {
                    return Optional.empty();
                }

                if ((maxSize > 0) && (index.get() > maxSize)){
                    return Optional.empty();
                }

                SolrDocument solrDoc = solrDocList.get(index.getAndIncrement());

                Map<String,Object> data = new HashMap<>();

                data.put("id",solrDoc.getFieldValue("id"));
                fields.forEach(f -> data.put(f, solrDoc.getFieldValue(f)));

                return Optional.of(data);
            }catch (Exception e){
                LOG.error("Unexpected error on iterated list of solr docs",e);
                if (e instanceof java.lang.IndexOutOfBoundsException) return Optional.empty();
                return Optional.of(new HashMap<>());
            }
        }
    }
}
