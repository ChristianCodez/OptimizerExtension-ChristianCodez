package cpsc326;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import java.io.*;
import java.util.*;

class OptimizerPerformanceTests {

    // Test Configuration
    private static final int WARMUP_ITERATIONS = 3;
    private static final int MEASURED_ITERATIONS = 10;
    private static final int LOOP_SIZE = 100_000; // Reduced for faster tests

    // Output capture
    private final PrintStream stdout = System.out;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setup() {
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
    }

    @AfterEach
    void teardown() {
        System.setOut(stdout);
    }

    // ----------------------------------------------------------
    // Performance Test Cases
    // ----------------------------------------------------------

    @Test
    void testConstantFoldingPerformance() {
        String program = """
                void main() {
                    var total = 0
                    for i from 0 to %d {
                        total = (2 + 3 * 4)  # Folds to 14
                    }
                    print(total)
                }
                """.formatted(LOOP_SIZE);

        var results = runPerformanceTest(program);
        assertOptimizationImprovement(results);
        assertEquals("14", getOutput()); // Verify correctness
    }

    @RepeatedTest(3)
    void testBitShiftOptimizationPerformance() {
        String program = """
                void main() {
                    var total = 0
                    for i from 0 to %d {
                        total = (i * 8) + (i / 4)  # Should become (i << 3) + (i >> 2)
                    }
                    print(total)
                }
                """.formatted(LOOP_SIZE);

        var results = runPerformanceTest(program);
        assertOptimizationImprovement(results);
    }

    @Test
    void testMixedOptimizationsPerformance() {
        String program = """
                void main() {
                    var total = 0
                    for i from 0 to %d {
                        total = (i * 16) + (3 + 2 * 5) + (i / 8)
                        # Optimizes to: (i << 4) + 13 + (i >> 3)
                    }
                    print(total)
                }
                """.formatted(LOOP_SIZE);

        var results = runPerformanceTest(program);
        assertOptimizationImprovement(results);

    }

    // ----------------------------------------------------------
    // Test Infrastructure
    // ----------------------------------------------------------

    private PerformanceResults runPerformanceTest(String program) {
        // 1. Measure Unoptimized
        long unoptTime = measureExecution(program, false);
        long unoptMemory = measureMemoryUsage(program, false);

        // 2. Measure Optimized
        long optTime = measureExecution(program, true);
        long optMemory = measureMemoryUsage(program, true);

        return new PerformanceResults(
                unoptTime, optTime,
                unoptMemory, optMemory);
    }

    private long measureExecution(String program, boolean optimize) {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            buildAndRun(program, optimize);
        }

        // Timed runs
        long total = 0;
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            long start = System.nanoTime();
            buildAndRun(program, optimize);
            total += System.nanoTime() - start;
        }
        return total / MEASURED_ITERATIONS;
    }

    private long measureMemoryUsage(String program, boolean optimize) {
        System.gc();
        long before = Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory();

        buildAndRun(program, optimize);

        System.gc();
        long after = Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory();

        return after - before;
    }

    private void buildAndRun(String program, boolean optimize) {
        Program p = new ASTParser(new Lexer(istream(program))).parse();
        p.accept(new SemanticChecker());

        if (optimize) {
            p.accept(new ASTOptimizer());
        }

        VM vm = new VM();
        p.accept(new CodeGenerator(vm));
        vm.run();
    }

    private void assertOptimizationImprovement(PerformanceResults results) {
        assertTrue(results.timeSpeedup() >= 1.0,
                "Expected time improvement, got speedup: " + results.timeSpeedup());
        assertTrue(results.memoryRatio() >= 1.0,
                "Expected memory improvement, got ratio: " + results.memoryRatio());

        System.out.printf("""
                Performance Results:
                  Time: %dns -> %dns (%.2fx faster)
                  Memory: %d bytes -> %d bytes (%.2fx better)
                """,
                results.unoptTime, results.optTime, results.timeSpeedup(),
                results.unoptMemory, results.optMemory, results.memoryRatio());
    }

    private String getOutput() {
        return output.toString().replaceAll("\\s+", "");
    }

    private InputStream istream(String str) {
        return new ByteArrayInputStream(str.getBytes());
    }

    private record PerformanceResults(
            long unoptTime, long optTime,
            long unoptMemory, long optMemory) {
        double timeSpeedup() {
            return (double) unoptTime / optTime;
        }

        double memoryRatio() {
            return (double) unoptMemory / optMemory;
        }
    }
}