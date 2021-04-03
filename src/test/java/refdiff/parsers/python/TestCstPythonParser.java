package refdiff.parsers.python;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.diff.CstRootHelper;
import refdiff.core.io.SourceFolder;
import refdiff.parsers.python.NodeType;
import refdiff.parsers.python.PythonPlugin;
import refdiff.test.util.PythonParserSingleton;

public class TestCstPythonParser {
	
	private PythonPlugin parser = PythonParserSingleton.get();
	
	@Test
	public void shouldMatchNodes() throws Exception {
		Path basePath = Paths.get("test-data/parser/python/");
		SourceFolder sources = SourceFolder.from(basePath, Paths.get("example.py"));
		CstRoot root = parser.parse(sources);
		
		assertThat(root.getNodes().size(), is(1));

		CstNode fileNode = root.getNodes().get(0);
		assertThat(fileNode.getType(), is(NodeType.FILE));
		assertThat(fileNode.getSimpleName(), is("example.py"));
		assertThat(fileNode.getLocalName(), is("example.py"));
		assertThat(fileNode.getNodes().size(), is(1));

		CstNode classNode = fileNode.getNodes().get(0);
		assertThat(classNode.getType(), is(NodeType.CLASS));
		assertThat(classNode.getSimpleName(), is("test"));
		assertThat(classNode.getParent().get().getSimpleName(), is("example.py"));
		assertThat(classNode.getNodes().size(), is(2));

		CstNode sumFunctionNode = classNode.getNodes().get(0);
		assertThat(sumFunctionNode.getType(), is(NodeType.FUNCTION));
		assertThat(sumFunctionNode.getSimpleName(), is("sum"));
		assertThat(sumFunctionNode.getParent(), is(classNode));

		CstNode multiFunctionNode = classNode.getNodes().get(1);
		assertThat(multiFunctionNode.getType(), is(NodeType.FUNCTION));
		assertThat(multiFunctionNode.getSimpleName(), is("multi"));
		assertThat(multiFunctionNode.getParent(), is(classNode));
	}
	
	@Test
	public void shouldTokenizeSimpleFile() throws Exception {
		Path basePath = Paths.get("test-data/parser/python/");
		SourceFolder sources = SourceFolder.from(basePath, Paths.get("example.py"));

		CstRoot cstRoot = parser.parse(sources);
		CstNode sumFunction = cstRoot.getNodes().get(0).getNodes().get(0).getNodes().get(0);
		String sourceCode = sources.readContent(sources.getSourceFiles().get(0));
		
		List<String> actual = CstRootHelper.retrieveTokens(cstRoot, sourceCode, sumFunction, false);
		List<String> expected = Arrays.asList("def", "sum", "(", "a", ",", "b", ")", ":",
				"c", "=", "a", "+", "b", "return", "c");
		
		assertArrayEquals(expected.toArray(), actual.toArray());
		
		CstNode multiFunction = cstRoot.getNodes().get(0).getNodes().get(0).getNodes().get(1);
		actual = CstRootHelper.retrieveTokens(cstRoot, sourceCode, multiFunction, false);
		expected = Arrays.asList("def", "multi", "(", "q", ",", "w", ")", ":",
				"e", "=", "q", "*", "w", "return", "e");

		assertArrayEquals(expected.toArray(), actual.toArray());
	}
}
