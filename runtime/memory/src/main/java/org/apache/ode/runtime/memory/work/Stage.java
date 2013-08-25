package org.apache.ode.runtime.memory.work;

import java.util.LinkedList;

import org.apache.ode.spi.work.ExecutionUnit.ExecutionUnitException;
import org.apache.ode.spi.work.ExecutionUnit.Transform;

public abstract class Stage {

	protected LinkedList<Pipe<?, ?>> inPipes = null;
	protected LinkedList<Pipe<?, ?>> outPipes = null;
	//made final for performance reasons and to avoid NPEs
	final protected Object[] input;
	final protected Object[] output;

	public Stage(Object[] input, Object[] output) {
		this.input = input;
		this.output = output;
	}

	public void addInPipe(Stage stage, Transform[] transforms) {
		if (inPipes == null) {
			inPipes = new LinkedList<>();
		}
		if (stage.outPipes == null) {
			stage.outPipes = new LinkedList<>();
		}
		for (Pipe p : inPipes) {
			if (transforms != null) {
				for (Transform t : transforms) {
					if (p.transform == t) {
						appendFrom(p, stage);
					} else {
						Pipe p2 = new Pipe(stage, this, t);
						appendFrom(p2, stage);
						inPipes.add(p2);
						stage.outPipes.add(p2);
					}
				}
				return;
			} else if (isFromAppendable(p, stage)) {
				appendFrom(p, stage);
				return;
			}
		}
		Pipe p2 = new Pipe(stage, this);
		inPipes.add(p2);
		stage.outPipes.add(p2);

	}

	public void appendFrom(Pipe pipe, Stage stage) {
		if (pipe.froms != null) {
			pipe.froms.add(stage);
		} else if (pipe.from != null) {
			pipe.froms = new LinkedList<>();
			pipe.froms.add(pipe.from);
			pipe.from = null;
		} else {
			pipe.from = stage;
		}
	}

	public boolean isFromAppendable(Pipe pipe, Stage stage) {
		if (pipe.from != null && pipe.from.getClass().isAssignableFrom(stage.getClass())) {
			return true;
		}
		if (pipe.froms != null && !pipe.froms.isEmpty() && pipe.froms.getFirst().getClass().isAssignableFrom(stage.getClass())) {
			return true;
		}

		return false;

	}

	public void addOutPipe(Stage stage, Transform[] transforms) {
		if (stage.inPipes == null) {
			stage.inPipes = new LinkedList<>();
		}
		if (outPipes == null) {
			outPipes = new LinkedList<>();
		}
		for (Pipe p : outPipes) {
			if (transforms != null) {
				for (Transform t : transforms) {
					if (p.transform == t) {
						appendFrom(p, stage);
					} else {
						Pipe p2 = new Pipe(stage, this, t);
						appendFrom(p2, stage);
						outPipes.add(p2);
						stage.inPipes.add(p2);
					}
				}
				return;
			} else if (isFromAppendable(p, stage)) {
				appendFrom(p, stage);
				return;
			}
		}
		Pipe p2 = new Pipe(this, stage);
		outPipes.add(p2);
		stage.inPipes.add(p2);

	}

	public void appendTo(Pipe pipe, Stage stage) {
		if (pipe.tos != null) {
			pipe.tos.add(stage);
		} else if (pipe.to != null) {
			pipe.tos = new LinkedList<>();
			pipe.tos.add(pipe.to);
			pipe.to = null;
		} else {
			pipe.to = stage;
		}
	}

	public boolean isToAppendable(Pipe pipe, Stage stage) {
		if (pipe.to != null && pipe.to.getClass().isAssignableFrom(stage.getClass())) {
			return true;
		}
		if (pipe.tos != null && !pipe.tos.isEmpty() && pipe.tos.getFirst().getClass().isAssignableFrom(stage.getClass())) {
			return true;
		}

		return false;

	}

	public static class Pipe<F extends Stage, T extends Stage> {
		F from;
		LinkedList<F> froms;
		T to;
		LinkedList<T> tos;
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

		public Pipe(LinkedList<F> froms, T to, Transform transform) {
			this.froms = froms;
			this.to = to;
			this.transform = transform;
		}

		public Pipe(F from, LinkedList<T> tos, Transform transform) {
			this.from = from;
			this.tos = tos;
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
