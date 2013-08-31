package org.apache.ode.runtime.memory.work;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.ode.runtime.memory.work.ExecutionUnitBuilder.Frame;
import org.apache.ode.runtime.memory.work.Stage.Pipe;
import org.apache.ode.runtime.memory.work.Stage.StageException;
import org.apache.ode.runtime.memory.work.WorkImpl.RootFrame;
import org.apache.ode.spi.work.ExecutionUnit.Execution;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionState;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitException;
import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitState;
import org.apache.ode.spi.work.ExecutionUnit.InBuffer;
import org.apache.ode.spi.work.ExecutionUnit.InExecution;
import org.apache.ode.spi.work.ExecutionUnit.InOutExecution;
import org.apache.ode.spi.work.ExecutionUnit.ManyToOne;
import org.apache.ode.spi.work.ExecutionUnit.OneToMany;
import org.apache.ode.spi.work.ExecutionUnit.OneToOne;
import org.apache.ode.spi.work.ExecutionUnit.OutBuffer;
import org.apache.ode.spi.work.ExecutionUnit.OutExecution;
import org.apache.ode.spi.work.ExecutionUnit.Transform;

public abstract class ExecutionStage extends Stage implements Execution, InExecution, OutExecution, InOutExecution, Runnable {

	private static final Logger log = Logger.getLogger(ExecutionStage.class.getName());

	protected Mode mode;
	protected Frame frame;
	protected WorkImpl work;
	protected ExecutionStage supDependency;
	protected LinkedList<ExecutionStage> infDependency;

	volatile protected boolean inPipesProcessed = false;
	volatile protected boolean outPipesProcessed = false;

	protected boolean initializer = false;
	protected boolean finalizer = false;
	protected AtomicReference<ExecutionState> execState = new AtomicReference<>(ExecutionState.READY);

	//protected AtomicBoolean active = new AtomicBoolean(false);

