
package org.fastcc;


import org.fastcc.FastCCBuilder.Language;
import org.javacc.JJParser;
import org.javacc.JJTree;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@link FastCCBuilder2} class.
 */
public class FastCCBuilder2 {

  private Language language;
  private File     outputDirectory;

  private File     jj;
  private File     jjt;

  /**
   * Set the code generator.
   *
   * @param language
   */
  public final FastCCBuilder2 setCodeGenerator(Language language) {
    this.language = language;
    return this;
  }

  /**
   * Set the output directory.
   *
   * @param outputDirectory
   */
  public final FastCCBuilder2 setOutputDirectory(File outputDirectory, String... pathes) {
    this.outputDirectory = FastCCBuilder2.toFile(outputDirectory, pathes);
    return this;
  }

  /**
   * Set the jj file.
   *
   * @param file
   */
  public final FastCCBuilder2 setJJFile(File file, String... pathes) {
    this.jj = FastCCBuilder2.toFile(file, pathes);
    return this;
  }

  /**
   * Set the jj file.
   *
   * @param file
   */
  public final FastCCBuilder2 setJJTreeFile(File file, String... pathes) {
    this.jjt = FastCCBuilder2.toFile(file, pathes);
    return this;
  }

  public static FastCCBuilder2 of(Language language) {
    FastCCBuilder2 builder = new FastCCBuilder2();
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
    FastCCBuilder2 builderJJ = FastCCBuilder2.of(Language.Java);
    builderJJ.setOutputDirectory(FastCCBuilder2.BASE, "generated/org/javacc/parser");
    builderJJ.setJJFile(FastCCBuilder2.BASE, "javacc/JavaCC.jj");
    builderJJ.build();

    FastCCBuilder2 builderJJT = FastCCBuilder2.of(Language.Java);
    builderJJT.setOutputDirectory(FastCCBuilder2.BASE, "generated/org/javacc/jjtree");
    builderJJT.setJJTreeFile(FastCCBuilder2.BASE, "jjtree/JJTree.jjt");
    builderJJT.build();
  }
}
