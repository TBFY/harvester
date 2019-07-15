package es.upm.oeg.tbfy.harvester.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.tbfy.harvester.data.Credential;
import es.upm.oeg.tbfy.harvester.data.OCDS;
import es.upm.oeg.tbfy.harvester.data.TED;
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

public class OpenOppsRestClient {

    private static final Logger LOG = LoggerFactory.getLogger(OpenOppsRestClient.class);
    private String token;

    private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper;


    public OpenOppsRestClient(String user, String pwd) {

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

            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            Credential credential = new Credential();
            credential.setUsername(user);
            credential.setPassword(pwd);

            HttpResponse<JsonNode> result = Unirest.post("http://api.openopps.com/api/api-token-auth/")
                    .headers(headers)
                    .body(credential)
                    .asJson();

            if (result.getStatus() != 200){
                throw new RuntimeException("Invalid Credentials [" + result.getStatus() + "] - " + result.getBody());
            }

            token = result.getBody().getObject().getString("token");
            LOG.info("Token received: " + token);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }




    }


    public List<OCDS> getOCDS(Integer page, Integer pageSize){

        List<OCDS> ocdsList = new ArrayList<>();

        try {
            Map<String,Object> queryParameters = new HashMap<>();
            queryParameters.put("page",page);
            queryParameters.put("page_size",pageSize);

            Map<String,String> headers = new HashMap<>();
            //headers.put("X-CSRFToken",token);
            headers.put("accept","application/json");
            headers.put("Authorization","JWT " +token);


            HttpResponse<JsonNode> result = Unirest.get("http://api.openopps.com/api/tbfy/ocds/")
                    .headers(headers)
                    .queryString(queryParameters)
                    .asJson();

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
                    ocds.setSource("oo");
                    ocds.setId(release.getString("ocid"));
                    ocds.setLanguage(release.getString("language"));

                    JSONObject buyer = release.getJSONObject("buyer");
                    if (buyer.has("name")) ocds.setBuyer(buyer.getString("name").replace(" ","_"));

                    JSONObject tender = release.getJSONObject("tender");
                    if (tender.has("title")) ocds.setTitle(tender.getString("title"));
                    if (tender.has("description")) ocds.setDescription(tender.getString("description"));
                    if (tender.has("value")){
                        JSONObject value = tender.getJSONObject("value");
                        if (value.has("amount")) ocds.setAmount(value.getInt("amount"));
                    }

                    if (tender.has("tenderPeriod")){
                        JSONObject period = tender.getJSONObject("tenderPeriod");
                        if (period.has("endDate"))  ocds.setEndDate(period.getString("endDate"));
                        if (period.has("startDate"))  ocds.setStartDate(period.getString("startDate"));
                    }

                    if (tender.has("items")){
                        Iterator<Object> items = tender.getJSONArray("items").iterator();
                        while(items.hasNext()){
                            JSONObject item = (JSONObject) items.next();

                            if (item.has("classification")){
                                JSONObject classification = item.getJSONObject("classification");
                                if (classification.has("description")){
                                    if (classification.getString("scheme").equalsIgnoreCase("CPV")){
                                        ocds.setCpv(classification.getString("description").toLowerCase());
                                    }
                                }
                            }
                        }
                    }

                    ocdsList.add(ocds);
                }

            }


        } catch (Exception e) {
            LOG.error("Unexpected error", e);
        }

        return ocdsList;
    }


    public List<TED> getTED(Integer page, Integer pageSize, Optional<String> lang, Optional<String> gt, Optional<String> lt){

        List<TED> articles = new ArrayList<>();

        try {
            Map<String,Object> queryParameters = new HashMap<>();
            queryParameters.put("page",page);
            queryParameters.put("page_size",pageSize);
            queryParameters.put("source","ted_notices");
            if (gt.isPresent()) queryParameters.put("releasedate__gte",gt.get());//"2018-01-01"
            if (lt.isPresent()) queryParameters.put("releasedate__lte",lt.get());//"2019-01-01"
            if (lang.isPresent()) queryParameters.put("language",lang.get());

            Map<String,String> headers = new HashMap<>();
            //headers.put("X-CSRFToken",token);
            headers.put("accept","application/json");
            headers.put("Authorization","JWT " +token);


            try{
                HttpResponse<JsonNode> result = Unirest.get("http://api.openopps.com/api/tbfy/ocds/")
                        .headers(headers)
                        .queryString(queryParameters)
                        .asJson();

                if (result.getStatus() != 200){
                    LOG.warn("Response error from Rest API: " + result.getStatus() + " - " + result.getStatusText());
                    return Collections.emptyList();
                }

                Iterator<Object> resultIterator = result.getBody().getObject().getJSONArray("results").iterator();
                while(resultIterator.hasNext()){
                    JSONObject json = (JSONObject) resultIterator.next();
                    LOG.debug("json: " + json);


                    Iterator<Object> releases = json.getJSONObject("json").getJSONArray("releases").iterator();

                    while(releases.hasNext()){
                        JSONObject release = (JSONObject) releases.next();
                        TED article = new TED();
                        article.setFormat("json");
                        article.setSource("ted");
                        article.setId(release.getString("ocid"));
                        article.setLanguage(release.getString("language"));

                        JSONObject buyer = release.getJSONObject("buyer");
                        if (buyer.has("name")) article.setBuyer(buyer.getString("name").replace(" ","_"));

                        JSONObject tender = release.getJSONObject("tender");
                        if (tender.has("title")) article.setTitle(tender.getString("title"));
                        if (tender.has("description")) article.setDescription(tender.getString("description"));


                        if (tender.has("items")){
                            Iterator<Object> items = tender.getJSONArray("items").iterator();
                            while(items.hasNext()){
                                JSONObject item = (JSONObject) items.next();
                                List<String> cpvCodes = new ArrayList<>();
                                if (item.has("additionalClassifications")){
                                    Iterator<Object> codes = item.getJSONArray("additionalClassifications").iterator();
                                    while(codes.hasNext()){
                                        JSONObject code = (JSONObject) codes.next();
                                        if (code.has("scheme")){
                                            if (code.getString("scheme").equalsIgnoreCase("CPV")){
                                                cpvCodes.add(code.getString("id").toLowerCase());
                                            }
                                        }
                                    }
                                }
                                article.setCpv(cpvCodes);
                            }
                        }

                        articles.add(article);
                    }


                }
            }catch (Exception e){
                LOG.error("Unexpected error parsing API response", e);
                HttpResponse<String> result = Unirest.get("http://api.openopps.com/api/tbfy/ocds/")
                        .headers(headers)
                        .queryString(queryParameters).asString();
                LOG.error("Response: " + result.getStatus() + " - " + result.getStatusText() + " -> " + result.getBody());
                return Collections.emptyList();
            }


        } catch (Exception e) {
            LOG.error("Unexpected error", e);
        }

        return articles;
    }

}
