package org.apache.ode.runtime.exec.platform;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

public class JMSUtil {

	//A full blown JMS implementation would be overkill to support only a single node so we will provide
	// a minimal implementation for a s
	public static class TopicImpl implements Topic {

		Set<MessageConsumerImpl> consumers = new CopyOnWriteArraySet<MessageConsumerImpl>();

		private String name;

		public TopicImpl(String name) {
			this.name = name;
		}

		@Override
		public String getTopicName() throws JMSException {
			return name;
		}

		public void addConsumer(MessageConsumerImpl consumer) {
			consumers.add(consumer);
		}

		public void removeConsumer(MessageConsumerImpl consumer) {
			consumers.remove(consumer);
		}

		public void publish(Message msg) throws JMSException {
			for (MessageConsumerImpl consumer : consumers) {
				if (consumer.getMessageListener() != null) {
					consumer.getMessageListener().onMessage(msg);
				} else {
					consumer.enqueue(msg);
				}
			}
		}

	}

	public static class MessageConsumerImpl implements MessageConsumer {
		BlockingDeque<Message> queue = new LinkedBlockingDeque<>();
		MessageListener listener;
		TopicImpl topic;

		MessageConsumerImpl(TopicImpl topic) {
			this.topic = topic;
		}
		
		public void enqueue(Message msg){
			queue.add(msg);
		}

		@Override
		public void close() throws JMSException {
			topic.removeConsumer(this);
		}

		@Override
		public MessageListener getMessageListener() throws JMSException {

			return listener;
		}

		@Override
		public String getMessageSelector() throws JMSException {

			return null;
		}

		@Override
		public Message receive() throws JMSException {
			try {
				return queue.take();
			} catch (InterruptedException e) {
				throw new JMSException(e.getMessage());
			}

		}

		@Override
		public Message receive(long time) throws JMSException {
			try {
				return queue.poll(time, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				throw new JMSException(e.getMessage());
			}
		}

		@Override
		public Message receiveNoWait() throws JMSException {
			return queue.poll();
		}

		@Override
		public void setMessageListener(MessageListener listener) throws JMSException {
			this.listener = listener;
		}

	}

	public static class MessageProducerImpl implements MessageProducer {

		TopicImpl topic;

		MessageProducerImpl(TopicImpl topic) {
			this.topic = topic;
		}

		@Override
		public void close() throws JMSException {

		}

		@Override
		public int getDeliveryMode() throws JMSException {

			return 0;
		}

		@Override
		public Destination getDestination() throws JMSException {

			return null;
		}

		@Override
		public boolean getDisableMessageID() throws JMSException {

			return false;
		}

		@Override
		public boolean getDisableMessageTimestamp() throws JMSException {

			return false;
		}

		@Override
		public int getPriority() throws JMSException {

			return 0;
		}

		@Override
		public long getTimeToLive() throws JMSException {

			return 0;
		}

		@Override
		public void send(Message msg) throws JMSException {
			topic.publish(msg);
		}

		@Override
		public void send(Destination dest, Message msg) throws JMSException {
			((TopicImpl) dest).publish(msg);
		}

		@Override
		public void send(Message msg, int arg1, int arg2, long arg3) throws JMSException {
			topic.publish(msg);
		}

		@Override
		public void send(Destination dest, Message msg, int arg2, int arg3, long arg4) throws JMSException {
			((TopicImpl) dest).publish(msg);
		}

		@Override
		public void setDeliveryMode(int arg0) throws JMSException {

		}

		@Override
		public void setDisableMessageID(boolean arg0) throws JMSException {

		}

		@Override
		public void setDisableMessageTimestamp(boolean arg0) throws JMSException {

		}

		@Override
		public void setPriority(int arg0) throws JMSException {

		}

		@Override
		public void setTimeToLive(long arg0) throws JMSException {

		}

	}

	public static class SessionImpl implements Session {

		@Override
		public void close() throws JMSException {

		}

		@Override
		public void commit() throws JMSException {

		}

		@Override
		public QueueBrowser createBrowser(Queue arg0) throws JMSException {

			return null;
		}

		@Override
		public QueueBrowser createBrowser(Queue arg0, String arg1) throws JMSException {

			return null;
		}

		@Override
		public BytesMessage createBytesMessage() throws JMSException {
			return new BytesMessageImpl();
		}

		@Override
		public MessageConsumer createConsumer(Destination dest) throws JMSException {
			return new MessageConsumerImpl((TopicImpl) dest);
		}

