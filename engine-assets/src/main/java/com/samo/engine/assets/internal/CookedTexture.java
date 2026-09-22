package com.samo.engine.assets.internal;

import java.util.List;

record CookedTexture(List<TextureMipLevel> mipLevels) {
    CookedTexture {
        if (mipLevels == null || mipLevels.isEmpty()) {
            throw new IllegalArgumentException("Cooked texture requires at least one mip level");
        }
        mipLevels = List.copyOf(mipLevels);
    }

    int width() {

        return mipLevels.getFirst().width();

    }

    int height() {

        return mipLevels.getFirst().height();

    }
}
