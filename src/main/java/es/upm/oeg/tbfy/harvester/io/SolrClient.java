package es.upm.oeg.tbfy.harvester.io;

import com.google.common.base.Strings;
import es.upm.oeg.tbfy.harvester.data.Document;
import es.upm.oeg.tbfy.harvester.data.OCDS;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

    public void close(){
        try {
            solrClient.commit();
        } catch (Exception e) {
            LOG.error("Unexpected error",e);
        }
    }

}
