package com.ihubin.webrtc.signal.model;

import lombok.Data;

@Data
public class SignalMessage {

    private String type;

    private String contactTo;

    private String sdp;

    private String id;

    private String label;

    private String candidate;

}
