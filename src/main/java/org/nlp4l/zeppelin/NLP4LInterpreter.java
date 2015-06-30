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

package org.nlp4l.zeppelin;

import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterUtils;
import org.nlp4l.repl.NLP4LILoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Console;
import scala.Some;
import scala.tools.nsc.Settings;
import scala.tools.nsc.interpreter.IMain;
import scala.tools.nsc.settings.MutableSettings;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class NLP4LInterpreter extends Interpreter {

  Logger logger = LoggerFactory.getLogger(NLP4LInterpreter.class);

  private ByteArrayOutputStream out;
  private PrintStream printStream;
  private NLP4LILoop interpreter;
  private IMain intp;

  static {
    Interpreter.register("nlp4l", NLP4LInterpreter.class.getName());
  }

  public NLP4LInterpreter(Properties property) {
    super(property);
    //logger.info("*** NLP4LInterpreter() has been called");
    out = new ByteArrayOutputStream();
  }


  @Override
  public void open() {
    //logger.info("*** open() has been called");

    Settings settings = new Settings();


    // set classpath for scala compiler
    MutableSettings.PathSetting pathSettings = settings.classpath();
    String classpath = "";
    List<File> paths = currentClassPath();
    for (File f : paths) {
      if (classpath.length() > 0) {
        classpath += File.pathSeparator;
      }
      classpath += f.getAbsolutePath();
    }

    pathSettings.v_$eq(classpath);
    settings.scala$tools$nsc$settings$ScalaSettings$_setter_$classpath_$eq(pathSettings);


    // set classloader for scala compiler
    settings.explicitParentLoader_$eq(new Some<ClassLoader>(Thread.currentThread()
            .getContextClassLoader()));
    MutableSettings.BooleanSetting b = (MutableSettings.BooleanSetting) settings.usejavacp();
    b.v_$eq(true);
    settings.scala$tools$nsc$settings$StandardScalaSettings$_setter_$usejavacp_$eq(b);

    printStream = new PrintStream(out);
    interpreter = new NLP4LILoop((BufferedReader)null, new PrintWriter(out));

    interpreter.settings_$eq(settings);
    interpreter.createInterpreter();
    intp = interpreter.intp();
    intp.setContextClassLoader();
    intp.initializeSynchronous();

    // see repl.init
    intp.interpret("import org.nlp4l.core._");
    intp.interpret("import org.nlp4l.core.analysis._");
    intp.interpret("import org.nlp4l.lm._");
    intp.interpret("import org.nlp4l.stats._");
    intp.interpret("import org.nlp4l.gui._");
    intp.interpret("import org.nlp4l.repl.NLP4L");
    intp.interpret("import org.nlp4l.repl.NLP4L._");
    intp.interpret("import org.nlp4l.repl.Corpora");
    intp.interpret("import org.nlp4l.repl.Corpora._");
    intp.interpret("import org.nlp4l.repl.ZeppelinVisualizer");
    intp.interpret("import org.nlp4l.repl.ZeppelinVisualizer._");
  }

  private List<File> currentClassPath() {
    List<File> paths = classPath(Thread.currentThread().getContextClassLoader());
    String[] cps = System.getProperty("java.class.path").split(File.pathSeparator);
    if (cps != null) {
      for (String cp : cps) {
        paths.add(new File(cp));
      }
    }
    return paths;
  }

  private List<File> classPath(ClassLoader cl) {
    List<File> paths = new LinkedList<File>();
    if (cl == null) {
      return paths;
    }

    if (cl instanceof URLClassLoader) {
      URLClassLoader ucl = (URLClassLoader) cl;
      URL[] urls = ucl.getURLs();
      if (urls != null) {
        for (URL url : urls) {
          paths.add(new File(url.getFile()));
        }
      }
    }
    return paths;
  }

  @Override
  public void close() {
    //logger.info("*** close() has been called");
    intp.close();
  }

  @Override
  public InterpreterResult interpret(String line, InterpreterContext context) {
    if (line == null || line.trim().length() == 0) {
      return new InterpreterResult(InterpreterResult.Code.SUCCESS);
    }
    return interpret(line.split("\n"));
  }

  public InterpreterResult interpret(String[] lines) {
    synchronized (this) {
      InterpreterResult r = interpretInput(lines);
      return r;
    }
  }

  public InterpreterResult interpretInput(String[] lines) {
    // add print("") to make sure not finishing with comment
    // see https://github.com/NFLabs/zeppelin/issues/151
    String[] linesToRun = new String[lines.length + 1];
    for (int i = 0; i < lines.length; i++) {
      linesToRun[i] = lines[i];
    }
    linesToRun[lines.length] = "print(\"\")";

    Console.setOut(printStream);
    out.reset();
    InterpreterResult.Code r = null;
    String incomplete = "";
    for (String s : linesToRun) {
      scala.tools.nsc.interpreter.Results.Result res = null;
      try {
        res = intp.interpret(incomplete + s);
      } catch (Exception e) {
        logger.info("Interpreter exception", e);
        return new InterpreterResult(InterpreterResult.Code.ERROR, InterpreterUtils.getMostRelevantMessage(e));
      }

      r = getResultCode(res);

      if (r == InterpreterResult.Code.ERROR) {
        return new InterpreterResult(r, out.toString());
      } else if (r == InterpreterResult.Code.INCOMPLETE) {
        incomplete += s + "\n";
      } else {
        incomplete = "";
      }
    }

    if (r == InterpreterResult.Code.INCOMPLETE) {
      return new InterpreterResult(r, "Incomplete expression");
    } else {
      // hack for Zeppelin table
      String outstr = out.toString();
      int start = outstr.indexOf("%table ");
      return new InterpreterResult(r, start >= 0 ? outstr.substring(start) : outstr);
    }
  }

  private InterpreterResult.Code getResultCode(scala.tools.nsc.interpreter.Results.Result r) {
    if (r instanceof scala.tools.nsc.interpreter.Results.Success$) {
      return InterpreterResult.Code.SUCCESS;
    } else if (r instanceof scala.tools.nsc.interpreter.Results.Incomplete$) {
      return InterpreterResult.Code.INCOMPLETE;
    } else {
      return InterpreterResult.Code.ERROR;
    }
  }

  @Override
  public void cancel(InterpreterContext interpreterContext) {
    //logger.info("*** cancel() has been called");
  }

  @Override
  public Interpreter.FormType getFormType() {
    //logger.info("*** getFormType() has been called");
    return Interpreter.FormType.SIMPLE;
  }

  @Override
  public int getProgress(InterpreterContext interpreterContext) {
    //logger.info("*** getProgress() has been called");
    return 0;
  }

  @Override
  public List<String> completion(String s, int i) {
    //logger.info("*** completion() has been called");
    return null;
  }
}
