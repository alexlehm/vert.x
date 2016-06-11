/**
 * 
 */
package io.vertx.core.impl.launcher.commands;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class LogCaptureHandler extends Handler {

  private static ByteArrayOutputStream os;
  private static PrintStream ps;

  /**
   * @param name
   */
  public LogCaptureHandler() {
    System.out.println("constructor called");
    flush();
  }

  /* (non-Javadoc)
   * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
   */
  @Override
  public void publish(LogRecord logRecord) {
    ps.println(logRecord.getMessage());
    System.err.println("LOG:"+logRecord.getMessage());
  }

  /* (non-Javadoc)
   * @see java.util.logging.Handler#flush()
   */
  @Override
  public void flush() {
    os = new ByteArrayOutputStream();
    ps = new PrintStream(os);
  }

  /* (non-Javadoc)
   * @see java.util.logging.Handler#close()
   */
  @Override
  public void close() throws SecurityException {
    flush();
  }

  public static String getLog() {
    return os == null ? "" : os.toString();
  }

}
