package es.upm.oeg.tbfy.harvester.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 * 
 */

public class Textset {

    private static final Logger LOG = LoggerFactory.getLogger(Textset.class);

    Corpus corpus;

    Integer indexSize;

    String  idField;

    String nameField;

    String txtField;

    String labelField;

    public Textset(Corpus corpus, Integer indexSize, String idField, String nameField, String txtField, String labelField) {
        this.corpus = corpus;
        this.indexSize = indexSize;
        this.idField = idField;
        this.nameField = nameField;
        this.txtField = txtField;
        this.labelField = labelField;
    }

    public String getNameField() {
        return nameField;
    }

    public void setNameField(String nameField) {
        this.nameField = nameField;
    }

    public Corpus getCorpus() {
        return corpus;
    }

    public void setCorpus(Corpus corpus) {
        this.corpus = corpus;
    }

    public Integer getIndexSize() {
        return indexSize;
    }

    public void setIndexSize(Integer indexSize) {
        this.indexSize = indexSize;
    }

    public String getIdField() {
        return idField;
    }

    public void setIdField(String idField) {
        this.idField = idField;
    }

    public String getTxtField() {
        return txtField;
    }

    public void setTxtField(String txtField) {
        this.txtField = txtField;
    }

    public String getLabelField() {
        return labelField;
    }

    public void setLabelField(String labelField) {
        this.labelField = labelField;
    }

    @Override
    public String toString() {
        return "Textset{" +
                "corpus=" + corpus +
                ", indexSize=" + indexSize +
                ", idField='" + idField + '\'' +
                ", nameField='" + nameField + '\'' +
                ", txtField='" + txtField + '\'' +
                '}';
    }
}
