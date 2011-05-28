/*
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
package org.apache.ode.bpel.compiler;

import org.apache.ode.bpel.exec.xml.ObjectFactory;
import org.apache.ode.bpel.exec.xml.Process;
import org.apache.ode.bpel.plugin.BPELPlugin;
import org.apache.ode.spi.compiler.CompilerContext;
import org.apache.ode.spi.compiler.CompilerPass;
import org.apache.ode.spi.compiler.CompilerPhase;
import org.apache.ode.spi.compiler.Source;
import org.apache.ode.spi.exec.xml.Block;
import org.apache.ode.spi.exec.xml.InstructionSet;



public class DiscoveryPass implements CompilerPass{
	
	@Override
	public void compile(CompilerPhase phase, CompilerContext ctx, Source artifact){
		BPELContext bctx = ctx.getSubContext(BPELContext.ID);
		bctx.getClass();
		ObjectFactory bpelFactory = new ObjectFactory();
		switch (phase){
		case EMIT:
			InstructionSet set = ctx.executable().getInstructionSet();
			if (set == null){
				set = new InstructionSet();
				ctx.executable().setInstructionSet(set);
			}
			set.getInstructionSet().add(BPELPlugin.BPEL_INSTRUCTION_SET);
			Block b = new Block();
			ctx.executable().getBlock().add(b );
			b.getBody().add(bpelFactory.createProcess(new Process()));
			break;
		}
	}

}
