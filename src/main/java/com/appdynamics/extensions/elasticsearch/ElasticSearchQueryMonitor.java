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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.elasticsearch.config.Configuration;
import com.appdynamics.extensions.elasticsearch.config.ElasticSearchRequest;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.util.MetricUtils;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class ElasticSearchQueryMonitor extends AManagedMonitor {

	private static final String METRIC_SEPARATOR = "|";
	private static final Logger logger = Logger.getLogger(ElasticSearchQueryMonitor.class);
	public static final String CONFIG_ARG = "config-file";
	private static final int DEFAULT_NUMBER_OF_THREADS = 5;
	private static final int TIMEOUT = 30;

	public ElasticSearchQueryMonitor() {
		String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
		logger.info(msg);
		System.out.println(msg);
	}

	/*
	 * Main execution method that uploads the metrics to AppDynamics Controller
	 * 
	 * @see
	 * com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map,
	 * com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
	 */
	public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext arg1) throws TaskExecutionException {
		if (taskArguments != null) {
			logger.info("Starting Elastic Search Query Monitoring Task");
			String configFilename = getConfigFilename(taskArguments.get(CONFIG_ARG));
			ExecutorService threadPool = null;
			try {
				Configuration config = YmlReader.readFromFile(configFilename, Configuration.class);
				Map<String, String> clientArguments = buildHttpClientArguments(config);

				SimpleHttpClient httpClient = SimpleHttpClient.builder(clientArguments).build();

				threadPool = Executors.newFixedThreadPool(config.getNumberOfThreads() == 0 ? DEFAULT_NUMBER_OF_THREADS : config.getNumberOfThreads());
				CompletionService<QueryMetrics> parallelTasks = createConcurrentTasks(threadPool, config, httpClient);

				List<QueryMetrics> metrics = collectMetrics(parallelTasks, config);

				printMetrics(config, metrics);

				logger.info("Elastic Search Query Monitoring Task completed");
				return new TaskOutput("Elastic Search Query Monitoring Task completed");
			} catch (Exception e) {
				logger.error("Metrics collection failed", e);
			} finally {
				if (threadPool != null && !threadPool.isShutdown()) {
					threadPool.shutdown();
				}
			}
		}
		throw new TaskExecutionException("Elastic Search Monitoring Task failed");
	}

	private CompletionService<QueryMetrics> createConcurrentTasks(ExecutorService threadPool, Configuration config, SimpleHttpClient httpClient) {
		CompletionService<QueryMetrics> parallelTasks = new ExecutorCompletionService<QueryMetrics>(threadPool);
		if (config != null && config.getElasticSearchRequests() != null) {
			for (ElasticSearchRequest esRequest : config.getElasticSearchRequests()) {
				QueryMonitorTask monitorTask = new QueryMonitorTask(httpClient, esRequest);
				parallelTasks.submit(monitorTask);
			}
		}
		return parallelTasks;
	}

	private List<QueryMetrics> collectMetrics(CompletionService<QueryMetrics> parallelTasks, Configuration config) {
		List<QueryMetrics> allMetrics = new ArrayList<QueryMetrics>();
		for (int i = 0; i < config.getElasticSearchRequests().size(); i++) {
			QueryMetrics metric = null;
			try {
				metric = parallelTasks.take().get(TIMEOUT, TimeUnit.SECONDS);
				allMetrics.add(metric);
			} catch (InterruptedException e) {
				logger.error("Task interrupted. ", e);
			} catch (ExecutionException e) {
				logger.error("Task execution failed. ", e);
			} catch (TimeoutException e) {
				logger.error("Task timed out. ", e);
			}
		}
		return allMetrics;
	}

	private Map<String, String> buildHttpClientArguments(Configuration config) {
		Map<String, String> clientArgs = new HashMap<String, String>();
		clientArgs.put(TaskInputArgs.HOST, config.getHost());
		clientArgs.put(TaskInputArgs.PORT, String.valueOf(config.getPort()));
		clientArgs.put(TaskInputArgs.USE_SSL, Boolean.toString(config.isUsessl()));
		clientArgs.put(TaskInputArgs.USER, config.getUsername());
		clientArgs.put(TaskInputArgs.PASSWORD, config.getPassword());
		return clientArgs;
	}

	private void printMetrics(Configuration config, List<QueryMetrics> metrics) {
		String metricPrefix = config.getMetricPrefix();
		for (QueryMetrics queryMetrics : metrics) {
			Map<String, String> entry = queryMetrics.getMetrics();
			for (Map.Entry<String, String> metric : entry.entrySet()) {
				StringBuilder metricPath = new StringBuilder();
				metricPath.append(metricPrefix).append(metric.getKey());
				printMetric(metricPath.toString(), metric.getValue());
			}
		}
	}

	private void printMetric(String metricPath, String metricValue) {
		printMetric(metricPath, metricValue, MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
	}

	private void printMetric(String metricPath, String metricValue, String aggType, String timeRollupType, String clusterRollupType) {
		MetricWriter metricWriter = getMetricWriter(metricPath, aggType, timeRollupType, clusterRollupType);
		if (metricValue != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType + "] metric = "
						+ metricPath + " = " + metricValue);
				System.out.println(metricPath + "    " + metricValue);
			}
			metricWriter.printMetric(MetricUtils.toWholeNumberString(metricValue));
		}
	}

	private String getConfigFilename(String filename) {
		if (filename == null) {
			return "";
		}
		// for absolute paths
		if (new File(filename).exists()) {
			return filename;
		}
		// for relative paths
		File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
		String configFileName = "";
		if (!Strings.isNullOrEmpty(filename)) {
			configFileName = jarPath + File.separator + filename;
		}
		return configFileName;
	}

	public static String getImplementationVersion() {
		return ElasticSearchQueryMonitor.class.getPackage().getImplementationTitle();
	}

	public static void main(String[] args) throws TaskExecutionException {
		Map<String, String> taskArgs = Maps.newHashMap();
		taskArgs.put("config-file",
				"/home/balakrishnav/AppDynamics/ExtensionsProject/elasticsearch-query-monitoring-extension/src/main/resources/conf/config.yml");
		ElasticSearchQueryMonitor esMonitor = new ElasticSearchQueryMonitor();
		esMonitor.execute(taskArgs, null);
	}
}
