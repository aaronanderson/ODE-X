package org.apache.ode.runtime.core.work;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitException;
import org.apache.ode.spi.work.ExecutionUnit.ManyToOne;
import org.apache.ode.spi.work.ExecutionUnit.OneToMany;
import org.apache.ode.spi.work.ExecutionUnit.OneToOne;
import org.apache.ode.spi.work.ExecutionUnit.Transform;

public abstract class Stage {

	protected HashSet<Pipe<?, ?>> inPipes = null;
	protected HashSet<Pipe<?, ?>> outPipes = null;

	protected AtomicInteger inCount;
	protected AtomicInteger outCount;
	protected AtomicReference<IOState> ioState = new AtomicReference<>(IOState.BLOCK_INPUT);

	//made final for performance reasons and to avoid NPEs
	final protected Object[] input;
	final protected Object[] output;

	public Stage(Object[] input, Object[] output) {
		this.input = input;
		this.output = output;
	}

	public static enum IOState {
		BLOCK_INPUT, READY, PRE_INPUT, TRANSFORM_INPUT, POST_INPUT, RUN, PRE_OUTPUT, TRANSFORM_OUTPUT, POST_OUTPUT, BLOCK_OUTPUT, COMPLETE;
	}

	protected void preInput() throws StageException {

	}

	protected void postInput() throws StageException {

	}

	protected void preOutput() throws StageException {

	}

	protected void postOutput() throws StageException {

	}

	public boolean inPipesReady() {
		IOState current = ioState.get();
		if (current.ordinal() > IOState.BLOCK_INPUT.ordinal()) {
			return true;
		}
		boolean ready = true;

		if (inPipes != null) {
			for (Pipe p : inPipes) {
				if (p.from != null) {
					ready &= p.from.ioState.get().ordinal() > IOState.BLOCK_INPUT.ordinal();
				} else {
					for (Stage s : (Set<Stage>) p.froms.get()) {
						ready &= s.ioState.get().ordinal() > IOState.BLOCK_INPUT.ordinal();
					}
				}
			}
		}
		if (ready && current == IOState.BLOCK_INPUT) {
			ioState.compareAndSet(current, IOState.READY);
		}
		return ready;
	}

	public void addInPipe(Stage stage, Transform[] transforms) {
		if (inPipes == null) {
			inPipes = new HashSet<>();
			inCount = new AtomicInteger();

		}
		if (stage.outPipes == null) {
			stage.outPipes = new HashSet<>();
			stage.outCount = new AtomicInteger();
		}
		Pipe appendFrom = appendFromPipe(inPipes, stage, transforms);
		Pipe appendTo = stage.appendToPipe(stage.outPipes, this, transforms);

		if (appendFrom != null && appendTo == null) {
			inPipes.add(appendFrom);
		} else if (appendFrom == null && appendTo != null) {
			stage.outPipes.add(appendTo);
		} else {
			Pipe p2 = new Pipe(stage, this);
			inPipes.add(p2);
			stage.outPipes.add(p2);
		}
	}

	public Pipe appendFromPipe(HashSet<Pipe<?, ?>> pipes, Stage stage, Transform[] transforms) {
		for (Pipe p : pipes) {
			if (transforms.length > 0) {
				for (Transform t : transforms) {
					if (p.transform == t) {
						appendFrom(p, stage);
						return p;
					} else {
						Pipe p2 = new Pipe(stage, this, t);
						appendFrom(p2, stage);
						return p2;
					}
				}
			} else if (isFromAppendable(p, stage)) {
				appendFrom(p, stage);
				return p;
			}
		}
		return null;
	}

	public boolean outPipesFinished() {
		IOState current = ioState.get();

		if (current == IOState.COMPLETE) {
			return true;
		}

		boolean finished = true;

		if (outPipes != null) {
			for (Pipe p : outPipes) {
				if (p.to != null) {
					finished &= p.to.ioState.get().ordinal() >= IOState.RUN.ordinal();
				} else {
					for (Stage s : (Set<Stage>) p.tos.get()) {
						finished &= s.ioState.get().ordinal() >= IOState.RUN.ordinal();
					}
				}
			}
		}
		if (finished && current == IOState.BLOCK_OUTPUT) {
			ioState.compareAndSet(current, IOState.COMPLETE);
		}
		return finished;
	}

