//
// Copyright (C) 2006 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
// 
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
// 
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.search;


import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.Debug;


/**
 * this is a straight execution pseudo-search - it doesn't search at
 * all (i.e. it doesn't backtrack), but just behaves like a 'normal' VM,
 * going forward() until there is no next state
 *
 * <2do> pcm - of course it doesn't quite behave like a normal VM, since it
 * doesn't honor thread priorities yet (needs a special scheduler)
 *
 * <?> pcm - it's not really clear to me how this differs from a 'PathSearch'
 * other than using a different scheduler. Looks like there should be just one
 *
 */
public class Simulation extends Search {
  
  public Simulation (Config config, JVM vm) throws Config.Exception {
    super(config, vm);

    Debug.println(Debug.WARNING, "Simulation Search");
  }

  public void search () {
    int    depth = 0;

    depth++;

    if (hasPropertyTermination()) {
      return;
    }

    notifySearchStarted();
    
    while (!done) {
      boolean next = vm.forward();

      if (next) {
        if (hasPropertyTermination()) {
          return;
        }

        depth++;
      } else { // no next state

        // <2do> we could check for more things here. If the last insn wasn't
        // the main return, or a System.exit() call, we could flag a JPFException
        isPropertyViolated();
        done = true;
      }
    }
    notifySearchFinished();
  }
}
