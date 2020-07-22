import os

"""
This script takes the combination dataset of Text Mining(TM) and Static Analysis(ASA) and the Software Metrics (SM) dataset
in order to create a unique dataset that contains all these three metrics
@Param name_csv_mining: name of combination text mining-StaticAnalysis dataset (with extension)
@Param name_csv_soft_m: name of SM dataset (with extension)
@Param new_Union: file object of the destination file.

It combines the first line that contains the name of the metrics (first TM-ASA and then SM)
Then, for each line in TM-ASA dataset:
-Find a line in Software Metrics dataset with the same file name (<commit>/<filename.java>) 
-combines the two resulting metrics values
-writes the combination line in the destination file
"""

def initialize(name_csv_mining, name_csv_soft_m, new_Union):
	cwd = os.getcwd()
	#Return to Union folder
	os.chdir("..")
	os.chdir("Union_TM_ASA")
	csv_mining = open(name_csv_mining, "r+")
	#Return to Union
	os.chdir("..")
	#Return to Dataset2
	os.chdir("..")
	os.chdir("Software_Metrics")
	csv_software_metric = open(name_csv_soft_m, "r+")
	os.chdir("..")
	#Return to Total_Combination
	os.chdir("Union/Total_Combination")
	number_of_file = 0
	flag_mining = True
	flag_soft_met = True
	for line_tm in csv_mining:
		#if it's the first line of the TM-ASA dataset
		if(flag_mining == True):
			flag_mining = False
			line_sm = csv_software_metric.readline()
			#If it's the first line of the SM dataset
			if( flag_soft_met == True):
				flag_soft_met = False
					
				withoutFirst2Argument = line_sm.split(',')
				withoutFirst2Argument = withoutFirst2Argument[2:11]
				toString = ""
				for element in withoutFirst2Argument:
					toString+= "," + element
				withoutClassInMining = line_tm[:-7]
				new_Union.write(withoutClassInMining + toString + ",class")
				new_Union.write("\n")
				
		else:
			csv_software_metric.seek(0,0)
			csv_software_metric.readline()
			for line_sm in csv_software_metric:
				file_name_sm = line_sm.split(',')[1].replace("\"", "")
				file_name_tm = line_tm.split(',')[0].replace(".java_",".java")
				if(file_name_tm == file_name_sm):
					number_of_file += 1
					class_element = getClass(line_tm)
					element_text_mining = another_option(None, line_tm, class_element)
					element_software_metrics = another_option(line_sm, None, class_element)

					new_Union.write(element_text_mining + element_software_metrics + class_element)

	print("The files that are read and written are :" + str(number_of_file))
	print("BUILD SUCCESS")

'''
@Param "line_sm" : line of the dataset Software Metrics that contains all the values resulting by Software Metrics
@Param "line_tm" : line of the dataset TextMining-Static Analysis that contains all the values resulting by TEXT MINING-STATIC ANALYSIS
@Param "class_element" : describe the class of the file [pos || neg]

It's possible to call this function passing only one of the two parameter (passing None on the other parameter)

IF WE PASS line_tm=None
1. then it execute a split of the SM elements in a list
2. for each element of the resulting list, it deletes the possible "\n" characters and the count the elements
3. skip the first two elements that are the type and the name of the file.
4. It returns the concatenation of the element separated by ","

If line_sm=None
1. then it execute a split of the TM-ASA elements in a list
2. it deletes the class element
3. for each element of the resulting list, it deletes the possible "\n" characters
4. It returns the concatenation of the element separated by ","
'''
def another_option(line_sm, line_tm, class_element):
	if(line_tm == None):
		toString = ""
		lista = line_sm.split(",")
		count = 0
		for element in lista:
			elem = element.replace("\n", "")
			if(count > 1):
				toString += elem + ","
			count += 1
		return toString
	elif(line_sm == None):
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
	new_Union = open("3COMBINATION.csv", "w")
	name_csv_mining = "Union_TM_ASA.csv"
	name_csv_soft_m = "mining_results_sm_final.csv"
	initialize(name_csv_mining, name_csv_soft_m, new_Union)


if __name__ == '__main__':
	main()