	public void appendFrom(Pipe pipe, Stage stage) {
		if (pipe.froms != null) {
			((Set) pipe.froms.get()).add(stage);
		} else if (pipe.from != null) {
			pipe.froms = new AtomicReference<>(new HashSet<>());
			((Set) pipe.froms.get()).add(pipe.from);
			((Set) pipe.froms.get()).add(stage);
			pipe.fromsCompleted = new AtomicInteger();
			pipe.from = null;
		} else {
			pipe.from = stage;
		}
	}

	public boolean isFromAppendable(Pipe pipe, Stage stage) {
		if (pipe.from != null && pipe.from.getClass().isAssignableFrom(stage.getClass())) {
			return true;
		}
		if (pipe.froms != null && !((Set) pipe.froms.get()).isEmpty() && ((Set) pipe.froms.get()).iterator().next().getClass().isAssignableFrom(stage.getClass())) {
			return true;
		}

		return false;

	}

	public void addOutPipe(Stage stage, Transform[] transforms) {
		if (stage.inPipes == null) {
			stage.inPipes = new HashSet<>();
			stage.inCount = new AtomicInteger();
		}
		if (outPipes == null) {
			outPipes = new HashSet<>();
			outCount = new AtomicInteger();
		}
		Pipe appendTo = appendToPipe(outPipes, stage, transforms);
		Pipe appendFrom = stage.appendFromPipe(stage.inPipes, this, transforms);

		if (appendTo != null && appendFrom == null) {
			stage.inPipes.add(appendTo);
		} else if (appendTo == null && appendFrom != null) {
			outPipes.add(appendFrom);
		} else {
			Pipe p2 = new Pipe(this, stage);
			outPipes.add(p2);
			stage.inPipes.add(p2);

		}

	}

	public Pipe appendToPipe(HashSet<Pipe<?, ?>> pipes, Stage stage, Transform[] transforms) {

		for (Pipe p : pipes) {
			if (transforms.length > 0) {
				for (Transform t : transforms) {
					if (p.transform == t) {
						appendTo(p, stage);
						return p;
					} else {
						Pipe p2 = new Pipe(stage, this, t);
						appendTo(p2, stage);
						return p2;
					}
				}
			} else if (isToAppendable(p, stage)) {
				appendTo(p, stage);
				return p;
			}
		}
		return null;
	}

	public void appendTo(Pipe pipe, Stage stage) {
		if (pipe.tos != null) {
			((Set) pipe.tos.get()).add(stage);
		} else if (pipe.to != null) {
			pipe.tos = new AtomicReference<>(new HashSet<>());
			((Set) pipe.tos.get()).add(pipe.to);
			((Set) pipe.tos.get()).add(stage);
			pipe.to = null;
		} else {
			pipe.to = stage;
		}
	}

	public boolean isToAppendable(Pipe pipe, Stage stage) {
		if (pipe.to != null && pipe.to.getClass().isAssignableFrom(stage.getClass())) {
			return true;
		}
		if (pipe.tos != null && !((Set) pipe.tos.get()).isEmpty() && ((Set) pipe.tos.get()).iterator().next().getClass().isAssignableFrom(stage.getClass())) {
			return true;
		}

		return false;

	}

	protected static void transformOutPipe(Pipe p) throws StageException {
		if (p.processed.compareAndSet(false, true)) {
			preTransformOutPipe(p);
			transformPipe(p);
			postTransformOutPipe(p);
		}
	}

	protected static void transformInPipe(Pipe p) throws StageException {
		if (p.processed.compareAndSet(false, true)) {
			preTransformInPipe(p);
			transformPipe(p);
			postTransformInPipe(p);
		}
	}

