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
		
		assertThat(root.getNodes().size(), is(4));

		assertThat(root.getNodes().get(0).getType(), is(NodeType.FILE));
		assertThat(root.getNodes().get(0).getSimpleName(), is("example.py"));
		
		assertThat(root.getNodes().get(1).getType(), is(NodeType.CLASS));
		assertThat(root.getNodes().get(1).getSimpleName(), is("test"));
		assertThat(root.getNodes().get(1).getParent().get().getSimpleName(), is("example.py"));
		
		assertThat(root.getNodes().get(2).getType(), is(NodeType.FUNCTION));
		assertThat(root.getNodes().get(2).getSimpleName(), is("sum"));
		assertThat(root.getNodes().get(1).getParent().get().getSimpleName(), is("test"));
		
		assertThat(root.getNodes().get(3).getType(), is(NodeType.FUNCTION));
		assertThat(root.getNodes().get(3).getSimpleName(), is("multi"));
		assertThat(root.getNodes().get(1).getParent().get().getSimpleName(), is("test"));
	}
	
	@Test
	public void shouldTokenizeSimpleFile() throws Exception {
		Path basePath = Paths.get("test-data/parser/python/");
		SourceFolder sources = SourceFolder.from(basePath, Paths.get("example.py"));

		CstRoot cstRoot = parser.parse(sources);
		CstNode sumFunction = cstRoot.getNodes().get(2);
		String sourceCode = sources.readContent(sources.getSourceFiles().get(0));
		
		List<String> actual = CstRootHelper.retrieveTokens(cstRoot, sourceCode, sumFunction, false);
		List<String> expected = Arrays.asList("def", "sum", "(", "a", ",", "b", ")", ":",
				"c", "=", "a", "+", "b", "return", "c");
		
		assertArrayEquals(expected.toArray(), actual.toArray());
		
		CstNode multiFunction = cstRoot.getNodes().get(3);
		actual = CstRootHelper.retrieveTokens(cstRoot, sourceCode, multiFunction, false);
		expected = Arrays.asList("def", "multi", "(", "q", ",", "w", ")", ":",
				"e", "=", "q", "*", "w", "return", "e");

		assertArrayEquals(expected.toArray(), actual.toArray());
	}
}
