package org.openelisglobal.datasubmission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.datasubmission.valueholder.DataIndicator;
import org.openelisglobal.datasubmission.valueholder.DataResource;
import org.openelisglobal.datasubmission.valueholder.DataValue;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataSubmitter {

    private @Autowired SiteInformationService siteInformationService;

    public boolean sendDataIndicator(DataIndicator indicator) throws IOException, ParseException {
        boolean success = true;
        for (DataResource resource : indicator.getResources()) {
            String result;
            JSONObject jsonResult;

            // expand levels if level is ALL
            List<String> levels = new ArrayList<>();
            if (resource.getLevel().equals(DataResource.ALL)) {
                levels = DataResource.getAllNamedLevels();
            } else {
                levels.add(resource.getLevel());
            }

            List<DataValue> columnValues = resource.getColumnValues();
            List<DataValue> commonValues = new ArrayList<>();
            // put in extra data that is often used
            commonValues.add(new DataValue("month", Integer.toString(indicator.getMonth()), false));
            commonValues.add(new DataValue("year", Integer.toString(indicator.getYear()), false));
            commonValues.add(new DataValue("facility", indicator.getFacilityCode(), false));
            columnValues.addAll(commonValues);
            // trust OE side has correct information
            Map<String, String> idsByLevel = resource.getLevelIdMap();
            // check for resource on server instead of trusting info is correct on OE side
            // Map<String,String> idsByLevel = getIdsBySearchKeys(resource,
            // columnValuePairs);
            try {
                // resource doesn't exist in any capacity on server. Send as normal
                if (idsByLevel.isEmpty()) {
                    result = sendJSONPost(resource);
                    jsonResult = (JSONObject) (new JSONParser()).parse(result);
                    if (jsonResult.get("error") != null) {
                        success = false;
                    }
                    for (String level : levels) {
                        if (jsonResult.containsKey(level)) {
                            String id = Long.toString((Long) ((JSONObject) jsonResult.get(level)).get("id"));
                            resource.getLevelIdMap().put(level, id);
                        }
                    }
                    // resource fully, or partially exists on server, send as sub resources
                } else {
                    resource.setLevelIdMap(idsByLevel);
                    for (String level : levels) {
                        if (idsByLevel.containsKey(level)) {
                            result = sendJSONPut(resource, level);
                            jsonResult = (JSONObject) (new JSONParser()).parse(result);
                            if (jsonResult.get("error") != null) {
                                success = false;
                            }
                        } else {
                            result = sendJSONPost(resource, level);
                            jsonResult = (JSONObject) (new JSONParser()).parse(result);
                            if (jsonResult.get("error") != null) {
                                success = false;
                            }
                            if (jsonResult.containsKey(level)) {
                                String id = Long.toString(
                                        (Long) ((JSONObject) jsonResult.get(level.toString().toLowerCase())).get("id"));
                                resource.getLevelIdMap().put(level, id);
                            }
                        }
                    }
                }
            } catch (RuntimeException e) {
                success = false;
                LogEvent.logError(e);
            } finally {
                // remove extra information as it does not need to be saved to database
                columnValues.removeAll(commonValues);
            }
        }
        return success;
    }

    @SuppressWarnings("unused")
    private Map<String, String> getIdsBySearchKeys(DataResource resource, List<DataValue> searchKeys)
            throws ClientProtocolException, IOException, ParseException {
        Map<String, String> ids = new HashMap<>();
        if (searchKeys.isEmpty()) {
            return null;
        }
        String result = sendGet(resource, resource.getLevel(), searchKeys);
        JSONObject jsonResult = (JSONObject) (new JSONParser()).parse(result);
        List<String> levels = new ArrayList<>();
        if (resource.getLevel().equals(DataResource.ALL)) {
            levels = DataResource.getAllNamedLevels();
        } else {
            levels.add(resource.getLevel());
        }
        for (String level : levels) {
            JSONArray message = (JSONArray) jsonResult.get(level);
            if (message.size() > 0) {
                JSONObject obj = (JSONObject) message.get(0);
                if (obj.containsKey("id")) {
                    ids.put(level, (String) obj.get("id"));
                } else if (obj.containsKey("ID")) {
                    ids.put(level, (String) obj.get("ID"));
                }
            }
        }

        return ids;
    }

    // get a resource based on its column-value pairs.
    public String sendGet(DataResource resource, String level, List<DataValue> searchKeys)
            throws ClientProtocolException, IOException {
        StringBuilder url = new StringBuilder();
        url.append(getBaseURL());
        url.append("/");
        url.append(resource.getCollectionName());
        url.append("/");
        url.append(resource.getLevel());
        url.append("?");
        String prefix = "";
        for (DataValue value : searchKeys) {
            url.append(prefix);
            url.append(value.getColumnName());
            url.append("=");
            url.append(value.getValue());
            prefix = "&";
        }
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(url.toString());
            request.setHeader("Accept", "application/json");
            LogEvent.logInfo(this.getClass().getSimpleName(), "sendGet", "GET: " + request.getURI());

            response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            String body = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            LogEvent.logInfo(this.getClass().getSimpleName(), "sendGet", "Server returned: " + body);
            return body;
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
        }
    }

    private String sendJSONPost(DataResource resource) throws ClientProtocolException, IOException {
        return sendJSONPost(resource, resource.getLevel());
    }

    private String sendJSONPost(DataResource resource, String level) throws ClientProtocolException, IOException {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {

            client = HttpClientBuilder.create().build();
            String url = getBaseURL() + "/" + resource.getCollectionName();
            url += "/" + resource.getLevel();
            HttpPost request = new HttpPost(url);
            StringEntity entity = new StringEntity(createJSONString(resource.getColumnValues()));
            entity.setContentType("application/json");
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");
            request.setEntity(entity);
            LogEvent.logInfo(this.getClass().getSimpleName(), "sendJSONPost",
                    "POST: " + request.getURI() + " " + createJSONString(resource.getColumnValues()));

            response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            String body = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            LogEvent.logInfo(this.getClass().getSimpleName(), "sendJSONPost", "Server returned: " + body);
            return body;
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
        }
    }

    private String sendJSONPut(DataResource resource, String level) throws ClientProtocolException, IOException {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = HttpClientBuilder.create().build();
            String url = getBaseURL() + "/" + resource.getName();
            url += "/" + level + "/" + resource.getLevelIdMap().get(level);

            HttpPut request = new HttpPut(url);
            StringEntity entity = new StringEntity(createJSONString(resource.getColumnValues()));
            entity.setContentType("application/json");
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");
            request.setEntity(entity);
            LogEvent.logInfo(this.getClass().getSimpleName(), "sendJSONPut",
                    "PUT: " + request.getURI() + " " + createJSONString(resource.getColumnValues()));

            response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            String body = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            // LogEvent.logInfo(this.getClass().getSimpleName(), "sendJSONPut", "Server
            // returned: " + body);

            return body;
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
        }
    }

    // get a resource based on its column-value pairs.
    public String sendGet(String table, List<DataValue> columnValues) throws ClientProtocolException, IOException {
        StringBuilder url = new StringBuilder();
        url.append(getBaseURL());
        url.append("/");
        url.append(table);
        url.append("?");
        String prefix = "";
        for (DataValue value : columnValues) {
            url.append(prefix);
            url.append(value.getColumnName());
            url.append("=");
            url.append(value.getValue());
            prefix = "&";
        }
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(url.toString());
            request.setHeader("Accept", "application/json");

            url.append("/");
            response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            String body = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            // LogEvent.logInfo(this.getClass().getSimpleName(), "sendGet", body);
            return body;
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
        }
    }

    // get a resource based on its column-value pairs.
    public String sendGet(String table, String id) throws ClientProtocolException, IOException {
        StringBuilder url = new StringBuilder();
        url.append(getBaseURL());
        url.append("/");
        url.append(table);
        url.append("/");
        url.append(id);
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(url.toString());
            request.setHeader("Accept", "application/json");

            response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            String body = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            // LogEvent.logInfo(this.getClass().getSimpleName(), "sendGet", body);
            return body;
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
        }
    }

    // used for talking to VL-DASHBOARD api to update an old entry
    public String sendJSONPut(String table, String foreignKey, List<DataValue> values) throws IOException {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = HttpClientBuilder.create().build();

            HttpPut request = new HttpPut(getBaseURL() + "/" + table + "/" + foreignKey);
            StringEntity entity = new StringEntity(createJSONString(values));
            entity.setContentType("application/json");
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");
            request.setEntity(entity);

            response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }
            String body = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            // LogEvent.logInfo(this.getClass().getSimpleName(), "sendJSONPut", body);
            return body;
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
        }
    }

    // used for talking to VL-DASHBOARD api to insert a new entry
    public String sendJSONPost(String table, List<DataValue> values) throws ClientProtocolException, IOException {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(getBaseURL() + "/" + table);
            StringEntity entity = new StringEntity(createJSONString(values));
            entity.setContentType("application/json");
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");
            request.setEntity(entity);

            LogEvent.logInfo(this.getClass().getSimpleName(), "sendJSONPost", getBaseURL() + "/" + table);

            response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            String body = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            // LogEvent.logInfo(this.getClass().getSimpleName(), "sendJSONPost", body);
            return body;
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String createJSONString(List<DataValue> values) {
        JSONObject json = new JSONObject();
        for (DataValue value : values) {
            json.put(value.getColumnName(), value.getValue());
        }
        return json.toString();
    }

    private String getBaseURL() {
        String url = siteInformationService.getSiteInformationByName("Data Sub URL").getValue();
        return url;
    }
}
