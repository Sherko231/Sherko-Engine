package com.samo.game.client;

import com.samo.game.client.internal.ClientVersionReport;

public final class ClientMain {
    private ClientMain() {
    }

    public static void main(String[] args) {
        if (args.length == 1 && "--version".equals(args[0])) {
            ClientVersionReport.print();
            return;
        }

        System.out.println("Sherko Engine game-client foundation started.");
        System.out.println("Sherko Engine game-client foundation stopped cleanly.");
    }
}