	protected static void transformPipe(Pipe p) throws StageException {
		if (p.from != null && p.to != null) {//one to one
			transformOneToOnePipe(p);
		} else if (p.from != null && p.tos != null) {//one to many, perform transform once. No delay is necessary.
			transformOneToManyPipe(p);
		} else if (p.froms != null && p.to != null) {//many to one, perform transform once. Only when last execution completes perform transform						
			transformManyToOnePipe(p);
		} else if (p.froms != null && p.tos != null) {//many to many

		}

	}

	protected static void doPreOutput(Stage s) throws StageException {
		poll: while (true) {
			IOState current = s.ioState.get();
			switch (current) {
			case READY:
			case RUN:
				if (s.ioState.compareAndSet(current, IOState.PRE_OUTPUT)) {
					s.preOutput();
					s.ioState.set(IOState.TRANSFORM_OUTPUT);
				}
			case PRE_OUTPUT:
				break;
			default:
				break poll;
			}
		}
	}

	protected static void doPreInput(Stage s) throws StageException {
		poll: while (true) {
			IOState current = s.ioState.get();
			switch (current) {
			case READY:
				if (s.ioState.compareAndSet(current, IOState.PRE_INPUT)) {
					s.preInput();
					s.ioState.set(IOState.TRANSFORM_INPUT);
				}
			case PRE_INPUT:
				break;
			default:
				break poll;
			}
		}
	}

	protected static void doPostOutput(Stage s) throws StageException {
		poll: while (true) {
			IOState current = s.ioState.get();
			switch (current) {
			case TRANSFORM_OUTPUT:
				if (s.ioState.compareAndSet(current, IOState.POST_OUTPUT)) {
					s.postOutput();
					s.ioState.set(IOState.BLOCK_OUTPUT);
				}
			case POST_OUTPUT:
				break;
			default:
				break poll;
			}
		}
	}

	protected static void doPostInput(Stage s) throws StageException {
		poll: while (true) {
			IOState current = s.ioState.get();
			switch (current) {
			case TRANSFORM_INPUT:
				if (s.ioState.compareAndSet(current, IOState.POST_INPUT)) {
					s.postInput();
					s.ioState.set(IOState.RUN);
				}
			case POST_INPUT:
				break;
			default:
				break poll;
			}
		}
	}

	protected static void preTransformOutPipe(Pipe p) throws StageException {
		if (p.from != null) {
			if (p.from.outCount.get() == 0) {
				doPreOutput(p.from);
			}
		} else if (p.froms != null) {
			for (Stage s : (Set<Stage>) p.froms.get()) {
				if (s.outCount.get() == 0) {
					doPreOutput(s);
				}
			}
		}
		if (p.to != null) {
			if (p.to.inCount.get() == 0) {
				doPreInput(p.to);
			}
		} else if (p.tos != null) {
			for (Stage s : (Set<Stage>) p.tos.get()) {
				if (s.inCount.get() == 0) {
					doPreInput(s);
				}
			}
		}

	}

protected static void postTransformOutPipe(Pipe p) throws StageException {
		if (p.to != null) {
			if (p.to.inCount.get() == 1) {
				doPostInput(p.to);
			}
		} else if (p.tos != null) {
			for (Stage s : (Set<Stage>) p.tos.get()) {
				if (s.inCount.get() == s.inPipes.size()) {
					doPostInput(s);
				}
			}
		}
		if (p.from != null) {
			if (p.from.outCount.get() == 1) {
				doPostOutput(p.from);
			}
		} else if (p.froms != null) {
			for (Stage s : (Set<Stage>) p.froms.get()) {
				if (s.outCount.get() == s.outPipes.size()) {
					doPostOutput(s);
				}
			}
		}

	}

	protected static void preTransformInPipe(Pipe p) throws StageException {
		if (p.to != null) {
			doPreInput(p.to);
		} else if (p.tos != null) {
			for (Stage s : (Set<Stage>) p.tos.get()) {
				doPreInput(s);
			}
		}
		if (p.from != null) {
			doPreOutput(p.from);
		} else if (p.froms != null) {
			for (Stage s : (Set<Stage>) p.froms.get()) {
				doPreOutput(s);
			}
		}

	}

