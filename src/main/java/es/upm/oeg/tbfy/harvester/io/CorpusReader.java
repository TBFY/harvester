package es.upm.oeg.tbfy.harvester.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.upm.oeg.tbfy.harvester.utils.ParallelExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class CorpusReader {

    private static final Logger LOG = LoggerFactory.getLogger(CorpusReader.class);

    public static void from(String path, Integer offset, CorpusAction action, CorpusValidation predicate, Integer ratio, Integer max, String idField, String nameField, String txtField, String labelField){
        try{
            BufferedReader reader = ReaderUtils.from(path);
            String row;
            AtomicInteger counter = new AtomicInteger();
            AtomicInteger rowNumber = new AtomicInteger();
            ParallelExecutor executor = new ParallelExecutor();
            ObjectMapper jsonMapper = new ObjectMapper();
            while((row = reader.readLine()) != null){
                if (rowNumber.incrementAndGet() < offset) continue;
                String dId;
                String dName;
                String txt;
                String label = null;
                if (row.startsWith("{")){
                    // json
                    JsonNode json = jsonMapper.readTree(row);
                    dId = json.get(idField).asText();
                    dName = json.get(nameField).asText();
                    txt = json.get(txtField).asText();
                    JsonNode out = json.get(labelField);
                    if (out.isArray()){
                        Iterator<JsonNode> it = out.iterator();
                        StringBuilder labels = new StringBuilder();
                        while(it.hasNext()){
                            labels.append(it.next().asText().replace(" ","_")).append(" ");
                        }
                        label = labels.toString();
                    }
                }else{
                    // csv
                    String[] values = row.replace("\",",",").replace(",\"",";;;").split(";;;");
                    dId = values[Integer.valueOf(idField)];
                    dName = values[Integer.valueOf(nameField)];
                    txt = values[Integer.valueOf(txtField)];
                    label = values[Integer.valueOf(labelField)];
                }
                if (!predicate.isValid(dId, dName, txt, label)) continue;

                String finalDId = dId;
                String finalDName = dName;
                String finalTxt = txt;
                String finalLabel = label;
                executor.submit(() -> action.handle(finalDId, finalDName, finalTxt, finalLabel));
                if (counter.incrementAndGet() % ratio == 0) LOG.debug(counter.get() + " docs read" );
                if ((max> 0) && (counter.get() >= max)) break;
            }
            executor.awaitTermination(1l, TimeUnit.HOURS);
            reader.close();
            LOG.info(counter.get() + " documents finally read" );
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }


    public interface CorpusAction {
        void handle(String id, String name, String txt, String label);
    }

    public interface CorpusValidation {
        boolean isValid(String id, String name, String txt, String label);
    }

}
