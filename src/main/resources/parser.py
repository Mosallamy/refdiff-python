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
        a = atok.get_tokens(node, include_extra=False)
        hasbody= False
        for i in a:
            if isStart:
                line = i.start[0]
                isStart = False
            if i.string.strip() and i.string != "\n": # not empty token
                tokens.append("{}-{}".format(i.startpos, i.endpos))
        if int(tokens[-1].split('-')[1])>0:
            hasbody =True
        jsonData.append({
            "type": "File",
            "name": file_name[1],
            "function_calls": list(),
            "line": line,
            "namespace": '',
            "parent": None,
            "start": tokens[0].split('-')[0],
            "end": tokens[-1].split('-')[1],
            "tokens": tokens,
            "bodyBegin" : int(tokens[0].split('-')[0]),
            "bodyEnd" : int(tokens[-1].split('-')[1]),
            "has_body": hasbody
        })
    else:
        a = atok.get_tokens(node, include_extra=False)
        isStart = True
        startToken = endToken = line = 0
        tokens.append(node.name)
        bodyBegin = 0
        for i in a:
            if (i.string == '(' or i.string == ':') and bodyBegin == 0:
                bodyBegin = i.startpos
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
                i["bodyBegin"] = bodyBegin
                i["bodyEnd"] = tokens[2]


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
                callsList.append(child)
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
        return False


def addFunction2JSON(node, key, value):
    try:
        parent = get_real_parent(node)
        if parent:
            for i in jsonData:
                if i['name'] == parent.name:
                    a = i[key]
                    i[key] = a + value
    except:
        pass


def addSlash(text1, text2):
    try:
        string = ""
        if not text1.startswith('/'):
            text1= '/{0}'.format(text1)
        if (text2.startswith('/')) or (text1.endswith('/')):
            string = text1 +""+ text2
            return string
        else:
            if text2=="":
                string = text1
            else:
                string = text1+'/'+text2
            return string
    except:
        pass

def searchnamespace(node, namespace =""):
    try:
        parent = node.parent
        if isinstance(parent, astroid.Module):
            namespace = addSlash(file_name[1],namespace)
            return namespace

        if isinstance(parent, astroid.ClassDef):
            node = parent
            namespace = addSlash(node.name, namespace)
            namespace = searchnamespace(node, namespace)
            return namespace

    except:
        return False


def getFunctionParent(node):
    stack = list()
    stack.append(node)
    functionNodes = dict()
    functions = get_all_functions(node)
    while stack:
        currentNode = stack.pop()
        for child in currentNode.get_children():
            stack.append(child)
            has_body = True
            try:
                if child.body:
                    # Remove comments if you consider a function with just pass had no body
                    # if isinstance(child.body[0], astroid.node_classes.Pass):
                    #    has_body="False"
                    pass
            except:
                has_body = False
            if isinstance(child, astroid.FunctionDef):
                functionNodes[child.name] = child.parent.name
                jsonData.append({
                    "type": "Function",
                    "name": child.name,
                    "parameter_names": [a.name for a in child.args.args],
                    "parent": searchnamespace(child),
                    "namespace": searchnamespace(child),
                    "function_calls": list(),
                    "line": child.lineno,
                    "has_body": has_body
                })
            if isinstance(child, astroid.ClassDef):
                jsonData.append({
                    "type": "Class",
                    "name": child.name,
                    "parameter_names": [None],
                    "parent": searchnamespace(child),
                    "namespace": searchnamespace(child),
                    "line": child.lineno,
                    "has_body": has_body
                })

            if isinstance(child, astroid.Call):
                try:
                    name = ""
                    if child.func._other_fields[0] == "attrname":
                        name = child.func.attrname
                    elif child.func._other_fields[0] == "name":
                        name = child.func.name
                    if name in functions:
                        index = functions.index(name)
                        index = functions[index+1]
                        parent = get_real_parent(child)
                        if isinstance(parent, astroid.FunctionDef) or isinstance(parent, astroid.Module):

                            for i in jsonData:
                                temp = addSlash(searchnamespace(index),name)
                                if i['name'] == parent.name and (temp not in i['function_calls']):
                                    a = i['function_calls']
                                    i['function_calls'] = a + [temp]
                except:
                    pass
    return functionNodes


file_name = os.path.split(args.file)  # file_name[0] = path to file name, file_name[1] = file name
path = os.path.split(file_name[0])  # path[1] = namespace
with open(args.file, "r") as file:
    code = file.read()

atok = asttokens.ASTTokens(code, parse=True)
[get_tokenz(n) for n in ast.walk(atok.tree) if isinstance(n, ast.Module)]  # gets tokens for module

parsedCode = astroid.parse(code, file_name[1])
getFunctionParent(parsedCode)

[get_tokenz(n) for n in ast.walk(atok.tree) if isinstance(n, ast.FunctionDef) or isinstance(n, ast.ClassDef)]  # gets tokens for functions

for i in jsonData:
    if i['type'] == "Function" or i['type'] == "File":
        if not i['function_calls']:
            i['function_calls'] = None

ident = 2 if args.pretty else None
print(json.dumps(jsonData, indent=ident))