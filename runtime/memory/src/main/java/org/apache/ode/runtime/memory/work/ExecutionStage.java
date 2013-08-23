package org.apache.ode.runtime.memory.work;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ode.runtime.memory.work.ExecutionUnitBuilder.Frame;
import org.apache.ode.runtime.memory.work.WorkImpl.RootFrame;
import org.apache.ode.spi.work.ExecutionUnit.Buffer;
import org.apache.ode.spi.work.ExecutionUnit.Execution;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionState;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitException;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitState;
import org.apache.ode.spi.work.ExecutionUnit.InExecution;
import org.apache.ode.spi.work.ExecutionUnit.InOutExecution;
import org.apache.ode.spi.work.ExecutionUnit.ManyToOne;
import org.apache.ode.spi.work.ExecutionUnit.OneToMany;
import org.apache.ode.spi.work.ExecutionUnit.OneToOne;
import org.apache.ode.spi.work.ExecutionUnit.OutExecution;
import org.apache.ode.spi.work.ExecutionUnit.Transform;

public abstract class ExecutionStage extends Stage implements Execution, InExecution, OutExecution, InOutExecution, Runnable {
	protected Mode mode;
	protected Frame frame;
	protected WorkImpl work;
	protected ExecutionStage seqDependency;

	protected boolean initializer = false;
	protected boolean finalizer = false;
	protected AtomicReference<ExecutionState> execState = new AtomicReference<>(ExecutionState.READY);
	protected boolean inPipesProcessed = false;
	protected boolean outPipesProcessed = false;

	//protected AtomicBoolean active = new AtomicBoolean(false);

	public ExecutionStage(Frame frame, Mode mode) {
		this.frame = frame;
		this.mode = mode;
		Frame f = frame;
		while (f != null) {
			if (f instanceof RootFrame) {
				this.work = ((RootFrame) f).work;
			}
			f = f.parentFrame;
		}
	}

	public static enum Mode {
		JOB, IN, OUT, INOUT;
	}

	@Override
	public void initializer() {
		initializer = true;
	}

	@Override
	public void finalizer() {
		finalizer = true;
	}

	@Override
	public ExecutionState state() throws ExecutionUnitException {
		return execState.get();
	}

	public abstract void exec() throws Throwable;

	@Override
	public void run() {
		ExecutionStage target = this;
		boolean finished = false;
		while (!finished) {
			ExecutionState current = target.execState.get();
			switch (current) {
			case READY:
				target.execState.compareAndSet(current, ExecutionState.BLOCK_IN);
				break;
			case COMPLETE:
			case CANCEL:
			case ABORT:
				if (work.executionCount.decrementAndGet() == 0) {
					if (work.hasCancels.get()) {
						work.stateChange(ExecutionUnitState.RUN, ExecutionUnitState.CANCEL);
					} else {
						work.stateChange(ExecutionUnitState.RUN, ExecutionUnitState.COMPLETE);
					}
				}
				finished = true;
				break;
			case BLOCK_IN:
				if (target.inPipesReady()) {
					target.execState.compareAndSet(current, ExecutionState.RUN);
				} else {
					finished = true;
				}
				break;
			case BLOCK_SEQ:
				finished = true;
				break;
			case RUN:
				try {
					if (work.execState.get() == ExecutionUnitState.READY) {
						work.execState.compareAndSet(ExecutionUnitState.READY, ExecutionUnitState.RUN);
					}
					if (!target.inPipesProcessed) {
						target.transformInPipes();
					}
					target.exec();
					if (target.frame.block != null && target.frame.block.availablePermits() == 0) {
						target.execState.compareAndSet(ExecutionState.RUN, ExecutionState.BLOCK_RUN);
						continue;
					}
					ExecutionStage es = null;
					if (!target.outPipesProcessed) {
						es = target.transformOutAndSteal();
					}
					if (es != null && es.checkInDependency(es.seqDependency)) {
						ExecutionStage currentExec = target;
						target = es;
						currentExec.execState.set(ExecutionState.RUN);
						currentExec.execState.compareAndSet(ExecutionState.RUN, ExecutionState.BLOCK_OUT);
					} else {
						target.execState.compareAndSet(ExecutionState.RUN, ExecutionState.BLOCK_OUT);
					}
				} catch (Throwable t) {
					Frame f = frame;
					while (f != null) {
						for (Map.Entry<Class<? extends Throwable>, ? extends HandlerExecution> e : f.handlers.entrySet()) {
							if (e.getKey().isAssignableFrom(t.getClass())) {
								e.getValue().exec();
							}
						}
						f = f.parentFrame;
					}
					target.execState.compareAndSet(ExecutionState.RUN, ExecutionState.ABORT);
				}
				break;

			case BLOCK_RUN:
				finished = true;
				break;

			case BLOCK_OUT:
				if (target.outPipesFinished()) {
					target.execState.compareAndSet(ExecutionState.BLOCK_OUT, ExecutionState.COMPLETE);
				} else {
					finished = true;
				}
				break;

			default:
				break;

			}
		}

	}

