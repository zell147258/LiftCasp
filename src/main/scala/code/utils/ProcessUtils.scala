package code.utils

/**
 * Created by ivan on 06/01/14.
 */
case class ProcessUtils(command: String, timeoutInSeconds: Float = 0) {
  val p = Runtime.getRuntime().exec(command)

  def isAlive(p: Process): Boolean = {
    try {
      p.exitValue()
      return false
    } catch {
      case e: IllegalThreadStateException =>
        return true
    }

  }

  def run(): Process = {

    if (timeoutInSeconds <= 0) {
      p.waitFor()
    }
    else {
      val now = System.currentTimeMillis();
      val timeoutInMillis = 1000L * timeoutInSeconds;
      val finish = now + timeoutInMillis;
      while (isAlive(p) && (System.currentTimeMillis() < finish)) {
        Thread.sleep(10);
      }

    }
    p
  }

}
