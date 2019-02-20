package es.upm.oeg.tbfy.harvester.data;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class OCDS {

    private static final Logger LOG = LoggerFactory.getLogger(OCDS.class);

    private String id;

    private String title;

    private String description;

    private String cpv;

    private String buyer;

    private Integer amount;

    private String startDate;

    private String endDate;

    private String language;

    private String format;

    private String source;

    public OCDS() {
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

    public String getCpv() {
        return cpv;
    }

    public void setCpv(String cpv) {
        this.cpv = cpv;
    }

    public String getBuyer() {
        return buyer;
    }

    public void setBuyer(String buyer) {
        this.buyer = buyer;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
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


        String labels = cpv;
        if (cpv.contains(":")){
            labels = StringUtils.substringAfter(cpv,":");
        }

        if (labels.contains(",")){
            document.setLabels(Arrays.stream(labels.split(",")).map(t -> t.trim()).collect(Collectors.toList()));
        }else{
            document.setLabels(Arrays.asList(labels));
        }

        document.setLanguage(this.language.toLowerCase().contains("-")? StringUtils.substringBefore(this.language,"-") : this.language );
        document.setSource(this.source);
        return document;
    }

    @Override
    public String toString() {
        return "OCDS{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
