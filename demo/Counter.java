package demo;

class Counter {
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