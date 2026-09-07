package com.claircore.shared.application.outboundservices;

/** Sends a reconciliation hint to the edge. */
public interface EdgeNotifier {
    void notifyChange(String resource, String hint);
}
