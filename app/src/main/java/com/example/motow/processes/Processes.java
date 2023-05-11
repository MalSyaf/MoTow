package com.example.motow.processes;

import java.io.Serializable;

public class Processes implements Serializable {

    public String processId;

    public Processes(String processId) {
        this.processId = processId;
    }

    public Processes() {
        //
    }
}
