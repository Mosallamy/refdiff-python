import ast
import json
import asttokens
import astroid


jsonData = list()


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
                jsonData.append({
                    "type": "Function",
                    "parent": child.parent.name,
                    "name": child.name,
                    "parameters": [a.name for a in child.args.args],
                    "line": child.lineno
                })
    return functionNodes



with open("example.py","r") as file:
    code = file.read()

parsedCode = astroid.parse(code)
funtionParent = getFunctionParent(parsedCode)


atok = asttokens.ASTTokens(code, parse=True)

[get_tokenz(n) for n in ast.walk(atok.tree) if isinstance(n, ast.Module)]#gets tokens for module
[get_tokenz(n) for n in ast.walk(atok.tree) if isinstance(n, ast.FunctionDef)]#gets tokens for functions

print("[")
for i in jsonData:
    print(" {")
    for key,value in i.items():
        print("     ",key, ":", value)
    print(" }")
print("]")