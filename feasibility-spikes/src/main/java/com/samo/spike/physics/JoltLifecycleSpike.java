package com.samo.spike.physics;

import com.github.stephengold.joltjni.BodyCreationSettings;
import com.github.stephengold.joltjni.BodyInterface;
import com.github.stephengold.joltjni.BoxShape;
import com.github.stephengold.joltjni.BroadPhaseLayerInterfaceTable;
import com.github.stephengold.joltjni.JobSystemThreadPool;
import com.github.stephengold.joltjni.Jolt;
import com.github.stephengold.joltjni.ObjVsBpFilter;
import com.github.stephengold.joltjni.ObjectLayerPairFilterTable;
import com.github.stephengold.joltjni.PhysicsSystem;
import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import com.github.stephengold.joltjni.TempAllocatorMalloc;
import com.github.stephengold.joltjni.Vec3;
import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import electrostatic4j.snaploader.LibraryInfo;
import electrostatic4j.snaploader.LoadingCriterion;
import electrostatic4j.snaploader.NativeBinaryLoader;
import electrostatic4j.snaploader.filesystem.DirectoryPath;
import electrostatic4j.snaploader.platform.NativeDynamicLibrary;
import electrostatic4j.snaploader.platform.util.PlatformPredicate;

public final class JoltLifecycleSpike {
    private static final int DEFAULT_CYCLES = 4;
    private static final int STEPS_PER_CYCLE = 600;
    private static final float TIME_STEP_SECONDS = 1.0f / 60.0f;

    private static final int OBJ_LAYER_NON_MOVING = 0;
    private static final int OBJ_LAYER_MOVING = 1;
    private static final int NUM_OBJECT_LAYERS = 2;

    private static final int BP_LAYER_NON_MOVING = 0;
    private static final int BP_LAYER_MOVING = 1;
    private static final int NUM_BROAD_PHASE_LAYERS = 2;

    private JoltLifecycleSpike() {
    }

    public static void main(String[] args) {
        int cycles = Integer.getInteger("spike.cycles", DEFAULT_CYCLES);
        if (cycles < 1) {
            throw new IllegalArgumentException("spike.cycles must be >= 1");
        }

        loadNativeLibrary();
        Jolt.registerDefaultAllocator();
        Jolt.installDefaultTraceCallback();
        Jolt.installDefaultAssertCallback();

        boolean debugBuild = "Debug".equalsIgnoreCase(Jolt.buildType());
        long previousBalance = debugBuild ? allocationBalance() : 0L;

        System.out.printf("Jolt JNI version: %s%n", Jolt.versionString());
        System.out.printf("Native build type: %s%n", Jolt.buildType());
        if (debugBuild) {
            System.out.printf("Initial native allocation balance: %d%n", previousBalance);
        }

        for (int cycle = 1; cycle <= cycles; cycle++) {
            runCycle(cycle);

            if (debugBuild) {
                long currentBalance = allocationBalance();
                long growth = currentBalance - previousBalance;
                System.out.printf(
                        "Cycle %d/%d native allocation balance: %d (delta %+d)%n",
                        cycle, cycles, currentBalance, growth
                );

                if (cycle > 1 && growth > 0) {
                    throw new IllegalStateException(
                            "Native allocation balance increased after cleanup: " + growth
                    );
                }
                previousBalance = currentBalance;
            }
        }

        System.out.printf(
                "P0-T04 passed: %d repeated Jolt start/stop cycles completed.%n",
                cycles
        );
    }

