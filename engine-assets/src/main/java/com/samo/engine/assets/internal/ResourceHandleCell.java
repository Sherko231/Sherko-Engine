package com.samo.engine.assets.internal;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.ResourceHandle;
import com.samo.engine.assets.api.ResourceHandleState;
import java.util.Objects;
import java.util.Optional;

final class ResourceHandleCell<T> implements ResourceHandle<T> {
    private final AssetId assetId;
    private ResourceHandleState state = ResourceHandleState.LOADING;
    private T readyValue;

    ResourceHandleCell(AssetId assetId) {

        this.assetId = Objects.requireNonNull(assetId, "assetId");

    }

    synchronized void completeReady(T value) {

        Objects.requireNonNull(value, "value");
        requireLoadingCompletionState();
        readyValue = value;
        state = ResourceHandleState.READY;

    }

    synchronized boolean tryCompleteReady(T value) {

        Objects.requireNonNull(value, "value");
        if (state != ResourceHandleState.LOADING) {
            return false;
        }
        readyValue = value;
        state = ResourceHandleState.READY;
        return true;

    }

    synchronized boolean tryReplaceReady(T value) {

        Objects.requireNonNull(value, "value");
        if (state != ResourceHandleState.READY) {
            return false;
        }
        readyValue = value;
        return true;

    }

    synchronized void completeFailed() {

        requireLoadingCompletionState();
        state = ResourceHandleState.FAILED;

    }

    synchronized boolean tryCompleteFailed() {

        if (state != ResourceHandleState.LOADING) {
            return false;
        }
        state = ResourceHandleState.FAILED;
        return true;

    }

    @Override
    public AssetId assetId() {

        return assetId;

    }

    @Override
    public synchronized ResourceHandleState state() {

        return state;

    }

    @Override
    public synchronized Optional<T> readyValue() {

        return state == ResourceHandleState.READY ? Optional.of(readyValue) : Optional.empty();

    }

    @Override
    public synchronized T requireReady() {

        if (state != ResourceHandleState.READY) {
            throw new IllegalStateException("Resource " + assetId + " is not ready; current state is " + state);
        }
        return readyValue;

    }

    @Override
    public synchronized void close() {

        if (state == ResourceHandleState.RELEASED) {
            return;
        }
        readyValue = null;
        state = ResourceHandleState.RELEASED;

    }

    private void requireLoadingCompletionState() {

        if (state != ResourceHandleState.LOADING) {
            throw new IllegalStateException("Resource " + assetId + " cannot complete from state " + state);
        }

    }
}
