import java.io.File;
import java.util.ArrayList;

import refdiff.core.RefDiff;
import refdiff.core.cst.TokenPosition;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.Relationship;
import refdiff.core.io.GitHelper;
import refdiff.parsers.python.PythonPlugin;

public class RefDiffExamplePythonlang {
    public static void main(String[] args) throws Exception {
        runExample();
    }

    private static void runExample() throws Exception {
        // This is a temp folder to clone or checkout git repositories.
        File tempFolder = new File("temp");

        // Creates a RefDiff instance configured with the Python plugin.
        try (PythonPlugin pythonPlugin = new PythonPlugin(tempFolder)) {

            RefDiff refDiffPython = new RefDiff(pythonPlugin);
            
            //remove the comments above to find refactorings from all the commits in repo, then comment the below code
            File repo = refDiffPython.cloneGitRepository(new File(tempFolder, "refactoring-python-example.git"), "https://github.com/rodrigo-brito/refactoring-python-example.git");
 
            refDiffPython.computeDiffForCommitHistory(repo, 1000000, (rev, diff) -> {
    			printRefactorings(String.format("Refactorings from %s", rev), diff);
    		});
        }
    }

    private static void printRefactorings(String headLine, CstDiff diff) {
        System.out.println(headLine);
        for (Relationship rel : diff.getRefactoringRelationships()) {
            System.out.println(rel.getStandardDescription());
        }
    }
}
