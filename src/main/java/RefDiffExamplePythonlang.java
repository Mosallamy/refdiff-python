import java.io.File;
import java.util.ArrayList;

import refdiff.core.RefDiff;
import refdiff.core.cst.TokenPosition;
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


            ArrayList<File> files= new ArrayList<>();
            files.add(refDiffGo.cloneGitRepository(
                    new File(tempFolder, "test8"),
                    "https://github.com/QassemNa/python-refactoring-example.git"));

            for(File repo : files) {
                System.out.println("Starting a new Repo"+repo.getName());
                refDiffGo.computeDiffForCommitHistory(repo, 10, (commit, diff) -> {
                    printRefactorings("Refactorings found in Refactoring example " + commit.getId().name(), diff);
                });


            }

            /*
            File repo = refDiffGo.cloneGitRepository(
                    new File(tempFolder, "test5"),
                    "https://github.com/QassemNa/python-refactoring-example.git");//https://github.com/pallets/flask.git
            CstDiff diffForCommit = refDiffGo.computeDiffForCommit(repo, "2c06acfd81973ce88e4d0a2618ee22d60665e130");//with files 3dfc12e8d82a66c4041783fcaec58a7a24dbe348/
            printRefactorings("Refactorings found in Python-refactoring-example 2f9137d5c06681ec885fb44a553e426c171bdd57", diffForCommit);
            */

        }
    }

    private static void printRefactorings(String headLine, CstDiff diff) {
        System.out.println(headLine);
        for (Relationship rel : diff.getRefactoringRelationships()) {
            System.out.println(rel.getStandardDescription());
        }
    }
}