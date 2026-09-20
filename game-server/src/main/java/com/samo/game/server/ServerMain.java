package com.samo.game.server;

import com.samo.game.server.internal.ServerVersionReport;

public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) {
        if (args.length == 1 && "--version".equals(args[0])) {
            ServerVersionReport.print();
            return;
        }

        System.out.println("Sherko Engine game-server foundation started in headless mode.");
        System.out.println("Sherko Engine game-server foundation stopped cleanly.");
    }
}
