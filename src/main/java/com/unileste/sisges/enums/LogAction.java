package com.unileste.sisges.enums;

public enum LogAction {

    DELETE(0),
    CREATE(1),
    UPDATE(2),
    LOGIN(3),
    LOGOUT(4);

    private final int id;

    LogAction(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static LogAction fromId(int id) {
        for (LogAction action : values()) {
            if (action.id == id) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown LogAction id: " + id);
    }
}
