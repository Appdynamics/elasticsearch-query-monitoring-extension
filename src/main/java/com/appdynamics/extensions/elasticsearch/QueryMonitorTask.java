package com.appdynamics.extensions.elasticsearch;

import com.appdynamics.extensions.elasticsearch.config.ElasticSearchRequest;
import com.appdynamics.extensions.elasticsearch.config.Metric;
import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;

public class QueryMonitorTask implements Callable<QueryMetrics> {
  private SimpleHttpClient httpClient;
  
  private ElasticSearchRequest esRequest;
  
  private static final String SEARCH_JSON_URI = "_count?pretty";
  
  private static final Logger logger = Logger.getLogger(QueryMonitorTask.class);
  
  private static final String METRIC_SEPARATOR = "|";
  
  public QueryMonitorTask(SimpleHttpClient httpClient, ElasticSearchRequest esRequest) {
    this.httpClient = httpClient;
    this.esRequest = esRequest;
  }
  
  public QueryMetrics call() throws Exception {
    QueryMetrics queryMetrics = new QueryMetrics();
    Map<String, Double> metrics = fetchMetrics();
    queryMetrics.setMetrics(metrics);
    return queryMetrics;
  }
  
  private Map<String, Double> fetchMetrics() {
    Map<String, Double> processedMetrics = Maps.newHashMap();
    try {
      String responseString = getJsonResponseString(this.httpClient, this.esRequest.getIndex(), this.esRequest.getQuery());
      processedMetrics = processResponeForMetrics(responseString, this.esRequest);
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("Exception while processing query " + this.esRequest.getQuery(), e);
    } 
    return processedMetrics;
  }
  
  private Map<String, Double> processResponeForMetrics(String responseString, ElasticSearchRequest esRequest) throws JsonParseException, JsonMappingException, IOException {
    Map<String, Double> queryMetrics = Maps.newHashMap();
    if (esRequest.getQueryDisplayName() != null) {
      String queryName = esRequest.getQueryDisplayName();
      for (Metric metric : esRequest.getMetrics()) {
        if (metric.getJsonPathInResponse() != null) {
          String[] paths = metric.getJsonPathInResponse().trim().split(">");
          JsonNode jsonNode = (JsonNode)(new ObjectMapper()).readValue(responseString.getBytes(), JsonNode.class);
          int i;
          for (i = 0; i < paths.length; i++)
            jsonNode = jsonNode.path(paths[i].trim()); 
          if (jsonNode.isNumber()) {
            Double metricValue = Double.valueOf(jsonNode.asDouble());
            StringBuilder sb = (new StringBuilder(queryName)).append("|").append(buildMetricDisplayName(metric));
            String metricName = sb.toString();
            queryMetrics.put(metricName, metricValue);
            continue;
          } 
          if (jsonNode.isArray()) {
            i = 0;
            Iterator<JsonNode> elements = jsonNode.elements();
            while (elements.hasNext()) {
              JsonNode elementNode = elements.next();
              Iterator<Map.Entry<String, JsonNode>> fields = elementNode.fields();
              while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (((JsonNode)entry.getValue()).isNumber()) {
                  StringBuilder sb = (new StringBuilder(queryName)).append("|").append(
                      buildMetricDisplayName(metric));
                  String metricName = sb.append("|").toString() + i + "|" + (String)entry.getKey();
                  Double metricValue = Double.valueOf(((JsonNode)entry.getValue()).asDouble());
                  queryMetrics.put(metricName, metricValue);
                } 
              } 
              i++;
            } 
          } 
        } 
      } 
    } else {
      logger.error("queryDisplayName cannot be null for " + esRequest.getQuery());
    } 
    return queryMetrics;
  }
  
  private String buildMetricDisplayName(Metric metric) {
    if (metric.getMetricDisplayName() != null)
      return metric.getMetricDisplayName(); 
    return metric.getJsonPathInResponse().replace(">", "|");
  }
  
  private String getJsonResponseString(SimpleHttpClient httpClient, String index, String data) throws Exception {
    Response response = null;
    try {
      long myMilliseconds = System.currentTimeMillis();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
      Date rsDate = new Date(myMilliseconds);
      int thisHours = rsDate.getHours();
      int thisMinutes = rsDate.getMinutes();
      if (thisHours > 7 || (thisHours == 7 && thisMinutes >= 30))
        index = index + "_" + sdf.format(rsDate); 
      response = httpClient.target().path(index).path(SEARCH_JSON_URI).header("Content-Type","application/json").post(data);
      return response.string();
    } catch (Exception e) {
      throw e;
    } finally {
      if (response != null)
        response.close(); 
    } 
  }
}
