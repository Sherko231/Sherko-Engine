package com.samo.engine.world.internal;

import com.samo.engine.assets.api.AssetId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Checks the reachable prefab reference graph before resolution can apply overrides or produce scene groups.
 */
final class PrefabCycleValidator {
    private PrefabCycleValidator() {

    }

    static void validate(AssetId rootPrefabAssetId, Map<AssetId, PrefabDocument> sources) {

        Objects.requireNonNull(rootPrefabAssetId, "rootPrefabAssetId");
        Objects.requireNonNull(sources, "sources");
        PrefabDocument root = sources.get(rootPrefabAssetId);
        if (root == null) {
            // Preserve the resolver's established missing-source diagnostic.
            return;
        }

        Set<AssetId> active = new HashSet<>();
        Set<AssetId> completed = new HashSet<>();
        List<AssetId> chain = new ArrayList<>();
        List<String> incomingKeys = new ArrayList<>();
        Deque<Frame> stack = new ArrayDeque<>();
        active.add(rootPrefabAssetId);
        chain.add(rootPrefabAssetId);
        incomingKeys.add(null);
        stack.push(new Frame(rootPrefabAssetId, root));

        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            if (frame.nextInstance == frame.source.instances().size()) {
                stack.pop();
                active.remove(frame.assetId);
                completed.add(frame.assetId);
                chain.removeLast();
                incomingKeys.removeLast();
                continue;
            }

            PrefabInstanceDocument instance = frame.source.instances().get(frame.nextInstance++);
            AssetId target = instance.prefabAssetId();
            if (active.contains(target)) {
                throw new PrefabFormatException(formatChain(chain, incomingKeys, instance.instanceKey(), target));
            }
            if (completed.contains(target)) {
                continue;
            }
            PrefabDocument child = sources.get(target);
            if (child == null) {
                // The existing resolver reports missing references without classifying them as cycles.
                continue;
            }
            active.add(target);
            chain.add(target);
            incomingKeys.add(instance.instanceKey());
            stack.push(new Frame(target, child));
        }

    }

    private static String formatChain(List<AssetId> chain, List<String> incomingKeys, String closingKey, AssetId repeatedId) {

        StringBuilder message = new StringBuilder("cyclic prefab reference: ").append(chain.getFirst());
        for (int i = 1; i < chain.size(); i++) {
            message.append(" --[").append(incomingKeys.get(i)).append("]--> ").append(chain.get(i));
        }
        return message.append(" --[").append(closingKey).append("]--> ").append(repeatedId).toString();

    }

    private static final class Frame {
        private final AssetId assetId;
        private final PrefabDocument source;
        private int nextInstance;

        private Frame(AssetId assetId, PrefabDocument source) {

            this.assetId = assetId;
            this.source = source;

        }
    }
}
