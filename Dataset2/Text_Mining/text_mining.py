import re
import os
'''
This script takes every java class and return a dictionary of words in order to 
perform the Text Mining.
'''


'''
@Param s: Input String.
Delete the constant value inside the string line and return a vector of unigrams.
'''
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
'''
@Param java_file: file object 
Delete the different type of comments inside the "java_file".
'''
def removeComments(java_file):
	text=java_file.read()
	text=re.sub(r'\/\*(.|\n)*?\*\/',' ',text)
	return text
'''
@Param java_file_name: name of the file.
Execute the Tokenization deleting the comments, strings and no-Alpha characters.
'''
def takeJavaClass(java_file_name):
	final = []
	current = []
	dic ={}
	with open(java_file_name, "r",) as java_file:
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

'''
@Param tokens: list of words.
Return the list of words without no-Alpha characters.
'''
def removeNotAlpha(tokens):
	rightOne = []
	for element in tokens:
		if(element.isalpha()):
			rightOne.append(element)
	return rightOne

def main():
	java_file_name=""
	os.chdir("..")
	cwd = os.getcwd()
	repo_name = "RepositoryMining"
	os.chdir(cwd+"/mining_results")
	for count in range(2,36,1):
		repo = repo_name + str(count)
		if repo != ".DS_Store":
			os.chdir(repo)
			for cvd_id in os.listdir():
				if cvd_id not in [".DS_Store", "CHECK.txt", "ERRORS.txt"]:
					print(cvd_id)
					os.chdir(cvd_id)
					print(os.listdir())
					for folder in os.listdir():
						if folder != ".DS_Store":
							os.chdir(folder)
							for file in os.listdir():
								if file != ".DS_Store":
									java_file_name = file
									dic = takeJavaClass(java_file_name)
									file = open(java_file_name+"_text_mining.txt","w+")
									file.write(str(dic))
									file.close()
								else:
									print(".DS_Store occured")
							os.chdir("..")
					os.chdir("..")
			os.chdir("..")

if __name__ == '__main__':
	main()