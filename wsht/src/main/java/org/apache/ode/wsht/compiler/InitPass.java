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
package org.apache.ode.wsht.compiler;

import org.apache.ode.wsht.spi.WSHTContext;
import org.apache.ode.spi.compiler.CompilerPass;
import org.apache.ode.spi.compiler.ExecCompilerContext;



public class InitPass implements CompilerPass<ExecCompilerContext>{
	
	@Override
	public void compile(ExecCompilerContext ctx){
		WSHTContext wctx = ctx.<WSHTContext>subContext(WSHTContext.ID, WSHTContext.class);
		switch (ctx.phase()){
		case INITIALIZE:
			
			break;
		}
	}

}
