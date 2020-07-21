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
	text=re.sub(r'\/\*(.|\n)*?\*\/',' ',text)
	return text

def takeJavaClass(java_file_name):
	final = []
	current = []
	dic ={}
	with open(java_file_name, "r") as java_file:
		text=removeComments(java_file)
		for line in text.splitlines():
			current = stringTokenizer(line)
			final+=current
			for eachElem in current:
				if eachElem in dic:
					dic[eachElem] += 1
				else:
					dic[eachElem] = 1
		return dic
def removeNotAlpha(tokens):
	rightOne = []
	for element in tokens:
		if(element.isalpha()):
			rightOne.append(element)
	return rightOne

def main():
	java_file_name="file_try.java"	
	dic = takeJavaClass(java_file_name)
	file = open(java_file_name+"_text_mining.txt","w+")
	file.write(str(dic))

if __name__ == '__main__':
	main()