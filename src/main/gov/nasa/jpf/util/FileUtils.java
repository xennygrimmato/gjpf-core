//
// Copyright (C) 2010 United States Government as represented by the
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
package gov.nasa.jpf.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * utility class to find all files matching (possibly hierarchical)
 * wildcard path specs
 *
 * we support single '*' wildcards as in filename matching, plus "**" patterns
 * that match all (recursive) subdirectories
 */
// example:  List<File> list = findMatches("/U*/p*/tmp/**/*.java");

public class FileUtils {

  public static boolean containsWildcards (String pattern) {
    return (pattern.indexOf('*') >= 0);
  }

  //--- processing wildcard path specs

  public static String[] expandWildcards (String[] pathNames){
    ArrayList<String> list = null;

    if (pathNames == null){
      return new String[0];
    }

    for (int i=0; i<pathNames.length; i++){
      String e = pathNames[i];

      if (containsWildcards(e)){
        if (list == null){
          list= new ArrayList<String>(pathNames.length + 20);
          for (int j=0; j<i; j++){
            list.add(pathNames[j]);
          }
        }

        for (File f : findMatches(e)){
          list.add(f.getAbsolutePath());
        }

      } else {
        if (list != null){
          list.add(e);
        }
      }
    }

    if (list != null){
      return list.toArray(new String[list.size()]);
    } else {
      return pathNames;
    }
  }


  private static List<File> splitPath (String pattern) {
    ArrayList<File> list = new ArrayList<File>();

    for (File f = new File(pattern); f != null; f = f.getParentFile()) {
      list.add(f);
    }

    Collections.reverse(list);
    return list;
  }

  private static void addSubdirs (List<File> list, File dir){
    for (File f : dir.listFiles()) {
      if (f.isDirectory()){
        list.add(f);
        addSubdirs(list, f);
      }
    }
  }

  private static List<File> findMatches (File dir, String pattern) {
    ArrayList<File> list = new ArrayList<File>();

    if (dir.isDirectory()) {
      if ("**".equals(pattern)) { // recursively add all subdirectories
        addSubdirs(list, dir);

      } else {
        StringMatcher sm = new StringMatcher(pattern);
        for (File f : dir.listFiles()) {
          if (sm.matches(f.getName())) {
            list.add(f);
          }
        }
      }
    }

    return list;
  }

  public static List<File> findMatches (String pattern) {
    List<File> pathComponents = splitPath(pattern);
    List<File> matches = null;

    for (File f : pathComponents) {
      String fname = f.getName();
      if (matches == null) { // first one
        if (fname.isEmpty()) { // filesystem root
          matches = new ArrayList<File>();
          matches.add(f);
        } else {
          matches = findMatches(new File(System.getProperty("user.dir")), fname);
        }

      } else {
        List<File> newMatches = new ArrayList<File>();
        for (File d : matches) {
          newMatches.addAll(findMatches(d, fname));
        }
        matches = newMatches;
      }

      if (matches.isEmpty()) {
        return matches;
      }
    }
    return matches;
  }


  //--- URL conversion

  public static URL getURL (String spec){
    try {
      // check if there is a protocol specification
      if (spec.indexOf("://") >= 0) {
        return new URL(spec);

      } else {
        File f = new File(spec).getCanonicalFile();
        return f.toURI().toURL();
      }
    } catch (Throwable x) {
      throw new RuntimeException("illegal pathname: " + spec);
    }
  }

  public static URL[] getURLs (String[] paths){
    ArrayList<URL> urls = new ArrayList<URL>();

    for (String p : paths) {
      urls.add( getURL(p));
    }

    return urls.toArray(new URL[urls.size()]);
  }

  public static URL[] getURLs (List<String> paths){
    ArrayList<URL> urls = new ArrayList<URL>();

    for (String p : paths) {
      urls.add( getURL(p));
    }

    return urls.toArray(new URL[urls.size()]);
  }


  //--- platform specific path conversion

  /**
   * turn a mixed path list into a valid Unix path set without drive letters,
   * and with '/' and ':' separators. Also remove multiple consecutive separators
   * this assumes the path String to be already expanded
   */
  public static String asCanonicalUnixPath (String p) {
    boolean changed = false;

    int n = p.length();
    char[] buf = new char[n];
    p.getChars(0, n, buf, 0);

    for (int i=0; i<n; i++) {
      char c = buf[i];
      if (c == '/' || c == '\\') {
        if (c == '\\'){
          buf[i] = '/'; changed = true;
        }

        // remove multiple occurrences of dir separators
        int i1 = i+1;
        if (i1 < n){
          for (c = buf[i1]; i1 < n && (c == '/' || c == '\\'); c = buf[i1]) {
            System.arraycopy(buf, i + 2, buf, i1, n - (i + 2));
            n--;
            changed = true;
          }
        }

      } else if (c == ':') {
        // strip drive letters - maybe this is trying to be too smart,
        // since we only do this for a "...:X:\..." but not a
        // "...:X:/...", which could be a valid unix path list

        // is this part of a drive letter spec?
        int i1 = i+1;
        if (i1<n) {
          if (buf[i1] == '\\') {
            if (i>0) {
              if (i == 1 || (buf[i-2] == ':')){  // strip the drive letter
                System.arraycopy(buf, i1, buf, i-1, n - (i1));
                n-=2;
                changed = true;
              }
            }
          }
        }

      } else if (c == ';'){
        buf[i] = ':'; changed = true;

      } else if (c == ',') {
        buf[i] = ':'; changed = true;
      }

      if (buf[i] == ':') {  // remove multiple occurrences of path separators
        int i1 = i+1;
        if (i1<n) {
          for (c = buf[i1] ;(c == ':' || c == ';' || c == ','); c = buf[i1]){
            System.arraycopy(buf, i+2, buf, i1, n - (i+2));
            n--;
            changed = true;
          }
        }
      }
    }

    if (changed) {
      p = new String(buf, 0, n);
    }

    return p;
  }

