/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ode.runtime.memory.work;

import java.util.concurrent.PriorityBlockingQueue;

import org.apache.ode.runtime.core.work.WorkScheduler;
import org.apache.ode.runtime.memory.work.WorkManager.WorkThreadPoolExecutor;

public class WorkSchedulerImpl extends WorkScheduler {

	public WorkSchedulerImpl(org.apache.ode.runtime.memory.work.xml.WorkScheduler config, WorkThreadPoolExecutor wtp) {
		scanTime = config.getScan() > 0 ? config.getScan() : 500;
		workQueue = new PriorityBlockingQueue<>(config.getQueueSize(), new ExecutionUnitBaseComparator());
		this.wtp = wtp;
	}

}
