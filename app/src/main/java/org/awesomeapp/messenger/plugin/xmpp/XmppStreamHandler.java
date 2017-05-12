package org.awesomeapp.messenger.plugin.xmpp;

import org.awesomeapp.messenger.util.LogCleaner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.DefaultExtensionElement;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider;

import org.xmlpull.v1.XmlPullParser;

import android.util.Log;

public class XmppStreamHandler {
    public static final String URN_SM_2 = "urn:xmpp:sm:2";
    private static final int MAX_OUTGOING_QUEUE_SIZE = 20;
    private static final int OUTGOING_FILL_RATIO = 4;
    private XMPPTCPConnection mConnection;
    private boolean isSmAvailable = false;
    private boolean isSmEnabled = false;
    private boolean isOutgoingSmEnabled = false;
    private long previousIncomingStanzaCount = -1;
    private String sessionId;
    private long incomingStanzaCount = 0;
    private long outgoingStanzaCount = 0;
    private Queue<Stanza> outgoingQueue;
    private int maxOutgoingQueueSize = MAX_OUTGOING_QUEUE_SIZE;
    private ConnectionListener mConnectionListener;

    public XmppStreamHandler(XMPPTCPConnection connection, ConnectionListener connectionListener) {
        mConnection = connection;
        mConnectionListener = connectionListener;
        startListening();
    }

    /** Perform a quick shutdown of the XMPPConnection if a resume is possible */
    public void quickShutdown() {
        if (isResumePossible()) {
            mConnection.disconnect();
            // We will not necessarily get any notification from a quickShutdown, so adjust our state here.
            closeOnError();
        } else {
            mConnection.disconnect();
        }
    }

    public void setMaxOutgoingQueueSize(int maxOutgoingQueueSize) {
        this.maxOutgoingQueueSize = maxOutgoingQueueSize;
    }

    public boolean isResumePossible() {
        return sessionId != null;
    }

    public boolean isResumePending() {
        return isResumePossible() && !isSmEnabled;
    }

    public static void addExtensionProviders() {
        addSimplePacketExtension("sm", URN_SM_2);
        addSimplePacketExtension("r", URN_SM_2);
        addSimplePacketExtension("a", URN_SM_2);
        addSimplePacketExtension("enabled", URN_SM_2);
        addSimplePacketExtension("resumed", URN_SM_2);
        addSimplePacketExtension("failed", URN_SM_2);
    }

    public void notifyInitialLogin() {
        if (sessionId == null && isSmAvailable)
            try {
                sendEnablePacket();
            }
            catch (Exception e)
            {}
    }

    private void sendEnablePacket() throws Exception {
        debug("sm send enable " + sessionId);
        if (sessionId != null) {
            isOutgoingSmEnabled = true;
            // TODO binding
            StreamHandlingPacket resumePacket = new StreamHandlingPacket("resume", URN_SM_2);
            resumePacket.addAttribute("h", String.valueOf(previousIncomingStanzaCount));
            resumePacket.addAttribute("previd", sessionId);
            mConnection.sendStanza(resumePacket);
        } else {
            outgoingStanzaCount = 0;
            outgoingQueue = new ConcurrentLinkedQueue<Stanza>();
            isOutgoingSmEnabled = true;

            StreamHandlingPacket enablePacket = new StreamHandlingPacket("enable", URN_SM_2);
            enablePacket.addAttribute("resume", "true");
            mConnection.sendStanza(enablePacket);
        }
    }

    private void closeOnError() {
        if (isSmEnabled && sessionId != null) {
            previousIncomingStanzaCount = incomingStanzaCount;
        }
        isSmEnabled = false;
        isOutgoingSmEnabled = false;
        isSmAvailable = false;
    }

