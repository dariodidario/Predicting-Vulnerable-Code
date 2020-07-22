import ast
import os
import re 
'''
This script takes every single word inside the dictionary and perform the CamelCaseSplitting.
'''

def main():
	dizionario_finale = initialize()
	print("I'm writing into a new File, please wait..")
	writeToFile(dizionario_finale)
	print("BUILD SUCCESS!")


def initialize():
	dizionario_finale = {}
	dict_file_name = "text_mining_dict.txt"
	os.chdir("..")
	cwd = os.getcwd()
	os.chdir(cwd + "/mining_results")
	for file in os.listdir():
		if dict_file_name in file:
			read_txt = open(dict_file_name, "r")
			diction = ast.literal_eval(read_txt.read())
			return splitCamelCase(diction)
		else:
			print("File doesn't exist, sorry :(")
			
'''
1. Takes as input the dictionary obtained from the total repository mining
2. split it according to the regex written below
3. inserts any word that is not already present in the dictionary, otherwise it changes only the value.
4. perform a lower case of all words
5. returns the dictionary.
'''
def splitCamelCase(fake_dic):
	regexForCCSplit ='.+?(?:(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|$)'
	dic1 = {}
	for key in fake_dic:
		matches = re.finditer(regexForCCSplit, key)
		#Per ogni split in ottenuto dalla regex
		for m in matches:
			#se lo split è già nel dizionario allora aggiorna solo il valore, altrimenti inseriscilo
			if m.group(0).lower() in dic1:
				dic1[m.group(0).lower()] += fake_dic[key]
			else:
				dic1[m.group(0).lower()] = fake_dic[key]
	return dic1

'''
@Param dizionario_finale: dictionary
Write the dict into the destination file.
'''
def writeToFile(dizionario_finale):
	newFile = open("FilteredTextMining.txt", "w+")
	newFile.write(str(dizionario_finale))
	newFile.close()

if __name__ == '__main__':
	main()