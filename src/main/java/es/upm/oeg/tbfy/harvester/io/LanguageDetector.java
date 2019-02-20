package es.upm.oeg.tbfy.harvester.io;

import org.apache.tika.language.LanguageIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class LanguageDetector {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageDetector.class);

    public static String identifyLanguage(String text){
        LanguageIdentifier identifier = new LanguageIdentifier(text);
        return identifier.getLanguage();
    }


}
