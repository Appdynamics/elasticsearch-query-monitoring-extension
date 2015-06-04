# AppDynamics Elasticsearch Query Monitoring Extension
This extension works only with the standalone machine agent.

##Use Case
Elasticsearch is a distributed RESTful search server based on Lucene which provides a distributed multitenant-capable full text search engine.
This extension fetches metrics by parsing the JSON response of the custom query executed on the Elastic Search engine and reports them to AppDynamics Metric Browser.

##Installation
1. Run `mvn clean install` from the elasticsearch-query-monitoring-extension directory
2. Download and unzip ElasticSearchQueryMonitor.zip located in the 'target' directory into `<MACHINE_AGENT_HOME>/monitors`
3. Configure the extension by referring to the below section.
4. Restart the Machine Agent.

In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | Elastic Search

##Configuration
Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)
1. Configure the ElasticSearch host, port and query parameters by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/ElasticSearchQueryMonitor/`.
Below is the sample.
   ```
    # Elastic Search particulars
	host: localhost
	port: 9200
	username: 
	password: 
	usessl: false

	# number of concurrent tasks
	numberOfThreads: 5

	metricPrefix: "Custom Metrics|Elastic Search|"


	# Elastic Search Queries: queryDisplayName and metricDisplayName are the names appended in AppD Metric Browser
	# index: is the Elastic Search index that the request is queried upon. This can be a single index or multiple indices separated by comma
	# query: the actual query enclosed in single quote OR with escape characters
	# jsonPathInResponse: the xpath to the datapoint in the JSON response of the query separated by ">"
	#   If the datapoint is within a JSON Array, specify the xpath in jsonPathInResponse till the JSON Array like in facets>terms>terms
	#   All the object values with in the array whose values are numbers would be reported to controller.
	# If metricDisplayName is not specified, the jsonPathInResponse is picked up as the displayName
	# Check for metric values in AppD Metric Browser at the following path
	#  Custom Metrics|Elastic Search|queryDisplayName|metricDisplayName 
	#       OR
	# Custom Metrics|Elastic Search|queryDisplayName|metricDisplayName|array_index|key (JSON Array)

	# Example Query: ` curl -XPOST 'localhost:9200/_search?pretty' -d '{  "query": { "match_all": {} } }' `
	# Example Response: `{
						  "took" : 3,
						  "timed_out" : false,
						  "_shards" : {
						    "total" : 5,
						    "successful" : 5,
						    "failed" : 0
						  },
						  "hits" : {
						    "total" : 1,
						    "max_score" : 1.0,
						    "hits" : [ {
						      "_index" : "blog",
						      "_type" : "user",
						      "_id" : "dilbert",
						      "_score" : 1.0,
						      "_source":{ "name" : "Dilbert Brown" }
						    } ]
						  }
						}`

	elasticSearchRequests:
	   - queryDisplayName: blog_query
	     index: blog
	     query: '{ "query":{"match_all":{}}}'
	     metrics:
	        - jsonPathInResponse: hits>total
	          metricDisplayName: blog_total
	        - jsonPathInResponse: hits>hits
	        
	   - queryDisplayName: index2
	     index: index2
	     query: '{ "query":{"match_all":{}}}'
	     metrics:
	        - jsonPathInResponse: hits>total
	          metricDisplayName: index2_total
   ```
2. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/ElasticSearchQueryMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/ElasticSearchQueryMonitor/config.yml" />
          ....
     </task-arguments>
    ```

## Metrics
Note : By default, a Machine agent or a AppServer agent can send a fixed number of metrics to the controller. To change this limit, please follow the instructions mentioned [here](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).
For eg.  
```    
    java -Dappdynamics.agent.maxMetrics=2500 -jar machineagent.jar
```

##Contributing
Always feel free to fork and contribute any changes directly here on GitHub.

##Community
Find out more in the [AppSphere]() community.

##Support

For any questions or feature request, please contact [AppDynamics Support](mailto:help@appdynamics.com).