	boolean inPipesReady() {
		return inPipesReady(null);
	}

	boolean inPipesReady(ExecutionStage except) {
		boolean ready = true;

		if (inPipes != null) {
			for (Pipe p : inPipes) {
				if (p.from instanceof ExecutionStage && p.from != except) {
					ready &= checkInDependency((ExecutionStage) p.from);
				}

				if (p.froms != null) {
					for (Stage s : (LinkedList<Stage>) p.froms) {
						if (s instanceof ExecutionStage && s != except) {
							ready &= checkInDependency((ExecutionStage) s);
						}
					}
				}
			}
		}
		return ready;
	}

	boolean outPipesFinished() {
		boolean finished = true;

		if (outPipes != null) {
			for (Pipe p : inPipes) {
				if (p.to instanceof ExecutionStage) {
					finished &= checkOutDependency((ExecutionStage) p.to);
				}

				if (p.tos != null) {
					for (Stage s : (LinkedList<Stage>) p.tos) {
						if (s instanceof ExecutionStage) {
							finished &= checkOutDependency((ExecutionStage) s);
						}
					}
				}
			}
		}
		return finished;
	}

	public boolean checkInDependency(ExecutionStage dependency) {
		if (dependency != null) {
			switch (dependency.execState.get()) {
			case COMPLETE:
				return true;
			case ABORT:
			case CANCEL:
				ExecutionState current = execState.get();
				if (current == ExecutionState.READY || current == ExecutionState.BLOCK_IN) {
					execState.compareAndSet(current, ExecutionState.CANCEL);
				}
				return false;
			default:
				return false;
			}
		}
		return true;
	}

	public boolean checkOutDependency(ExecutionStage dependency) {

		if (dependency != null) {
			switch (dependency.execState.get()) {
			case ABORT:
			case CANCEL:
			case COMPLETE:
				return true;
			default:
				return false;
			}
		}
		return true;
	}

	protected void transformInPipes() throws StageException {
		inPipesProcessed = true;
		if (inPipes != null) {
			for (Pipe p : inPipes) {
				if (p.from instanceof BufferStage) {
					BufferStage bs = (BufferStage) p.from;
					if (!bs.read) {
						bs.read();
					}
				} else if (p.froms != null) {
					for (Stage s : (List<Stage>) p.froms) {
						if (s instanceof BufferStage) {
							BufferStage bs = (BufferStage) s;
							if (!bs.read) {
								bs.read();
							}
						}
					}
				} else if (p.from != null && p.to != null) {//one to one
					if (p.transform != null) {
						if (p.transform instanceof OneToOne) {
							((OneToOne) p.transform).transform(p.from.value, p.to.value);
						} else {
							throw new StageException(String.format("Expected OneToOne transform but found type %s", p.transform.getClass()));
						}
					} else {
						for (int i = 0; i < p.from.value.length && i < p.to.value.length; i++) {
							p.to.value[i] = p.from.value[i];
						}
					}
				} else if (p.from != null && p.tos != null) {//one to many
					Object[][] values = new Object[p.tos.size()][];
					int i = 0;
					for (Stage s : (List<Stage>) p.tos) {
						values[i++] = s.value;
					}
					if (p.transform != null) {
						if (p.transform instanceof OneToMany) {
							((OneToMany) p.transform).transform(p.from.value, values);
						} else {
							throw new StageException(String.format("Expected OneToMany transform but found type %s", p.transform.getClass()));
						}
					} else {
						for (Object[] val : values) {
							for (int j = 0; j < p.from.value.length && j < val.length; j++) {
								val[j] = p.from.value[j];
							}
						}
					}
				} else if (p.froms != null && p.to != null) {//many to one
					Object[][] values = new Object[p.froms.size()][];
					int i = 0;
					for (Stage s : (List<Stage>) p.froms) {
						values[i++] = s.value;
					}
					if (p.transform != null) {
						if (p.transform instanceof ManyToOne) {

							((ManyToOne) p.transform).transform(values, p.to.value);
						} else {
							throw new StageException(String.format("Expected ManyToOne transform but found type %s", p.transform.getClass()));
						}
					} else {
						int k = 0;
						for (Object[] val : values) {
							for (int j = 0; j < p.to.value.length && j < val.length; j++) {
								if (p.to.value[j] instanceof Object[]) {
									((Object[]) p.to.value[j])[k++] = val[j];
								}
								if (p.to.value[j] instanceof Collection) {
									((Collection) p.to.value[j]).add(val[j]);
								} else {
									p.to.value[j] = val[j];
								}
							}
						}
					}
				}
			}
		}
	}

