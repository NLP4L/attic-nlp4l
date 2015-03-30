/*
 * Copyright 2015 RONDHUIT Co.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
