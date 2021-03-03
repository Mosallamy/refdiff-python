import java.io.File;

import refdiff.core.RefDiff;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.Relationship;
import refdiff.parsers.python.PythonPlugin;

public class RefDiffExamplePythonlang {
    public static void main(String[] args) throws Exception {
        runExample();
    }

    private static void runExample() throws Exception {
        // This is a temp folder to clone or checkout git repositories.
        File tempFolder = new File("temp");

        // Creates a RefDiff instance configured with the Go plugin.
        try (PythonPlugin goPlugin = new PythonPlugin(tempFolder)) {
            RefDiff refDiffGo = new RefDiff(goPlugin);

            File repo = refDiffGo.cloneGitRepository(
                    new File(tempFolder, "python_refactoring_example"),
                    "https://github.com/QassemNa/python-refactoring-example.git");

            CstDiff diffForCommit = refDiffGo.computeDiffForCommit(repo, "4e729930e163ce375103bc00a35b516634c20bd0");
            printRefactorings("Refactorings found in go-refactoring-example 4e729930e163ce375103bc00a35b516634c20bd0", diffForCommit);
        }
    }

    private static void printRefactorings(String headLine, CstDiff diff) {
        System.out.println(headLine);
        for (Relationship rel : diff.getRefactoringRelationships()) {
            System.out.println(rel.getStandardDescription());
        }
    }
}