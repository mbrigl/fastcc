
package org.javacc;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@link JavaCCBuilder2} class.
 */
public class JavaCCBuilder2 {

  public enum Language {

    Java("Java"),
    Cpp("C++");

    public final String name;

    Language(String name) {
      this.name = name;
    }

  }

  private Language language;
  private File     outputDirectory;

  private File     jj;
  private File     jjt;

  /**
   * Set the code generator.
   *
   * @param language
   */
  public final JavaCCBuilder2 setCodeGenerator(Language language) {
    this.language = language;
    return this;
  }

  /**
   * Set the output directory.
   *
   * @param outputDirectory
   */
  public final JavaCCBuilder2 setOutputDirectory(File outputDirectory, String... pathes) {
    this.outputDirectory = JavaCCBuilder2.toFile(outputDirectory, pathes);
    return this;
  }

  /**
   * Set the jj file.
   *
   * @param file
   */
  public final JavaCCBuilder2 setJJFile(File file, String... pathes) {
    this.jj = JavaCCBuilder2.toFile(file, pathes);
    return this;
  }

  /**
   * Set the jj file.
   *
   * @param file
   */
  public final JavaCCBuilder2 setJJTreeFile(File file, String... pathes) {
    this.jjt = JavaCCBuilder2.toFile(file, pathes);
    return this;
  }

  public static JavaCCBuilder2 of(Language language) {
    JavaCCBuilder2 builder = new JavaCCBuilder2();
    builder.setCodeGenerator(language);
    return builder;
  }

  /**
   * Run the parser generator.
   */
  public final void build() {
    try {
      List<String> arguments = new ArrayList<>();
      arguments.add("-CODE_GENERATOR=" + this.language.name);
      arguments.add("-OUTPUT_DIRECTORY=" + this.outputDirectory.getAbsolutePath());
      if (this.jjt != null) {
        arguments.add(this.jjt.getAbsolutePath());
        String path = this.jjt.getAbsolutePath();

        JJTree.main(arguments.toArray(new String[arguments.size()]));

        int offset = path.lastIndexOf("/");
        int length = path.lastIndexOf(".");
        arguments.set(arguments.size() - 1, this.outputDirectory + path.substring(offset, length) + ".jj");
      } else {
        arguments.add(this.jj.getAbsolutePath());
      }

      JJParser.main(arguments.toArray(new String[arguments.size()]));
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private static File toFile(File file, String... pathes) {
    return (pathes.length == 0) ? file : new File(file, String.join(File.separator, pathes));
  }

  public static final File USER = new File(System.getProperty("user.dir"));
  public static final File BASE = new File("src/main/").getAbsoluteFile();

  /**
   * {@link #main}.
   *
   * @param args
   */
  public static void main(String[] args) {
    JavaCCBuilder2 builderJJ = JavaCCBuilder2.of(Language.Java);
    builderJJ.setOutputDirectory(JavaCCBuilder2.BASE, "generated/org/javacc/parser");
    builderJJ.setJJFile(JavaCCBuilder2.BASE, "javacc/JavaCC.jj");
    builderJJ.build();

    JavaCCBuilder2 builderJJT = JavaCCBuilder2.of(Language.Java);
    builderJJT.setOutputDirectory(JavaCCBuilder2.BASE, "generated/org/javacc/jjtree");
    builderJJT.setJJTreeFile(JavaCCBuilder2.BASE, "jjtree/JJTree.jjt");
    builderJJT.build();
  }
}
