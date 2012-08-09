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
package org.apache.ode.runtime.interpreter;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.apache.ode.runtime.interpreter.IndexedBlockAddress.IndexedBlcAddAdapter;
import org.apache.ode.runtime.interpreter.IndexedBlockAddress.IndexedBlcAddRefAdapter;
import org.apache.ode.runtime.interpreter.IndexedInstructionAddress.IndexedInsAddAdapter;
import org.apache.ode.runtime.interpreter.IndexedInstructionAddress.IndexedInsAddRefAdapter;
import org.apache.ode.spi.exec.executable.xml.Block;
import org.apache.ode.spi.exec.xml.BlockAddress.BlcAddAdapter;
import org.apache.ode.spi.exec.xml.BlockAddress.BlcAddRefAdapter;
import org.apache.ode.spi.exec.executable.xml.Executable;
import org.apache.ode.spi.exec.executable.xml.Instruction;
import org.apache.ode.spi.exec.xml.InstructionAddress.InsAddAdapter;
import org.apache.ode.spi.exec.xml.InstructionAddress.InsAddRefAdapter;

public class IndexedExecutable {

	public IndexedBlockAddress[] blocks;

	private IndexedExecutable() {
	}

	public static IndexedExecutable configure(Unmarshaller unMarshaller) {
		Map<String, IndexedBlockAddress> blocks = new HashMap<String, IndexedBlockAddress>();
		Map<String, IndexedInstructionAddress> instructions = new HashMap<String, IndexedInstructionAddress>();
		unMarshaller.setAdapter(BlcAddAdapter.class, new IndexedBlcAddAdapter(blocks));
		unMarshaller.setAdapter(BlcAddRefAdapter.class, new IndexedBlcAddRefAdapter(blocks));
		unMarshaller.setAdapter(InsAddAdapter.class, new IndexedInsAddAdapter(instructions));
		unMarshaller.setAdapter(InsAddRefAdapter.class, new IndexedInsAddRefAdapter(instructions));
		IndexedListener listener = new IndexedListener(blocks, instructions);
		unMarshaller.setListener(listener);
		return listener.getExecutable();
	}

	static class IndexedListener extends Unmarshaller.Listener {
		Map<String, IndexedBlockAddress> blocks;
		Map<String, IndexedInstructionAddress> instructions;
		IndexedExecutable executable;

		IndexedListener(Map<String, IndexedBlockAddress> blocks, Map<String, IndexedInstructionAddress> instructions) {
			this.blocks = blocks;
			this.instructions = instructions;
			this.executable = new IndexedExecutable();
		}

		IndexedExecutable getExecutable() {
			return executable;
		}

		@Override
		public void afterUnmarshal(Object target, Object parent) {
			if (target instanceof Executable) {
				executable.blocks = new IndexedBlockAddress[blocks.size()];
				Executable exec = (Executable) target;
				for (Block block : exec.getBlocks()) {
					index(block);
				}
			}
			super.afterUnmarshal(target, parent);
		}

		public void index(Block block) {
			IndexedBlockAddress iblock = (IndexedBlockAddress) block.getBlc();
			executable.blocks[iblock.bIndex] = iblock;
			iblock.addresses = new IndexedInstructionAddress[block.getInstructions().size()];
			for (JAXBElement<? extends Instruction> i : block.getInstructions()) {
				Instruction ins = i.getValue();
				IndexedInstructionAddress iins = (IndexedInstructionAddress) ins.getIns();
				iblock.addresses[iins.iIndex] = iins;
				iins.ref = ins;
			}
			for (Block b : block.getBlocks()) {
				index(b);
			}

		}

	}

}
