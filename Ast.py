import ast
import json
import asttokens
import astroid

jsonData = list()

r = open('example.py','r')
file = ast.parse(r.read())

class astWalk(ast.NodeVisitor): # tarvrse all the nodes in the ast tree
    def visit_Module(self,node): # get the names of the functions and variables that are defined in a module or a file
        self.names = []
        self.functions = []
        self.generic_visit(node) # itrate over a node and all if its children
        #print('variables: ',self.names)
        #print('functions: ',self.functions)
    def visit_Name(self, node):
        self.names.append(node.id)
    def visit_FunctionDef(self, node):
        function_name = [node.name]
        arguments = [argument.arg for argument in node.args.args]
        tempList = list()
        tempList.append(node.name)
        tempList.append(arguments)
        self.functions.append(tempList)

        jsonData.append({
            "type": "Function",
            "name": function_name[0],
            "parameters": arguments,
            "line": node.lineno,
        })

        #print('Start_line: ',node.lineno, 'Last_line: ',node.end_lineno)
        #print('Start_col ', node.col_offset, 'Last_col: ',node.end_col_offset)

astWalk().visit(file)

r = open('example.py')
file = r.read()

atok = asttokens.ASTTokens(file, parse=True)


def get_tokenz(node): #pass a node and it will return the tokens
    tokens = []
    if isinstance(node, ast.Module):
        isStart = True
        line = 0
        a = atok.get_tokens(node,include_extra=True)
        for i in a:
            if isStart:
                line = i.start[0]
                isStart = False
            tokens.append([i.startpos,i.endpos])
        jsonData.append({
            "type" : "File",
            "start" : tokens[0][0],
            "end": tokens[-1][1],
            "line": line,
            "name": "",
            "parent": "null",
            "tokens": [t for t in tokens]
        })

    else:
        a = atok.get_tokens(node,include_extra=True)
        isStart = True
        startToken = endToken = line = 0
        tokens.append(node.name)
        for i in a:
            if isStart:
                startToken = i.startpos
                line = i.start[0]
                isStart = False
            endToken = i.endpos
        tokens.append(startToken)
        tokens.append(endToken)
        tokens.append(line)
        for i in jsonData:
            if i["name"] == node.name:
                i["start"] = tokens[1]
                i["end"] = tokens[2]

functions = [n for n in ast.walk(atok.tree) if isinstance(n, ast.Module)]
[get_tokenz(a) for a in functions]

functions = [n for n in ast.walk(atok.tree) if isinstance(n, ast.FunctionDef)]
[get_tokenz(a) for a in functions]

with open("example.py","r") as file:
    code = file.read()

parsedCode = astroid.parse(code)

def getFunctionParent(node):
    stack = list()
    stack.append(node)
    functionNodes = dict()
    while(stack):
        currentNode = stack.pop()
        for child in currentNode.get_children():
            stack.append(child)
            if isinstance(child,astroid.FunctionDef):
                functionNodes[child.name] = child.parent.name
    return functionNodes

funtionParent = getFunctionParent(parsedCode)

for i in jsonData:
    for fun, parent in funtionParent.items():
        if i["name"] == fun:
            i["parent"] = parent

print("[")
for i in jsonData:
    print(" {")
    for key,value in i.items():
        print("     ",key, ":", value)
    print(" }")
print("]")