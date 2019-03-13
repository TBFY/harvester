package harvest.legal;

import es.upm.oeg.tbfy.harvester.utils.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.parser.Parser;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class EscapeTest {

    private static final Logger LOG = LoggerFactory.getLogger(EscapeTest.class);

    @Test
    public void execute() throws UnsupportedEncodingException {


        String text = "Reglamento (CE) n%deg%%nbsp%1087/2005 de la Comisi%oacute%n, de 8 de julio de 2005, por el que se modifica el Reglamento (CE) n%deg%%nbsp%1210/2003 del Consejo relativo a determinadas restricciones espec%iacute%ficas aplicables a las relaciones econ%oacute%micas y financieras con Iraq";
        String text2 = "Verordnung (EG) Nr.%nbsp%1087/2005 der Kommission vom 8. Juli 2005 zur %Auml%nderung der Verordnung (EG) Nr.%nbsp%1210/2003 des Rates %uuml%ber bestimmte spezifische Beschr%auml%nkungen in den wirtschaftlichen und finanziellen Beziehungen zu Irak";
        String text3 = "R&egrave;glement (CE) n&deg;&nbsp;1087/2005 de la Commission du 8 juillet 2005 modifiant le r&egrave;glement (CE) n&deg;&nbsp;1210/2003 du Conseil du 7 juillet 2003 concernant certaines restrictions sp&eacute;cifiques applicables aux relations &eacute;conomiques et financi&egrave;res avec l'Iraq";
        String text4 = "Commission Regulation (EC) No&nbsp;1087/2005 of 8 July 2005 amending Council Regulation (EC) No&nbsp;1210/2003 concerning certain specific restrictions on economic and financial relations with Iraq";

        LOG.info("-> " + StringEscapeUtils.unescapeXml(text));
        LOG.info("-> " + StringEscapeUtils.unescapeCsv(text));
        LOG.info("-> " + StringEscapeUtils.unescapeEcmaScript(text));
        LOG.info("-> " + StringEscapeUtils.unescapeHtml3(text));
        LOG.info("-> " + StringEscapeUtils.unescapeHtml4(text.replaceAll("%","&")));
        LOG.info("-> " + StringEscapeUtils.unescapeJava(text));
        LOG.info("-> " + StringEscapeUtils.unescapeJson(text));
        LOG.info("-> " + StringEscapeUtils.unescapeXSI(text));

        LOG.info("-->" + TextUtils.unescapePercentageCoding(text));
        LOG.info("-->" + TextUtils.unescapePercentageCoding(text2));
        LOG.info("-->" + TextUtils.unescapePercentageCoding(text3));
        LOG.info("-->" + TextUtils.unescapePercentageCoding(text4));


    }
}
