package es.upm.oeg.tbfy.harvester.data;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class TED {

    private static final Logger LOG = LoggerFactory.getLogger(TED.class);

    private String id;

    private String title;

    private String description;

    private List<String> cpv;

    private String buyer;

    private String language;

    private String format;

    private String source;

    public TED() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getCpv() {
        return cpv;
    }

    public void setCpv(List<String> cpv) {
        this.cpv = cpv;
    }

    public String getBuyer() {
        return buyer;
    }

    public void setBuyer(String buyer) {
        this.buyer = buyer;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Document toDocument(){
        Document document = new Document();
        document.setId(this.id);
        document.setName(this.title);
        document.setContent(this.description);
        document.setFormat(this.format);
        document.setLabels(cpv);

        document.setLanguage(this.language.toLowerCase().contains("-")? StringUtils.substringBefore(this.language,"-") : this.language );
        document.setSource(this.source);
        return document;
    }

    @Override
    public String toString() {
        return "TED{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", cpv='" + cpv + '\'' +
                '}';
    }
}
