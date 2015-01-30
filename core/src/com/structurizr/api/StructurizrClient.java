package com.structurizr.api;

import com.structurizr.Workspace;
import com.structurizr.io.json.JsonReader;
import com.structurizr.io.json.JsonWriter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class StructurizrClient {

    private static final String GMT_TIME_ZONE = "GMT";

    private String url;
    private String apiKey;
    private String apiSecret;

    public StructurizrClient(String url, String apiKey, String apiSecret) {
        this.url = url;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public Workspace getWorkspace(long workspaceId) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url + "/workspace/" + workspaceId);
        addHeaders(httpGet, "", "");

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            debugResponse(httpGet, response);
            System.out.println(response.getEntity().getContentType());

            String json = EntityUtils.toString(response.getEntity());
            System.out.println(json);

            return new JsonReader().read(new StringReader(json));
        }
    }

    public void updateWorkspace(Workspace workspace) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPut httpPut = new HttpPut(url + "/workspace/" + workspace.getId());

        JsonWriter jsonWriter = new JsonWriter(true);
        StringWriter stringWriter = new StringWriter();
        jsonWriter.write(workspace, stringWriter);

        StringEntity stringEntity = new StringEntity(stringWriter.toString(), ContentType.APPLICATION_JSON);
        httpPut.setEntity(stringEntity);
        addHeaders(httpPut, EntityUtils.toString(stringEntity), ContentType.APPLICATION_JSON.toString());

        try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
            debugResponse(httpPut, response);
        }
    }

    private void debugResponse(HttpRequestBase httpRequest, CloseableHttpResponse response) {
        System.out.println("---");
        System.out.println(httpRequest.getMethod() + " " + httpRequest.getURI().getPath());
        System.out.println(response.getStatusLine());
    }

    private void addHeaders(HttpRequestBase httpRequest, String content, String contentType) throws Exception {
        String httpMethod = httpRequest.getMethod();
        String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of(GMT_TIME_ZONE)));
        String path = httpRequest.getURI().getPath();
        String contentMd5 = new Md5Digest().generate(content);

        HashBasedMessageAuthenticationCode hmac = new HashBasedMessageAuthenticationCode(apiKey, apiSecret);
        httpRequest.addHeader(HttpHeaders.AUTHORIZATION, hmac.generate(httpMethod, contentMd5, contentType, date, path));
        httpRequest.addHeader(HttpHeaders.DATE, date);
        httpRequest.addHeader(HttpHeaders.CONTENT_MD5, contentMd5);
        httpRequest.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
    }

    public static void main(String[] args) throws Exception {
        StructurizrClient structurizrClient = new StructurizrClient("https://structurizr-api.cfapps.io", "key", "secret");
        Workspace workspace = structurizrClient.getWorkspace(1);
        structurizrClient.updateWorkspace(workspace);
    }



}