	protected static void postTransformInPipe(Pipe p) throws StageException {
		if (p.from != null) {
			doPostOutput(p.from);
		} else if (p.froms != null) {
			for (Stage s : (Set<Stage>) p.froms.get()) {
				doPostOutput(s);
			}
		}
		if (p.to != null) {
			doPostInput(p.to);
		} else if (p.tos != null) {
			for (Stage s : (Set<Stage>) p.tos.get()) {
				doPostInput(s);
			}
		}
	}

	protected static void transformOneToOnePipe(Pipe p) throws StageException {
		p.from.outCount.incrementAndGet();
		p.to.inCount.incrementAndGet();
		if (p.transform != null) {
			if (p.transform instanceof OneToOne) {
				((OneToOne) p.transform).transform(p.from.output, p.to.input);
			} else {
				throw new StageException(String.format("Expected OneToOne transform but found type %s", p.transform.getClass()));
			}
		} else {
			for (int i = 0; i < p.from.output.length && i < p.to.input.length; i++) {
				p.to.input[i] = p.from.output[i];
			}
		}

	}

	protected static void transformOneToManyPipe(Pipe p) throws StageException {
		Object[][] inputs = new Object[((Set) p.tos.get()).size()][];
		int i = 0;
		p.from.outCount.incrementAndGet();
		for (Stage s : (Set<Stage>) p.tos.get()) {
			s.inCount.incrementAndGet();
			inputs[i++] = s.input;
		}
		if (p.transform != null) {
			if (p.transform instanceof OneToMany) {
				((OneToMany) p.transform).transform(p.from.output, inputs);
			} else {
				throw new StageException(String.format("Expected OneToMany transform but found type %s", p.transform.getClass()));
			}
		} else {
			for (Object[] val : inputs) {
				for (int j = 0; j < p.from.output.length && j < val.length; j++) {
					val[j] = p.from.output[j];
				}
			}
		}
	}

	protected static void transformManyToOnePipe(Pipe p) throws StageException {
		Object[][] outputs = new Object[((Set) p.froms.get()).size()][];
		int i = 0;
		p.to.inCount.incrementAndGet();
		for (Stage s : (Set<Stage>) p.froms.get()) {
			s.outCount.incrementAndGet();
			outputs[i++] = s.output;
		}
		if (p.transform != null) {
			if (p.transform instanceof ManyToOne) {

				((ManyToOne) p.transform).transform(outputs, p.to.input);
			} else {
				throw new StageException(String.format("Expected ManyToOne transform but found type %s", p.transform.getClass()));
			}
		} else {
			int k = 0;
			for (Object[] val : outputs) {
				for (int j = 0; j < p.to.input.length && j < val.length; j++) {
					if (p.to.input[j] instanceof Object[]) {
						((Object[]) p.to.input[j])[k++] = val[j];
					}
					if (p.to.input[j] instanceof Collection) {
						((Collection) p.to.input[j]).add(val[j]);
					} else {
						p.to.input[j] = val[j];
					}
				}
			}
		}
	}

	public static class Pipe<F extends Stage, T extends Stage> {
		final AtomicBoolean processed = new AtomicBoolean(false);

		F from;
		AtomicReference<Set<F>> froms;
		AtomicInteger fromsCompleted;
		T to;
		AtomicReference<Set<T>> tos;
		AtomicInteger tosCompleted;

		Transform transform;

		public Pipe(F from, T to) {
			this.from = from;
			this.to = to;
		}

		public Pipe(F from, T to, Transform transform) {
			this.from = from;
			this.to = to;
			this.transform = transform;
		}

	}

	public static class StageException extends ExecutionUnitException {
		StageException(String msg) {
			super(msg);
		}

		StageException(Throwable t) {
			super(t);
		}
	}
}
