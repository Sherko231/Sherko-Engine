package com.samo.engine.assets.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResourceHandleTest {
    @Test
    void publicContractIsTypedAndContainsNoProducerOrNativeSurface() {

        assertThat(ResourceHandleState.values()).containsExactly(
            ResourceHandleState.LOADING,
            ResourceHandleState.READY,
            ResourceHandleState.FAILED,
            ResourceHandleState.RELEASED);

        Set<String> methodNames = Set.of("assetId", "state", "readyValue", "requireReady", "close");
        assertThat(ResourceHandle.class.getDeclaredMethods()).extracting(Method::getName).containsExactlyInAnyOrderElementsOf(methodNames);

        assertThat(ResourceHandle.class.getDeclaredMethods()).allSatisfy(method -> {
            Class<?> returnType = method.getReturnType();
            assertThat(returnType).isNotEqualTo(long.class).isNotEqualTo(int.class).isNotEqualTo(ByteBuffer.class);
            assertThat(returnType.getName()).doesNotStartWith("org.lwjgl").doesNotContain("opengl").doesNotContain("openal");
        });

    }
}
