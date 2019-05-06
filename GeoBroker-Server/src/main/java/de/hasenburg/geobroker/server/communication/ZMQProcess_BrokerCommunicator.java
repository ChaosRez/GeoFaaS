package de.hasenburg.geobroker.server.communication;

import de.hasenburg.geobroker.commons.communication.ZMQControlUtility;
import de.hasenburg.geobroker.commons.communication.ZMQProcess;
import de.hasenburg.geobroker.commons.model.BrokerInfo;
import de.hasenburg.geobroker.server.distribution.IDistributionLogic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import java.util.Arrays;
import java.util.List;

public class ZMQProcess_BrokerCommunicator extends ZMQProcess {

	private static final Logger logger = LogManager.getLogger();

	private final int number;
	private int numberOfProcessedMessages = 0;

	private final int PULL_INDEX = 0; // the pull socket
	private final int SOCKET_OFFSET = 1; // we have one other socket that is not a dealer broker socket
	private List<BrokerInfo> otherBrokerInfos;
	private IDistributionLogic distributionLogic;

	/**
	 * @param brokerId - identity should be the broker id this broker communicator is running on
	 * @param number - incrementing number for this message processor (as there might be many), starts with 1
	 */
	ZMQProcess_BrokerCommunicator(String brokerId, int number, IDistributionLogic distributionLogic,
								  List<BrokerInfo> otherBrokerInfos) {
		super(getBrokerCommunicatorId(brokerId, number));
		this.number = number;
		this.distributionLogic = distributionLogic;
		this.otherBrokerInfos = otherBrokerInfos;
	}

	static String getBrokerCommunicatorId(String brokerId, int number) {
		return brokerId + "-broker_communicator-" + number;
	}

	@Override
	protected List<Socket> bindAndConnectSockets(ZContext context) {
		Socket[] socketArray = new Socket[otherBrokerInfos.size() + SOCKET_OFFSET];

		// bind pull socket
		Socket puller = context.createSocket(SocketType.PULL);
		puller.setIdentity(identity.getBytes());
		puller.bind("inproc://" + identity);
		socketArray[PULL_INDEX] = puller;

		// bind dealer sockets
		int i = SOCKET_OFFSET;
		for (BrokerInfo brokerInfo : otherBrokerInfos) {
			Socket dealer = context.createSocket(SocketType.DEALER);
			// ok because only one dealer is communicating with each target broker
			dealer.setIdentity(identity.getBytes());
			String targetBrokerAddress = "tcp://" + brokerInfo.getIp() + ":" + brokerInfo.getPort();
			logger.debug("Connecting to {} at {}", brokerInfo.getBrokerId(), targetBrokerAddress);
			dealer.connect(targetBrokerAddress);
			socketArray[i] = dealer;
			i++;
		}

		return Arrays.asList(socketArray);
	}

	@Override
	protected void processZMQControlCommandOtherThanKill(ZMQControlUtility.ZMQControlCommand zmqControlCommand,
														 ZMsg msg) {
		// no other commands are of interest
	}

	/*****************************************************************
	 * Process polled messages
	 ****************************************************************/

	@Override
	protected void processZMsg(int socketIndex, ZMsg msg) {
		numberOfProcessedMessages++;
		logger.trace("ZMQProcess_BrokerCommunicator {} processing message number {}",
				identity,
				numberOfProcessedMessages);

		if (PULL_INDEX == socketIndex) {
			processPullSocketMessage(msg);
			return;
		}

		int dealerIndex = socketIndex - SOCKET_OFFSET;

		if (otherBrokerInfos.size() - 1 < dealerIndex) {
			distributionLogic.processOtherBrokerAcknowledgement(msg);
			return;
		}

		logger.error("Received a message on socket {}, but there should not be a socket.", socketIndex);
	}

	/**
	 * This message was sent to the pull socket by any {@link ZMQProcess_MessageProcessor}.
	 *
	 * @param msg - should be generated by {@link ZMQProcess_BrokerCommunicator#generatePULLSocketMessage(String,
	 * InternalBrokerMessage)}
	 */
	private void processPullSocketMessage(ZMsg msg) {
		String targetBrokerId = null;

		// get the target brokerId
		if (msg != null && msg.size() == 3) { // target broker id + two parts for InternalBrokerMessage
			targetBrokerId = msg.pop().getString(ZMQ.CHARSET);
		}

		if (targetBrokerId == null) {
			logger.error("Message is missing target broker id");
			return;
		}

		logger.trace("Sending a message to broker {}", targetBrokerId);
		int socketIndex = getSocketIndexForBrokerId(targetBrokerId);

		if (socketIndex < 0) {
			logger.error("Broker {} does not have a socket.", socketIndex);
		}

		Socket socket = sockets.get(socketIndex);

		distributionLogic.sendMessageToOtherBrokers(msg, socket);
	}

	/*****************************************************************
	 * Message Generation Helpers
	 ****************************************************************/

	public static ZMsg generatePULLSocketMessage(String targetBrokerId, InternalBrokerMessage ibm) {
		ZMsg msg = ZMsg.newStringMsg(targetBrokerId);
		ZMsg ibm_message = ibm.getZMsg();
		msg.add(ibm_message.pop());
		msg.add(ibm_message.pop());
		if (ibm_message.size() > 0) {
			logger.error("All message frames should have been popped, it remains {}", msg);
		}
		return msg;
	}

	/*****************************************************************
	 * Others
	 ****************************************************************/

	@Override
	protected void shutdownCompleted() {
		logger.info("Shut down ZMQProcess_Server {}", getBrokerCommunicatorId(identity, number));
	}

	private int getSocketIndexForBrokerId(String brokerId) {
		// TODO make more efficient
		int i = SOCKET_OFFSET; // first socket is at this index
		for (BrokerInfo otherBrokerInfo : otherBrokerInfos) {
			if (otherBrokerInfo.getBrokerId().equals(brokerId)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	int getNumberOfProcessedMessages() {
		return numberOfProcessedMessages;
	}

}
