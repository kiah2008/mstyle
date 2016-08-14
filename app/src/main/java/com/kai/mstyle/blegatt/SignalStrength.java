package com.kai.mstyle.blegatt;

/**
 * Created by kiah on 8/13/2016.
 */
public class SignalStrength {
    public enum SIGNAL_LEVEL {
        SIGNAL_NONE,
        SIGNAL_POOR,
        SIGNAL_GOOD,
        SIGNAL_GREAT
    }
    public static SIGNAL_LEVEL getLevel(int rssi) {
        if (rssi > -30 && rssi <0) {
            return SIGNAL_LEVEL.SIGNAL_GREAT;
        } else if(rssi > -60) {
            return SIGNAL_LEVEL.SIGNAL_GOOD;
        } else if (rssi > -90) {
            return SIGNAL_LEVEL.SIGNAL_POOR;
        } else {
            return SIGNAL_LEVEL.SIGNAL_NONE;
        }
    }
}
