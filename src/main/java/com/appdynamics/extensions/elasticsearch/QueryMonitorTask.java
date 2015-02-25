/**
 * Copyright 2015 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appdynamics.extensions.elasticsearch;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.elasticsearch.config.ElasticSearchRequest;
import com.appdynamics.extensions.elasticsearch.config.Metric;
import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

public class QueryMonitorTask implements Callable<QueryMetrics> {

	private static final String JSON_ARRAY_FIELD_COUNT = "count";
	private static final String JSON_ARRAY_FIELD_TERM = "term";
	private SimpleHttpClient httpClient;
	private ElasticSearchRequest esRequest;
	private static final String SEARCH_JSON_URI = "_search?pretty";
	private static final Logger logger = Logger.getLogger(QueryMonitorTask.class);
	private static final String METRIC_SEPARATOR = "|";

	public QueryMonitorTask(SimpleHttpClient httpClient, ElasticSearchRequest esRequest) {
		this.httpClient = httpClient;
		this.esRequest = esRequest;
	}

	public QueryMetrics call() throws Exception {
		QueryMetrics queryMetrics = new QueryMetrics();
		Map<String, String> metrics = fetchMetrics();
		queryMetrics.setMetrics(metrics);
		return queryMetrics;
	}

	private Map<String, String> fetchMetrics() {
		Map<String, String> processedMetrics = Maps.newHashMap();
		try {
			String responseString = getJsonResponseString(httpClient, esRequest.getIndex(), esRequest.getQuery());

			processedMetrics = processResponeForMetrics(responseString, esRequest);

		} catch (Exception e) {
			logger.error("Exception while processing query " + esRequest.getQuery(), e);
		}
		return processedMetrics;
	}

	private Map<String, String> processResponeForMetrics(String responseString, ElasticSearchRequest esRequest) throws JsonParseException,
			JsonMappingException, IOException {
		Map<String, String> queryMetrics = Maps.newHashMap();
		for (Metric metric : esRequest.getMetrics()) {
			String[] paths = metric.getJsonPathInResponse().trim().split(">");
			JsonNode jsonNode = new ObjectMapper().readValue(responseString.getBytes(), JsonNode.class);
			for (int i = 0; i < paths.length; i++) {
				jsonNode = jsonNode.path(paths[i].trim());
			}
			if(jsonNode.isNumber()) {
				String metricValue = jsonNode.asText();
				String metricName = esRequest.getQueryDisplayName() + METRIC_SEPARATOR + metric.getMetricDisplayName();
				queryMetrics.put(metricName, metricValue);
			} else if(jsonNode.isArray()) {
				Iterator<JsonNode> elements = jsonNode.elements();
				while (elements.hasNext()) {
					JsonNode elementNode = elements.next();
					if(elementNode.get(JSON_ARRAY_FIELD_TERM) != null) {
						String termName = elementNode.get(JSON_ARRAY_FIELD_TERM).asText();
						String metricValue = elementNode.get(JSON_ARRAY_FIELD_COUNT).asText();
						String metricName = esRequest.getQueryDisplayName() + METRIC_SEPARATOR + metric.getMetricDisplayName() + METRIC_SEPARATOR + termName;
						queryMetrics.put(metricName, metricValue);
					}
				}
			}
		}
		return queryMetrics;
	}

	/**
	 * Connects to the provided web resource and returns the JSON response
	 * string
	 * 
	 * @param httpClient
	 *            The URL for the resource
	 * @return The JSON response string
	 * @throws Exception
	 */
	private String getJsonResponseString(SimpleHttpClient httpClient, String index, String data) throws Exception {
		Response response = null;
		try {
			response = httpClient.target().path(index).path(SEARCH_JSON_URI).post(data);
			return response.string();
		} catch (Exception e) {
			throw e;
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}

}