		@Override
		public MessageConsumer createConsumer(Destination dest, String arg1) throws JMSException {
			return new MessageConsumerImpl((TopicImpl) dest);
		}

		@Override
		public MessageConsumer createConsumer(Destination dest, String arg1, boolean arg2) throws JMSException {
			return new MessageConsumerImpl((TopicImpl) dest);
		}

		@Override
		public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1) throws JMSException {

			return null;
		}

		@Override
		public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1, String arg2, boolean arg3) throws JMSException {

			return null;
		}

		@Override
		public MapMessage createMapMessage() throws JMSException {

			return null;
		}

		@Override
		public Message createMessage() throws JMSException {
			return new MessageImpl();
		}

		@Override
		public ObjectMessage createObjectMessage() throws JMSException {

			return null;
		}

		@Override
		public ObjectMessage createObjectMessage(Serializable arg0) throws JMSException {

			return null;
		}

		@Override
		public MessageProducer createProducer(Destination arg0) throws JMSException {

			return null;
		}

		@Override
		public Queue createQueue(String arg0) throws JMSException {

			return null;
		}

		@Override
		public StreamMessage createStreamMessage() throws JMSException {

			return null;
		}

		@Override
		public TemporaryQueue createTemporaryQueue() throws JMSException {

			return null;
		}

		@Override
		public TemporaryTopic createTemporaryTopic() throws JMSException {

			return null;
		}

		@Override
		public TextMessage createTextMessage() throws JMSException {

			return null;
		}

		@Override
		public TextMessage createTextMessage(String arg0) throws JMSException {

			return null;
		}

		@Override
		public Topic createTopic(String arg0) throws JMSException {

			return null;
		}

		@Override
		public int getAcknowledgeMode() throws JMSException {

			return 0;
		}

		@Override
		public MessageListener getMessageListener() throws JMSException {

			return null;
		}

		@Override
		public boolean getTransacted() throws JMSException {

			return false;
		}

		@Override
		public void recover() throws JMSException {

		}

		@Override
		public void rollback() throws JMSException {

		}

		@Override
		public void run() {

		}

		@Override
		public void setMessageListener(MessageListener arg0) throws JMSException {

		}

