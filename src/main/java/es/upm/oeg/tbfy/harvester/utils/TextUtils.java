package es.upm.oeg.tbfy.harvester.utils;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class TextUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TextUtils.class);


    public static String unescapePercentageCoding(String text){
        if (Strings.isNullOrEmpty(text)) return text;
        String value = text;
        String[] escapeCharacters = StringUtils.substringsBetween(text, "%", "%");
        if (escapeCharacters != null) {
            for(String escapeCharacter : escapeCharacters){
                value = value.replace("%"+escapeCharacter+"%","&"+escapeCharacter+";");
            }
        }
        return StringEscapeUtils.unescapeHtml4(value);
    }

}