  /**
   * turn a mixed path list into a valid Windows path set with drive letters,
   * and '\' and ';' separators. Also remove multiple consecutive separators
   * this assumes the path String to be already expanded
   */
  public static String asCanonicalWindowsPath (String p) {
    boolean changed = false;

    int n = p.length();
    char[] buf = new char[n];
    p.getChars(0, n, buf, 0);

    for (int i=0; i<n; i++) {
      char c = buf[i];
      if (c == '/' || c == '\\') {
        if (c == '/'){
          buf[i] = '\\'; changed = true;
        }

        // remove multiple occurrences of dir separators
        int i1 = i+1;
        if (i1 < n) {
          for (c = buf[i1]; i1 < n && (c == '/' || c == '\\'); c = buf[i1]) {
            System.arraycopy(buf, i + 2, buf, i1, n - (i + 2));
            n--;
            changed = true;
          }
        }

      } else if (c == ':') {
        // is this part of a drive letter spec?
        int i1 = i+1;
        if (i1<n && (buf[i1] == '\\' || buf[i1] == '/')) {
          if (i>0) {
            if (i == 1 || (buf[i-2] == ';')){
              continue;
            }
          }
        }
        buf[i] = ';'; changed = true;

      } else if (c == ',') {
        buf[i] = ';'; changed = true;
      }

      if (buf[i] == ';') { // remove multiple occurrences of path separators
        int i1 = i+1;
        if (i1<n) {
          for (c = buf[i1] ;(c == ':' || c == ';' || c == ','); c = buf[i1]){
            System.arraycopy(buf, i+2, buf, i1, n - (i+2));
            n--;
            changed = true;
          }
        }
      }
    }

    if (changed) {
      p = new String(buf, 0, n);
    }

    return p;
  }


  public static String asPlatformPath (String p) {
    if (File.separatorChar == '/') { // Unix'ish file system
      p = asCanonicalUnixPath(p);
    } else { // Windows'ish file system
      p = asCanonicalWindowsPath(p);
    }

    return p;
  }

  public static void printFile (PrintWriter pw, File file){
    try {
      FileReader fr = new FileReader(file);
      BufferedReader r = new BufferedReader(fr);

      String line;
      while ((line = r.readLine()) != null){
        pw.println(line);
      }

      r.close();

    } catch (IOException iox){
      pw.println("!! error printing file: " + file.getPath());
    }
  }

  public static boolean removeRecursively(File file) {
    if (file.exists()) {
      File[] childs = file.listFiles();

      for (File child : childs) {
        if (child.isDirectory()){
          removeRecursively(child);
        } else {
          child.delete();
        }
      }

      return file.delete();
    }

    return false;
  }

  public static byte[] getContents( File file) throws IOException {
    if (file.isFile()){
      long length = file.length();
      byte[] data = new byte[(int)length];

      FileInputStream is = new FileInputStream(file);
      int nRead = 0;

      while (nRead < data.length) {
        int n = is.read(data, nRead, (data.length - nRead));
        if (n < 0) {
          throw new IOException("premature end of file: " + file);
        }
        nRead += n;
      }

      is.close();

      return data;
    }

    return null;
  }
  
  public static String getContentsAsString( File file) throws IOException {
    byte[] data = getContents(file);
    return new String(data);
  }
  
  public static void setContents(File file, byte[] data) throws IOException {
    FileOutputStream os = new FileOutputStream(file);
    os.write(data);
    os.close();
  }

  public static void setContents(File file, String data) throws IOException {
    FileWriter fw = new FileWriter(file);
    fw.append(data);
    fw.close();
  }
  
  
  public static String asUnixPathName (File file){
    String userHome = System.getProperty("user.home") + File.separatorChar;
    int uhLen = userHome.length();

    String pn = file.getAbsolutePath();
    if (pn.startsWith(userHome)) {
      pn = "~/" + pn.substring(uhLen).replace('\\', '/');
    } else {
      pn = pn.replace('\\', '/');
    }
    return pn;

  }
  
  public static String unixToUserPathName (String unixPathName){
    if (unixPathName.startsWith("~/")){
      return "${user.home}" + unixPathName.substring(1);
    } else {
      String userHome = System.getProperty("user.home");
      int len = userHome.length();
      if (unixPathName.startsWith(userHome) && unixPathName.charAt(len) == '/'){
        return "${user.home}" + unixPathName.substring(len);
      } else {
        return unixPathName;
      }
    }
  }
  
  public static boolean ensureDirs (File file){
    File dir = file.getParentFile();
    if (!dir.isDirectory()){
      return dir.mkdirs();
    } else {
      return true;
    }
  }
  
}
