package refdiff.parsers.python;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import refdiff.core.cst.*;
import refdiff.core.io.FilePathFilter;
import refdiff.core.io.SourceFile;
import refdiff.core.io.SourceFileSet;
import refdiff.parsers.LanguagePlugin;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PythonPlugin implements LanguagePlugin, Closeable {
	private File tempDir = null;
	private String parserPath;
	private String envpath;

	public PythonPlugin() throws Exception {
		this.parserPath = "C:\\Users\\Asper\\Projects\\Git\\RefDiff\\refdiff-python\\src\\main\\resources\\parser.py";
		this.envpath = "C:\\Users\\Asper\\Projects\\Git\\RefDiff\\refdiff-python\\venv\\Scripts\\python.exe";
	}

	public PythonPlugin(File tempDir) {
		this.tempDir = tempDir;
		this.parserPath = "C:\\Users\\Asper\\Projects\\Git\\RefDiff\\refdiff-python\\src\\main\\resources\\parser.py";
		this.envpath = "C:\\Users\\Asper\\Projects\\Git\\RefDiff\\refdiff-python\\venv\\Scripts\\python.exe";
	}

	public String getParserPath() {
		String parser = System.getenv("REFDIFF_PYTHON_PARSER");
		if (parser != null && !parser.isEmpty()) {
			return parser;
		}

		return PythonPlugin.class.getClassLoader().getResource("parser.py").getPath(); // TODO load dependencies in Java
	}

	public Node[] execParser(String rootFolder, String path) throws IOException {
		ProcessBuilder builder = new ProcessBuilder(envpath, parserPath,"-f", Paths.get(rootFolder, path).toString());
		Process proc = builder.start();
		Node[] nodes = new Node[0];
		try {
			ObjectMapper mapper = new ObjectMapper();
			nodes = mapper.readValue(proc.getInputStream(), Node[].class);
		} catch (Exception e) {
			String errors = new BufferedReader(new InputStreamReader(proc.getErrorStream()))
					.lines().collect(Collectors.joining("\n"));
			if (errors.length() > 0) {
				throw new RuntimeException(errors);
			}
			e.printStackTrace();
		}

		return nodes;
	}

	private void updateChildrenNodes(CstRoot root, Map<String, CstNode> nodeByAddress, Map<String, CstNode> fallbackByAddress,
									 Map<String, HashSet<String>> childrenByAddress) {

		for (Map.Entry<String, HashSet<String>> parent : childrenByAddress.entrySet()) {
			if (!nodeByAddress.containsKey(parent.getKey()) ) {

				// check nodes in fallbacks and add to root
				CstNode fallbackNode = fallbackByAddress.get(parent.getKey());
				if (fallbackNode == null) {
					continue;
				}
				nodeByAddress.put(parent.getKey(), fallbackNode);
				root.addNode(fallbackNode);
			}

			CstNode parentNode = nodeByAddress.get(parent.getKey());
			for (String childAddress: parent.getValue()) {
				if (!nodeByAddress.containsKey(childAddress)) {
					throw new RuntimeException("node not found: " + childAddress);
				}
				parentNode.addNode(nodeByAddress.get(childAddress));
			}
		}
	}

	private void updateFunctionCalls(CstRoot root, Map<String, CstNode> nodeByAddress, Map<String, CstNode> fallbackByAddress,
									 Map<String, HashSet<String>> functionCalls) {

		for (Map.Entry<String, HashSet<String>> node : functionCalls.entrySet()) {
			if (!nodeByAddress.containsKey(node.getKey())) {
				throw new RuntimeException("node not found: " + node.getKey());
			}

			CstNode caller = nodeByAddress.get(node.getKey());
			for (String functionCall: node.getValue()) {
				if (!nodeByAddress.containsKey(functionCall)) {
					CstNode fallbackNode = fallbackByAddress.get(functionCall);
					if (fallbackNode == null) {
						continue;
					}
					nodeByAddress.put(functionCall, fallbackNode);
					root.addNode(fallbackNode);
				}
				root.getRelationships().add(new CstNodeRelationship(CstNodeRelationshipType.USE, caller.getId(),
						nodeByAddress.get(functionCall).getId()));
			}
		}
	}

	private TokenizedSource tokenizeSourceFile(Node node, SourceFileSet sources, SourceFile sourceFile) throws IOException {
		ArrayList<TokenPosition> tokens = node.getTokenPositions(sources.readContent(sourceFile));
		return new TokenizedSource(sourceFile.getPath(), tokens);
	}

	private boolean isValidPythonFile(String path) {
		return path.endsWith(".py");
	}

	@Override
	public CstRoot parse(SourceFileSet sources) throws Exception {
		Optional<Path> optBasePath = sources.getBasePath();
		Map<String, CstNode> nodeByAddress = new HashMap<>();
		Map<String, CstNode> fallbackByAddress = new HashMap<>();
		Map<String, HashSet<String>> childrenByAddress = new HashMap<>();
		Map<String, HashSet<String>> functionCalls = new HashMap<>();
		List<SourceFile> additionalFiles = new ArrayList<>();
		List<SourceFile> sourceFiles = new ArrayList<>();

		for (SourceFile sourceFile : sources.getSourceFiles()) {
			if (!isValidPythonFile(sourceFile.getPath())) {
				continue;
			}

			sourceFiles.add(sourceFile);
			File parent = new File(sourceFile.getPath()).getParentFile();

			String sourceFolder = "";
			if (parent != null) {
				sourceFolder = parent.getPath();
				//System.out.println(sourceFolder);
				for (SourceFile file : sources.getFilesFromPath(Paths.get(sourceFolder), this.tempDir)) {
					if (!isValidPythonFile(file.getPath())) {
						continue;
					}

					additionalFiles.add(file);
				}
			}
		}

		sources.getSourceFiles().addAll(additionalFiles);

		if (!optBasePath.isPresent()) {
			if (this.tempDir == null) {
				throw new RuntimeException("The GoParser requires a SourceFileSet that is materialized on the file system. " +
						"Either pass a tempDir to GoParser's contructor or call SourceFileSet::materializeAt before calling this method.");
			} else {
				sources.materializeAtBase(tempDir.toPath());
				optBasePath = sources.getBasePath();
			}
		}

		File rootFolder = optBasePath.get().toFile();

		try {
			CstRoot root = new CstRoot();
			Map<String, Boolean> fileProcessed = new HashMap<>();
			int nodeCounter = 1;

			for (SourceFile sourceFile : sourceFiles) {
				String temp = Paths.get(rootFolder.toString(),sourceFile.getPath()).toString();
				String temp1 = temp.substring(temp.indexOf("\\")+1);
				//System.out.println("1: "+temp1);
				String[] arrOfStr = temp1.split("-");
				temp1 = arrOfStr[0]+"\\";
				//System.out.println("2: "+temp1);
				fileProcessed.put(sourceFile.getPath(), true);

				Node[] astNodes = this.execParser(rootFolder.toString(), sourceFile.getPath());
				for (Node node : astNodes) {
					node.setId(nodeCounter++);

					if (node.getType().equals(NodeType.FILE)) {
						root.addTokenizedFile(tokenizeSourceFile(node, sources, sourceFile));
					}

					CstNode cstNode = toCSTNode(node, sourceFile.getPath());
					// save parent information
					nodeByAddress.put(node.getAddress(), cstNode);
					if (node.getParent() != null) {
						// initialize if key not present
						if (!childrenByAddress.containsKey(node.getParent())) {
							childrenByAddress.put(node.getParent(), new HashSet<>());
						}

						childrenByAddress.get(node.getParent()).add(node.getAddress());
					}

					// save call graph information
					if (node.getType().equals(NodeType.FUNCTION) && node.getFunctionCalls() != null) {
						// initialize if key not present
						if (!functionCalls.containsKey(node.getAddress())) {
							functionCalls.put(node.getAddress(), new HashSet<>());
						}
						functionCalls.get(node.getAddress()).addAll(node.getFunctionCalls());
					}
					if (node.getType().equals(NodeType.FILE)) {
						root.addNode(cstNode);
					}

				}
			}

			for (SourceFile sourceFile: additionalFiles) {
				if (fileProcessed.getOrDefault(sourceFile.getPath(), false)) { // avoid duplicate parser
					continue;
				}

				fileProcessed.put(sourceFile.getPath(), true);
				Node[] astNodes = this.execParser(rootFolder.toString(), sourceFile.getPath());
				for (Node node : astNodes) {
					node.setId(nodeCounter++);

					if (node.getType().equals(NodeType.FILE)) {
						root.addTokenizedFile(tokenizeSourceFile(node, sources, sourceFile));
					}

					CstNode cstNode = toCSTNode(node, sourceFile.getPath());
					fallbackByAddress.put(node.getAddress(), cstNode);
				}
			}

			updateChildrenNodes(root, nodeByAddress, fallbackByAddress, childrenByAddress);
			updateFunctionCalls(root, nodeByAddress, fallbackByAddress, functionCalls);
			return root;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, HashSet<String>> AddtoArraylist(Map<String, HashSet<String>> arraylist, String temp1, Node node){
		if (!arraylist.containsKey(node.getAddress())) {
				arraylist.put(node.getAddress(), new HashSet<>());
		}
		arraylist.get(node.getAddress()).add(node.getAddress());
		return arraylist;
	}

	private CstNode toCSTNode(Node node, String filePath) {
		CstNode cstNode = new CstNode(node.getId());
		cstNode.setType(node.getType());
		cstNode.setSimpleName(node.getName());
		cstNode.setNamespace(node.getNamespace());
		cstNode.setLocation(new Location(filePath, node.getStart(), node.getEnd(), node.getLine(), node.getBodyBegin(), node.getBodyEnd()));

		if (node.getType().equals(NodeType.CLASS)) {
			cstNode.getStereotypes().add(Stereotype.TYPE_MEMBER);
		}

		if (node.hasBody()) {
			cstNode.getStereotypes().add(Stereotype.HAS_BODY);
		} else if(node.getType().equals(NodeType.FUNCTION) || node.getType().equals(NodeType.CLASS))  {
			cstNode.getStereotypes().add(Stereotype.ABSTRACT);
		}

		if (node.getParametersNames() != null && !node.getParametersNames().isEmpty()) {
			List<Parameter> parameters = new ArrayList<>();
			for (String name : node.getParametersNames()) {
				parameters.add(new Parameter(name));
			}
			cstNode.setParameters(parameters);
		}

		if (node.getType().equals(NodeType.FUNCTION)) {
			String localName = String.format("%s(%s)", node.getName(), String.join(",", node.getParameterTypes()));
			if (node.getReceiver() != null && !node.getReceiver().isEmpty()) {
				localName = String.format("%s.%s", node.getReceiver(), localName);
			}
			cstNode.setLocalName(localName);
		} else {
			cstNode.setLocalName(node.getName());
		}

		return cstNode;
	}

	@Override
	public FilePathFilter getAllowedFilesFilter() {
		List<String> ignoreFiles = Arrays.asList("");
		return new FilePathFilter(Arrays.asList(".py"));
	}


	@Override
	public void close() {}
}