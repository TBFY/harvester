package es.upm.oeg.tbfy.harvester.io;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class FileReader {

    private static final Logger LOG = LoggerFactory.getLogger(FileReader.class);

    private static final Integer MAXIMUM_TEXT_CHUNK_SIZE = 100000;

    public static String getText(InputStream inputStream, String filename){

        Metadata metadata = new Metadata();
        metadata.set(Metadata.RESOURCE_NAME_KEY, filename);
        try {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            parser.parse(inputStream, handler, metadata);
            return handler.toString();
        } catch (Exception e) {
            LOG.error("Error parsing document: '" + filename + "'", e);
            return "";
        }

    }

    public static String getTextChunks(InputStream inputStream, String filename){
        final List<String> chunks = new ArrayList<>();
        chunks.add("");
        ContentHandlerDecorator handler = new ContentHandlerDecorator() {
            @Override
            public void characters(char[] ch, int start, int length) {
                String lastChunk = chunks.get(chunks.size() - 1);
                String thisStr = new String(ch, start, length);

                if (lastChunk.length() + length > MAXIMUM_TEXT_CHUNK_SIZE) {
                    chunks.add(thisStr);
                } else {
                    chunks.set(chunks.size() - 1, lastChunk + thisStr);
                }
            }
        };

        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        metadata.set(Metadata.RESOURCE_NAME_KEY, filename);
        try {
            parser.parse(inputStream, handler, metadata);
            return chunks.stream().collect(Collectors.joining(" "));
        } catch (Exception e) {
            LOG.error("Error parsing document: '" + filename + "'", e);
            return "";
        }
    }


}
