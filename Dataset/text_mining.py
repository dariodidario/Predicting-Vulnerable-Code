import re

def stringTokenizer(s):
    tokens = re.findall(r"[\w']+|[^\w\s']", s)
    withoutAlpha = removeNotAlpha(tokens)
    return withoutAlpha

def takeJavaClass():
	final = []
	with open("file_try.java", "r") as java_file:
		for line in java_file:
			final += stringTokenizer(line)
		print(final)
def removeNotAlpha(tokens):
	rightOne = []
	for element in tokens:
		if(element.isalpha()):
			rightOne.append(element)
	return rightOne
def main():
	takeJavaClass()

if __name__ == '__main__':
	main()