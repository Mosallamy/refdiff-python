class newme:
    class Nani:
        pass
    def printOwing1(self, a, b):
        # print details

        print("name:", a)
        self.print_amount1(b)
        return a

    def print_amount1(self, b):
        while b:
            print("amount", b)
            b=0
def normal(a):
    a.printOwing1("sad", 45)
    print("yolo")
a= newme
normal(a)
a.printOwing1("sad", 45)
