package es.upm.oeg.tbfy.harvester.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class WriterUtils {

    private static final Logger LOG = LoggerFactory.getLogger(WriterUtils.class);

    public static BufferedWriter to(String path) throws IOException {
        File out = new File(path);
        if (out.exists()) out.delete();
        else out.getParentFile().mkdirs();
        return path.toLowerCase().contains("gzip") ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(path)))) : new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
    }

}