	protected void transformOutPipes() throws StageException {//only write out buffers (sink nodes); executions will always transform their input first
		outPipesProcessed = true;
		for (Pipe p : outPipes) {
			if (p.to instanceof BufferStage) {
				BufferStage bs = (BufferStage) p.to;
				if (!bs.write) {
					bs.write();
				}
			}
			if (p.tos != null) {
				for (Stage s : (List<Stage>) p.tos) {
					if (s instanceof BufferStage) {
						BufferStage bs = (BufferStage) s;
						if (!bs.write) {
							bs.write();
						}
					}
				}
			}
		}
	}

	protected ExecutionStage transformOutAndSteal() throws StageException {//only write out buffers (sink nodes); executions will always transform their input first
		ExecutionStage canidate = null;
		if (outPipes != null) {
			for (Pipe p : outPipes) {
				if (p.to instanceof BufferStage) {
					BufferStage bs = (BufferStage) p.to;
					if (!bs.write) {
						bs.write();
					}
				} else if (p.to instanceof ExecutionStage) {
					ExecutionStage es = (ExecutionStage) p.to;
					if (canidate == null && es.inPipesReady(this)) {
						canidate = es;
					}
				}
				if (p.tos != null) {
					for (Stage s : (List<Stage>) p.tos) {
						if (s instanceof BufferStage) {
							BufferStage bs = (BufferStage) s;
							if (!bs.write) {
								bs.write();
							}
						} else if (s instanceof ExecutionStage) {
							ExecutionStage es = (ExecutionStage) s;
							if (canidate == null && es.inPipesReady(this)) {
								canidate = es;
							}
						}
					}
				}
			}
		}
		if (canidate == null && seqDependency != null && seqDependency.inPipesReady()) {
			canidate = seqDependency;
		}
		return canidate;
	}

	@Override
	public <I extends Buffer> OutExecution pipeOut(I buffer, Transform... transforms) throws ExecutionUnitException {
		BufferStage bs = frame.buffers.get(buffer);
		if (bs == null) {
			throw new ExecutionUnitException("unknown buffer");
		}
		this.addOutPipe(bs, transforms);
		//} else {
		//	this.outPipes.add(new OneToOneBufferPipe(bs, this));
		//}
		return this;
	}

	@Override
	public InExecution pipeOut(InExecution execUnit, Transform... transforms) throws ExecutionUnitException {
		ExecutionStage inExec = (ExecutionStage) execUnit;
		this.addOutPipe(inExec, transforms);
		return this;
	}

	@Override
	public InOutExecution pipeOut(InOutExecution execUnit, Transform... transforms) throws ExecutionUnitException {
		//TODO
		return execUnit;
	}

	@Override
	public <O extends Buffer> InExecution pipeIn(O buffer, Transform... transforms) throws ExecutionUnitException {
		BufferStage bs = frame.buffers.get(buffer);
		if (bs == null) {
			throw new ExecutionUnitException("unknown buffer");
		}
		this.addInPipe(bs, transforms);
		return this;
	}

	@Override
	public OutExecution pipeIn(OutExecution execUnit, Transform... transforms) {
		//TODO
		return execUnit;
	}

	@Override
	public InOutExecution pipeIn(InOutExecution execUnit, Transform... transforms) throws ExecutionUnitException {
		//TODO
		return execUnit;
	}

}
