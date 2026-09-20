package com.samo.engine.platform.api;

record GlfwDimensions(int width, int height) {
}

record GlfwPosition(int x, int y) {
}

record GlfwVideoMode(int width, int height, int refreshRate) {
}

record GlfwWindowGeometry(int x, int y, int width, int height) {
}

record GlfwMonitorTarget(long handle, GlfwPosition position, GlfwVideoMode videoMode) {
}

record GlfwWindowTransitionPlan(WindowMode mode, Boolean decorated, long monitor, int x, int y, int width, int height, int refreshRate) {
}
