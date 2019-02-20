package es.upm.oeg.tbfy.harvester.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class Corpus {

    private static final Logger LOG = LoggerFactory.getLogger(Corpus.class);

    private String id;

    private String path;


    public Corpus(String id, String path) {
        this.id = id;
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "Corpus{" +
                "id='" + id + '\'' +
                '}';
    }
}
