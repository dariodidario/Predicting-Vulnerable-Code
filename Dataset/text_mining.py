import re

def stringTokenizer(s):
	#Take the string constant values
    strings_values= re.findall(r'\"(.+?)\"', s)
    #Take the comments
    comments= re.findall(r'\/\/(.*)', s)
    #Put the comments and the string values in a single list
    discard_list= strings_values+comments
    tokens = re.findall(r"[\w']+|[^\w\s']", s)
    #split the discard_list in words
    for string in discard_list:
        word_vector=string.split(" ")
        #remove each word in tokens
        for word in word_vector:
           if word in tokens:
              tokens.remove(word)
    withoutAlpha = removeNotAlpha(tokens)
    return withoutAlpha

def removeComments(java_file):
	text=java_file.read()
	print(text)
	text=re.sub(r'\/\*(.|\n)*?\*\/',' ',text)
	print(text)
	return text

def takeJavaClass():
	final = []
	with open("file_try.java", "r") as java_file:
		text=removeComments(java_file)
		for line in text.splitlines():
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