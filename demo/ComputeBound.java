package demo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalLong;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.LongStream;

public class ComputeBound {
    static final int BLOCK_SIZE = 1000000;

    /**
     * An example of a compute-bound computation that uses a fixed-size thread pool
     * to spread the work across all available processors. Generates the product of
     * two large primes, then searches for the factorization (simulating an attempt
     * at breaking RSA encryption).
     */
    public static void run() {
	final int BITS = 31; // must be <= 31 so that the product will fit in a long
	final long MAX = 1L << BITS;
	final long MIN = MAX / 2;

	Random random = new Random();
	long p1 = BigInteger.probablePrime(BITS, random).longValue();
	long p2 = BigInteger.probablePrime(BITS, random).longValue();
	long product = p1 * p2;
	System.out.println("Prime 1 = " + p1);
	System.out.println("Prime 2 = " + p2);
	System.out.println("Factoring " + product);

	// Search with one processor
	System.out.println("Using 1 processor");
	Callable<Long> search = factorSearch(product, MIN, MAX);
	try {
	    long start = System.currentTimeMillis();
	    long factor = search.call();
	    long stop = System.currentTimeMillis();
	    System.out.println("Found factor " + factor);
	    System.out.println("Time taken = " + (stop - start) + " ms");
	} catch (Exception e) {
	    e.printStackTrace();
	}

	// Search with all processors, dividing the range evenly into batches
	int processors = Runtime.getRuntime().availableProcessors();
	ExecutorService service = Executors.newFixedThreadPool(processors);
	System.out.println("Using " + processors + " processors");

	List<Callable<Long>> tasks = new ArrayList<>();
	long batchSize = (MAX - MIN) / processors;
	for (int i = 0; i < processors; i++) {
	    long from = MIN + i * batchSize;
	    long to = from + batchSize;
	    tasks.add(factorSearch(product, from, to));
	}

	try {
	    long start = System.currentTimeMillis();
	    long factor = service.invokeAny(tasks);
	    long stop = System.currentTimeMillis();
	    System.out.println("Found factor " + factor);
	    System.out.println("Time taken = " + (stop - start) + " ms");
	} catch (Exception e) {
	    e.printStackTrace();
	}
	service.shutdown();

	// Search with parallel stream
	try {
	    System.out.println("Using parallel stream");

	    long start = System.currentTimeMillis();
	    OptionalLong factor = LongStream.range(MIN, MAX).parallel().filter(n -> product % n == 0).findFirst();
	    long stop = System.currentTimeMillis();
	    System.out.println("Found factor " + factor.getAsLong());
	    System.out.println("Time taken = " + (stop - start) + " ms");
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Construct a Callable that will search through the range [from, to) to find a
     * factor of the given product. Checks for thread interruption after every block
     * (specified by BLOCK_SIZE). If no factor found in the range, throw
     * NoSuchElementException.
     * 
     * @param product
     * @param from
     * @param to
     * @return
     */
    static Callable<Long> factorSearch(long product, long from, long to) {
	return () -> {
	    for (long blockStart = from; blockStart < to; blockStart += ComputeBound.BLOCK_SIZE) {
		long blockEnd = Math.min(blockStart + ComputeBound.BLOCK_SIZE, to);

		// Search one block
		for (long factor = blockStart; factor < blockEnd; factor++) {
		    if (product % factor == 0) {
			return factor;
		    }
		}

		// Check for thread interruption (throws InterruptedException if so)
		Thread.sleep(0);
	    }

	    // Factor not found in range
	    throw new NoSuchElementException();
	};
    }
}
