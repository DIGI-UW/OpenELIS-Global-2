package org.openelisglobal.dataexchange.common;

public interface IAsyncExternalSender {

    void sendMessage(IExternalSender sender, ITransmissionResponseHandler responseHandler, String msg);
}
