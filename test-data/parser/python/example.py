from calculator import Calculator

def isEven(value):
	return value % 2 == 0

def printIsEven(value):
	if isEven(value):
		print("even")
	else:
		print("odd")
calculator = Calculator()
print("1 + 2 = ", calculator.summary(1, 2))
print("2 x 2 = ", calculator.multiply(2, 2))
print("2 is = ",)
printIsEven(2)
print("3 is = ",)
printIsEven(3)