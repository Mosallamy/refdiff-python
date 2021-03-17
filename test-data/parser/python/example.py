def sum(a, b):
    c = a + b
    print(multi(b, c))
    return c


def multi(a, b):
    c = a * b
    return c


class calculation():
    class basic():
        def sum(self, a, b):
            c = a + b
            print('addition is ', c)
            [print(self.substraction(b, a-1)) for a in range(0,5)]

            return c

        def substraction(self, a, b):
            c = a - b
            return c


temp = calculation.basic()
temp.sum(5, 6)
