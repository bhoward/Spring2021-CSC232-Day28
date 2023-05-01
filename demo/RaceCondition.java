package demo;

public class RaceCondition {
    private static final int STEPS = 1000000;

    /**
     * Example of threads using unsafe access to shared data. The output is unlikely
     * to be zero with multiple threads.
     */
    public static void runBad() {
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
    public static void runGood() {
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
}
