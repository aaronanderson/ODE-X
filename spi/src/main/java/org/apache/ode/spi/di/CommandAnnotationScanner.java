package org.apache.ode.spi.di;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Qualifier;
import javax.xml.namespace.QName;

import org.apache.ode.spi.di.CommandAnnotationScanner.CommandModel;
import org.apache.ode.spi.di.OperationAnnotationScanner.BaseModel;
import org.apache.ode.spi.di.OperationAnnotationScanner.OperationModel;
import org.apache.ode.spi.work.Command;
import org.apache.ode.spi.work.Command.CommandSet;
import org.apache.ode.spi.work.Command.CommandSetRef;
import org.apache.ode.spi.work.ExecutionUnit.InBuffer;
import org.apache.ode.spi.work.ExecutionUnit.OutBuffer;
import org.apache.ode.spi.work.Operation;
import org.apache.ode.spi.work.Operation.I;
import org.apache.ode.spi.work.Operation.IO;
import org.apache.ode.spi.work.Operation.IOP;
import org.apache.ode.spi.work.Operation.IP;
import org.apache.ode.spi.work.Operation.O;
import org.apache.ode.spi.work.Operation.OP;

public class CommandAnnotationScanner implements AnnotationScanner<Map<QName, CommandModel>> {
	protected static final Logger log = Logger.getLogger(CommandAnnotationScanner.class.getName());
	protected ConcurrentHashMap<Class<?>, Class<?>> commandSetRefs = new ConcurrentHashMap<>();

	public Map<QName, CommandModel> scan(Class<?> clazz) {
		log.fine(String.format("scanned class %s\n", clazz));
		if (clazz.isAnnotationPresent(CommandSetRef.class)) {

			CommandSetRef cmdSetRef = clazz.getAnnotation(CommandSetRef.class);
			if (cmdSetRef.value().length == 0) {
				return null;
			}

			Map<QName, CommandModel> commands = new HashMap<>();
			for (Class<?> cmdClazz : cmdSetRef.value()) {
				if (!cmdClazz.isAnnotationPresent(CommandSet.class)) {
					log.severe(String.format("Class %s is referenced from %s as a CommandSet but it is missing the annotation\n", cmdClazz, clazz));
					continue;
				}

				Class<?> exists = commandSetRefs.putIfAbsent(cmdClazz, clazz);
				if (exists != null) {
					log.fine(String.format("Class %s is referenced from %s as a CommandSet but it has already been processed from reference in %s, skipping\n", cmdClazz, clazz,
							commandSetRefs.get(exists)));
					continue;
				}

				if (!Modifier.isInterface(cmdClazz.getModifiers())) {
					log.severe(String.format("Class %s is a CommandSet but is not an interface\n", cmdClazz, clazz));
					continue;
				}

				CommandSet cmdSet = cmdClazz.getAnnotation(CommandSet.class);
				for (Method m : cmdClazz.getMethods()) {
					if (m.isAnnotationPresent(Command.class)) {

						Command cmd = m.getAnnotation(Command.class);
						String commandNamespace = cmd.namespace();
						if (commandNamespace.length() == 0) {
							commandNamespace = cmdSet.namespace();
						}
						if (commandNamespace.length() == 0) {
							log.severe(String.format("Unable to determine command namespace for Class %s method %s, skipping", cmdClazz.getName(), m.getName()));
							continue;
						}
						String commandName = cmd.name();
						if (commandName.length() == 0) {
							commandName = m.getName();
						}
						QName commandQName = new QName(commandNamespace, commandName);
						CommandModel cm = new CommandModel(commandQName);

						if (OperationAnnotationScanner.inspect(cmdClazz, false, m, cm)) {
							commands.put(commandQName, cm);
						}

					}
				}
			}
			return commands;
		} else {
			return null;
		}
	}

	@Qualifier
	@Retention(RUNTIME)
	@Target(FIELD)
	public @interface Commands {

	}

	public static class CommandModel extends BaseModel {

		private final QName commandName;

		public CommandModel(QName commandName) {
			this.commandName = commandName;
		}

		public QName commandName() {
			return commandName;
		}

	}

}
