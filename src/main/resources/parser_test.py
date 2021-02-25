#!/usr/bin/env python3

import sys, os
import ast
import json
import asttokens
import astroid
import argparse

jsonData = list()

parser = argparse.ArgumentParser(description='RefDiff parser for Python programming language')
parser.add_argument('-f', '--file', help='input file with path', type=str, required=True)
parser.add_argument('-p', '--pretty', help='output json in pretty format', action='store_true')
args = parser.parse_args()


def get_tokenz(node):  # pass a node and it will return the tokens
    tokens = []
    if isinstance(node, ast.Module):
        isStart = True
        line = 0
        a = atok.get_tokens(node, include_extra=True)
        for i in a:
            if isStart:
                line = i.start[0]
                isStart = False
            tokens.append("{}-{}".format(i.startpos, i.endpos))
        jsonData.append({
            "type": "File",
            "name": file_name[1],
            "line": line,
            "namespace": path[1],
            "parent": "null",
            "start": tokens[0].split('-')[0],
            "end": tokens[-1].split('-')[1],
            "tokens": [t for t in tokens]
        })
    else:
        a = atok.get_tokens(node, include_extra=True)
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


def get_all_functions(node):
    stack = list()
    stack.append(node)
    callsList = list()
    while (stack):
        currentNode = stack.pop()
        for child in currentNode.get_children():
            stack.append(child)
            if isinstance(child, astroid.FunctionDef):
                callsList.append(child.name)
    return callsList


def get_real_parent(node):  # this returns weather the nodes parent is a function or a class or a module
    try:
        while 1:
            parent = node.parent
            if isinstance(parent, astroid.FunctionDef):
                return parent
            if isinstance(parent, astroid.ClassDef):
                return parent
            if isinstance(parent, astroid.Module):
                return parent

            node = parent
    except:
        pass


def getFunctionParent(node):
    stack = list()
    stack.append(node)
    functionNodes = dict()
    functions = get_all_functions(node)
    while stack:
        currentNode = stack.pop()
        for child in currentNode.get_children():
            stack.append(child)
            has_body = "True"
            try:
                if child.body:
                    # Remove comments if you consider a function with just pass had no body
                    # if isinstance(child.body[0], astroid.node_classes.Pass):
                    #    has_body="False"
                    pass
            except:
                has_body = "False"
            if isinstance(child, astroid.FunctionDef):
                functionNodes[child.name] = child.parent.name
                jsonData.append({
                    "type": "Function",
                    "name": child.name,
                    "parameters": [a.name for a in child.args.args],
                    "parent": child.parent.name,
                    "namespace": path[1],
                    "calls": list(),
                    "line": child.lineno,
                    "has_body": has_body
                })
            if isinstance(child, astroid.ClassDef):
                jsonData.append({
                    "type": "Class",
                    "name": child.name,
                    "parameters": 'None',
                    "parent": child.parent.name,
                    "namespace": path[1],
                    "line": child.lineno,
                    "has_body": has_body
                })

            if isinstance(child, astroid.Call):
                try:
                    if child.func.name in functions:
                        parent = get_real_parent(child)
                        if parent.is_function:
                            for i in jsonData:
                                if i['name'] == parent.name:
                                    a = i['calls']
                                    i['calls'] = a + [child.func.name]
                except:
                    pass
    return functionNodes


file_name = os.path.split(args.file)  # file_name[0] = path to file name, file_name[1] = file name
path = os.path.split(file_name[0])  # path[1] = namespace

with open(args.file, "r") as file:
    code = file.read()

parsedCode = astroid.parse(code, file_name[1])
getFunctionParent(parsedCode)

atok = asttokens.ASTTokens(code, parse=True)

[get_tokenz(n) for n in ast.walk(atok.tree) if isinstance(n, ast.Module)]  # gets tokens for module
[get_tokenz(n) for n in ast.walk(atok.tree) if isinstance(n, ast.FunctionDef)]  # gets tokens for functions
[get_tokenz(n) for n in ast.walk(atok.tree) if isinstance(n, ast.ClassDef)]  # gets tokens for functions

ident = 2 if args.pretty else None
print(json.dumps(jsonData, indent=ident))