		@Override
		public void unsubscribe(String arg0) throws JMSException {

		}

	}

	public static class MessageImpl implements Message {

		@Override
		public void acknowledge() throws JMSException {

		}

		@Override
		public void clearBody() throws JMSException {

		}

		@Override
		public void clearProperties() throws JMSException {

		}

		@Override
		public boolean getBooleanProperty(String arg0) throws JMSException {

			return false;
		}

		@Override
		public byte getByteProperty(String arg0) throws JMSException {

			return 0;
		}

		@Override
		public double getDoubleProperty(String arg0) throws JMSException {

			return 0;
		}

		@Override
		public float getFloatProperty(String arg0) throws JMSException {

			return 0;
		}

		@Override
		public int getIntProperty(String arg0) throws JMSException {

			return 0;
		}

		@Override
		public String getJMSCorrelationID() throws JMSException {

			return null;
		}

		@Override
		public byte[] getJMSCorrelationIDAsBytes() throws JMSException {

			return null;
		}

		@Override
		public int getJMSDeliveryMode() throws JMSException {

			return 0;
		}

		@Override
		public Destination getJMSDestination() throws JMSException {

			return null;
		}

		@Override
		public long getJMSExpiration() throws JMSException {

			return 0;
		}

		@Override
		public String getJMSMessageID() throws JMSException {

			return null;
		}

		@Override
		public int getJMSPriority() throws JMSException {

			return 0;
		}

		@Override
		public boolean getJMSRedelivered() throws JMSException {

			return false;
		}

		@Override
		public Destination getJMSReplyTo() throws JMSException {

			return null;
		}

		@Override
		public long getJMSTimestamp() throws JMSException {

			return 0;
		}

		@Override
		public String getJMSType() throws JMSException {

			return null;
		}

		@Override
		public long getLongProperty(String arg0) throws JMSException {

			return 0;
		}

		@Override
		public Object getObjectProperty(String arg0) throws JMSException {

			return null;
		}

		@Override
		public Enumeration getPropertyNames() throws JMSException {

			return null;
		}

		@Override
		public short getShortProperty(String arg0) throws JMSException {

			return 0;
		}

		@Override
		public String getStringProperty(String arg0) throws JMSException {

			return null;
		}

		@Override
		public boolean propertyExists(String arg0) throws JMSException {

			return false;
		}

		@Override
		public void setBooleanProperty(String arg0, boolean arg1) throws JMSException {

		}

		@Override
		public void setByteProperty(String arg0, byte arg1) throws JMSException {

		}

		@Override
		public void setDoubleProperty(String arg0, double arg1) throws JMSException {

		}

		@Override
		public void setFloatProperty(String arg0, float arg1) throws JMSException {

		}

		@Override
		public void setIntProperty(String arg0, int arg1) throws JMSException {

		}

		@Override
		public void setJMSCorrelationID(String arg0) throws JMSException {

		}

		@Override
		public void setJMSCorrelationIDAsBytes(byte[] arg0) throws JMSException {

		}

		@Override
		public void setJMSDeliveryMode(int arg0) throws JMSException {

		}

		@Override
		public void setJMSDestination(Destination arg0) throws JMSException {

		}

		@Override
		public void setJMSExpiration(long arg0) throws JMSException {

		}

		@Override
		public void setJMSMessageID(String arg0) throws JMSException {

		}

		@Override
		public void setJMSPriority(int arg0) throws JMSException {

		}

		@Override
		public void setJMSRedelivered(boolean arg0) throws JMSException {

		}

		@Override
		public void setJMSReplyTo(Destination arg0) throws JMSException {

		}

		@Override
		public void setJMSTimestamp(long arg0) throws JMSException {

		}

		@Override
		public void setJMSType(String arg0) throws JMSException {

		}

		@Override
		public void setLongProperty(String arg0, long arg1) throws JMSException {

		}

		@Override
		public void setObjectProperty(String arg0, Object arg1) throws JMSException {

		}

		@Override
		public void setShortProperty(String arg0, short arg1) throws JMSException {

		}

		@Override
		public void setStringProperty(String arg0, String arg1) throws JMSException {

		}

	}

	public static class BytesMessageImpl extends MessageImpl implements BytesMessage {
		byte[] bytes;
		int index =-1;

		@Override
		public long getBodyLength() throws JMSException {
			if (bytes != null) {
				return bytes.length;
			}
			return 0;
		}

		@Override
		public boolean readBoolean() throws JMSException {

			return false;
		}

		@Override
		public byte readByte() throws JMSException {

			return 0;
		}

		@Override
		public int readBytes(byte[] bytes) throws JMSException {
			int len = this.bytes.length > bytes.length ? bytes.length : this.bytes.length;
			System.arraycopy(this.bytes, 0, bytes, 0, len);
			index =-1;
			return len;
		}

		@Override
		public int readBytes(byte[] bytes, int l) throws JMSException {
			int len = l > this.bytes.length ? this.bytes.length : l;
			System.arraycopy(this.bytes, 0, bytes, 0, len);
			index +=len;
			return len;
		}

		@Override
		public char readChar() throws JMSException {
			return (char)bytes[index++];
		}

		@Override
		public double readDouble() throws JMSException {
			return (double)bytes[index++];
		}

		@Override
		public float readFloat() throws JMSException {
			return (float)bytes[index++];		}

		@Override
		public int readInt() throws JMSException {
			return (int)bytes[index++];		}

		@Override
		public long readLong() throws JMSException {
			return (long)bytes[index++];		}

		@Override
		public short readShort() throws JMSException {
			return (short)bytes[index++];		}

		@Override
		public String readUTF() throws JMSException {

			return null;
		}

		@Override
		public int readUnsignedByte() throws JMSException {

			return 0;
		}

		@Override
		public int readUnsignedShort() throws JMSException {

			return 0;
		}

		@Override
		public void reset() throws JMSException {
			bytes=null;
			index=-1;
		}

		@Override
		public void writeBoolean(boolean arg0) throws JMSException {

		}

		@Override
		public void writeByte(byte arg0) throws JMSException {

		}

		@Override
		public void writeBytes(byte[] bytes) throws JMSException {
			this.bytes=bytes;
			this.index=0;
		}

		@Override
		public void writeBytes(byte[] arg0, int arg1, int arg2) throws JMSException {

		}

		@Override
		public void writeChar(char arg0) throws JMSException {

		}

		@Override
		public void writeDouble(double arg0) throws JMSException {

		}

		@Override
		public void writeFloat(float arg0) throws JMSException {

		}

		@Override
		public void writeInt(int arg0) throws JMSException {

		}

		@Override
		public void writeLong(long arg0) throws JMSException {

		}

		@Override
		public void writeObject(Object arg0) throws JMSException {

		}

		@Override
		public void writeShort(short arg0) throws JMSException {

		}

		@Override
		public void writeUTF(String arg0) throws JMSException {

		}

	}

}
