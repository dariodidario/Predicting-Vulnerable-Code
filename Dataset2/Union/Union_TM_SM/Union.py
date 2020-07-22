import os
"""
This script takes the text mining and the software metrics dataset in order to 
create a unique dataset that contains both metrics
@Param name_csv_mining: name of text mining dataset (with extension)
@Param name_csv_soft_m: name of software metrics dataset (with extension)
@Param new_Union: file object of the destination file.

It combines the first line that contains the name of the metrics (first TM and then SM)
Then, for each line in text mining dataset:
-Find a line in software metrics dataset with the same file name (<commit>/<filename.java>) 
-combines the two resulting metrics values
-writes the combination line in the destination file
"""
def initialize(name_csv_mining, name_csv_soft_m, new_Union):
	cwd = os.getcwd()
	#Return to Union folder
	os.chdir("..")
	#Return to Dataset2
	os.chdir("..")
	os.chdir("Text_Mining")
	csv_mining = open(name_csv_mining, "r+")
	#Return to Dataset2
	os.chdir("..")
	os.chdir("Software_Metrics")
	csv_software_metric = open(name_csv_soft_m, "r+")
	os.chdir("..")
	#Return to Union_TM_SM
	os.chdir("Union/Union_TM_SM")
	number_of_file = 0
	

	flag_mining = True
	flag_soft_met = True
	for line_tm in csv_mining:
		#If it's the first line of the text mining
		if(flag_mining == True):
			flag_mining = False
			line_sm = csv_software_metric.readline()
			#If it's the first line of the software metrics
			if( flag_soft_met == True):
				flag_soft_met = False
					
				withoutFirst2Argument = line_sm.split(',')
				withoutFirst2Argument = withoutFirst2Argument[2:10]
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
				file_name_tm = line_tm.split(',')[0].replace("_","")
				print("Software Metric :" + file_name_sm)
				print("Text Mining :" + file_name_tm)
				if(file_name_tm == file_name_sm):
					number_of_file += 1


					class_element = getClass(line_tm)
					element_text_mining = another_option(None, line_tm, class_element)
					#SOFTWARE METRICS
					element_software_metrics = another_option(line_sm, None, class_element)

					new_Union.write(element_text_mining + element_software_metrics + class_element)
	print("The files that are read and written are :" + str(number_of_file))
	print("BUILD SUCCESS")

'''
@Param "line_sm" : line of the software metrics dataset that contains all software metrics values
@Param "line_tm" : line of the text mining dataset
@Param "class_element" : class element of a line [pos || neg]
It's possible to call this function passing only one of the two parameter (passing None on the other parameter)
IF WE PASS line_tm=None
1. then it execute a split of the software metrics elements in a list
2. for each element of the resulting list, it deletes the possible "\n" characters and the count the elements
3. skip the first two elements that are the type and the name of the file.
4. It returns the concatenation of the element separated by ","

If line_sm=None
1. then it execute a split of the text mining elements in a list
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
	new_Union = open("union_SM_TM.csv", "w")
	name_csv_mining = "csv_mining_final.csv"
	name_csv_soft_m = "mining_results_sm_final.csv"
	initialize(name_csv_mining, name_csv_soft_m, new_Union)


if __name__ == '__main__':
	main()