package kr.hhplus.be.server.waiting.infra.redis;

public class WaitingQueueKeys {
    private WaitingQueueKeys() {}
    private static final String TAG = "{queue}";

    public static String waitingZ()      { return "z:" + TAG + ":waiting"; }
    public static String workingZ()      { return "z:" + TAG + ":working"; }
    public static String activeTokenH()  { return "h:" + TAG + ":active_token"; }
    public static String ticketH(String token) { return "h:" + TAG + ":ticket:" + token; }
    public static String perMsSeq(long ms)     { return "seq:" + TAG + ":waiting:" + ms; }
}
