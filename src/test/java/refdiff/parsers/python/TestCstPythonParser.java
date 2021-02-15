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
		
		assertThat(root.getNodes().size(), is(3));

		assertThat(root.getNodes().get(0).getType(), is(NodeType.FILE));
		assertThat(root.getNodes().get(0).getSimpleName(), is("example.py"));
	}	
}
