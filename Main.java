import demo.ComputeBound;
import demo.IOBound;
import demo.RaceCondition;

public class Main {
    public static void main(String[] args) {
	RaceCondition.runBad();
	RaceCondition.runGood();
	ComputeBound.run();
	IOBound.ioBound();
    }
}
