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
package com.appdynamics.extensions.elasticsearch.config;

import java.util.List;

public class Configuration {

	private String host;
	private int port;
	private List<ElasticSearchRequest> elasticSearchRequests;
	private String metricPrefix;
	private int numberOfThreads;
	private String username;
	private String password;
	private boolean usessl;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public List<ElasticSearchRequest> getElasticSearchRequests() {
		return elasticSearchRequests;
	}

	public void setElasticSearchRequests(List<ElasticSearchRequest> elasticSearchRequests) {
		this.elasticSearchRequests = elasticSearchRequests;
	}

	public String getMetricPrefix() {
		return metricPrefix;
	}

	public void setMetricPrefix(String metricPrefix) {
		this.metricPrefix = metricPrefix;
	}

	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isUsessl() {
		return usessl;
	}

	public void setUsessl(boolean usessl) {
		this.usessl = usessl;
	}
}
