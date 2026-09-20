import java.util.*;

final class InvalidCheckstyleFixture {
    void demonstrateInvalidPatterns() {

        try {
            java.lang.Math.max(1, 2);
        } catch (RuntimeException exception) {
        }

    }
}
