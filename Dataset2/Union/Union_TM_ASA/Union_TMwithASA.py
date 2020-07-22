import os
"""
This script takes the text mining and the ASA dataset in order to create a unique dataset that contains both metrics
@Param name_csv_mining: name of text mining dataset (with extension)
@Param name_csv_asa: name of ASA dataset (with extension)
@Param new_Union: file object of the destination file.

It combines the first line that contains the name of the metrics (first TM and then ASA)
Then, for each line in text mining dataset:
-Find a line in ASA dataset with the same file name (<commit>/<filename.java>) 
-if not found, it generates a combination of 0 values for the ASA line.
-combines the two resulting metrics values
-writes the combination line in the destination file
"""

def initialize(name_csv_mining, name_csv_asa, new_Union):
	cwd = os.getcwd()
	#Return to folder Union
	os.chdir("..")
	#Return to folder Dataset2
	os.chdir("..")
	os.chdir("Text_Mining")
	csv_mining = open(name_csv_mining, "r+",encoding="utf-8")
	#Return to Dataset2
	os.chdir("..")
	os.chdir("mining_results_asa")
	csv_asa = open(name_csv_asa, "r+",encoding="utf-8")
	os.chdir("..")
	os.chdir("Union/Union_TM_ASA")
	number_of_file = 0
	found=False
	flag_mining = True
	flag_asa = True
	for line_tm in csv_mining:
		found = False
		#If it's the first line of the text mining
		if(flag_mining == True):
			flag_mining = False
			line_asa = csv_asa.readline()
			#If it's the first line of the asa
			if( flag_asa == True):
				flag_asa = False
					
				withoutFirst2Argument = line_asa.split(',')
				withoutFirst2Argument = withoutFirst2Argument[1:20]
				toString = ""
				for element in withoutFirst2Argument:
					toString+= "," + element
				withoutClassInMining = line_tm[:-7]
				new_Union.write(withoutClassInMining + toString + ",class")
				new_Union.write("\n")
				
		else:
			csv_asa.seek(0,0)
			csv_asa.readline()
			file_name_tm = line_tm.split(',')[0].replace(".java_",".java")
			class_element = getClass(line_tm)
			element_doesnt_match = another_option(None, line_tm, class_element)
			for line_asa in csv_asa:
				file_name_asa = line_asa.split(',')[0].replace("\"", "")
				if(file_name_tm == file_name_asa):
					number_of_file += 1
					element_text_mining = another_option(None, line_tm, class_element)
					#Static Analysis Results
					element_ASA = another_option(line_asa, None, class_element)
					found=True
					#write the line of the new dataset
					new_Union.write(element_text_mining + element_ASA + class_element)
			if(found==False): #if the script doesn't find the corresponding line in the ASA dataset
				element_ASA ="" # insert 19 "0" values (one for each ASA attribute)
				for i in range(0,19): #
					element_ASA +="0,"
				new_Union.write(element_doesnt_match +element_ASA + class_element)
	print("The files that are read and written are :" + str(number_of_file))
	print("BUILD SUCCESS")

'''
@Param "line_asa" : line of the dataset ASA that contains all the values resulting by Static Analysis
@Param "line_tm" : line of the dataset TextMining that contains all the values resulting by Text Mining
@Param "class_element" : describe the class of the file [pos || neg]

It's possible to call this function passing only one of the two parameter (passing None on the other parameter)
IF WE PASS line_tm=None
1. then it execute a split of the ASA elements in a list
2. for each element of the resulting list, it deletes the possible "\n" characters and the count the elements
3. skip the first two elements that are the type and the name of the file.
4. It returns the concatenation of the element separated by ","

If line_asa=None
1. then it execute a split of the text mining elements in a list
2. it deletes the class element
3. for each element of the resulting list, it deletes the possible "\n" characters
4. It returns the concatenation of the element separated by ","
'''
def another_option(line_asa, line_tm, class_element):
	if(line_tm == None):
		toString = ""
		class_element = class_element.replace(" ", "")
		class_element = class_element.replace("\n", "")
		lista = line_asa.replace(" ", "").replace("\n","").split(",")
		lista.remove(class_element)
		count = 0
		for element in lista:
			elem = element.replace("\n", "")
			if(count > 0):
				toString += elem + ","
			count += 1
		return toString
	elif(line_asa == None):
		toString = ""
		lista = line_tm.split(",")
		lista.remove(class_element)
		for element in lista:
			elem = element.replace("\n", "")
			toString += elem + ","
		return toString

'''
@Param "line" : line of the dataset that contains class_element(pos || neg) 
It returns the class element of the line
'''
def getClass(line):
	lista = line.split(",")
	count = 0
	for element in lista:
		count+=1
	return lista[count-1]
	

def main():
	new_Union = open("union_TM_ASA.csv", "w")
	name_csv_mining = "csv_mining_final.csv"
	name_csv_asa = "csv_ASA_final.csv"
	initialize(name_csv_mining, name_csv_asa, new_Union)


if __name__ == '__main__':
	main()