    private void startListening() {
        mConnection.addConnectionListener(new ConnectionListener() {
            public void reconnectionSuccessful() {
            }

            public void reconnectionFailed(Exception e) {
            }

            public void reconnectingIn(int seconds) {
            }

            public void connectionClosedOnError(Exception e) {
                if (e instanceof XMPPException &&
                        ((XMPPException)e).getMessage() != null) {
                    // Non-resumable stream error
                    close();
                } else {
                    // Resumable
                    closeOnError();
                }
            }

            @Override
            public void connected(XMPPConnection connection) {

            }

            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {

            }

            public void connectionClosed() {
                previousIncomingStanzaCount = -1;
            }
        });

        mConnection.addPacketSendingListener(new PacketListener() {
            public void processStanza(Stanza packet) {
                // Ignore our own request for acks - they are not counted
                /**
                if (!isStanza(packet)) {
                    trace("send " + packet.toXML());
                    return;
                }**/

                if (isOutgoingSmEnabled && !outgoingQueue.contains(packet)) {
                    outgoingStanzaCount++;
                    outgoingQueue.add(packet);

                    trace("send " + outgoingStanzaCount + " : " + packet.toXML());


                    try {
                        // Don't let the queue grow beyond max size.  Request acks and drop old packets
                        // if acks are not coming.
                        if (outgoingQueue.size() >= maxOutgoingQueueSize / OUTGOING_FILL_RATIO) {
                            mConnection.sendStanza(new StreamHandlingPacket("r", URN_SM_2));

                        }
                    }
                    catch (Exception e)
                    {
                        //he's not watching
                    }

                    if (outgoingQueue.size() > maxOutgoingQueueSize) {
//                        Log.e(XmppConnection.TAG, "not receiving acks?  outgoing queue full");
                        outgoingQueue.remove();
                    }
                } else if (isOutgoingSmEnabled && outgoingQueue.contains(packet)) {
                    outgoingStanzaCount++;
                    trace("send DUPLICATE " + outgoingStanzaCount + " : " + packet.toXML());
                } else {
                    trace("send " + packet.toXML());
                }
            }
        }, new PacketFilter() {
            public boolean accept(Stanza packet) {
                return true;
            }
        });

        mConnection.addAsyncStanzaListener(new StanzaListener()
        {
            public void processStanza(Stanza stanza) {
                if (isSmEnabled) {
                    incomingStanzaCount++;
                    trace("recv " + incomingStanzaCount + " : " + stanza.toXML());
                } else {
                    trace("recv " + stanza.toXML());
                }

                if (stanza instanceof StreamHandlingPacket) {
                    StreamHandlingPacket shPacket = (StreamHandlingPacket) stanza;
                    String name = shPacket.getElementName();

                    try {
                        if ("sm".equals(name)) {
                            debug("sm avail");
                            isSmAvailable = true;
                            if (sessionId != null)
                                sendEnablePacket();
                        } else if ("r".equals(name)) {
                            StreamHandlingPacket ackPacket = new StreamHandlingPacket("a", URN_SM_2);
                            ackPacket.addAttribute("h", String.valueOf(incomingStanzaCount));
                            mConnection.sendPacket(ackPacket);
                        } else if ("a".equals(name)) {
                            long ackCount = Long.valueOf(shPacket.getAttribute("h"));
                            removeOutgoingAcked(ackCount);
                            trace(outgoingQueue.size() + " in outgoing queue after ack");
                        } else if ("enabled".equals(name)) {
                            incomingStanzaCount = 0;
                            isSmEnabled = true;
                         //   mConnection.getRoster().setOfflineOnError(false);
                            String resume = shPacket.getAttribute("resume");
                            if ("true".equals(resume) || "1".equals(resume)) {
                                sessionId = shPacket.getAttribute("id");
                            }
                            debug("sm enabled " + sessionId);
                        } else if ("resumed".equals(name)) {
                            debug("sm resumed");
                            incomingStanzaCount = previousIncomingStanzaCount;
                            long resumeStanzaCount = Long.valueOf(shPacket.getAttribute("h"));
                            // Removed acked packets
                            removeOutgoingAcked(resumeStanzaCount);
                            trace(outgoingQueue.size() + " in outgoing queue after resume");

                            // Resend any unacked packets
                            for (Stanza resendPacket : outgoingQueue) {
                                //     mConnection.sendPacket(resendPacket);
                               // mConnection.send(resendPacket);

                            }

                            // Enable only after resend, so that the interceptor does not
                            // queue these again or increment outgoingStanzaCount.
                            isSmEnabled = true;

                            // Re-notify the listener - we are really ready for packets now
                            // Before this point, isSuspendPending() was true, and the listener should have
                            // ignored reconnectionSuccessful() from XMPPConnection.
                            mConnectionListener.reconnectionSuccessful();
                        } else if ("failed".equals(name)) {
                            // Failed, shutdown and the parent will retry
                            debug("sm failed");
                            //       mConnection.getRoster().setOfflineOnError(true);
                            //     mConnection.getRoster().setOfflinePresences();
                            sessionId = null;
                            mConnection.disconnect();
                            // isSmEnabled / isOutgoingSmEnabled are already false
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }, new StanzaFilter() {
            public boolean accept(Stanza packet) {
                return true;
            }
        });
    }


    private void removeOutgoingAcked(long ackCount) {
        if (ackCount > outgoingStanzaCount) {
            Log.e(XmppConnection.TAG,
                    "got ack of " + ackCount + " but only sent " + outgoingStanzaCount);
            // Reset the outgoing count here in a feeble attempt to re-sync.  All bets
            // are off.
            outgoingStanzaCount = ackCount;
        }

        int size = outgoingQueue.size();
        while (size > outgoingStanzaCount - ackCount) {
            outgoingQueue.remove();
            size--;
        }
    }

    private static void addSimplePacketExtension(final String name, final String namespace) {
        ProviderManager.addExtensionProvider(name, namespace,
                new DataPacketProvider.PacketExtensionProvider() {
                    public Stanza parseExtension(XmlPullParser parser) throws Exception {
                        StreamHandlingPacket packet = new StreamHandlingPacket(name, namespace);
                        int attributeCount = parser.getAttributeCount();
                        for (int i = 0; i < attributeCount; i++) {
                            packet.addAttribute(parser.getAttributeName(i),
                                    parser.getAttributeValue(i));
                        }
                        return packet;
                    }
                });
    }

    private void debug(String message) {
        if (Log.isLoggable(XmppConnection.TAG, Log.DEBUG)) {
            Log.d(XmppConnection.TAG, LogCleaner.clean(message));
        }
    }

    private void trace(String message) {
        if (Log.isLoggable(XmppConnection.TAG, Log.VERBOSE)) {
            Log.v(XmppConnection.TAG, LogCleaner.clean(message));
        }
    }

    static class StreamHandlingPacket extends Stanza {
        private String name;
        private String namespace;
        Map<String, String> attributes;

        StreamHandlingPacket(String name, String namespace) {
            this.name = name;
            this.namespace = namespace;
            attributes = Collections.emptyMap();
        }

        public void addAttribute(String name, String value) {
            if (attributes == Collections.EMPTY_MAP)
                attributes = new HashMap<String, String>();
            attributes.put(name, value);
        }

        public String getAttribute(String name) {
            return attributes.get(name);
        }

        public String getNamespace() {
            return namespace;
        }

        public String getElementName() {
            return name;
        }

        public String toString ()
        {
            return toXML();
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append("<").append(getElementName());

            // TODO Xmlns??
            if (getNamespace() != null) {
                buf.append(" xmlns=\"").append(getNamespace()).append("\"");
            }
            for (String key : attributes.keySet()) {
                buf.append(" ").append(key).append("=\"")
                        .append(StringUtils.escapeForXML(attributes.get(key))).append("\"");
            }
            buf.append("/>");
            return buf.toString();
        }

    }

    /** Returns true if the packet is a Stanza as defined in RFC-6121 - a Message, IQ or Presence packet. */
    public static boolean isStanza(Stanza packet) {
        if (packet instanceof Message)
            return true;
        if (packet instanceof IQ)
            return true;
        if (packet instanceof Presence)
            return true;
        return false;
    }

    public void queue(Stanza packet) {
        if (outgoingQueue.size() >= maxOutgoingQueueSize) {
            Log.e(XmppConnection.TAG, "outgoing queue full");
            return;
        }
        outgoingStanzaCount++;
        outgoingQueue.add(packet);
    }

    private void close() {
        isSmEnabled = false;
        isOutgoingSmEnabled = false;
        sessionId = null;
    }
}
