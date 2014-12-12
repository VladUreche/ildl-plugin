package ildl.infrastructure

import org.junit.Test
import java.io.{ File => JFile }
import scala.tools.nsc.io._
import java.io.FileNotFoundException
import difflib._
import java.io.PrintWriter
import difflib.myers._

/* Taken from: [[https://github.com/nicolasstucki/specialized/commit/f7ee90610d0052cb3607cef138051575db3c2eb9]] */
class TestSuite {

  private[this] def files(dirs: List[String], ext: String) = {
    val cwd = sys.props.get("user.dir").getOrElse(".")
    val res = dirs.foldLeft(Path(new JFile(cwd)))((path, dir) => path / dir)
    System.err.println("Picking tests from: " + res)
    res.jfile.listFiles().filter(_.getName().endsWith(ext)).sortBy(_.getName())
  }

  private[this] def replaceExtension(source: JFile, ext: String) =
    new JFile(source.toString.replaceAll("\\.scala", "." + ext))

  private[this] def slurp(source: JFile) =
    try {
      File(source).slurp
    } catch {
      case _: FileNotFoundException => ""
    }

  // TODO: This needs to be general, it's currently a mess
  private[this] def pluginCompilerFlag() =
    try {
      "-Xplugin:" + files(List("..", "..", "components", "plugin", "target", "scala-2.11"), ".jar").head.toString
    } catch {
      case x: NoSuchElementException =>
        sys.error("The plugin jar is not available! Run \"sbt ildl-plugin/package\" to generate it.")
    }

  @Test def testCompileOutput() = {
    var failed = false
    var totalTests = 0
    var failedTests = 0
    val pluginFlag = pluginCompilerFlag()
    var UPDATE_CHECKFILE = false
    // use carefully:
//    UPDATE_CHECKFILE = true

    val tests = files(List("resources", "tests"), ".scala").toList

    for (source <- tests) {
      System.err.print(f"Compiling ${source.getName()}%-60s ... ")

      totalTests += 1

      // source code:
      val code = File(source).slurp
      val flags = pluginFlag + " " + slurp(replaceExtension(source, "flags"))
      val check_file = replaceExtension(source, "check")
      val launch_file = replaceExtension(source, "launch")
      val expect = slurp(check_file)
      val launch = slurp(launch_file).replace("\n","")
      val output = new CompileTest(code, flags, launch).compilationOutput()
      import scala.collection.JavaConversions._
      def stripTrailingWS(s: String) = s.replaceAll("\\s*$","")
      val output_lines = seqAsJavaList(output.split("\n").toList.map(stripTrailingWS)).filter(_ != "")
      val expect_lines = seqAsJavaList(expect.split("\n").toList.map(stripTrailingWS)).filter(_ != "")
      val generator = new DiffRowGenerator.Builder().showInlineDiffs(true).ignoreWhiteSpaces(true).columnWidth(100).build()
      val differ = new MyersDiff(new Equalizer[String] {
        def equals(a: String, b: String): Boolean = {
            val _a = a.trim.replaceAll("\\s+", " ")
            val _b = b.trim.replaceAll("\\s+", " ")
            return _a.equals(_b)
        }
      })
      val sdiff = DiffUtils.diff(expect_lines, output_lines, differ)
      val udiff = DiffUtils.generateUnifiedDiff("output", "expected", expect_lines, sdiff, 2)

      if (sdiff.getDeltas().size() != 0) {
        if (UPDATE_CHECKFILE) {
          System.err.println("[ UP ] " + "\n" + check_file + "\n")
          Some(new PrintWriter(check_file)).foreach{p => p.write(output_lines.mkString("\n")); p.close}
        } else
          System.err.println("[FAIL]")
          //System.err.println("\nDifference in test for: " + source)
          System.err.println("Diff: ")
          for (line <- udiff)
            System.err.println(line)
            //System.err.println("\nCompiler output:\n" + output)
            //System.err.println("\nExpected output:\n" + expect)
          System.err.println("\n\n")
          failed = true
          failedTests += 1
      } else
        System.err.println("[ OK ]")
    }

    if (failedTests != 0)
      System.err.println(s"\n  $totalTests tests ran, $failedTests ${if (UPDATE_CHECKFILE) "updated" else "failed"} :(\n")
    else
      System.err.println(s"\n  $totalTests tests ran, all good :)\n")

    assert(!UPDATE_CHECKFILE && !failed, "Some tests failed. (or UPDATE_CHECKFILE is on)")
  }
}
