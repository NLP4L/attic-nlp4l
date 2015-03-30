package org.nlp4l.gui;

import java.io.IOException;
import java.io.OutputStream;

public abstract class Presentation {

  protected int port;
  protected boolean debug;

  public void setup(int port, boolean debug){
    this.port = port;
    this.debug = debug;
  }

  public void start(){
    System.out.println("WARNING: This function is experimental and might change in incompatible ways in the future release.");
    System.out.printf("\nTo see the chart, access this URL -> http://localhost:%d/chart\n", port);
    System.out.printf("To shutdown the server, access this URL -> http://localhost:%d/shutdown\n\n", port);
  }

  public abstract int length();

  public abstract void execute(String path, OutputStream out) throws IOException;

  public void shutdown(){
    System.out.println("Bye!");
  }
}
