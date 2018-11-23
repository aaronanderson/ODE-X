package org.apache.ode.runtime;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

import org.apache.ignite.internal.client.marshaller.jdk.GridClientJdkMarshaller;
import org.apache.ignite.internal.processors.rest.client.message.GridClientHandshakeRequest;
import org.apache.ignite.internal.processors.rest.client.message.GridClientHandshakeResponse;
import org.apache.ignite.internal.processors.rest.client.message.GridClientResponse;
import org.apache.ignite.internal.processors.rest.client.message.GridClientTaskRequest;
import org.apache.ignite.internal.processors.rest.client.message.GridClientTaskResultBean;
import org.apache.ignite.internal.processors.rest.protocols.tcp.GridMemcachedMessage;
import org.apache.ignite.internal.util.GridClientByteUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.ode.spi.config.Config;

public class Client implements AutoCloseable {

	public static final Logger LOG = LogManager.getLogger(Client.class);

	private static final Client CLIENT = new Client();
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
	private static final GridClientJdkMarshaller MARSH = new GridClientJdkMarshaller();

	private Client() {
	}

	public Client start(String configFile) throws IOException {
		Config odeConfig = Configurator.loadConfigFile(configFile);

		String host = odeConfig.getString("ode.ignite.connector.host").orElse(Configurator.DEFAULT_HOST);
		int port = odeConfig.getNumber("ode.ignite.connector.port").orElse(Configurator.DEFAULT_CONNECTOR_PORT);
		int portRange = odeConfig.getNumber("ode.ignite.connector.portRange").orElse(20);

		socket = new Socket();
		for (int i = 0; i < portRange; i++) {
			try {
				socket.connect(new InetSocketAddress(host, port + i));
				break;
			} catch (IOException e) {

			}
		}
		if (!socket.isConnected()) {
			throw new IOException(String.format("Unable to connect to Ignite connector %s:%d", host, port));
		}

		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());

		out.write(GridMemcachedMessage.IGNITE_HANDSHAKE_FLAG);
		GridClientHandshakeRequest req = new GridClientHandshakeRequest();
		req.marshallerId(GridClientJdkMarshaller.ID);
		out.write(req.rawBytes());
		out.flush();
		byte[] handshakeResponse = new byte[2];
		in.read(handshakeResponse); // Read handshake response.
		if (handshakeResponse[0] != GridMemcachedMessage.IGNITE_HANDSHAKE_RES_FLAG || handshakeResponse[1] != GridClientHandshakeResponse.OK.resultCode()) {
			throw new IOException(String.format("Client handshake failed [res=%s code=%s\n", handshakeResponse[0], handshakeResponse[1]));
		}

		return this;
	}

	public <A, R> R send(String taskName, A argument) throws IOException {
		GridClientTaskRequest clientMessage = new GridClientTaskRequest();
		clientMessage.taskName(taskName);
		clientMessage.argument(argument);
		byte[] data = MARSH.marshal(clientMessage, 0).array();

		int len = data.length + 40;

		out.write(GridMemcachedMessage.IGNITE_REQ_FLAG); // Package type.
		out.write(GridClientByteUtils.intToBytes(len));
		out.write(new byte[40]);
		// Stream header.
//		GridClientByteUtils.longToBytes(reqId);
//		GridClientByteUtils.uuidToBytes(clientId);
//		GridClientByteUtils.uuidToBytes(destId);
		out.write(data);

		byte[] resultHeader = new byte[45];
		in.read(resultHeader);
		if (resultHeader[0] != GridMemcachedMessage.IGNITE_REQ_FLAG) {
			throw new IOException(String.format("Client task failed [res=%s \n", resultHeader[0]));
		}

		int length = GridClientByteUtils.bytesToInt(resultHeader, 1);
		long requestId = GridClientByteUtils.bytesToLong(resultHeader, 5);
		UUID clientId = GridClientByteUtils.bytesToUuid(resultHeader, 13);
		UUID destinationId = GridClientByteUtils.bytesToUuid(resultHeader, 29);
		if (length > 0) {
			byte[] resultBody = new byte[length];
			in.read(resultBody);
			GridClientResponse response = MARSH.unmarshal(resultBody);
			GridClientTaskResultBean bean = (GridClientTaskResultBean) response.result();
			return bean.getResult();
		}
		return null;
	}

	public static Client instance() {
		return CLIENT;
	}

	@Override
	public void close() throws IOException {
		if (socket != null) {
			socket.close();
		}
		socket = null;
	}

}
