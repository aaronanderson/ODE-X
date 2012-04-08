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
package org.apache.ode.repo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.CommandMap;
import javax.activation.DataContentHandler;
import javax.activation.DataSource;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class RepoCommandMap extends CommandMap {

	Map<String, DataContentHandler> contentHandlers = new ConcurrentHashMap<String, DataContentHandler>();
	Map<String, CommandInfo<?>[]> commandInfos = new ConcurrentHashMap<String, CommandInfo<?>[]>();

	@Override
	public DataContentHandler createDataContentHandler(String mimeType) {
		return  contentHandlers.get(mimeType);
	}

	@Override
	public DataContentHandler createDataContentHandler(String mimeType, DataSource ds) {
		return createDataContentHandler(mimeType);
	}

	@Override
	public CommandInfo<?>[] getAllCommands(String mimeType) {
		return commandInfos.get(mimeType);
	}

	@Override
	public CommandInfo<?> getCommand(String mimeType, String cmdName) {
		CommandInfo<?>[] infos = commandInfos.get(mimeType);
		if (infos != null) {
			for (CommandInfo<?> info : infos) {
				if (info.getCommandName().equals(cmdName)) {
					return info;
				}
			}
		}
		return null;
	}

	@Override
	public CommandInfo<?>[] getPreferredCommands(String mimeType) {
		CommandInfo<?>[] infos = commandInfos.get(mimeType);
		List<CommandInfo<?>> preferredInfos = new ArrayList<CommandInfo<?>>();
		if (infos != null) {
			for (CommandInfo<?> info : infos) {
				if (info.isPreferred()) {
					preferredInfos.add(info);
				}
			}
		}
		return preferredInfos.toArray(new CommandInfo[preferredInfos.size()]);
	}

	public void registerDataContentHandler(String mimeType, DataContentHandler handler) {
		contentHandlers.put(mimeType, handler);
	}

	public void unregisterDataContentHandler(String mimeType) {
		contentHandlers.remove(mimeType);
	}

	public void registerCommandInfo(String mimeType, CommandInfo<?> info) {
		CommandInfo<?>[] currentInfos = commandInfos.get(mimeType);
		int index = 0;
		if (currentInfos == null) {
			currentInfos = new CommandInfo<?>[1];
		} else {
			index = currentInfos.length;
			CommandInfo<?>[] oldCurrentInfos = currentInfos;
			currentInfos = new CommandInfo<?>[currentInfos.length+1];
			System.arraycopy(oldCurrentInfos, 0, currentInfos, 0, index);
		}
		currentInfos[index] = info;
		commandInfos.put(mimeType, currentInfos);
	}

	public void unRegisterCommandInfo(String mimeType, String commandName) {
		CommandInfo<?>[] currentInfos = commandInfos.get(mimeType);
		if (currentInfos != null) {
			for (int i = 0; i < currentInfos.length; i++) {
				if (currentInfos[i].getCommandName().equals(commandName)) {
					CommandInfo<?>[] newCurrentInfos = new CommandInfo<?>[currentInfos.length - 1];
					System.arraycopy(currentInfos, 0, newCurrentInfos, 0, i);
					System.arraycopy(currentInfos, i, newCurrentInfos, i, currentInfos.length - 1);
					commandInfos.put(mimeType, newCurrentInfos);
					break;
				}

			}
		}
	}

}
