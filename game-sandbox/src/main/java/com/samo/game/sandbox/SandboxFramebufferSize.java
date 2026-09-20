package com.samo.game.sandbox;

final class SandboxFramebufferSize {
    private int width;
    private int height;

    SandboxFramebufferSize(int width, int height) {

        this.width = width;
        this.height = height;

    }

    void update(int width, int height) {

        this.width = width;
        this.height = height;

    }

    int width() {

        return width;

    }

    int height() {

        return height;

    }
}
