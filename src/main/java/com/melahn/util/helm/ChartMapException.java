package com.melahn.util.helm;

import java.util.UUID;

/*
 *  ChartMapException is an exception class designed for use by ChartMap.
*/

public class ChartMapException extends Exception { 
    static final long serialVersionUID = UUID.fromString("5a8dba66-71e1-492c-bf3b-53cceb67b785").getLeastSignificantBits();

    public ChartMapException(String message) {
        super(message);
    }
}
