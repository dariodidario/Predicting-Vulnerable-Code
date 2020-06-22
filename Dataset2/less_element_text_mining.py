import ast
import os
import re 

def main():
	dizionario_finale = initialize()
	print("I'm writing into a new File, please wait..")
	writeToFile(dizionario_finale)
	print("BUILD SUCCESS!")


def initialize():
	dizionario_finale = {}
	dict_file_name = "text_mining_dict.txt"
	cwd = os.getcwd()
	os.chdir(cwd + "/mining_results")
	for file in os.listdir():
		if dict_file_name in file:
			read_txt = open(dict_file_name, "r")
			diction = ast.literal_eval(read_txt.read())
			return splitCamelCase(diction)
		else:
			print("File doesn't exist, sorry :(")
	#fake_dic = {'requestUri': 1, 'getRequestUri': 2, 'request': 3, 'eAncheOggiGilbertoNonHaFattoNienteUri': 5}
			

#1. Prende in input il dizionario ottenuto dal mining totale delle repository
#2. ne effettua lo split secondo la regex scritta sotto
#3. inserisce ogni parola che non sia già presente nel dizionario, altrimenti modifica solo il valore.
#4. effettua un lower case di tutte le parole
#5. restituisce il dizioario.
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


def writeToFile(dizionario_finale):
	newFile = open("newTextMining.txt", "w+")
	newFile.write(str(dizionario_finale))
	newFile.close()

if __name__ == '__main__':
	main()