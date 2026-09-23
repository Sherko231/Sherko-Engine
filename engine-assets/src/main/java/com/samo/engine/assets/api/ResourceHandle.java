package com.samo.engine.assets.api;

import java.util.Optional;

/**
 * Read-only typed runtime resource view with explicit local release semantics.
 *
 * @param <T>
 *            typed Java resource value exposed when the handle is ready
 */
public interface ResourceHandle<T> extends AutoCloseable {
    /**
     * Returns the stable identity represented by this handle.
     *
     * @return asset identity
     */
    AssetId assetId();

    /**
     * Returns the current observable lifecycle state.
     *
     * @return current state
     */
    ResourceHandleState state();

    /**
     * Returns the typed value only while the handle is ready.
     *
     * @return ready value, or empty in every non-ready state
     */
    Optional<T> readyValue();

    /**
     * Returns the typed value when ready.
     *
     * @return ready resource value
     * @throws IllegalStateException
     *             if the handle is not ready
     */
    T requireReady();

    /**
     * Releases this handle view. Repeated calls are harmless.
     */
    @Override
    void close();
}