	public ExecutionStage(Object[] input, Object[] output, Frame frame, Mode mode) {
		super(input, output);
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

	/*@Override
	public void initializer() {
		initializer = true;
	}

	@Override
	public void finalizer() {
		finalizer = true;
	}*/

	@Override
	public ExecutionState state() throws ExecutionUnitException {
		return execState.get();
	}

	public abstract void exec() throws Throwable;

	@Override
	public void run() {
		ExecutionStage target = this;
		ExecutionStage next = null;
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
				if (next != null) {
					target = next;
					next = null;
				} else {
					finished = true;
				}
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

					if (!target.outPipesProcessed) {
						ExecutionStageResult res = target.transformOutAndSteal();
						if (res.isFinished) {
							if (res.canidate != null) {
								next = res.canidate;
								next.execState.set(ExecutionState.RUN);
							}
							target.execState.compareAndSet(ExecutionState.RUN, ExecutionState.COMPLETE);
						} else {
							target.execState.compareAndSet(ExecutionState.RUN, ExecutionState.BLOCK_OUT);
							work.signalExecWaiting();
						}
					} else {//probably should never reach this state
						target.execState.compareAndSet(ExecutionState.RUN, ExecutionState.BLOCK_OUT);
					}

				} catch (Throwable t) {
					Frame f = frame;
					boolean handled = false;
					while (f != null) {
						for (Map.Entry<Class<? extends Throwable>, ? extends HandlerExecution> e : f.handlers.entrySet()) {
							if (e.getKey().isAssignableFrom(t.getClass())) {
								e.getValue().exec();
								handled = true;
							}
						}
						f = f.parentFrame;
					}
					if (!handled) {
						log.log(Level.SEVERE, "", t);
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
					for (Stage s : (Set<Stage>) p.froms) {
						if (s instanceof ExecutionStage && s != except) {
							ready &= checkInDependency((ExecutionStage) s);
						}
					}
				}
			}
		}
		return ready;
	}

	boolean superiorDependencyReady() {
		if (supDependency != null) {
			switch (supDependency.execState.get()) {
			case COMPLETE:
			case ABORT:
			case CANCEL:
				return true;
			default:
				return false;
			}
		}
		return true;
	}

	boolean outPipesFinished() {
		boolean finished = true;

		if (outPipes != null) {
			for (Pipe p : outPipes) {
				if (p.to instanceof ExecutionStage) {
					finished &= checkOutDependency((ExecutionStage) p.to);
				}

				if (p.tos != null) {
					for (Stage s : (Set<Stage>) p.tos) {
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
			case BLOCK_OUT:
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

	protected static class ExecutionStageResult {
		boolean isFinished = true;
		ExecutionStage canidate = null;
	}

	protected void transformInPipes() throws StageException {
		inPipesProcessed = true;
		if (inPipes != null) {

			for (Pipe p : inPipes) {

				/*if (p.from instanceof BufferStage) { //read in buffers
					BufferStage bs = (BufferStage) p.from;
					if (!bs.read) {
						bs.read();
					}
				} else if (p.froms != null) {
					for (Stage s : (Set<Stage>) p.froms) {
						if (s instanceof BufferStage) {
							BufferStage bs = (BufferStage) s;
							if (!bs.read) {
								bs.read();
							}
						}
					}
				}*/
				//for ManyToOne we only want to execute the transform once all executions have completed
				if (p.fromsCompleted != null && p.fromsCompleted.incrementAndGet() != p.froms.size()) {
					continue;
				}
				transformInPipe(p);

			}
		}
	}

	protected void transformOutPipes() throws StageException {
		outPipesProcessed = true;
		if (outPipes != null) {
			for (Pipe p : outPipes) {

				//for OneToMany we only want to execute the transform once all executions have completed
				if (p.tosCompleted != null && p.tosCompleted.incrementAndGet() != p.tos.size()) {
					continue;
				}

				transformOutPipe(p);
				/*
								if (p.to instanceof BufferStage) {
									BufferStage bs = (BufferStage) p.to;
									if (!bs.write) {
										bs.write();
									}
								}
								if (p.tos != null) {
									for (Stage s : (Set<Stage>) p.tos) {
										if (s instanceof BufferStage) {
											BufferStage bs = (BufferStage) s;
											if (!bs.write) {
												bs.write();
											}
										}
									}
								}*/
			}
		}
	}

	protected ExecutionStageResult transformOutAndSteal() throws StageException {
		ExecutionStageResult res = new ExecutionStageResult();
		if (outPipes != null) {
			for (Pipe p : outPipes) {

				if (p.tosCompleted != null && p.tosCompleted.incrementAndGet() != p.tos.size() - 1) {
					res.isFinished = false;
					break;
				}

				transformOutPipe(p);

				if (p.to instanceof ExecutionStage) {
					ExecutionStage es = (ExecutionStage) p.to;
					if (res.canidate == null && es.inPipesReady(this)) {
						res.canidate = es;
					} else {
						res.isFinished = false;
					}
				}
				if (p.tos != null) {
					for (Stage s : (Set<Stage>) p.tos) {
						if (s instanceof ExecutionStage) {
							ExecutionStage es = (ExecutionStage) s;
							if (res.canidate == null && es.inPipesReady(this)) {
								res.canidate = es;
							} else {
								res.isFinished = false;
							}
						}
					}
				}
			}
		}
		if (infDependency != null) {
			ListIterator<ExecutionStage> i = infDependency.listIterator();
			while (i.hasNext()) {
				ExecutionStage ex = i.next();
				ExecutionState current = ex.execState.get();
				if (current == ExecutionState.READY || current == ExecutionState.BLOCK_SEQ) {
					if (res.canidate == null && ex.inPipesReady()) {
						res.canidate = ex;
						if (i.hasNext()) {
							res.isFinished = false;
						}
						break;
					} else {
						res.isFinished = false;
						break;
					}
				}
			}
		}

		return res;
	}

	@Override
	public <I extends InBuffer> OutExecution pipeOut(I buffer, Transform... transforms) throws ExecutionUnitException {
		BufferStage bs = frame.outBuffers.get(buffer);
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
		ExecutionStage inOutExec = (ExecutionStage) execUnit;
		this.addOutPipe(inOutExec, transforms);
		return execUnit;
	}

	@Override
	public <O extends OutBuffer> InExecution pipeIn(O buffer, Transform... transforms) throws ExecutionUnitException {
		BufferStage bs = frame.inBuffers.get(buffer);
		if (bs == null) {
			throw new ExecutionUnitException("unknown buffer");
		}
		this.addInPipe(bs, transforms);
		return this;
	}

	@Override
	public OutExecution pipeIn(OutExecution execUnit, Transform... transforms) {
		ExecutionStage outExec = (ExecutionStage) execUnit;
		this.addInPipe(outExec, transforms);
		return execUnit;
	}

	@Override
	public InOutExecution pipeIn(InOutExecution execUnit, Transform... transforms) throws ExecutionUnitException {
		ExecutionStage inOutExec = (ExecutionStage) execUnit;
		this.addInPipe(inOutExec, transforms);
		return execUnit;
	}

}