    private static void runCycle(int cycle) {
        if (!Jolt.newFactory()) {
            throw new IllegalStateException("Failed to create Jolt factory");
        }
        Jolt.registerTypes();

        BroadPhaseLayerInterfaceTable layerMap = null;
        ObjectLayerPairFilterTable objectLayerFilter = null;
        ObjVsBpFilter broadPhaseFilter = null;
        PhysicsSystem physicsSystem = null;
        TempAllocatorMalloc tempAllocator = null;
        JobSystemThreadPool jobSystem = null;
        BoxShape floorShape = null;
        BoxShape boxShape = null;
        BodyCreationSettings floorSettings = null;
        BodyCreationSettings boxSettings = null;

        try {
            objectLayerFilter = new ObjectLayerPairFilterTable(NUM_OBJECT_LAYERS);
            objectLayerFilter.enableCollision(OBJ_LAYER_MOVING, OBJ_LAYER_MOVING);
            objectLayerFilter.enableCollision(OBJ_LAYER_MOVING, OBJ_LAYER_NON_MOVING);

            layerMap = new BroadPhaseLayerInterfaceTable(
                    NUM_OBJECT_LAYERS,
                    NUM_BROAD_PHASE_LAYERS
            );
            layerMap.mapObjectToBroadPhaseLayer(
                    OBJ_LAYER_NON_MOVING,
                    BP_LAYER_NON_MOVING
            );
            layerMap.mapObjectToBroadPhaseLayer(
                    OBJ_LAYER_MOVING,
                    BP_LAYER_MOVING
            );

            broadPhaseFilter = new ObjVsBpFilter(
                    NUM_OBJECT_LAYERS,
                    NUM_BROAD_PHASE_LAYERS
            );
            broadPhaseFilter.disablePair(
                    OBJ_LAYER_NON_MOVING,
                    BP_LAYER_NON_MOVING
            );

            physicsSystem = new PhysicsSystem();
            physicsSystem.init(
                    1_024,
                    0,
                    1_024,
                    1_024,
                    layerMap,
                    broadPhaseFilter,
                    objectLayerFilter
            );

            tempAllocator = new TempAllocatorMalloc();
            int workerThreads = Math.max(
                    1,
                    Runtime.getRuntime().availableProcessors() - 1
            );
            jobSystem = new JobSystemThreadPool(
                    Jolt.cMaxPhysicsJobs,
                    Jolt.cMaxPhysicsBarriers,
                    workerThreads
            );

            BodyInterface bodies = physicsSystem.getBodyInterface();

            floorShape = new BoxShape(new Vec3(10.0f, 0.5f, 10.0f));
            floorSettings = new BodyCreationSettings(
                    floorShape,
                    new RVec3(0.0, -0.5, 0.0),
                    new Quat(),
                    EMotionType.Static,
                    OBJ_LAYER_NON_MOVING
            );
            int floorId = bodies.createAndAddBody(
                    floorSettings,
                    EActivation.DontActivate
            );

            boxShape = new BoxShape(new Vec3(0.5f, 0.5f, 0.5f));
            boxSettings = new BodyCreationSettings(
                    boxShape,
                    new RVec3(0.0, 5.0, 0.0),
                    new Quat(),
                    EMotionType.Dynamic,
                    OBJ_LAYER_MOVING
            );
            int boxId = bodies.createAndAddBody(
                    boxSettings,
                    EActivation.Activate
            );

            double startY = bodies.getPosition(boxId).y();
            double lowestY = startY;

            physicsSystem.optimizeBroadPhase();

            for (int step = 0; step < STEPS_PER_CYCLE; step++) {
                physicsSystem.update(
                        TIME_STEP_SECONDS,
                        1,
                        tempAllocator,
                        jobSystem
                );
                lowestY = Math.min(lowestY, bodies.getPosition(boxId).y());
            }

            double finalY = bodies.getPosition(boxId).y();
            if (lowestY >= startY - 0.5) {
                throw new IllegalStateException(
                        "Dynamic box did not fall under gravity."
                );
            }
            if (Math.abs(finalY - 0.5) > 0.15) {
                throw new IllegalStateException(
                        "Dynamic box did not settle on the floor. Final Y=" + finalY
                );
            }

            System.out.printf(
                    "Cycle %d physics result: startY=%.3f, finalY=%.3f%n",
                    cycle,
                    startY,
                    finalY
            );

            bodies.removeBody(boxId);
            bodies.destroyBody(boxId);
            bodies.removeBody(floorId);
            bodies.destroyBody(floorId);
        } finally {
            close(boxSettings);
            close(floorSettings);
            close(boxShape);
            close(floorShape);
            close(jobSystem);
            close(tempAllocator);

            if (physicsSystem != null) {
                physicsSystem.forgetMe();
            }
            close(physicsSystem);
            close(broadPhaseFilter);
            close(objectLayerFilter);
            close(layerMap);

            Jolt.unregisterTypes();
            Jolt.destroyFactory();
        }
    }

    private static long allocationBalance() {
        return (long) Jolt.countNews() - Jolt.countDeletes();
    }

    private static void close(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed to release Jolt native object",
                    exception
            );
        }
    }

    private static void loadNativeLibrary() {
        LibraryInfo info = new LibraryInfo(
                null,
                "joltjni",
                DirectoryPath.USER_DIR
        );
        NativeBinaryLoader loader = new NativeBinaryLoader(info);
        NativeDynamicLibrary[] libraries = {
                new NativeDynamicLibrary(
                        "windows/x86-64/com/github/stephengold",
                        PlatformPredicate.WIN_X86_64
                )
        };
        loader.registerNativeLibraries(libraries).initPlatformLibrary();
        try {
            loader.loadLibrary(LoadingCriterion.CLEAN_EXTRACTION);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load Jolt JNI native library",
                    exception
            );
        }
    }
}
