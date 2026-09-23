package com.samo.engine.assets.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.samo.engine.assets.api.AssetId;
import com.samo.engine.assets.api.ResourceHandleState;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ResourceHandleCellTest {
    private static final AssetId ASSET_ID = AssetId.parse("01234567-89ab-cdef-fedc-ba9876543210");

    @Test
    void startsLoadingAndRequiresNonNullIdentity() {

        assertThatThrownBy(() -> new ResourceHandleCell<String>(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("assetId");

        ResourceHandleCell<String> handle = new ResourceHandleCell<>(ASSET_ID);
        assertThat(handle.assetId()).isEqualTo(ASSET_ID);
        assertThat(handle.state()).isEqualTo(ResourceHandleState.LOADING);
        assertThat(handle.readyValue()).isEmpty();
        assertThatThrownBy(handle::requireReady).isInstanceOf(IllegalStateException.class).hasMessageContaining(ASSET_ID.toString()).hasMessageContaining("LOADING");

    }

    @Test
    void completesReadyWithExactTypedValueAndRejectsDuplicateCompletion() {

        Object value = new Object();
        ResourceHandleCell<Object> handle = new ResourceHandleCell<>(ASSET_ID);

        handle.completeReady(value);

        assertThat(handle.state()).isEqualTo(ResourceHandleState.READY);
        assertThat(handle.readyValue()).containsSame(value);
        assertThat(handle.requireReady()).isSameAs(value);
        assertThatThrownBy(() -> handle.completeReady(new Object())).isInstanceOf(IllegalStateException.class).hasMessageContaining("READY");
        assertThatThrownBy(handle::completeFailed).isInstanceOf(IllegalStateException.class).hasMessageContaining("READY");
        assertThat(handle.requireReady()).isSameAs(value);

    }

    @Test
    void completesFailedWithoutValueAndRejectsLaterCompletion() {

        ResourceHandleCell<String> handle = new ResourceHandleCell<>(ASSET_ID);

        handle.completeFailed();

        assertThat(handle.state()).isEqualTo(ResourceHandleState.FAILED);
        assertThat(handle.readyValue()).isEmpty();
        assertThatThrownBy(handle::requireReady).isInstanceOf(IllegalStateException.class).hasMessageContaining("FAILED");
        assertThatThrownBy(() -> handle.completeReady("late")).isInstanceOf(IllegalStateException.class).hasMessageContaining("FAILED");
        assertThatThrownBy(handle::completeFailed).isInstanceOf(IllegalStateException.class).hasMessageContaining("FAILED");

    }

    @Test
    void rejectsNullReadyWithoutMutation() {

        ResourceHandleCell<String> handle = new ResourceHandleCell<>(ASSET_ID);

        assertThatThrownBy(() -> handle.completeReady(null)).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
        assertThat(handle.state()).isEqualTo(ResourceHandleState.LOADING);
        assertThat(handle.readyValue()).isEmpty();

    }

    @Test
    void closeIsIdempotentFromLoadingReadyAndFailedAndReleasedIsTerminal() {

        ResourceHandleCell<String> loading = new ResourceHandleCell<>(ASSET_ID);
        loading.close();
        loading.close();
        assertReleased(loading);
        assertThatThrownBy(() -> loading.completeReady("late")).isInstanceOf(IllegalStateException.class).hasMessageContaining("RELEASED");
        assertThatThrownBy(loading::completeFailed).isInstanceOf(IllegalStateException.class).hasMessageContaining("RELEASED");

        ResourceHandleCell<String> ready = new ResourceHandleCell<>(ASSET_ID);
        ready.completeReady("ready");
        ready.close();
        ready.close();
        assertReleased(ready);

        ResourceHandleCell<String> failed = new ResourceHandleCell<>(ASSET_ID);
        failed.completeFailed();
        failed.close();
        failed.close();
        assertReleased(failed);

    }

    @Test
    void releasingReadyHandleClearsRetainedValue() throws Exception {

        Object value = new Object();
        ResourceHandleCell<Object> handle = new ResourceHandleCell<>(ASSET_ID);
        handle.completeReady(value);

        handle.close();

        assertReleased(handle);
        Field readyValue = ResourceHandleCell.class.getDeclaredField("readyValue");
        readyValue.setAccessible(true);
        assertThat(readyValue.get(handle)).isNull();

    }

    @Test
    void concurrentObservationCompletionAndReleaseNeverExposeImpossibleValueStatePair() throws Exception {

        for (int iteration = 0; iteration < 200; iteration++) {
            ResourceHandleCell<String> handle = new ResourceHandleCell<>(ASSET_ID);
            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            List<Thread> threads = new ArrayList<>();

            Thread completer = Thread.ofPlatform().unstarted(() -> {
                await(start);
                try {
                    handle.completeReady("value");
                } catch (IllegalStateException exception) {
                    if (handle.state() != ResourceHandleState.RELEASED) {
                        failure.compareAndSet(null, exception);
                    }
                } catch (Throwable throwable) {
                    failure.compareAndSet(null, throwable);
                }
            });
            threads.add(completer);

            Thread releaser = Thread.ofPlatform().unstarted(() -> {
                await(start);
                try {
                    handle.close();
                } catch (Throwable throwable) {
                    failure.compareAndSet(null, throwable);
                }
            });
            threads.add(releaser);

            Thread observer = Thread.ofPlatform().unstarted(() -> {
                await(start);
                try {
                    for (int sample = 0; sample < 200; sample++) {
                        ResourceHandleState before = handle.state();
                        boolean present = handle.readyValue().isPresent();
                        ResourceHandleState after = handle.state();
                        if (before != after) {
                            continue;
                        }
                        if (before == ResourceHandleState.READY && !present) {
                            throw new AssertionError("Stable READY observation had no value");
                        }
                        if (before != ResourceHandleState.READY && present) {
                            throw new AssertionError("Stable non-READY observation exposed a value");
                        }
                    }
                } catch (Throwable throwable) {
                    failure.compareAndSet(null, throwable);
                }
            });
            threads.add(observer);

            threads.forEach(Thread::start);
            start.countDown();
            for (Thread thread : threads) {
                thread.join();
            }

            assertThat(failure.get()).isNull();
            assertThat(handle.state()).isIn(ResourceHandleState.READY, ResourceHandleState.RELEASED);
        }

    }

    private static void assertReleased(ResourceHandleCell<?> handle) {

        assertThat(handle.state()).isEqualTo(ResourceHandleState.RELEASED);
        assertThat(handle.readyValue()).isEmpty();
        assertThatThrownBy(handle::requireReady).isInstanceOf(IllegalStateException.class).hasMessageContaining("RELEASED");

    }

    private static void await(CountDownLatch latch) {

        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }

    }
    @Test
    void asyncTryCompletionCannotResurrectReleasedHandle() {

        ResourceHandleCell<String> handle = new ResourceHandleCell<>(ASSET_ID);
        handle.close();

        assertThat(handle.tryCompleteReady("late")).isFalse();
        assertThat(handle.tryCompleteFailed()).isFalse();
        assertThat(handle.state()).isEqualTo(ResourceHandleState.RELEASED);
        assertThat(handle.readyValue()).isEmpty();

    }

}
