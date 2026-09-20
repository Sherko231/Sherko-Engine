package com.samo.engine.render.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.samo.engine.core.api.DebugFrame;
import com.samo.engine.core.api.DebugLine;
import com.samo.engine.core.api.DebugColor;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class RenderFramePacketTest {
    @Test
    void snapshotsMutableSourceMatrices() {
        Matrix4f sourceView = new Matrix4f().translation(1.0f, 2.0f, 3.0f);
        Matrix4f sourceProjection = new Matrix4f().scaling(2.0f, 3.0f, 4.0f);

        RenderFramePacket packet = new RenderFramePacket(sourceView, sourceProjection, 1280, 720);

        sourceView.identity();
        sourceProjection.identity();

        Matrix4f copiedView = packet.copyViewTo(new Matrix4f());
        Matrix4f copiedProjection = packet.copyProjectionTo(new Matrix4f());

        assertEquals(1.0f, copiedView.m30());
        assertEquals(2.0f, copiedView.m31());
        assertEquals(3.0f, copiedView.m32());
        assertEquals(2.0f, copiedProjection.m00());
        assertEquals(3.0f, copiedProjection.m11());
        assertEquals(4.0f, copiedProjection.m22());
        assertEquals(1280, packet.framebufferWidth());
        assertEquals(720, packet.framebufferHeight());
    }

    @Test
    void copiedMatricesCannotMutatePacket() {
        RenderFramePacket packet = new RenderFramePacket(new Matrix4f().translation(4.0f, 5.0f, 6.0f), new Matrix4f().scaling(2.0f), 800, 600);

        Matrix4f firstViewCopy = packet.copyViewTo(new Matrix4f());
        Matrix4f firstProjectionCopy = packet.copyProjectionTo(new Matrix4f());
        firstViewCopy.identity();
        firstProjectionCopy.identity();

        Matrix4f secondViewCopy = packet.copyViewTo(new Matrix4f());
        Matrix4f secondProjectionCopy = packet.copyProjectionTo(new Matrix4f());

        assertEquals(4.0f, secondViewCopy.m30());
        assertEquals(5.0f, secondViewCopy.m31());
        assertEquals(6.0f, secondViewCopy.m32());
        assertEquals(2.0f, secondProjectionCopy.m00());
        assertEquals(2.0f, secondProjectionCopy.m11());
        assertEquals(2.0f, secondProjectionCopy.m22());
    }

    @Test
    void snapshotsOrderedLocalLightsWithoutRetainingCallerList() {
        RenderPointLight first = new RenderPointLight(1.0f, 0.0f, 1.0f, 1.0f, 0.5f, 0.25f, 0.5f, 5.0f);
        RenderSpotLight second = new RenderSpotLight(-1.0f, 0.0f, 1.0f, 0.0f, 0.0f, -1.0f, 0.25f, 0.5f, 1.0f, 0.75f, 6.0f, 0.2f, 0.5f);
        ArrayList<RenderLocalLight> source = new ArrayList<>(List.of(first, second));

        RenderFramePacket packet = new RenderFramePacket(new Matrix4f(), new Matrix4f(), 800, 600, source);
        source.clear();

        assertEquals(List.of(first, second), packet.localLights());
        assertThrows(UnsupportedOperationException.class, () -> packet.localLights().add(first));
    }

    @Test
    void carriesImmutableDebugFrameAndLegacyPathsUseEmptyDebugFrame() {
        DebugFrame debugFrame = new DebugFrame(List.of(new DebugLine(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, new DebugColor(0.0f, 1.0f, 0.0f))), List.of());
        RenderFramePacket packet = new RenderFramePacket(new Matrix4f(), new Matrix4f(), 800, 600, List.of(), debugFrame);

        assertEquals(debugFrame, packet.debugFrame());
        assertEquals(DebugFrame.EMPTY, new RenderFramePacket(new Matrix4f(), new Matrix4f(), 800, 600).debugFrame());
        assertEquals(DebugFrame.EMPTY, new RenderFramePacket(new Matrix4f(), new Matrix4f(), 800, 600, List.of()).debugFrame());
    }

    @Test
    void legacyConstructorProducesEmptyLocalLightList() {
        RenderFramePacket packet = new RenderFramePacket(new Matrix4f(), new Matrix4f(), 800, 600);

        assertTrue(packet.localLights().isEmpty());
    }

    @Test
    void rejectsInvalidRequiredValues() {
        Matrix4f valid = new Matrix4f();

        assertThrows(NullPointerException.class, () -> new RenderFramePacket(null, valid, 800, 600));
        assertThrows(NullPointerException.class, () -> new RenderFramePacket(valid, null, 800, 600));
        assertThrows(IllegalArgumentException.class, () -> new RenderFramePacket(valid, valid, 0, 600));
        assertThrows(IllegalArgumentException.class, () -> new RenderFramePacket(valid, valid, 800, -1));
        assertThrows(NullPointerException.class, () -> new RenderFramePacket(valid, valid, 800, 600, null));
        assertThrows(NullPointerException.class, () -> new RenderFramePacket(valid, valid, 800, 600, java.util.Arrays.asList((RenderLocalLight) null)));
        assertThrows(NullPointerException.class, () -> new RenderFramePacket(valid, valid, 800, 600, List.of(), null));

        Matrix4f nonFinite = new Matrix4f();
        nonFinite.m00(Float.NaN);
        assertThrows(IllegalArgumentException.class, () -> new RenderFramePacket(nonFinite, valid, 800, 600));
    }
}
