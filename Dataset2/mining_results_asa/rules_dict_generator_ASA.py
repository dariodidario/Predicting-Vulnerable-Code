import ast
import os
import csv

'''
This script collect all possible ASA vulnerabilty rules contained in the ASA dataset in order to create a dictionary of rules
For each element having the field "type" = "VULNERABILITY",the script collect the rule of the element in a dictionary
Finally, it writes the resulting dictionary in ASA_rules_dict.csv
'''
def main():
	dict_file_name="ASA_rules_dict.csv"
	cwd = os.getcwd()
	repo_name = "RepositoryMining_ASAResults_" #pre-name of the csv export file of SonarQube
	rules_list={} #create a dict of java rules
	for count in ["pos","neg"]:
		repo = repo_name + str(count)
		if repo != ".DS_Store": #check for excluding metadata
			for file in os.listdir():
				if file != ".DS_Store":
					if repo in file:
						read_txt=open(file,"r")	
						first_line=True
						for line in read_txt:
							if(first_line):
								#skip first line
								first_line=False
							else:
								value_list=line.split("\t")
								if(value_list[10]=="VULNERABILITY"):
									rules_list[value_list[5]]=0
				else:
					print(".DS_Store occured")
						
	dict_file=open(dict_file_name,"w+")
	dict_file.write(str(rules_list))
	dict_file.close()
	print("BUILD SUCCESS !")
	count = 0
	for key in rules_list:
		count+=1
	print("Ci sono :" + str(count) + " chiavi(che) !")

if __name__ == '__main__':
	main()
