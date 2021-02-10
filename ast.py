import ast
r = open('example.py','r')
t = ast.parse(r.read())
import asttokens

class allnames(ast.NodeVisitor):
    def visit_Module(self,node):
        self.names = []
        self.functions = []
        self.generic_visit(node)
        print('variables: ',self.names)
        print('functions: ',self.functions)
    def visit_Name(self, node):
        self.names.append(node.id)
    def visit_FunctionDef(self, node):
        function_name = [node.name]
        arguments = [argument.arg for argument in node.args.args]
        test = list()
        #test.append("Funtion")
        test.append(arguments)
        test.append(node.name)
        self.functions.append(test)
        #print(node)
        print('Start_line: ',node.lineno, 'Last_line: ',node.end_lineno)
        #print('Start_col ', node.col_offset, 'Last_col: ',node.end_col_offset)
        #get_tokenz(node)
    def visit_Store(self, node):
        self.generic_visit(node)
    def testPrint(self):
        pass
allnames().visit(t)
