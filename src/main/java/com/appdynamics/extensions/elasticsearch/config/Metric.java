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

public class Metric {
	private String jsonPathInResponse;
	private String metricDisplayName;
	private String arrayKey;
	private String arrayValue;

	public String getJsonPathInResponse() {
		return jsonPathInResponse;
	}

	public void setJsonPathInResponse(String jsonPathInResponse) {
		this.jsonPathInResponse = jsonPathInResponse;
	}

	public String getMetricDisplayName() {
		return metricDisplayName;
	}

	public void setMetricDisplayName(String metricDisplayName) {
		this.metricDisplayName = metricDisplayName;
	}

	public String getArrayKey() {
		return arrayKey;
	}

	public void setArrayKey(String arrayKey) {
		this.arrayKey = arrayKey;
	}

	public String getArrayValue() {
		return arrayValue;
	}

	public void setArrayValue(String arrayValue) {
		this.arrayValue = arrayValue;
	}

}
