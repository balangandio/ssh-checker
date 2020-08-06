package com.trilead.ssh2.auth;

import java.io.IOException;
import java.util.Vector;

import com.trilead.ssh2.packets.PacketServiceAccept;
import com.trilead.ssh2.packets.PacketServiceRequest;
import com.trilead.ssh2.packets.PacketUserauthBanner;
import com.trilead.ssh2.packets.PacketUserauthFailure;
import com.trilead.ssh2.packets.PacketUserauthRequestNone;
import com.trilead.ssh2.packets.PacketUserauthRequestPassword;
import com.trilead.ssh2.packets.Packets;
import com.trilead.ssh2.transport.MessageHandler;
import com.trilead.ssh2.transport.TransportManager;

/**
 * AuthenticationManager.
 * 
 * @author Christian Plattner, plattner@trilead.com
 * @version $Id: AuthenticationManager.java,v 1.1 2007/10/15 12:49:57 cplattne
 *          Exp $
 */
public class AuthenticationManager implements MessageHandler {
	TransportManager tm;

	Vector packets = new Vector();
	boolean connectionClosed = false;

	String banner;

	String[] remainingMethods = new String[0];
	boolean isPartialSuccess = false;

	boolean authenticated = false;
	boolean initDone = false;

	public AuthenticationManager(TransportManager tm) {
		this.tm = tm;
	}


	public boolean authenticatePassword(String user, String pass)
			throws IOException {
		try {
			initialize(user);

			if (methodPossible("password") == false)
				throw new IOException(
						"Authentication method password not supported by the server at this stage.");

			PacketUserauthRequestPassword ua = new PacketUserauthRequestPassword(
					"ssh-connection", user, pass);
			tm.sendMessage(ua.getPayload());

			byte[] ar = getNextMessage();

			if (ar[0] == Packets.SSH_MSG_USERAUTH_SUCCESS) {
				authenticated = true;
				tm.removeMessageHandler(this, 0, 255);
				return true;
			}

			if (ar[0] == Packets.SSH_MSG_USERAUTH_FAILURE) {
				PacketUserauthFailure puf = new PacketUserauthFailure(ar, 0,
						ar.length);

				remainingMethods = puf.getAuthThatCanContinue();
				isPartialSuccess = puf.isPartialSuccess();

				return false;
			}

			throw new IOException("Unexpected SSH message (type " + ar[0] + ")");

		} catch (IOException e) {
			tm.close(e, false);
			throw (IOException) new IOException(
					"Password authentication failed.").initCause(e);
		}
	}

	byte[] deQueue() throws IOException {
		synchronized (packets) {
			while (packets.size() == 0) {
				if (connectionClosed)
					throw (IOException) new IOException(
							"The connection is closed.").initCause(tm
							.getReasonClosedCause());

				try {
					packets.wait();
				} catch (InterruptedException ign) {
				}
			}
			/* This sequence works with J2ME */
			byte[] res = (byte[]) packets.firstElement();
			packets.removeElementAt(0);
			return res;
		}
	}

	byte[] getNextMessage() throws IOException {
		while (true) {
			byte[] msg = deQueue();

			if (msg[0] != Packets.SSH_MSG_USERAUTH_BANNER)
				return msg;

			PacketUserauthBanner sb = new PacketUserauthBanner(msg, 0,
					msg.length);

			banner = sb.getBanner();
		}
	}

	public boolean getPartialSuccess() {
		return isPartialSuccess;
	}

	public String[] getRemainingMethods(String user) throws IOException {
		initialize(user);
		return remainingMethods;
	}

	@Override
	public void handleMessage(byte[] msg, int msglen) throws IOException {
		synchronized (packets) {
			if (msg == null) {
				connectionClosed = true;
			} else {
				byte[] tmp = new byte[msglen];
				System.arraycopy(msg, 0, tmp, 0, msglen);
				packets.addElement(tmp);
			}

			packets.notifyAll();

			if (packets.size() > 5) {
				connectionClosed = true;
				throw new IOException(
						"Error, peer is flooding us with authentication packets.");
			}
		}
	}

	private boolean initialize(String user) throws IOException {
		if (initDone == false) {
			tm.registerMessageHandler(this, 0, 255);

			PacketServiceRequest sr = new PacketServiceRequest("ssh-userauth");
			tm.sendMessage(sr.getPayload());

			PacketUserauthRequestNone urn = new PacketUserauthRequestNone(
					"ssh-connection", user);
			tm.sendMessage(urn.getPayload());

			byte[] msg = getNextMessage();
			new PacketServiceAccept(msg, 0, msg.length);
			msg = getNextMessage();

			initDone = true;

			if (msg[0] == Packets.SSH_MSG_USERAUTH_SUCCESS) {
				authenticated = true;
				tm.removeMessageHandler(this, 0, 255);
				return true;
			}

			if (msg[0] == Packets.SSH_MSG_USERAUTH_FAILURE) {
				PacketUserauthFailure puf = new PacketUserauthFailure(msg, 0,
						msg.length);

				remainingMethods = puf.getAuthThatCanContinue();
				isPartialSuccess = puf.isPartialSuccess();
				return false;
			}

			throw new IOException("Unexpected SSH message (type " + msg[0]
					+ ")");
		}
		return authenticated;
	}

	boolean methodPossible(String methName) {
		if (remainingMethods == null)
			return false;

		for (int i = 0; i < remainingMethods.length; i++) {
			if (remainingMethods[i].compareTo(methName) == 0)
				return true;
		}
		return false;
	}
}
