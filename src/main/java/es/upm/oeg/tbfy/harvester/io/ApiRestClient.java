package es.upm.oeg.tbfy.harvester.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.tbfy.harvester.data.OCDS;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class ApiRestClient {

    private static final Logger LOG = LoggerFactory.getLogger(ApiRestClient.class);
    private final String endpoint; //https://openopps.com/api/public
    private final String token;

    private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper;


    public ApiRestClient(String endpoint, String token) {
        this.endpoint   = endpoint;
        this.token      = token;

        jacksonObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        jacksonObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        Unirest.setObjectMapper(new ObjectMapper() {


            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return jacksonObjectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object value) {
                try {
                    return jacksonObjectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });


        try {

            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }

            } };


            SSLContext sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext);
            CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
            Unirest.setHttpClient(httpclient);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public List<OCDS> getOCDS(Integer page, Integer pageSize){

        List<OCDS> ocdsList = new ArrayList<>();



        try {
            Map<String,Object> paginatedParameters = new HashMap<>();
            paginatedParameters.put("page",page);
            paginatedParameters.put("page_size",pageSize);

            Map<String,String> headers = new HashMap<>();
            headers.put("X-CSRFToken",token);

            String uri = endpoint.endsWith("/")? endpoint+"ocds" : endpoint + "/ocds";

            HttpResponse<JsonNode> result = Unirest.get(uri).headers(headers).queryString(paginatedParameters).asJson();

            if (result.getStatus() != 200){
                LOG.warn("Response error from Rest API: " + result.getStatus() + " - " + result.getStatusText());
                return ocdsList;
            }

            Iterator<Object> resultIterator = result.getBody().getObject().getJSONArray("results").iterator();
            while(resultIterator.hasNext()){
                JSONObject json = (JSONObject) resultIterator.next();

                Iterator<Object> releases = json.getJSONObject("json").getJSONArray("releases").iterator();

                while(releases.hasNext()){
                    JSONObject release = (JSONObject) releases.next();
                    OCDS ocds = new OCDS();
                    ocds.setFormat("json");
                    ocds.setSource("oo-api");
                    ocds.setId(release.getString("id"));
                    ocds.setLanguage(release.getString("language"));

                    JSONObject buyer = release.getJSONObject("buyer");
                    ocds.setBuyer(buyer.getString("name").replace(" ","_"));

                    JSONObject tender = release.getJSONObject("tender");
                    ocds.setTitle(tender.getString("title"));
                    ocds.setDescription(tender.getString("description"));

                    JSONObject period = tender.getJSONObject("tenderPeriod");
                    if (!period.isNull("endDate"))  ocds.setEndDate(period.getString("endDate"));
                    if (!period.isNull("startDate"))  ocds.setStartDate(period.getString("startDate"));
                    ocds.setAmount(tender.getJSONObject("value").getInt("amount"));

                    Iterator<Object> items = tender.getJSONArray("items").iterator();
                    while(items.hasNext()){
                        JSONObject item = (JSONObject) items.next();

                        JSONObject classification = item.getJSONObject("classification");
                        if (classification.getString("scheme").equalsIgnoreCase("CPV")){
                            ocds.setCpv(classification.getString("description").toLowerCase());
                        }
                    }

                    ocdsList.add(ocds);
                }

            }


        } catch (UnirestException e) {
            LOG.error("Unexpected error", e);
        }

        return ocdsList;
    }

}
