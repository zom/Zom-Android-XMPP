package org.awesomeapp.messenger.model;

import net.java.otr4j.session.SessionStatus;

import org.awesomeapp.messenger.service.IDataListener;

import java.io.IOException;
import java.util.Map;

public interface DataHandler {
    /**
     * @param from this is OUR {@link Address}
     * @param to the receiving {@link Address}
     * @param value the serialized request
     */
    void onIncomingRequest(Address from, Address to, byte[] value);

    /**
     * @param from this is OUR {@link Address}
     * @param to the receiving {@link Address}
     * @param value the serialized response
     */
    void onIncomingResponse(Address from, Address to, byte[] value);

    /**
     * Offer data to peer
     *
     * @param offerId offer ID
     * @param us our {@link Address}
     * @param localUri URI of data
     * @param headers extra headrs or null
     */
    void offerData(String offerId, Address us, String localUri, Map<String, String> headers) throws IOException;

    void setDataListener(IDataListener dataListener);

    void onOtrStatusChanged(SessionStatus status);
}
