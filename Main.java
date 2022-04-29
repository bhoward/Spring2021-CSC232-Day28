import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
	private static final int STEPS = 1000000;
	private static final int BLOCK_SIZE = 1000000;
	private static final int LONG_LENGTH = 12;

	public static void main(String[] args) {
		raceCondition();
		noRaceCondition();
		computeBound();
		ioBound();
	}

	/**
	 * Example of threads using unsafe access to shared data. The output is unlikely
	 * to be zero with multiple processors.
	 */
	public static void raceCondition() {
		Counter counter = new Counter();

		Thread t1 = new Thread(() -> {
			for (int i = 0; i < STEPS; i++) {
				counter.unsafeIncrement();
			}
		});

		Thread t2 = new Thread(() -> {
			for (int i = 0; i < STEPS; i++) {
				counter.unsafeDecrement();
			}
		});

		t1.start();
		t2.start();

		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Unsafe result = " + counter.getCount());
	}

	/**
	 * Example of threads using safe access to shared data. The output is guaranteed
	 * to be zero.
	 */
	public static void noRaceCondition() {
		Counter counter = new Counter();

		Thread t1 = new Thread(() -> {
			for (int i = 0; i < STEPS; i++) {
				counter.safeIncrement();
			}
		});

		Thread t2 = new Thread(() -> {
			for (int i = 0; i < STEPS; i++) {
				counter.safeDecrement();
			}
		});

		t1.start();
		t2.start();

		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Safe result = " + counter.getCount());
	}

	private static class Counter {
		private int count = 0;

		public int getCount() {
			return count;
		}

		public void unsafeIncrement() {
			count++;
		}

		public void unsafeDecrement() {
			count--;
		}

		public synchronized void safeIncrement() {
			count++;
		}

		public synchronized void safeDecrement() {
			count--;
		}
	}

	/**
	 * An example of a compute-bound computation that uses a fixed-size thread pool
	 * to spread the work across all available processors. Generates the product of
	 * two large primes, then searches for the factorization (simulating an attempt
	 * at breaking RSA encryption).
	 */
	private static void computeBound() {
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
	private static Callable<Long> factorSearch(long product, long from, long to) {
		return () -> {
			for (long blockStart = from; blockStart < to; blockStart += BLOCK_SIZE) {
				long blockEnd = Math.min(blockStart + BLOCK_SIZE, to);

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

	/**
	 * An example of an IO-bound computation that uses a cached thread pool to try
	 * to keep busy while waiting for file or network access. Based on the "long
	 * word count" example from Horstmann, Section 9.1.2, but with data sources
	 * coming from both local files and web URLs.
	 */
	private static void ioBound() {
		List<Source> sources = new ArrayList<>();
		sources.add(new FileSource("Alice in Wonderland", "data/alice30.txt"));
		sources.add(new FileSource("Count of Monte Cristo", "data/crsto10.txt"));
		sources.add(new FileSource("War and Peace", "data/war-and-peace.txt"));
		sources.add(new URLSource("Frankenstein", "https://www.gutenberg.org/files/84/84-0.txt"));
		sources.add(new URLSource("Pride and Prejudice", "https://www.gutenberg.org/files/1342/1342-0.txt"));
		sources.add(new URLSource("Great Gatsby", "https://www.gutenberg.org/files/64317/64317-0.txt"));

		ExecutorService service = Executors.newCachedThreadPool();
		List<Callable<Integer>> tasks = new ArrayList<>();
		for (Source source : sources) {
			tasks.add(() -> {
				System.out.println("Starting " + source.getDescription());

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(source.getInputStream()))) {
					// Use the Stream library to process the lines from the source
					int result = reader.lines().mapToInt(line -> {
						String[] words = line.split("[\\PL]+");
						int count = 0;
						for (String word : words) {
							if (word.length() >= LONG_LENGTH)
								count++;
						}
						return count;
					}).sum();

					System.out.println("Ending " + source.getDescription());
					return result;
				} catch (IOException e) {
					e.printStackTrace();
					return 0;
				}
			});
		}

		// Add up all of the totals returned from the individual tasks
		try {
			List<Future<Integer>> resultFutures = service.invokeAll(tasks);

			int total = 0;
			for (Future<Integer> resultFuture : resultFutures) {
				total += resultFuture.get();
			}
			System.out.println("Found " + total + " long words");
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}

		service.shutdown();
	}

	/**
	 * An abstraction around data sources from which one may get an InputStream. A
	 * Source also has a String description for display purposes.
	 * 
	 * @author bhoward
	 */
	private static interface Source {
		String getDescription();

		InputStream getInputStream() throws IOException;
	}

	/**
	 * A data source that reads from a local file.
	 * 
	 * @author bhoward
	 */
	private static class FileSource implements Source {
		private String description;
		private String fileName;

		public FileSource(String description, String fileName) {
			this.description = description;
			this.fileName = fileName;
		}

		public String getDescription() {
			return description;
		}

		public InputStream getInputStream() throws IOException {
			return new FileInputStream(fileName);
		}
	}

	/**
	 * A data source that reads from a web URL.
	 * 
	 * Based in part on code from David Eck,
	 * http://math.hws.edu/eck/cs124/javanotes8/c11/s4.html
	 * 
	 * @author bhoward
	 */
	private static class URLSource implements Source {
		private String description;
		private String urlString;

		public URLSource(String description, String urlString) {
			this.description = description;
			this.urlString = urlString;
		}

		public String getDescription() {
			return description;
		}

		public InputStream getInputStream() throws IOException {
			URL url = new URL(urlString);
			URLConnection connection = url.openConnection();
			return connection.getInputStream();
		}
	}
}
